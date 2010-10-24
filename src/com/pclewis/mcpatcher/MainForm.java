package com.pclewis.mcpatcher;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.ZipException;

public class MainForm implements Runnable {
    private JPanel mainPanel;
    private JButton patchButton;
    private JProgressBar progressBar1;
    private JTextField origField;
    private JButton origBrowseButton;
    private JTextField outputField;
    private JButton outputBrowseButton;
    private JTextField packField;
    private JButton packBrowseButton;
    private JCheckBox animatedWaterCheckBox;
    private JCheckBox animatedLavaCheckBox;
    private JCheckBox animatedFireCheckBox;
    private JCheckBox packCheckBox;
    private JPanel optionsPanel;
    private JPanel filesPanel;
    private JLabel origLabel;
    private JLabel outputLabel;
    private JLabel packLabel;
	private JLabel textureInfoLabel;
	private JLabel classInfoLabel;
	private JButton runMinecraftButton;
	private JButton minecraftFolderButton;
	private JComboBox tileSizeCombo;
	private JCheckBox customWaterCheckBox;
	private JCheckBox customLavaCheckBox;
	private JCheckBox betterGrassCheckBox;
	private JFrame frame;

	private Minecraft minecraft;
	private TexturePack texturePack;
	private TexturePack mcTexturePack;

	private Runnable worker;
	private Thread workThread;

	private final boolean canOpenFolder = Util.isWindows() || Util.isMac();

    public MainForm(final JFrame frame) {
        this.frame = frame;

        final MainForm form = this;

	    textureInfoLabel.setVisible(false);
	    classInfoLabel.setVisible(false);

		minecraftFolderButton.setEnabled(canOpenFolder);


        origBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileDialog fd = new FileDialog(form.frame, form.origLabel.getText(), FileDialog.LOAD);
                fd.setFile("minecraft.jar");
                fd.setVisible(true);

	            if(fd.getFile() == null) {
		            form.origField.setText( "" );
		            minecraft = null;
	            } else {
		            String path = fd.getDirectory() + fd.getFile();
		            try {
			            setMinecraftPath(path);
		            } catch(Exception e1) {
			            e1.printStackTrace(MCPatcher.err);
			            MCPatcher.logWindow.setVisible(true);
		            }
	            }
	            updateControls();
            }
        });

        outputBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileDialog fd = new FileDialog(form.frame, form.outputLabel.getText(), FileDialog.SAVE);
                fd.setFile("minecraft.jar");
                fd.setVisible(true);
	            if(fd.getFile() == null) {
		            form.outputField.setText( "" );
	            } else {
                    form.outputField.setText( fd.getDirectory() + fd.getFile() );
	            }
	            updateControls();
            }
        });

        packBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileDialog fd = new FileDialog(form.frame, form.packLabel.getText(), FileDialog.LOAD);
                fd.setFile("*.zip;*.rar;*.jar");
                fd.setVisible(true);
	            if(fd.getFile() == null) {
		            Boolean set = false;
		            if(minecraft != null) {
			            try {
			                setTexturePack(minecraft.getPath());
				            set = true;
			            } catch (IOException e1) {
				            set = false;
			            }
		            }
		            if(!set) {
						form.packField.setText( "" );
						texturePack = null;
		            }
	            } else {
		            String path = fd.getDirectory() + fd.getFile();
		            try {
			            setTexturePack(path);
		            } catch(Exception e1) {
			            e1.printStackTrace(MCPatcher.err);
			            MCPatcher.logWindow.setVisible(true);
		            }
	            }
	            updateControls();
            }
        });


        packCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                boolean enabled = form.packCheckBox.isSelected();
                form.packField.setEnabled( enabled );
	            updateControls();
            }
        });

	    patchButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			    int tileSize = Integer.parseInt(tileSizeCombo.getSelectedItem().toString().split("x")[0], 10);
			    MCPatcher.globalParams.put("tileSize", ""+tileSize);
			    MCPatcher.globalParams.put("useAnimatedFire", ""+animatedFireCheckBox.isSelected());
				MCPatcher.globalParams.put("useAnimatedWater", ""+animatedWaterCheckBox.isSelected());
			    MCPatcher.globalParams.put("useAnimatedLava", ""+animatedLavaCheckBox.isSelected());
			    MCPatcher.globalParams.put("useCustomWater", ""+customWaterCheckBox.isSelected());
		        MCPatcher.globalParams.put("useCustomLava", ""+customLavaCheckBox.isSelected());
			    MCPatcher.globalParams.put("useBetterGrass", ""+betterGrassCheckBox.isSelected());

				runWorker(new Runnable() {
				    public void run() {
					    MCPatcher.applyPatch(minecraft, texturePack, new File(outputField.getText()));
				    }
			    });
			    MCPatcher.logWindow.setVisible(true);
		    }
	    });
	    runMinecraftButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			    runWorker( new Runnable() {
				    public void run() {
						String path = new File(origField.getText()).getParent();
						String cp = path + "/" + Util.joinString(Arrays.asList(
							"minecraft.jar", "lwjgl.jar", "lwjgl_util.jar", "jinput.jar"
						), File.pathSeparatorChar + path + "/");
						ProcessBuilder pb = new ProcessBuilder(
							"java",
								"-cp", cp,
								"-Djava.library.path=" + path + "/natives",
								"-Xmx1024M", "-Xms512M",
								"net.minecraft.client.Minecraft");
						MCPatcher.out.println( pb.command() );
						pb.redirectErrorStream(true);

						try {
							Process p = pb.start();
							if(p != null) {
								BufferedReader input = new BufferedReader( new InputStreamReader(p.getInputStream()) );
								String line = null;
								while((line=input.readLine())!= null) {
									MCPatcher.out.println(line);

								}
								p.waitFor();
							}
						} catch(Exception e1) {
							e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			            }
				    }
			    });
			    MCPatcher.logWindow.setVisible(true);
		    }
	    });
	    minecraftFolderButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			    String path = new File(origField.getText()).getParent();
			    ProcessBuilder pb = new ProcessBuilder(Util.isWindows() ? "explorer" : "open", path);
			    try {
				    Process p = pb.start();
			    } catch(Exception e1) {
				    e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			    }
		    }
	    });
    }

	public void setTexturePack(String path) throws IOException {
		try {
			texturePack = TexturePack.open(path, mcTexturePack);
		} catch (ZipException ex) {
			JOptionPane.showMessageDialog(null,
				"The compression used in this zip file is not supported. Please install the\n" +
				"texture pack manually, or re-package it as a .jar or .rar file.",
				"Error Opening Texture Pack",
				JOptionPane.ERROR_MESSAGE);
			if(mcTexturePack!=null)
				packField.setText(mcTexturePack.getPath());
			else
				packField.setText("");
			updateControls();
			return;
		}

		if(mcTexturePack == null || !path.equals(mcTexturePack.getPath())) {
			packCheckBox.setSelected(true);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<html><table>");
		sb.append("<tr><td>terrain.png</td><td>");
		int tts = texturePack.getTerrainTileSize();
		if(tts < 0)
			sb.append("Not found");
		else
			sb.append(tts).append("x").append(tts);
		sb.append("<td></td>");
		sb.append(new File(texturePack.getTerrainSource()).getName());
		sb.append("</td></tr>");

		sb.append("<tr><td>items.png</td><td>");
		tts = texturePack.getItemsTileSize();
		if(tts < 0)
			sb.append("not found");
		else
			sb.append(tts).append("x").append(tts);
		sb.append("<td></td>");
		sb.append(new File(texturePack.getItemsSource()).getName());
		sb.append("</td></tr>");

		sb.append("<tr><td>mojang.png</td><td>");
		sb.append("-");
		sb.append("<td></td>");
		sb.append(new File(texturePack.getFileSource("title/mojang.png")).getName());
		sb.append("</td></tr>");

		textureInfoLabel.setText(sb.toString());
		packField.setText(path);

		tileSizeCombo.setSelectedItem( texturePack.getTerrainTileSize() + "x" + texturePack.getTerrainTileSize() );
		frame.pack();
		updateControls();
	}

	public boolean setMinecraftPath(String path) throws Exception {
		String errors = "";
		try {
			minecraft = new Minecraft(new File(path));
		} catch(IOException ex) {
			errors = ex.getMessage();
			minecraft = null;
		}

		if(minecraft == null || !minecraft.isValid()) {
			if(minecraft != null) errors = Util.joinString(minecraft.getErrors(), "\n");
			MCPatcher.err.println(errors);
			MCPatcher.logWindow.setVisible(true);
			JOptionPane.showMessageDialog(null,
				"There was an error opening minecraft.jar. This may be because:\n"+
				" - The file has already been patched.\n" +
				" - There was an update that this patcher cannot handle.\n" +
				" - There is another, conflicting mod applied.\n" +
				" - The jar file is invalid or corrupt.\n" +
				"\n" +
				"You can re-download the original minecraft.jar by deleting your minecraft/bin folder and "+
				"running the game normally.\n" +
				"\n" +
				"Please see log window for more details.",
				"Error", JOptionPane.ERROR_MESSAGE);
			origField.setText( "" );
			minecraft = null;
			patchButton.setEnabled(false);
			updateControls();
			return false;
		} else {
			if(!minecraft.isBackup()) {
				boolean success = false;
				try {
					success = minecraft.createBackup();
					if(!success)
						JOptionPane.showMessageDialog(null, "Must create backup.", "Error", JOptionPane.ERROR_MESSAGE);
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(null, ex.getMessage(), "Error Creating Backup", JOptionPane.ERROR_MESSAGE);
				}

				if(!success) {
					origField.setText( "" );
					minecraft = null;
					patchButton.setEnabled(false);
					updateControls();
					return false;
				}

				path = minecraft.getPath();
			}

			origField.setText( path );
			StringBuilder sb = new StringBuilder();
			sb.append("<html><table>");
			for(Map.Entry<String,String> cfe : minecraft.getClassMap().entrySet()) {
				sb.append("<tr><td>").append(cfe.getKey()).append("</td><td>").append(cfe.getValue()).append("</td></tr>");
			}
			classInfoLabel.setText(sb.toString());

			if(outputField.getText().length() == 0) {
				if(path.endsWith(".original.jar")) {
					outputField.setText( path.replace(".original.jar", ".jar") );
				}
			}

			if(packField.getText().length() == 0) {
				mcTexturePack = TexturePack.open(path, null);
				setTexturePack(path);
			} else if (texturePack != null) {
				texturePack.setParent(mcTexturePack);
			}
			updateControls();
			frame.pack();
			return true;
		}
	}

	public static MainForm create() {
        JFrame frame = new JFrame("Minecraft Patcher");
        frame.setResizable(false);
        MainForm form = new MainForm(frame);
        frame.setContentPane(form.getMainPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        return form;
    }

    public void show() {
	    frame.setLocationRelativeTo( null );
        frame.setVisible(true);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void createUIComponents() {
        // place custom component creation code here
    }

	public void updateControls() {
		patchButton.setEnabled(false);
		runMinecraftButton.setEnabled(false);
		minecraftFolderButton.setEnabled(false);
		if(minecraft == null) {
			classInfoLabel.setText("");
		}
		if(texturePack == null) {
			textureInfoLabel.setText("");
		}

		if(worker != null) {
			return;
		}

		if(minecraft == null)
			return;

		runMinecraftButton.setEnabled(true);
		minecraftFolderButton.setEnabled(canOpenFolder);

		if(texturePack == null)
			return;

		if(texturePack.getTerrainTileSize() < 16 || texturePack.getItemsTileSize() < 16) {
			textureInfoLabel.setForeground(Color.RED);
			return;
		} else {
			textureInfoLabel.setForeground(Color.getColor("Label.foreground"));
		}

		if(outputField.getText().length() == 0) {
			outputLabel.setForeground(Color.RED);
			return;
		} else {
			outputLabel.setForeground(Color.getColor("Label.foreground"));
		}

		if( new File(outputField.getText()).equals(new File(origField.getText())) ) {
			outputLabel.setForeground(Color.RED);
			return;
		} else {
			outputLabel.setForeground(Color.getColor("Label.foreground"));
		}

		patchButton.setEnabled(true);
	}

	public void updateProgress(int at, int total) {
		progressBar1.setMaximum(total);
		progressBar1.setValue(at);
	}

	private void runWorker(Runnable worker) {
		this.worker = worker;
		this.workThread = new Thread(this);
		this.workThread.start();
		updateControls();
	}

	public void run() {
		this.worker.run();
		this.worker = null;
		updateControls();
	}
}
