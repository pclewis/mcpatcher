import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class MCPatcher {
	public static final String VERSION = "1.1.12";

	public static PrintStream out;
	public static PrintStream err;

	public static Params globalParams = new Params();
	public static JFrame logWindow;
	private static MainForm mainForm;

	public static void main(String[] argv) throws Exception {
		try {
			initLogWindow();
			out.println("Starting MCPatcher v" + VERSION);

			mainForm = MainForm.create();
			findMinecraft();

			mainForm.show();
		} catch (Throwable ex) {
			panic(ex);
		}
    }

	public static void panic(Throwable ex) {
		ex.printStackTrace(MCPatcher.err);
		MCPatcher.logWindow.setVisible(true);
		JOptionPane.showMessageDialog(null,
			"MCPatcher has encountered an error and cannot continue.\n"+
			"Please copy the messages from the log window if you need to report this problem.\n" +
			"You must restart MCPatcher before using it again.\n" +
			"\n" +
			"If your minecraft.jar has been corrupted, you can re-download the original minecraft.jar \n" +
			"by deleting your minecraft/bin folder and running the game normally.\n" +
			"\n" +
			"Please see log window for more details.",
			"Error", JOptionPane.ERROR_MESSAGE);
	}

	private static void findMinecraft() throws Exception {
		String appdata = System.getenv("APPDATA");
		String home = System.getProperty("user.home");
		String[] dirs = new String[] {
			".",
			(appdata==null?home:appdata) + "/.minecraft/bin",
			home + "/Library/Application Support/minecraft/bin",
			home + "/.minecraft/bin",
			home + "/minecraft/bin",
		};

		// Convert existing minecraft.original.jar to minecraft-<version>.jar
		for(String dir : dirs) {
			File f = new File(dir + "/minecraft.original.jar");
			if(f.exists()) {
				String version = Minecraft.extractVersion(f);
				if(version != null) {
					File newFile = new File(dir + "/minecraft-" + version + ".jar");
					if (f.renameTo(newFile)) {
						MCPatcher.out.println("renamed " + f.getPath() + " to " + newFile.getPath());
					}
				}
			}
		}

		// Find minecraft.jar
		// Extract its version and look for minecraft-<version>.jar
		// If found, use that as the input file instead
		// Else assume minecraft.jar is an unpatched original
		for(String dir : dirs) {
			File f = new File(dir + "/minecraft.jar");
			if(f.exists()) {
				MCPatcher.out.println("found " + f.getPath());
				String version = Minecraft.extractVersion(f);
				if(version != null) {
					File newFile = new File(dir + "/minecraft-" + version + ".jar");
					if (newFile.exists()) {
						if (mainForm.setMinecraftPath(newFile.getPath(), false)) {
							break;
						}
					}
				}
				if (mainForm.setMinecraftPath(f.getPath(), false)) {
					break;
				}
			}
		}
	}

	private static void initLogWindow() {
		logWindow = new JFrame("Log");
		final JTextArea ta = new JTextArea(20,50);
		((DefaultCaret)ta.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		//JPanel panel = new JPanel(new GridLayout());
		logWindow.add(new JScrollPane(ta), BorderLayout.CENTER);
		JButton button = new JButton("Copy to Clipboard");
		button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
					new StringSelection(ta.getText()), null
				);
			}
		});
		//panel.add(button);
		logWindow.add(button, BorderLayout.SOUTH);
		logWindow.pack();

		out = new JTextAreaPrintStream(ta);
		err = out;
	}

	public static void applyPatch(Minecraft minecraft, TexturePack texturePack, File outputFile) {

	    JarOutputStream newjar = null;

	    ArrayList<PatchSet> patches = new ArrayList<PatchSet>();
	    ArrayList<String> replaceFiles = new ArrayList<String>();

		out.println("Texture pack: " + texturePack.getPath());
		out.println("Minecraft version: " + minecraft.getVersion());

	    getPatches(patches, replaceFiles);

		try {
			newjar = new JarOutputStream(new FileOutputStream(outputFile));
		} catch(IOException e) {
			e.printStackTrace(MCPatcher.err);
			return;
		}

        if (newjar==null) {
            return;
        }

		javassist.bytecode.MethodInfo.doPreverify = true;

		try {
            List<String> customAnimations = new ArrayList<String>(Arrays.asList("custom_water_still.png",
                    "custom_water_flowing.png", "custom_lava_still.png", "custom_lava_flowing.png",
					"custom_portal.png"));
			int totalFiles = minecraft.getJar().size();
			int procFiles = 0;
			for(JarEntry entry : Collections.list(minecraft.getJar().entries())) {
				String name = entry.getName();
				String deobfName = name;
				for(String s : replaceFiles) {
					String t = mainForm.minecraft.getClassMap().get(s.replaceFirst("\\.class$", ""));
					if(name.equals(t)) {
						deobfName = s;
						break;
					}
				}

				procFiles += 1;
				mainForm.updateProgress(procFiles, totalFiles);
				if(name.startsWith("META-INF"))
					continue; // leave out manifest
				if(replaceFiles.contains(deobfName)) {
					replaceFiles.remove(deobfName);
					replaceFile(name, deobfName, newjar);
					continue;
				}

				newjar.putNextEntry(new ZipEntry(entry.getName()));
				if(entry.isDirectory()) {
					newjar.closeEntry();
					continue;
				}

				InputStream input = null;

				if(name.endsWith(".png")) {
					input = texturePack.getInputStream(name);
                    if(customAnimations.contains(name)) {
                        customAnimations.remove(name);
                    }
                } else {
					input = minecraft.getJar().getInputStream(entry);
                }

				boolean patched = false;

				if (name.endsWith(".class")) {
					patched = applyPatches(name, input, minecraft, patches, newjar);
				} else if(name.equals("gui/items.png") || name.equals("terrain.png")) {
					patched = resizeImage(name, 16, input, newjar);
					if(!patched) { // can't rewind, so reopen
						input = texturePack.getInputStream(entry.getName());
					}
                } else if(name.equals("misc/dial.png")) {
                    patched = resizeImage(name, 1, input, newjar);
                    if(!patched) { // can't rewind, so reopen
                        input = texturePack.getInputStream(entry.getName());
                    }
                }

				if(!patched) {
					Util.copyStream(input, newjar);
				}

				newjar.closeEntry();
			}

            // Add custom animations if present
            for(String f : customAnimations) {
                InputStream is;
                try {
                    is = texturePack.getInputStream(f);
                } catch (Exception ex) {
                    continue;
                }

                if(is!=null) {
                    MCPatcher.out.println("Adding file: " + f);
                    newjar.putNextEntry(new ZipEntry(f));
                    Util.copyStream(is, newjar);
                    newjar.closeEntry();
                }
            }

			// Add files in replaceFiles list that weren't encountered in src
			for(String f : replaceFiles) {
				replaceFile(f, f, newjar);
			}

			newjar.close();
		} catch(Exception e) {
			panic(e);
			try { if(newjar != null) newjar.close(); } catch(IOException e1) { e1.printStackTrace(MCPatcher.err); }
			try {
				Util.copyFile(new File(minecraft.getPath()), outputFile);
				MCPatcher.out.println("Restored " + outputFile.getPath() + " due to previous error");
			} catch(IOException e1) {
				MCPatcher.out.println("Unable to restore " + outputFile.getPath());
				e1.printStackTrace(MCPatcher.err);
			}
			return;
		}

		MCPatcher.out.println("\n\n#### Success! ...probably ####");
	}

	private static InputStream openResource(String name) throws FileNotFoundException {
		InputStream is = MCPatcher.class.getResourceAsStream("/newcode/" + name);
		if(is==null) {
			is = MCPatcher.class.getResourceAsStream(name);
		}
		return is;
	}

	private static void replaceFile(String name, String srcName, JarOutputStream newjar) throws IOException {
		if(name.equals(srcName)) {
			MCPatcher.out.println("Replacing " + name);
		} else {
			MCPatcher.out.println("Replacing " + name + " (" + srcName + ")");
		}
		newjar.putNextEntry(new ZipEntry(name));
		InputStream is = openResource(srcName);
		if(is == null)
			throw new FileNotFoundException("newcode/" + name);

		// Previously, we simply copied replacement class files verbatim from the newcode directory into
		// the new minecraft.jar.  This generally required a new mcpatcher release with each update of
		// Minecraft.
		//
		// Instead, we use the classes in newcode as templates and edit them with the proper Minecraft
		// references on the fly.  The newcode classes always use the long class field names.  In newcode,
		// there are also AnimTexture and net.minecraft.client.Minecraft classes, which are not actually
		// injected into the patched minecraft.jar but are simply stubs to get everything to build properly.
		//
		// Here we map long names to the short names in the currently selected Minecraft.jar.
		if(name.endsWith(".class")) {
			String className = name.replace(".class", "");
			String srcClassName = srcName.replace(".class", "");
			String animName = mainForm.minecraft.getClassMap().get("AnimTexture");
			if(animName == null) {
				animName = "AnimTexture";
			} else {
				animName = animName.replace(".class", "");
			}

			byte[] buffer = new byte[is.available()];
			is.read(buffer, 0, is.available());

			if(!className.equals(srcClassName)) {
				buffer = replaceClassString(buffer, srcClassName, className);
				buffer = replaceClassString(buffer, srcClassName + ";", className + ";");
				buffer = replaceClassString(buffer, srcClassName + ".java", className + ".java");
			}
			if(animName != null) {
				buffer = replaceClassString(buffer, "AnimTexture", animName);
			}

			// NOTE: These field mappings in the AnimTexture class are still hard-coded, which means
			// a new mcpatcher release may still be required occasionally.
			//
			// However, it is not a problem if the whole class is simply renamed (e.g., ae -> af), only
			// if the class internal structure changes significantly.
			buffer = replaceClassString(buffer, "render", "a");
			buffer = replaceClassString(buffer, "outBuf", "a");
			buffer = replaceClassString(buffer, "tile", "b");
			buffer = replaceClassString(buffer, "flow", "e");

			newjar.write(buffer, 0, buffer.length);
		} else {
			Util.copyStream(is, newjar);
		}

		newjar.closeEntry();

		for(int i = 1; true; ++i) {
			String src = (srcName.replace(".class", "$"+i+".class"));
			String dest = (name.replace(".class", "$"+i+".class"));
			is = openResource(src);
			if(is==null)
				break;
			MCPatcher.out.println("Adding " + dest);
			newjar.putNextEntry(new ZipEntry(dest));
			Util.copyStream(is, newjar);
			newjar.closeEntry();
		}

	}

	private static byte[] replaceClassString(byte[] code, String from, String to) throws IOException {
		byte[] a = getClassStringBytes(from);
		byte[] b = getClassStringBytes(to);
		int n = a.length;
		int m = b.length;
		byte t[] = new byte[n];

		if(n == 0 || m == 0) {
			return code.clone();
		}
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		for(int offset = 0; offset < code.length; ) {
			boolean match = false;
			if(offset + n < code.length) {
				match = true;
				for(int i = 0; match && i < n; i++) {
					match = (code[offset+i] == a[i]);
				}
			}
			if(match) {
				MCPatcher.out.println(String.format("  Replace string \"%s\" -> \"%s\" @%d", from, to, offset));
				os.write(b);
				offset += n;
			} else {
				os.write(code[offset]);
				offset++;
			}
		}

		return os.toByteArray();
	}

	private static byte[] getClassStringBytes(String code) throws IOException {
		byte a[] = code.getBytes();
		int len = a.length;
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		os.write((byte)(len / 256));
		os.write((byte)(len % 256));
		os.write(a);

		return os.toByteArray();
	}

	private static void getPatches(ArrayList<PatchSet> patches, ArrayList<String> replaceFiles) {
		PatchSet waterPatches = new PatchSet(Patches.water);
		boolean useCustomAnimation = false;
		if (globalParams.getBoolean("useCustomWater")) {
			for(PatchSpec ps : Patches.customWaterMC.getPatchSpecs()) {
				Patch p = ps.getPatch();
				if(p instanceof Patches.PassThisPatch) {
					((Patches.PassThisPatch)p).setClassMap(mainForm.minecraft.getClassMap());
				}
			}
		    patches.add(new PatchSet("Minecraft", new PatchSet(Patches.customWaterMC)));
			replaceFiles.add("StillWater.class");
			replaceFiles.add("FlowWater.class");
			useCustomAnimation = true;
		} else {
			if(!globalParams.getBoolean("useAnimatedWater")) {
				waterPatches.setParam("tileSize", "0");
				patches.add( new PatchSet(Patches.hideWater) );
			}
			patches.add(new PatchSet("FlowWater", waterPatches));
			patches.add(new PatchSet("StillWater", waterPatches));
		}

		PatchSet lavaPatches = new PatchSet(Patches.water);
		if(globalParams.getBoolean("useCustomLava")) {
			for(PatchSpec ps : Patches.customLavaMC.getPatchSpecs()) {
				Patch p = ps.getPatch();
				if(p instanceof Patches.PassThisPatch) {
					((Patches.PassThisPatch)p).setClassMap(mainForm.minecraft.getClassMap());
				}
			}
			patches.add(new PatchSet("Minecraft", new PatchSet(Patches.customLavaMC)));
			replaceFiles.add("StillLava.class");
			replaceFiles.add("FlowLava.class");
			useCustomAnimation = true;
			//lavaPatches.setParam("tileSize", "0");
			//patches.add(new PatchSet("StillLava", lavaPatches));
			//patches.add( new PatchSet(Patches.hideStillLava) );
		} else {
			if(!globalParams.getBoolean("useAnimatedLava")) {
				lavaPatches.setParam("tileSize", "0");
				patches.add( new PatchSet(Patches.hideLava) );
			}
			patches.add(new PatchSet("FlowLava", lavaPatches));
			patches.add(new PatchSet("StillLava", lavaPatches));
		}

		if(globalParams.getBoolean("useCustomPortal")) {
			for(PatchSpec ps : Patches.customPortalMC.getPatchSpecs()) {
				Patch p = ps.getPatch();
				if(p instanceof Patches.PassThisPatch) {
					((Patches.PassThisPatch)p).setClassMap(mainForm.minecraft.getClassMap());
				}
			}
			patches.add(new PatchSet("Minecraft", new PatchSet(Patches.customPortalMC)));
			replaceFiles.add("Portal.class");
			useCustomAnimation = true;
		}

		if (useCustomAnimation) {
			replaceFiles.add("CustomAnimation.class");
		}

		PatchSet firePatches = new PatchSet(Patches.fire);
		if(!globalParams.getBoolean("useAnimatedFire")) {
			firePatches.setParam("tileSize", "0");
			patches.add( new PatchSet(Patches.hideFire) );
		}

		if(globalParams.getInt("tileSize") > 16) {
            patches.add(new PatchSet(Patches.animManager));
            patches.add(new PatchSet(Patches.animTexture));            
		    patches.add(new PatchSet("Fire",firePatches));
		    patches.add(new PatchSet(Patches.compass));
			patches.add(new PatchSet(Patches.tool3d));
            patches.add(new PatchSet(Patches.watch));
			if(!globalParams.getBoolean("useCustomPortal")) {
				patches.add(new PatchSet(Patches.portal));
			}
		}

		if(globalParams.getBoolean("useBetterGrass")) {
			patches.add(new PatchSet(Patches.betterGrass));
			replaceFiles.add("BetterGrass.class");
		}

		if(globalParams.getBoolean("useHiResFont")) {
			patches.add(new PatchSet(Patches.font));
		}
	}

	private static boolean resizeImage(String name, int numTiles, InputStream input, JarOutputStream newjar) throws IOException {
		boolean patched = false;
		int size = globalParams.getInt("tileSize") * numTiles;
		MCPatcher.out.println("Reading " + name + "...");
		BufferedImage image = ImageIO.read(input);

		if(image.getWidth() != size || image.getHeight() != size) {
			MCPatcher.out.println("Resizing " + name + " from " + image.getWidth() + "x" +
				image.getHeight() + " to " + size + "x" + size);

			BufferedImage newImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics2D = newImage.createGraphics();
			graphics2D.drawImage(image, 0, 0, size, size, null);

			// Write the scaled image to the outputstream
			ImageIO.write(newImage, "PNG", newjar);
			patched = true;
		}
		return patched;
	}

	private static boolean applyPatches(String name, InputStream input, Minecraft minecraft, ArrayList<PatchSet> patches, JarOutputStream newjar) throws Exception {
		Boolean patched = false;
		ClassFile cf = null;
		ConstPool cp = null;
		for(PatchSet patch : patches) {
			if(name.equals(minecraft.getClassMap().get(patch.getClassName()))) {
				if(cf == null) {
					MCPatcher.out.println("Patching class: " + patch.getClassName() + " (" + name + ")");
					cf = new ClassFile(new DataInputStream(input));
					cp = cf.getConstPool();
				}
				patch.visitConstPool(cp);
				for(Object mo : cf.getMethods()) {
					patch.visitMethod((MethodInfo)mo);
				}
			}
		}
		if(cf != null) {
			cf.compact();
			cf.write(new DataOutputStream(newjar));
			patched = true;
		}
		return patched;
	}
}
