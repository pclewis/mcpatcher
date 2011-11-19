package com.pclewis.mcpatcher;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AddModDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField inputField;
    private JButton browseButton;
    private JTable fileTable;
    private JButton addButton;
    private JButton removeButton;
    private JScrollPane fileTableScrollPane;

    private ZipFile zipFile;
    private ZipTreeDialog zipDialog;
    private HashMap<String, String> fileMap;
    private ExternalMod mod;
    private boolean editMode;

    public AddModDialog() {
        this(null);
    }

    public AddModDialog(ExternalMod mod) {
        this.fileMap = new HashMap<String, String>();
        if (mod != null) {
            this.mod = mod;
            editMode = true;
            zipFile = mod.zipFile;
            inputField.setText(zipFile.getName());
            fileMap.putAll(mod.fileMap);
        }

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

        fileTableScrollPane.getViewport().setBackground(fileTable.getBackground());
        fileTable.setModel(new FileTableModel());

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showZipDialog(true);
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileTableModel model = (FileTableModel) fileTable.getModel();
                int rowIndex;
                while ((rowIndex = fileTable.getSelectedRow()) >= 0) {
                    Map.Entry<String, String> row = model.getRow(rowIndex);
                    if (row != null) {
                        fileMap.remove(row.getKey());
                        model.fireTableRowsDeleted(rowIndex, rowIndex);
                    }
                }
            }
        });

        updateControls();
    }

    public void dispose() {
        hideZipDialog();
        MCPatcherUtils.close(zipFile);
        zipFile = null;
        super.dispose();
    }

    private void onOK() {
        if (zipFile == null || fileMap == null) {
            onCancel();
            return;
        }
        if (mod == null) {
            mod = new ExternalMod(zipFile, fileMap);
        } else {
            mod.setFileMap(fileMap);
        }
        zipFile = null;
        dispose();
    }

    private void onCancel() {
        mod = null;
        dispose();
    }

    private void updateControls() {
        boolean exists = new File(inputField.getText()).exists();
        browseButton.setVisible(!editMode);
        addButton.setEnabled(exists);
        removeButton.setEnabled(exists);
    }

    boolean showBrowseDialog() {
        JFileChooser fd = new JFileChooser();
        fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fd.setFileHidingEnabled(false);
        fd.setDialogTitle("Select mod zip file");
        File defaultModDir = null;
        try {
            String modDirString = MCPatcherUtils.getString(Config.TAG_LAST_MOD_DIRECTORY, "");
            File lastModDir = new File(modDirString);
            String version = MCPatcher.minecraft.getVersion().getVersionString();
            defaultModDir = MCPatcherUtils.getMinecraftPath("mods", version);
            if (modDirString.equals("")) {
                defaultModDir.mkdirs();
                fd.setCurrentDirectory(defaultModDir);
            } else if (lastModDir.isDirectory()) {
                fd.setCurrentDirectory(lastModDir);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
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
            File file = fd.getSelectedFile();
            inputField.setText(file.getPath());
            File lastModDir = file.getParentFile();
            if (lastModDir.equals(defaultModDir)) {
                MCPatcherUtils.set(Config.TAG_LAST_MOD_DIRECTORY, "");
            } else {
                MCPatcherUtils.set(Config.TAG_LAST_MOD_DIRECTORY, lastModDir.getAbsolutePath());
            }
            fileMap.clear();
            MCPatcherUtils.close(zipFile);
            zipFile = null;
            ((FileTableModel) fileTable.getModel()).fireTableDataChanged();
            showZipDialog(false);
        }
        updateControls();
        return zipFile != null;
    }

    private void showZipDialog(boolean addMode) {
        hideZipDialog();
        File inputFile = new File(inputField.getText());
        try {
            zipFile = new ZipFile(inputFile);
            zipDialog = new ZipTreeDialog(zipFile);
            if (addMode || zipDialog.hasSubfolders()) {
                zipDialog.setLocationRelativeTo(this);
                zipDialog.setVisible(true);
            }
            String newPrefix = zipDialog.getPrefix();
            if (newPrefix != null) {
                addFiles(newPrefix);
            }
        } catch (IOException e) {
            inputField.setText("");
            JOptionPane.showMessageDialog(null,
                "There was an error reading\n" +
                    inputFile.getPath() + "\n" +
                    e.toString(),
                "Error reading zip file", JOptionPane.ERROR_MESSAGE
            );
            Logger.log(e);
        } finally {
            hideZipDialog();
            updateControls();
        }
    }

    private void hideZipDialog() {
        if (zipDialog != null) {
            zipDialog.setVisible(false);
            zipDialog.dispose();
            zipDialog = null;
        }
    }

    private void addFiles(String prefix) {
        boolean changed = false;
        for (ZipEntry entry : Collections.list(zipFile.entries())) {
            String name = entry.getName();
            if (!entry.isDirectory() && name.startsWith(prefix)) {
                String suffix = name.substring(prefix.length());
                if (!MinecraftJar.isGarbageFile(suffix)) {
                    fileMap.put(suffix, name);
                    changed = true;
                }
            }
        }
        if (changed) {
            ((DefaultTableModel) fileTable.getModel()).fireTableDataChanged();
        }
    }

    ExternalMod getMod() {
        return mod;
    }

    private class FileTableModel extends DefaultTableModel {
        FileTableModel() {
            super(new Object[]{"From", "To"}, 0);
        }

        public int getRowCount() {
            return fileMap.size();
        }

        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        Map.Entry<String, String> getRow(int rowIndex) {
            ArrayList<Map.Entry<String, String>> list = new ArrayList<Map.Entry<String, String>>();
            list.addAll(fileMap.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
                public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                    return o1.getValue().compareTo(o2.getValue());
                }
            });
            if (rowIndex >= 0 && rowIndex < list.size()) {
                return list.get(rowIndex);
            } else {
                return null;
            }
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Map.Entry<String, String> entry = getRow(rowIndex);
            if (entry != null) {
                switch (columnIndex) {
                    case 0:
                        return entry.getValue();

                    case 1:
                        return entry.getKey();

                    default:
                        break;
                }
            }
            return null;
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }
    }
}
