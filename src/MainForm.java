import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.Arrays;
import java.util.Map;

public class MainForm {
    private JPanel mainPanel;
    private JButton patchButton;
    private JProgressBar progressBar1;
    private JTextField origField;
    private JButton origBrowseButton;
    private JTextField backupField;
    private JTextField outputField;
    private JButton backupBrowseButton;
    private JButton outputBrowseButton;
    private JTextField packField;
    private JButton packBrowseButton;
    private JCheckBox animatedWaterCheckBox;
    private JCheckBox animatedLavaCheckBox;
    private JCheckBox animatedFireCheckBox;
	private JCheckBox backupCheckBox;
    private JCheckBox packCheckBox;
    private JPanel optionsPanel;
    private JPanel filesPanel;
    private JLabel origLabel;
    private JLabel backupLabel;
    private JLabel outputLabel;
    private JLabel packLabel;
	private JLabel textureInfoLabel;
	private JLabel classInfoLabel;
	private JButton runMinecraftButton;
	private JButton minecraftFolderButton;
	private JFrame frame;

	private Minecraft minecraft;
	private TexturePack texturePack;
	private TexturePack mcTexturePack;

	private SwingWorker worker;

    public MainForm(final JFrame frame) {
        this.frame = frame;

        final MainForm form = this;


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
			            setMinecraftPath(path, true);
		            } catch(Exception e1) {
			            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			            System.exit(1);
		            }
	            }
            }
        });

        backupBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileDialog fd = new FileDialog(form.frame, form.backupLabel.getText(), FileDialog.SAVE);
                fd.setFile("minecraft.original.jar");
                fd.setVisible(true);
	            if(fd.getFile() == null) {
		            form.backupField.setText( "" );
	            } else {
	                form.backupField.setText( fd.getDirectory() + fd.getFile() );
	            }
	            tryEnablePatch();
            }
        });

        outputBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileDialog fd = new FileDialog(form.frame, form.backupLabel.getText(), FileDialog.SAVE);
                fd.setFile("minecraft.jar");
                fd.setVisible(true);
	            if(fd.getFile() == null) {
		            form.outputField.setText( "" );
	            } else {
                    form.outputField.setText( fd.getDirectory() + fd.getFile() );
	            }
	            tryEnablePatch();
            }
        });

        packBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileDialog fd = new FileDialog(form.frame, form.backupLabel.getText(), FileDialog.LOAD);
                fd.setFile("*.zip;*.rar;*.jar");
                fd.setVisible(true);
	            if(fd.getFile() == null) {
		            form.packField.setText( "" );
		            texturePack = null;
	            } else {
		            String path = fd.getDirectory() + fd.getFile();
		            try {
			            setTexturePack(path);
		            } catch(Exception e1) {
			            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			            System.exit(1);
		            }
	            }

            }
        });


        backupCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                boolean enabled = form.backupCheckBox.isSelected();
                form.backupField.setEnabled( enabled );
                form.backupBrowseButton.setEnabled( enabled );
	            tryEnablePatch();
            }
        });

        packCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                boolean enabled = form.packCheckBox.isSelected();
                form.packField.setEnabled( enabled );
                form.packBrowseButton.setEnabled( enabled );
	            tryEnablePatch();
            }
        });

	    patchButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			    MCPatcher.globalParams.put("tileSize", ""+texturePack.getTerrainTileSize());
			    MCPatcher.globalParams.put("useAnimatedFire", ""+animatedFireCheckBox.isSelected());
				MCPatcher.globalParams.put("useAnimatedWater", ""+animatedWaterCheckBox.isSelected());
			    MCPatcher.globalParams.put("useAnimatedLava", ""+animatedLavaCheckBox.isSelected());

			    if(backupCheckBox.isSelected()) {
				    try {
					    String newPath = backupField.getText();
					    String texturePackPath = texturePack.getPath();
					    mcTexturePack.close();

					    if(texturePack.getPath().equals(mcTexturePack.getPath())) {
					        texturePack.close();
						    texturePackPath = newPath;
					    }

					    if (!minecraft.createBackup(new File(newPath)))
					    {
						    MCPatcher.err.println("Couldn't create backup");
						    return;
					    }
					    mcTexturePack = TexturePack.open(newPath, null);
					    texturePack = TexturePack.open(texturePackPath, mcTexturePack);

				    } catch(IOException e1) {
					    e1.printStackTrace(MCPatcher.err);
					    return;
				    }
			    }
				runWorker(new SwingWorker() {
				    protected Object doInBackground() throws Exception {
					    MCPatcher.applyPatch(minecraft, texturePack, new File(outputField.getText()));
					    return null;
				    }
			    });
			    worker.execute();
			    MCPatcher.logWindow.setVisible(true);
		    }
	    });
	    runMinecraftButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			    runWorker( new SwingWorker() {
				    protected Object doInBackground() throws Exception {
						String path = new File(origField.getText()).getParent();
						String cp = path + "/" + Util.joinString(Arrays.asList(
							"minecraft.jar", "lwjgl.jar", "lwjgl_util.jar", "jinput.jar"
						), ";" + path + "/");
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
					    return null;
				    }
			    });
			    MCPatcher.logWindow.setVisible(true);
		    }
	    });
	    minecraftFolderButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
			    String path = new File(origField.getText()).getParent();
			    ProcessBuilder pb = new ProcessBuilder("explorer", path);
			    try {
				    Process p = pb.start();
			    } catch(Exception e1) {
				    e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			    }
		    }
	    });
    }

	public void setTexturePack(String path) throws IOException {
		texturePack = TexturePack.open(path, mcTexturePack);

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
		frame.pack();
		tryEnablePatch();
	}

	public boolean setMinecraftPath(String path, boolean showErrors) throws Exception {
		String errors = "";
		try {
			minecraft = new Minecraft(new File(path));
		} catch(IOException ex) {
			errors = ex.getMessage();
			minecraft = null;
		}

		if(minecraft == null || !minecraft.isValid()) {
			if(showErrors) {
				if(minecraft != null) errors = Util.joinString(minecraft.getErrors(), "\n");
				JOptionPane.showMessageDialog(null, errors, "Error", JOptionPane.ERROR_MESSAGE);
			}
			origField.setText( "" );
			patchButton.setEnabled(false);
			return false;
		} else {
			origField.setText( path );
			StringBuilder sb = new StringBuilder();
			sb.append("<html><table>");
			for(Map.Entry<String,String> cfe : minecraft.getClassMap().entrySet()) {
				sb.append("<tr><td>").append(cfe.getKey()).append("</td><td>").append(cfe.getValue()).append("</td></tr>");
			}
			classInfoLabel.setText(sb.toString());

			if(backupCheckBox.isSelected() && backupField.getText().isEmpty()) {
				backupField.setText( path.replace(".jar", ".original.jar"));
			}

			if(outputField.getText().isEmpty()) {
				if(path.endsWith(".original.jar")) {
					outputField.setText( path.replace(".original.jar", ".jar") );
				} else if(backupCheckBox.isSelected()) {
					outputField.setText( path );
				}
			}

			if(packField.getText().isEmpty()) {
				mcTexturePack = TexturePack.open(path, null);
				setTexturePack(path);
			} else if (texturePack != null) {
				texturePack.setParent(mcTexturePack);
			}
			tryEnablePatch();
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

	public void noBackup() {
		backupCheckBox.setSelected(false);
	}

	public void tryEnablePatch() {
		patchButton.setEnabled(false);
		runMinecraftButton.setEnabled(false);
		minecraftFolderButton.setEnabled(false);

		if(worker != null) {
			if(!worker.isDone())
				return;
		}

		if(minecraft == null)
			return;

		runMinecraftButton.setEnabled(true);
		minecraftFolderButton.setEnabled(true);

		if(texturePack == null)
			return;

		if(texturePack.getTerrainTileSize() < 16 || texturePack.getItemsTileSize() < 16) {
			textureInfoLabel.setForeground(Color.RED);
			return;
		} else {
			textureInfoLabel.setForeground(Color.getColor("Label.foreground"));
		}

		if(backupCheckBox.isSelected() && backupField.getText().isEmpty()) {
			backupLabel.setForeground(Color.RED);
			return;
		} else {
			backupLabel.setForeground(Color.getColor("Label.foreground"));
		}

		if(backupCheckBox.isSelected() && (
				new File(backupField.getText()).equals(new File(outputField.getText())) ||
				new File(backupField.getText()).equals(new File(origField.getText())))) {
			backupLabel.setForeground(Color.RED);
			return;
		} else {
			backupLabel.setForeground(Color.getColor("Label.foreground"));
		}

		if(outputField.getText().isEmpty()) {
			outputLabel.setForeground(Color.RED);
			return;
		} else {
			outputLabel.setForeground(Color.getColor("Label.foreground"));
		}

		if( !backupCheckBox.isSelected() && new File(outputField.getText()).equals(new File(origField.getText())) ) {
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

	private void runWorker(SwingWorker worker) {
		this.worker = worker;
		worker.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("state") && evt.getNewValue().equals(SwingWorker.StateValue.DONE)) {
					tryEnablePatch();
				}
			}
		});
		worker.execute();
		tryEnablePatch();
	}
}
