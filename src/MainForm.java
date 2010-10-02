import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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
    private JCheckBox fixWaterCheckBox;
    private JCheckBox fixLavaCheckBox;
    private JCheckBox fixFireCheckBox;
    private JCheckBox fixCompassCheckBox;
    private JButton advancedButton;
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
	private JFrame frame;

	private Minecraft minecraft;

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
                form.backupField.setText( fd.getFile() );
            }
        });

        outputBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileDialog fd = new FileDialog(form.frame, form.backupLabel.getText(), FileDialog.SAVE);
                fd.setFile("minecraft.jar");
                fd.setVisible(true);
                form.outputField.setText( fd.getFile() );
            }
        });

        packBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileDialog fd = new FileDialog(form.frame, form.backupLabel.getText(), FileDialog.LOAD);
                fd.setFile("*.zip;*.rar;*.jar");
                fd.setVisible(true);
                form.packField.setText( fd.getFile() );
            }
        });


        backupCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                boolean enabled = form.backupCheckBox.isSelected();
                form.backupField.setEnabled( enabled );
                form.backupBrowseButton.setEnabled( enabled );
            }
        });

        packCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                boolean enabled = form.packCheckBox.isSelected();
                form.packField.setEnabled( enabled );
                form.packBrowseButton.setEnabled( enabled );
            }
        });

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
			return false;
		} else {
			origField.setText( path );
			StringBuilder sb = new StringBuilder();
			sb.append("<html><table><font size=\"80%\">");
			for(Map.Entry<String,String> cfe : minecraft.getClassMap().entrySet()) {
				sb.append("<tr><td>").append(cfe.getKey()).append("</td><td>").append(cfe.getValue()).append("</td></tr>");
			}
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
			classInfoLabel.setText(sb.toString());
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
	    frame.setLocationRelativeTo( null );	    
        return form;
    }

    public void show() {
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
}
