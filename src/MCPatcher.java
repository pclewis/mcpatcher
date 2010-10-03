import javassist.bytecode.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import javax.imageio.ImageIO;
import javax.swing.*;

public class MCPatcher {
	public static PrintStream out;
	public static PrintStream err;
	private static ByteArrayOutputStream baos;

	public static Params globalParams = new Params();

	public static void main(String[] argv) throws Exception {
        MainForm form = MainForm.create();
		String[] paths = new String[] {
			"minecraft.original.jar",
			System.getProperty("user.home") + "/AppData/Roaming/.minecraft/bin/minecraft.original.jar",
			"minecraft.jar",
			System.getProperty("user.home") + "/AppData/Roaming/.minecraft/bin/minecraft.jar",
		};

		for(String path : paths) {
			File f = new File(path);
			if(f.exists()) {
				if(path.endsWith(".original.jar"))
					form.noBackup();
				if(form.setMinecraftPath(f.getPath(), false)) // .getPath() to normalize /s
					break;
			}
		}

        form.show();
    }

    public static void applyPatch(Minecraft minecraft, TexturePack texturePack, File outputFile) {

		baos = new ByteArrayOutputStream();
		out = new PrintStream(baos);
		err = out;

	    JarOutputStream newjar = null;

	    ArrayList<PatchSet> patches = new ArrayList<PatchSet>();

	    patches.add(new PatchSet(Patches.animManager));
	    patches.add(new PatchSet(Patches.animTexture));

	    PatchSet waterPatches = new PatchSet(Patches.water);
	    if(!globalParams.getBoolean("useAnimatedWater")) {
		    waterPatches.setParam("tileSize", "0");
	    }
	    patches.add(new PatchSet("FlowWater", Patches.water));
	    patches.add(new PatchSet("StillWater", Patches.water));

	    PatchSet lavaPatches = new PatchSet(Patches.water);
	    if(!globalParams.getBoolean("useAnimatedLava")) {
		    lavaPatches.setParam("tileSize", "0");
	    }
	    patches.add(new PatchSet("FlowLava", Patches.water));
	    patches.add(new PatchSet("StillLava", Patches.water));

	    PatchSet firePatches = new PatchSet(Patches.fire);
	    if(!globalParams.getBoolean("useAnimatedFire")) {
		    firePatches.setParam("tileSize", "0");
	    }
	    patches.add(new PatchSet("Fire", Patches.fire));

	    patches.add(new PatchSet(Patches.compass));

		try {
			newjar = new JarOutputStream(new FileOutputStream(outputFile));
		} catch(IOException e) {
			MCPatcher.err.println(e.getMessage());
			exit(1);
		}

        if (newjar==null) {
            exit(1); return;
        }

		javassist.bytecode.MethodInfo.doPreverify = true;

		try {
			for(JarEntry entry : Collections.list(minecraft.getJar().entries())) {
				if(entry.getName().startsWith("META-INF"))
					continue; // leave out manifest

				newjar.putNextEntry(new ZipEntry(entry.getName()));
				if(entry.isDirectory()) {
					newjar.closeEntry();
					continue;
				}

				boolean patched = false;
				for(PatchSet patch : patches) {
					if(entry.getName().equals(minecraft.getClassMap().get(patch.getClassName()))) {
						MCPatcher.out.println("Patching class: " + patch.getClassName() + " (" + entry.getName() + ")");
						InputStream input = minecraft.getJar().getInputStream(entry);
						ClassFile cf = new ClassFile(new DataInputStream(input));
						ConstPool cp = cf.getConstPool();
						patch.visitConstPool(cp);
						for(Object mo : cf.getMethods()) {
							patch.visitMethod((MethodInfo)mo);
						}

						cf.compact();
						cf.write(new DataOutputStream(newjar));
						newjar.closeEntry();
						patched = true;
						break;
					}
				}
				
				if(entry.getName().equals("gui/items.png")) {
					MCPatcher.out.println("Resizing " + entry.getName());
					BufferedImage image = ImageIO.read(minecraft.getJar().getInputStream(entry));
					BufferedImage newImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
					Graphics2D graphics2D = newImage.createGraphics();
					/*
					graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
					                            */
					graphics2D.drawImage(image, 0, 0, 512, 512, null);

					// Write the scaled image to the outputstream
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					ImageIO.write(newImage, "PNG", newjar);
					patched = true;
				}

				if(!patched) {
					InputStream in = new BufferedInputStream(minecraft.getJar().getInputStream(entry));

					byte[] buffer = new byte[1024];
					while(true) {
						int count = in.read(buffer);
						if(count == -1)
							break;
						newjar.write(buffer, 0, count);
					}
				}

				newjar.closeEntry();
			}

			newjar.close();
		} catch(Exception e) {
			MCPatcher.err.println(e.getMessage());
		}

		MCPatcher.out.println("Success!...ostensibly");

		exit(0);
	}

	public static void exit(int code) {
		if(!GraphicsEnvironment.isHeadless()) {
			final JTextArea area = new JTextArea(baos.toString());
			area.setRows(10);
			area.setColumns(50);
			area.setLineWrap(true);
			final JScrollPane pane = new JScrollPane(area);

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					pane.getVerticalScrollBar().setValue(
					pane.getVerticalScrollBar().getMaximum());
					area.setCaretPosition(area.getDocument().getLength());
				}
			});
			JOptionPane.showMessageDialog(null, pane, "MCHDPatcher",
			                              (code==0) ? JOptionPane.PLAIN_MESSAGE : JOptionPane.ERROR_MESSAGE);
		}
		System.exit(code);
	}
}
