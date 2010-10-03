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
		baos = new ByteArrayOutputStream();
		out = new PrintStream(baos);
		err = out;
		
        MainForm form = MainForm.create();
		String appdata = System.getenv("APPDATA");
		String home = System.getProperty("user.home");
		String[] paths = new String[] {
			"minecraft.original.jar",
			(appdata==null?home:appdata) + "/.minecraft/bin/minecraft.original.jar",
			home + "Library/Application Support/minecraft/bin/minecraft.original.jar",
			home + "minecraft/bin/minecraft.original.jar",
			"minecraft.jar",
			(appdata==null?home:appdata) + "/.minecraft/bin/minecraft.jar",
			home + "Library/Application Support/minecraft/bin/minecraft.jar",
			home + "minecraft/bin/minecraft.jar",
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

		try {
			newjar = new JarOutputStream(new FileOutputStream(outputFile));
		} catch(IOException e) {
			e.printStackTrace(MCPatcher.err);
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


				if(entry.getName().equals("gui/items.png")) {
					if(texturePack.getTerrainTileSize() != texturePack.getItemsTileSize()) {
						int size = texturePack.getTerrainTileSize() * 16;
						MCPatcher.out.println("Resizing " + entry.getName() + " to " + size + "x" + size);
						BufferedImage image = ImageIO.read(input);
						BufferedImage newImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
						Graphics2D graphics2D = newImage.createGraphics();
						graphics2D.drawImage(image, 0, 0, size, size, null);

						// Write the scaled image to the outputstream
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						ImageIO.write(newImage, "PNG", newjar);
						patched = true;
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
			exit(1);
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
