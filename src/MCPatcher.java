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
	public static PrintStream out;
	public static PrintStream err;

	public static Params globalParams = new Params();
	public static JFrame logWindow;
	private static MainForm mainForm;

	public static void main(String[] argv) throws Exception {
		initLogWindow();

		mainForm = MainForm.create();
		findMinecraft();

        mainForm.show();
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
                    "custom_water_flowing.png", "custom_lava_still.png", "custom_lava_flowing.png"));
			int totalFiles = minecraft.getJar().size();
			int procFiles = 0;
			for(JarEntry entry : Collections.list(minecraft.getJar().entries())) {
				String name = entry.getName();

				procFiles += 1;
				mainForm.updateProgress(procFiles, totalFiles);
				if(name.startsWith("META-INF"))
					continue; // leave out manifest

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

				if(replaceFiles.contains(name)) {
					replaceFiles.remove(name);
					replaceFile(name, newjar);
					patched = true;
				} else if (name.endsWith(".class")) {
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
				newjar.putNextEntry(new ZipEntry(f));
				replaceFile(f, newjar);
				newjar.closeEntry();
			}

			newjar.close();
		} catch(Exception e) {
			e.printStackTrace(MCPatcher.err);
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

	public static boolean hasNewcode(String name) {
		InputStream is = null;
		boolean found = false;
		try {
			is = MCPatcher.class.getResourceAsStream("/newcode/" + name);
			if(is != null) {
				found = true;
			}
		} catch(Exception e) {
		} finally {
			try { is.close(); } catch(Exception e) { }
		}
		return found;
	}

	private static void replaceFile(String name, JarOutputStream newjar) throws IOException {
		MCPatcher.out.println("Replacing " + name);
		InputStream is = openResource(name);
		if(is == null)
			throw new FileNotFoundException("newcode/" + name);

		Util.copyStream(is, newjar);

		for(int i = 1; true; ++i) {
			String nn = (name.replace(".class", "$"+i+".class"));
			is = openResource(nn);
			if(is==null)
				break;
			MCPatcher.out.println("Adding " + nn);
			newjar.closeEntry();
			newjar.putNextEntry(new ZipEntry(nn));
			Util.copyStream(is,newjar);
		}

	}

	private static void getPatches(ArrayList<PatchSet> patches, ArrayList<String> replaceFiles) {
		PatchSet waterPatches = new PatchSet(Patches.water);
		if (globalParams.getBoolean("useCustomWater")) {
		    patches.add(new PatchSet("Minecraft", new PatchSet(Patches.customWaterMC)));
			replaceFiles.add("iz.class");
			replaceFiles.add("oe.class");
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
			patches.add(new PatchSet("Minecraft", new PatchSet(Patches.customLavaMC)));
			replaceFiles.add("ba.class");
			replaceFiles.add("ez.class");
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

		if (globalParams.getBoolean("useCustomWater")||
			globalParams.getBoolean("useCustomLava")) {
			replaceFiles.add("WaterAnimation.class");
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
            patches.add(new PatchSet(Patches.portal));
		}

		if(globalParams.getBoolean("useBetterGrass")) {
			patches.add(new PatchSet(Patches.betterGrass));
			replaceFiles.add("BetterGrass.class");
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
