import javassist.bytecode.*;

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
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

public class MCPatcher {
	public static PrintStream out;
	public static PrintStream err;

	public static Params globalParams = new Params();
	public static JFrame logWindow;
	private static MainForm mainForm;

	public static void main(String[] argv) throws Exception {
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

		mainForm = MainForm.create();
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
				if(path.endsWith(".original.jar"))
					mainForm.noBackup();
				if(mainForm.setMinecraftPath(f.getPath(), false)) // .getPath() to normalize /s
					break;
			}
		}

        mainForm.show();
    }

    public static void applyPatch(Minecraft minecraft, TexturePack texturePack, File outputFile) {

	    JarOutputStream newjar = null;

	    ArrayList<PatchSet> patches = new ArrayList<PatchSet>();

	    patches.add(new PatchSet(Patches.animManager));
	    patches.add(new PatchSet(Patches.animTexture));

	    PatchSet waterPatches = new PatchSet(Patches.water);
	    if(!globalParams.getBoolean("useAnimatedWater")) {
		    waterPatches.setParam("tileSize", "0");
		    patches.add( new PatchSet(Patches.hideWater) );
	    }
	    patches.add(new PatchSet("FlowWater", waterPatches));
	    patches.add(new PatchSet("StillWater", waterPatches));

	    PatchSet lavaPatches = new PatchSet(Patches.water);
	    if(!globalParams.getBoolean("useAnimatedLava")) {
		    lavaPatches.setParam("tileSize", "0");
		    patches.add( new PatchSet(Patches.hideLava) );
	    }
	    patches.add(new PatchSet("FlowLava", lavaPatches));
	    patches.add(new PatchSet("StillLava", lavaPatches));

	    PatchSet firePatches = new PatchSet(Patches.fire);
	    if(!globalParams.getBoolean("useAnimatedFire")) {
		    firePatches.setParam("tileSize", "0");
		    patches.add( new PatchSet(Patches.hideFire) );
	    }
	    if(texturePack.getTerrainTileSize() > 16) {
	        patches.add(new PatchSet("Fire",firePatches));
	    }

	    if(texturePack.getTerrainTileSize() > 16) {
	        patches.add(new PatchSet(Patches.compass));
	    }

	    if(texturePack.getTerrainTileSize() > 16) {
		    //patches.add(new PatchSet(Patches.tool3d));
	    }

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
			int totalFiles = minecraft.getJar().size();
			int procFiles = 0;
			for(JarEntry entry : Collections.list(minecraft.getJar().entries())) {
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
				ClassFile cf = null;
				ConstPool cp = null;
				for(PatchSet patch : patches) {
					if(entry.getName().equals(minecraft.getClassMap().get(patch.getClassName()))) {
						if(cf == null) {
							MCPatcher.out.println("Patching class: " + patch.getClassName() + " (" + entry.getName() + ")");
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
					newjar.closeEntry();
					patched = true;
				}


				if(entry.getName().equals("gui/items.png") || entry.getName().equals("terrain.png")) {
					int size = globalParams.getInt("tileSize") * 16;
					MCPatcher.out.println("Reading " + entry.getName() + "...");
					BufferedImage image = ImageIO.read(input);

					if(image.getWidth() != size || image.getHeight() != size) {
						MCPatcher.out.println("Resizing " + entry.getName() + " from " + image.getWidth() + "x" +
							image.getHeight() + " to " + size + "x" + size);

						BufferedImage newImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
						Graphics2D graphics2D = newImage.createGraphics();
						graphics2D.drawImage(image, 0, 0, size, size, null);

						// Write the scaled image to the outputstream
						ImageIO.write(newImage, "PNG", newjar);
						patched = true;
					} else {
						// can't rewind, so reopen
						input = texturePack.getInputStream(entry.getName());
					}
				}

				if(!patched) {
					byte[] buffer = new byte[1024];
					while(true) {
						int count = input.read(buffer);
						if(count == -1)
							break;
						newjar.write(buffer, 0, count);
					}
				}

				newjar.closeEntry();
			}

			newjar.close();
		} catch(Exception e) {
			e.printStackTrace(MCPatcher.err);
			return;
		}

		MCPatcher.out.println("\n\n#### Success! ...probably ####");
	}
}
