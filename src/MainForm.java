import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;

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
    private JLabel textureSizeLabel;
    private JCheckBox backupCheckBox;
    private JCheckBox packCheckBox;
    private JPanel optionsPanel;
    private JPanel filesPanel;
    private JLabel origLabel;
    private JLabel backupLabel;
    private JLabel outputLabel;
    private JLabel packLabel;
    private JFrame frame;

    public MainForm(final JFrame frame) {
        this.frame = frame;

        final MainForm form = this;


        origBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileDialog fd = new FileDialog(form.frame, form.origLabel.getText(), FileDialog.LOAD);
                fd.setFile("minecraft.jar");
                fd.setVisible(true);
                form.origField.setText( fd.getFile() );
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
        frame.setVisible(true);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void createUIComponents() {
        // place custom component creation code here
    }
}
