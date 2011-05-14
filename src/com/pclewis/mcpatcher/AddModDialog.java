package com.pclewis.mcpatcher;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class AddModDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField inputField;
    private JButton browseButton;
    private JTextField prefixField;
    private JButton chooseButton;

    private boolean busy;

    public AddModDialog() {
        setContentPane(contentPane);
        setTitle("Add external mod");
        setMinimumSize(new Dimension(512, 512));
        setResizable(true);
        setModal(true);
        pack();
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fd = new JFileChooser();
                fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fd.setFileHidingEnabled(false);
                fd.setDialogTitle("Select mod");
                fd.setCurrentDirectory(MCPatcherUtils.getMinecraftPath());
                fd.setAcceptAllFileFilterUsed(false);
                fd.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        String filename = f.getName().toLowerCase();
                        return f.isDirectory() || filename.endsWith(".zip") || filename.endsWith(".jar");
                    }

                    @Override
                    public String getDescription() {
                        return "*.zip;*.jar";
                    }
                });
                if (fd.showOpenDialog(contentPane) == JFileChooser.APPROVE_OPTION) {
                }
                updateControls();
            }
        });

        chooseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });

        setBusy(false);
    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        updateControls();
    }

    private void updateControls() {
        File file = new File(inputField.getText());
        browseButton.setEnabled(!busy);
        chooseButton.setEnabled(!busy && file.exists());
    }
}
