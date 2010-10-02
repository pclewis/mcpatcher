import javassist.bytecode.Bytecode;
import javassist.bytecode.ConstPool;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import javax.imageio.ImageIO;
import javax.swing.*;

public class MCPatcher {
	public static PrintStream out;
	public static PrintStream err;
	public static ByteArrayOutputStream baos;

    public static String getDefaultParam(String key) {
        return "";
    }
     
	private static final Replacement NEW_ARRAY_16x16 = new Replacement(
		"Change new array[16*16] to new array[32*32]",
		new int[]{
			Bytecode.SIPUSH, 16*16,
			Bytecode.NEWARRAY
		}, new int[]{
			Bytecode.SIPUSH, 32*32,
			Bytecode.NEWARRAY
		}
	);
	private static final Replacement WHILE_16 = new Replacement(
		"Change all while(...<16) to while(...<32)",
		new int[]{
			Bytecode.BIPUSH, 16,
			Bytecode.IF_ICMPGE,
		}, new int[]{
			Bytecode.BIPUSH, 32,
			Bytecode.IF_ICMPGE
		}
	);
	private static final Replacement WHILE_256 = new Replacement(
		"Change all while(...<16*16) to while(...<32*32)",
		new int[]{
			Bytecode.SIPUSH, 16*16,
			Bytecode.IF_ICMPGE
		}, new int[]{
			Bytecode.SIPUSH, 32*32,
			Bytecode.IF_ICMPGE
		}
	);
	private static final Replacement AND_0xF = new Replacement(
		"Change all &15 to &31",
		new int[]{
			Bytecode.BIPUSH, 15,
			Bytecode.IAND,
		}, new int[]{
			Bytecode.BIPUSH, 31,
			Bytecode.IAND
		}
	);
	private static final Replacement AND_0xFF = new Replacement(
		"Change all &0xFF to &0x1FF",
		new int[]{
			Bytecode.SIPUSH, 0x00, 0xFF,
			Bytecode.IAND
		}, new int[]{
			Bytecode.SIPUSH, 0x01, 0xFF,
			Bytecode.IAND
		}
	);
	private static final Replacement TIMES_16 = new Replacement(
		"Change all *16 to *32",
		new int[]{
			Bytecode.BIPUSH, 16,
			Bytecode.IMUL
		}, new int[]{
			Bytecode.BIPUSH, 32,
			Bytecode.IMUL
		}
	);
	private static final Replacement[] WATER_PATCHES = new Replacement[]{
		NEW_ARRAY_16x16,
		WHILE_16,
		WHILE_256,
		AND_0xF,
		TIMES_16,
		AND_0xFF,
	};


	private static final Replacement[] NEW_ARRAY_16x16x4 = new Replacement[]{
		new Replacement(
			"Change new array[16*16*4] to new array[32*32*4]",
			new int[]{
				Bytecode.SIPUSH, 16 * 16 * 4,
				Bytecode.NEWARRAY
			}, new int[]{
				Bytecode.SIPUSH, 32 * 32 * 4,
				Bytecode.NEWARRAY
			}
		),
	};

	private static final Replacement NEW_ARRAY_16x20 = new Replacement(
		"change new array[16*20] to new array[32*40]",
		new int[] {
			Bytecode.SIPUSH, 16*20,
			Bytecode.NEWARRAY
		}, new int[]{
			Bytecode.SIPUSH, 32*40,
			Bytecode.NEWARRAY
		}
	);
	private static final Replacement MOD16MUL16 = new Replacement(
		"change % 16 * 16 to % 16 * 32",
		new byte[]{
			Bytecode.BIPUSH, 16,
			Bytecode.IREM,
			Bytecode.BIPUSH, 16,
			Bytecode.IMUL
		}, new byte[]{
			Bytecode.BIPUSH, 16,
			Bytecode.IREM,
			Bytecode.BIPUSH, 32,
			Bytecode.IMUL
		}
	);
	private static final Replacement DIV16MUL16 = new Replacement(
		"change / 16 * 16 to / 16 * 32",
		new byte[]{
			Bytecode.BIPUSH, 16,
			Bytecode.IDIV,
			Bytecode.BIPUSH, 16,
			Bytecode.IMUL
		}, new byte[]{
			Bytecode.BIPUSH, 16,
			Bytecode.IDIV,
			Bytecode.BIPUSH, 32,
			Bytecode.IMUL
		}
	);
	public static PatchDefinition[] patches = {
		new PatchDefinition(
			"ey.class",
			new Replacement[]{
				/*
				MOD16MUL16,
				DIV16MUL16,*/
				new Replacement(
					"change %16*16+n*16 to %16*32+n*32",
				    new int[] {
					    Bytecode.BIPUSH, 16,
					    Bytecode.IREM,
					    Bytecode.BIPUSH, 16,
					    Bytecode.IMUL,
					    Bytecode.ILOAD_3,
					    Bytecode.BIPUSH, 16,
					    Bytecode.IMUL
				    }, new int[] {
						Bytecode.BIPUSH, 16,
						Bytecode.IREM,
						Bytecode.BIPUSH, 32,
						Bytecode.IMUL,
						Bytecode.ILOAD_3,
						Bytecode.BIPUSH, 32,
						Bytecode.IMUL
					}
				),

				new Replacement(
					"change /16*16+n*16 to /16*32+n*32",
				    new int[] {
					    Bytecode.BIPUSH, 16,
					    Bytecode.IDIV,
					    Bytecode.BIPUSH, 16,
					    Bytecode.IMUL,
					    Bytecode.ILOAD, 4,
					    Bytecode.BIPUSH, 16,
					    Bytecode.IMUL
				    }, new int[] {
						Bytecode.BIPUSH, 16,
						Bytecode.IDIV,
						Bytecode.BIPUSH, 32,
						Bytecode.IMUL,
						Bytecode.ILOAD, 4,
						Bytecode.BIPUSH, 32,
						Bytecode.IMUL
					}
				),

				new Replacement(
					"change glsub(...,16,16) to glsub(...,32,32)",
					new int[]{
						Bytecode.BIPUSH, 16,
						Bytecode.BIPUSH, 16,
						Bytecode.SIPUSH, 6408,
						Bytecode.SIPUSH, 5121,
					}, new int[]{
						Bytecode.BIPUSH, 32,
						Bytecode.BIPUSH, 32,
						Bytecode.SIPUSH, 6408,
						Bytecode.SIPUSH, 5121,
					}
				),
			}
		),

		new PatchDefinition(
			"z.class",
			NEW_ARRAY_16x16x4
		),

		new PatchDefinition(
			"ht.class",
			WATER_PATCHES
		),

		new PatchDefinition(
			"ml.class",
			WATER_PATCHES
		),
		new PatchDefinition(
			"at.class",
			WATER_PATCHES
		),
		new PatchDefinition(
			"eg.class",
			WATER_PATCHES
		),

		new PatchDefinition(
			"jz.class",
			new Replacement[]{
				NEW_ARRAY_16x20,
				WHILE_16,
				WHILE_256,
				new Replacement(
					"Change all while(...<20) to while(...<40)",
					new int[]{
						Bytecode.BIPUSH, 20,
						Bytecode.IF_ICMPGE,
					}, new int[]{
						Bytecode.BIPUSH, 40,
						Bytecode.IF_ICMPGE
					}
				),

				TIMES_16,

				new Replacement(
					"(unpatch) <init> *32 to *16",
					new int[]{
						Bytecode.ILOAD_1,
						Bytecode.BIPUSH, 32,
						Bytecode.IMUL
					}, new int[]{
						Bytecode.ILOAD_1,
						Bytecode.BIPUSH, 16,
						Bytecode.IMUL
					}
				),

				new Replacement(
					"Change all %20 to %40",
					new int[]{
						Bytecode.BIPUSH, 20,
						Bytecode.IREM
					}, new int[]{
						Bytecode.BIPUSH, 40,
						Bytecode.IREM
					}
				),

				new Replacement(
					"Change y<19 to y<38",
				    new int[]{
					    Bytecode.ILOAD_2,
					    Bytecode.BIPUSH, 19,
					    Bytecode.IF_ICMPLT
				    }, new int[]{
						Bytecode.ILOAD_2,
						Bytecode.BIPUSH, 38,
						Bytecode.IF_ICMPLT
					}
				),

				/*
				new Replacement(
					"Change h=18 to h=64",
				    new int[]{
					    Bytecode.BIPUSH, 18,
					    Bytecode.ISTORE_3
				    }, new int[]{
						Bytecode.BIPUSH, 64,
						Bytecode.ISTORE_3
					}
				), */

				/*
				new Replacement(
					"Remove flame falloff increase",
				    new int[] {
					    Bytecode.IINC, 3, 1,
					    Bytecode.IINC, 6, 1,
					    Bytecode.GOTO, 0xFF, 0xC0,
				    }, new int[] {
						Bytecode.NOP,Bytecode.NOP,Bytecode.NOP, 
						Bytecode.IINC, 6, 1,
						Bytecode.GOTO, 0xFF, 0xC0,
					}
				)*/
			},
			new ConstChanger[]{
				new ConstChanger() {
					public int index() { return 2; }
					public String description() { return "flame falloff 1.04 -> 0.52"; }
					public int add(ConstPool cp) { return cp.addFloatInfo(1.03F); }
				}
			}
		),
		new PatchDefinition(
			"aa.class",
		    new Replacement[]{
				NEW_ARRAY_16x16,
				NEW_ARRAY_16x20,
			    WHILE_256,
			    TIMES_16,
			    new Replacement(
				    "Change .getRGB(...16,16,...16) to .getRGB(...32,32,...32)",
			        new int[] {
				        Bytecode.BIPUSH, 16,
				        Bytecode.BIPUSH, 16,
				        Bytecode.ALOAD_0,
				        Bytecode.GETFIELD, 0x00, 0x2B,
				        Bytecode.ICONST_0,
				        Bytecode.BIPUSH, 16,
			        }, new int[] {
					    Bytecode.BIPUSH, 32,
					    Bytecode.BIPUSH, 32,
					    Bytecode.ALOAD_0,
					    Bytecode.GETFIELD, 0x00, 0x2B,
					    Bytecode.ICONST_0,
					    Bytecode.BIPUSH, 32
				    }
			    )
		    },
		    new ConstChanger[]{
		        new ConstChanger() {
			        public String description() { return "8.5D -> 16.5D"; }
			        public int index() { return 0x20; }
			        public int add(ConstPool cp) {
				        return cp.addDoubleInfo(16.5D);
			        }
		        },
		        new ConstChanger() {
					public String description() { return "7.5D -> 15.5D"; }
			        public int index() { return 0x1E; }
			        public int add(ConstPool cp) {
				        return cp.addDoubleInfo(15.5D);
			        }
		        },
		    }
		)
	};

	public static void main(String[] argv) {

        MainForm form = MainForm.create();
        form.show();
        if(true) return;

    }

    public static void runPatch(String orig, String backup, String output) {

		if(GraphicsEnvironment.isHeadless()) {
			out = System.out;
			err = System.err;
		} else {
			baos = new ByteArrayOutputStream();
			out = new PrintStream(baos);
			err = out;
		}

		JarFile mcjar = null;
		JarOutputStream newjar = null;

        File mc = new File(orig);
        File oc = new File(output);

        if(backup != null && !backup.isEmpty()) {
            File bc = new File(backup);
            if(!mc.renameTo(bc)) {
                MCPatcher.err.println("Can't move minecraft.jar to minecraft.original.jar");
                exit(1);
            }
            mc = bc;
        }

		try {
			mcjar = new JarFile(mc, false);
			newjar = new JarOutputStream(new FileOutputStream(oc));
		} catch(IOException e) {
			MCPatcher.err.println(e.getMessage());
			exit(1);
		}

        if (newjar==null) {
            exit(1); return;
        }

		javassist.bytecode.MethodInfo.doPreverify = true;

		try {
			for(JarEntry entry : Collections.list(mcjar.entries())) {
				if(entry.getName().startsWith("META-INF"))
					continue; // leave out manifest

				newjar.putNextEntry(new ZipEntry(entry.getName()));
				if(entry.isDirectory()) {
					newjar.closeEntry();
					continue;
				}

				boolean patched = false;
				for(PatchDefinition patch : patches) {
					if(entry.getName().equals(patch.fileName)) {
						MCPatcher.out.println("Patching file: " + entry.getName());
						patch.apply(mcjar.getInputStream(entry), newjar);
						newjar.closeEntry();
						patched = true;
						break;
					}
				}
				
				if(entry.getName().equals("gui/items.png")) {
					MCPatcher.out.println("Resizing " + entry.getName());
					BufferedImage image = ImageIO.read(mcjar.getInputStream(entry));
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
					InputStream in = new BufferedInputStream(mcjar.getInputStream(entry));

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

			mcjar.close();
			newjar.close();
		} catch(IOException e) {
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
