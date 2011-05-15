package com.pclewis.mcpatcher;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AddModDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField inputField;
    private JButton browseButton;
    private JTextField prefixField;
    private JButton chooseButton;
    private JList fileList;

    private ZipFile zipFile;
    private ZipTreeDialog zipDialog;
    private Mod mod;

    public AddModDialog() {
        this(null, "");
    }

    public AddModDialog(ZipFile zipFile, String prefix) {
        this.zipFile = zipFile;
        prefixField.setText(prefix);

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
                showBrowseDialog();
            }
        });

        chooseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showZipDialog();
            }
        });

        updateControls();
        if (prefix.equals("")) {
            showBrowseDialog();
        }
    }

    public void dispose() {
        hideZipDialog();
        super.dispose();
    }

    private void onOK() {
        mod = new ExternalMod(zipFile, prefixField.getText());
        dispose();
    }

    private void onCancel() {
        mod = null;
        dispose();
    }

    private void updateControls() {
        chooseButton.setEnabled(new File(inputField.getText()).exists());
    }

    private void showBrowseDialog() {
        JFileChooser fd = new JFileChooser();
        fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fd.setFileHidingEnabled(false);
        fd.setDialogTitle("Select mod zip file");
        File dir = MCPatcherUtils.getMinecraftPath("mods");
        if (!dir.isDirectory()) {
            dir = MCPatcherUtils.getMinecraftPath();
        }
        fd.setCurrentDirectory(dir);
        fd.setAcceptAllFileFilterUsed(false);
        fd.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String filename = f.getName().toLowerCase();
                return f.isDirectory() || filename.endsWith(".zip");
            }

            @Override
            public String getDescription() {
                return "*.zip";
            }
        });
        if (fd.showOpenDialog(contentPane) == JFileChooser.APPROVE_OPTION) {
            inputField.setText(fd.getSelectedFile().getPath());
            showZipDialog();
        }
        updateControls();
    }

    private void showZipDialog() {
        hideZipDialog();
        File inputFile = new File(inputField.getText());
        if (inputFile.exists()) {
            try {
                zipFile = new ZipFile(inputFile);
                zipDialog = new ZipTreeDialog(zipFile, prefixField.getText());
                zipDialog.setLocationRelativeTo(this);
                zipDialog.setVisible(true);
                String newPrefix = zipDialog.getPrefix();
                if (newPrefix != null) {
                    prefixField.setText(newPrefix);
                }
                updateFileList();
            } catch (IOException e) {
                inputField.setText("");
                JOptionPane.showMessageDialog(null,
                    "There was an error reading\n" +
                        inputFile.getPath() + "\n" +
                        "The file may be corrupt.",
                    "Error reading zip file", JOptionPane.ERROR_MESSAGE
                );
                Logger.log(e);
            } finally {
                hideZipDialog();
            }
        }
        updateControls();
    }

    private void hideZipDialog() {
        if (zipDialog != null) {
            zipDialog.dispose();
            zipDialog = null;
        }
    }

    private void updateFileList() {
        Vector<String> items = new Vector<String>();
        String prefix = prefixField.getText();
        for (ZipEntry entry : Collections.list(zipFile.entries())) {
            String name = entry.getName();
            if (!entry.isDirectory() && name.startsWith(prefix)) {
                String suffix = name.substring(prefix.length());
                items.add(suffix);
            }
        }
        Collections.sort(items);
        fileList.setListData(items);
    }

    Mod getMod() {
        return mod;
    }
}
