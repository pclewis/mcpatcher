package com.pclewis.mcpatcher;

import javassist.ClassPool;
import javassist.CtClass;
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
import java.util.Collections;
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
		String[] paths = new String[] {
			"minecraft.original.jar",
			(appdata==null?home:appdata) + "/.minecraft/bin/minecraft.original.jar",
			home + "/Library/Application Support/minecraft/bin/minecraft.original.jar",
			home + "/.minecraft/bin/minecraft.original.jar",
			home + "/minecraft/bin/minecraft.original.jar",
			"minecraft.jar",
			(appdata==null?home:appdata) + "/.minecraft/bin/minecraft.jar",
			home + "/Library/Application Support/minecraft/bin/minecraft.jar",
			home + "/.minecraft/bin/minecraft.jar",
			home + "/minecraft/bin/minecraft.jar",
		};

		for(String path : paths) {
			File f = new File(path);
			if(f.exists()) {
				if(mainForm.setMinecraftPath(f.getPath())) // .getPath() to normalize /s
					break;
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

        ClassPool cp = ClassPool.getDefault();
        for(JarEntry entry : Collections.list(minecraft.getJar().entries())) {
            if(entry.getName().endsWith(".class")) {
                try {
                    cp.makeClass(minecraft.getJar().getInputStream(entry));
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }

		try {
			int totalFiles = minecraft.getJar().size();
			int procFiles = 0;
			for(JarEntry entry : Collections.list(minecraft.getJar().entries())) {
				String name = entry.getName();

				procFiles += 1;
				mainForm.updateProgress(procFiles, totalFiles);
				if(entry.getName().startsWith("META-INF"))
					continue; // leave out manifest

				newjar.putNextEntry(new ZipEntry(entry.getName()));
				if(entry.isDirectory()) {
					newjar.closeEntry();
					continue;
				}

				InputStream input = null;

				if(entry.getName().endsWith(".png"))
					input = texturePack.getInputStream(entry.getName());
				else
					input = minecraft.getJar().getInputStream(entry);

				boolean patched = false;

				if(replaceFiles.contains(name)) {
					replaceFiles.remove(name);
					replaceFile(name, newjar);
					patched = true;
				} else if (name.endsWith(".class")) {
					patched = applyPatches(name, input, minecraft, patches, newjar, cp);
				} else if(name.equals("gui/items.png") || name.equals("terrain.png")) {
					patched = resizeImage(name, input, newjar);
					if(!patched) { // can't rewind, so reopen
						input = texturePack.getInputStream(entry.getName());
					}
				}

				if(!patched) {
					Util.copyStream(input, newjar);
				}

				newjar.closeEntry();
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
		patches.add(new PatchSet(Patches.animManager));
		patches.add(new PatchSet(Patches.animTexture));

		PatchSet waterPatches = new PatchSet(Patches.water);
		if (globalParams.getBoolean("useCustomWater")) {
		    patches.add(new PatchSet("Minecraft", new PatchSet(Patches.customWaterMC)));
			replaceFiles.add("ht.class");
			replaceFiles.add("ml.class");
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
			replaceFiles.add("eg.class");
			replaceFiles.add("at.class");
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
		    patches.add(new PatchSet("Fire",firePatches));
		    patches.add(new PatchSet(Patches.compass));
			patches.add(new PatchSet(Patches.tool3d));
		}

		if(globalParams.getBoolean("useBetterGrass")) {
			patches.add(new PatchSet(Patches.betterGrass));
			replaceFiles.add("BetterGrass.class");
		}

		patches.add(new PatchSet("gy", Patches.electricCart));
		patches.add(new PatchSet("jo", Patches.electricCart));
		replaceFiles.add("ElectricCart.class");

		patches.add(new PatchSet(Patches.cart));
		patches.add(new PatchSet("Block", Patches.electricWire));
		patches.add(new PatchSet(Patches.wire));
		replaceFiles.add("ElectricWire.class");
		replaceFiles.add("ElectricCart$Point.class");
		replaceFiles.add("ElectricCart$PoweredPoint.class");

		patches.add(new PatchSet(Patches.entityLoader));

		patches.add(new PatchSet(Patches.moreTracks));
	}

	private static boolean resizeImage(String name, InputStream input, JarOutputStream newjar) throws IOException {
		boolean patched = false;
		int size = globalParams.getInt("tileSize") * 16;
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

	private static boolean applyPatches(String name, InputStream input, Minecraft minecraft,
                                        ArrayList<PatchSet> patches, JarOutputStream newjar, ClassPool classPool) throws Exception {
		Boolean patched = false;
		ClassFile cf = null;
		ConstPool cp = null;
        CtClass ct = null;
		for(PatchSet patch : patches) {
			if(name.equals(minecraft.getClassMap().get(patch.getClassName()))) {
				if(cf == null) {
					MCPatcher.out.println("Patching class: " + patch.getClassName() + " (" + name + ")");
                    ct = classPool.makeClass(input);
					cf = ct.getClassFile(); //new ClassFile(new DataInputStream(input));
					cp = cf.getConstPool();
				}
                patch.visitClassPre(ct);
				patch.visitConstPool(cp);
				for(Object mo : cf.getMethods()) {
					patch.visitMethod((MethodInfo)mo);
				}
                patch.visitClassPost(ct);
			}
		}
		if(cf != null) {
			//cf.compact();
			cf.write(new DataOutputStream(newjar));
			patched = true;
		}
		return patched;
	}
}
