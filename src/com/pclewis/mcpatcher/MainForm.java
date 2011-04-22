package com.pclewis.mcpatcher;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

class MainForm {
    private static final int TAB_MODS = 0;
    private static final int TAB_OPTIONS = 1;
    private static final int TAB_LOG = 2;
    private static final int TAB_CLASS_MAP = 3;
    private static final int TAB_PATCH_SUMMARY = 4;

    private static final Color MOD_BUSY_COLOR = new Color(192, 192, 192);
    private static final String MOD_DESC_FORMAT = "<html>" +
        "<table border=\"0\" width=\"%1$d\"><tr style=\"font-weight: bold; font-size: larger;\">" +
        "<td align=\"left\">%2$s</td>" +
        "<td align=\"right\">%3$s</td>" +
        "</tr>" +
        "<tr><td colspan=\"2\" style=\"font-weight: normal; font-style: italic;\">%4$s</td></tr>" +
        "</table>" +
        "</html>";

    private JPanel mainPanel;
    private JFrame frame;
    private int frameWidth = 518;

    private JTextField origField;
    private JButton origBrowseButton;
    private JTextField outputField;
    private JButton outputBrowseButton;
    private JButton testButton;
    private JButton patchButton;
    private JButton undoButton;
    private JTable modTable;
    private JLabel statusText;
    private JButton refreshButton;
    private JProgressBar progressBar;
    private JTabbedPane tabbedPane;
    private JTextArea logText;
    private JButton copyLogButton;
    private JTextArea classMap;
    private JTextArea patchResults;
    private JButton copyClassMapButton;
    private JButton copyPatchResultsButton;
    private JScrollPane modTableScrollPane;
    private JPanel optionsPanel;

    private boolean busy = true;
    private Thread workerThread = null;

    public MainForm() {
        frame = new JFrame("Minecraft Patcher " + MCPatcher.VERSION_STRING);
        frame.setResizable(true);
        frame.setContentPane(mainPanel);
        frame.addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
                cancelWorker();
                frame.setVisible(false);
                frame.dispose();
                MCPatcherUtils.saveProperties();
                System.exit(0);
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowActivated(WindowEvent e) {
            }

            public void windowDeactivated(WindowEvent e) {
            }
        });
        frame.addComponentListener(new ComponentListener() {
            public void componentResized(ComponentEvent e) {
                frameWidth = (int) ((Component) e.getSource()).getSize().getWidth();
            }

            public void componentMoved(ComponentEvent e) {
            }

            public void componentShown(ComponentEvent e) {
            }

            public void componentHidden(ComponentEvent e) {
            }
        });
        frame.setMinimumSize(new Dimension(470, 488));
        frame.pack();

        origBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStatusText("");
                JFileChooser fd = new JFileChooser();
                fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fd.setFileHidingEnabled(false);
                fd.setDialogTitle("Select input file");
                fd.setCurrentDirectory(MCPatcherUtils.getMinecraftPath("bin"));
                fd.setAcceptAllFileFilterUsed(false);
                fd.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar");
                    }

                    @Override
                    public String getDescription() {
                        return "*.jar";
                    }
                });
                if (fd.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    if (MCPatcher.setMinecraft(fd.getSelectedFile(), false)) {
                        MCPatcher.getAllMods();
                        updateModList();
                    } else {
                        showCorruptJarError();
                    }
                }
                updateControls();
            }
        });

        outputBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStatusText("");
                JFileChooser fd = new JFileChooser();
                fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fd.setFileHidingEnabled(false);
                fd.setDialogTitle("Select output file");
                fd.setCurrentDirectory(MCPatcherUtils.getMinecraftPath("bin"));
                fd.setSelectedFile(MCPatcherUtils.getMinecraftPath("bin", "minecraft.jar"));
                fd.setAcceptAllFileFilterUsed(false);
                fd.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar");
                    }

                    @Override
                    public String getDescription() {
                        return "*.jar";
                    }
                });
                if (fd.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    MCPatcher.minecraft.setOutputFile(fd.getSelectedFile());
                }
                updateControls();
            }
        });

        modTable.setRowSelectionAllowed(true);
        modTable.setColumnSelectionAllowed(false);
        modTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setModList(null);
        modTableScrollPane.getViewport().setBackground(modTable.getBackground());
        modTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                setStatusText("");
                if (modTable.isEnabled()) {
                    int row = modTable.getSelectedRow();
                    int col = modTable.getSelectedColumn();
                    Mod mod = (Mod) modTable.getModel().getValueAt(row, col);
                    if (col == 0 && mod != null && mod.okToApply()) {
                        MCPatcher.modList.selectMod(mod, !mod.isEnabled());
                        modTable.repaint();
                    }
                }
                super.mouseClicked(e);
            }
        });

        patchButton.addActionListener(new ActionListener() {
            class PatchThread implements Runnable {
                public void run() {
                    if (!MCPatcher.patch()) {
                        tabbedPane.setSelectedIndex(TAB_LOG);
                        JOptionPane.showMessageDialog(frame,
                            "There was an error during patching.  " +
                                "See log for more information.  " +
                                "Your original minecraft.jar has been restored.",
                            "Error", JOptionPane.ERROR_MESSAGE
                        );
                    }
                    setBusy(false);
                }
            }

            public void actionPerformed(ActionEvent e) {
                setBusy(true);
                setStatusText("Patching %s...", MCPatcher.minecraft.getOutputFile().getName());
                runWorker(new PatchThread());
            }
        });

        undoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStatusText("");
                try {
                    MCPatcher.minecraft.restoreBackup();
                    MinecraftJar.setDefaultTexturePack();
                    JOptionPane.showMessageDialog(frame, "Restored original minecraft jar and reset texture pack to default.", "", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e1) {
                    Logger.log(e1);
                    JOptionPane.showMessageDialog(frame, "Failed to restore minecraft jar from backup:\n\n" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                updateControls();
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setStatusText("");
                MCPatcher.getAllMods();
                updateModList();
            }
        });

        testButton.addActionListener(new ActionListener() {
            class MinecraftThread implements Runnable {
                public void run() {
                    MCPatcher.minecraft.run();
                    setBusy(false);
                }
            }

            public void actionPerformed(ActionEvent e) {
                tabbedPane.setSelectedIndex(TAB_LOG);
                setBusy(true);
                setStatusText("Launching %s...", MCPatcher.minecraft.getOutputFile().getName());
                MCPatcherUtils.saveProperties();
                runWorker(new MinecraftThread());
            }
        });

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateActiveTab();
            }
        });

        ((DefaultCaret) logText.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        Logger.setOutput(new JTextAreaPrintStream(logText));
        copyLogButton.addActionListener(new CopyToClipboardListener(logText));

        ((DefaultCaret) classMap.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        copyClassMapButton.addActionListener(new CopyToClipboardListener(classMap));

        ((DefaultCaret) patchResults.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        copyPatchResultsButton.addActionListener(new CopyToClipboardListener(patchResults));
    }

    public void show() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public File chooseMinecraftDir(File minecraftDir) {
        JOptionPane.showMessageDialog(null,
            "Minecraft not found in\n" +
                minecraftDir.getPath() + "\n\n" +
                "If the game is installed somewhere else, please select the game\n" +
                "folder (the one containing bin, resources, saves, etc., subfolders).",
            "Minecraft not found", JOptionPane.ERROR_MESSAGE
        );
        JFileChooser fd = new JFileChooser();
        fd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fd.setFileHidingEnabled(false);
        fd.setDialogTitle("Select Minecraft directory");
        int result = fd.showDialog(null, null);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return fd.getSelectedFile();
    }

    public void showBetaWarning() {
        JOptionPane.showMessageDialog(frame,
            "This is a pre-release version of MCPatcher and is not intended\n" +
                "for general use.\n\n" +
                "Please make backups of your mods, save files, and texture packs\n" +
                "before using.  Report any problems in the thread for MCPatcher beta at\n" +
                "http://www.minecraftforum.net/viewtopic.php?f=1021&t=252531",
            "For testing only", JOptionPane.INFORMATION_MESSAGE
        );
    }

    public void showCorruptJarError() {
        tabbedPane.setSelectedIndex(TAB_LOG);
        JOptionPane.showMessageDialog(frame,
            "There was an error opening minecraft.jar. This may be because:\n" +
                " - You selected the launcher jar and not the main minecraft.jar in the bin folder.\n" +
                " - You selected a texture pack and not minecraft.jar.\n" +
                " - The file has already been patched.\n" +
                " - There was an update that this patcher cannot handle.\n" +
                " - There is another, conflicting mod applied.\n" +
                " - The jar file is invalid or corrupt.\n" +
                "\n" +
                "You can re-download the original minecraft.jar by using the Force Update\n" +
                "button in the Minecraft Launcher.\n",
            "Invalid or Corrupt jar", JOptionPane.ERROR_MESSAGE
        );
    }

    private void cancelWorker() {
        if (workerThread != null && workerThread.isAlive()) {
            try {
                workerThread.interrupt();
                setStatusText("Waiting for current task to finish...");
                workerThread.join();
            } catch (InterruptedException e) {
                Logger.log(e);
            }
            setStatusText("");
        }
        workerThread = null;
    }

    private void runWorker(Runnable runnable) {
        cancelWorker();
        workerThread = new Thread(runnable);
        workerThread.start();
    }

    public synchronized void setBusy(boolean busy) {
        this.busy = busy;
        if (!busy) {
            setStatusText("");
            updateProgress(0, 0);
        }
        updateControls();
    }

    public synchronized void setStatusText(String format, Object... params) {
        statusText.setText(String.format(format, params));
    }

    public synchronized void updateProgress(int value, int max) {
        if (max > 0) {
            progressBar.setVisible(true);
            progressBar.setMinimum(0);
            progressBar.setMaximum(max);
            progressBar.setValue(value);
        } else {
            progressBar.setVisible(false);
            progressBar.setMinimum(0);
            progressBar.setMaximum(1);
            progressBar.setValue(0);
        }
    }

    public void updateControls() {
        if (MCPatcher.minecraft == null) {
            origField.setText("");
            outputField.setText("");
        } else {
            origField.setText(MCPatcher.minecraft.getInputFile().getPath());
            outputField.setText(MCPatcher.minecraft.getOutputFile().getPath());
        }
        boolean outputSet = !outputField.getText().equals("");
        File orig = new File(origField.getText());
        File output = new File(outputField.getText());
        boolean origOk = orig.exists();
        boolean outputOk = output.exists();
        origBrowseButton.setEnabled(!busy);
        outputBrowseButton.setEnabled(!busy);
        modTable.setEnabled(!busy && origOk && outputSet);
        refreshButton.setEnabled(!busy);
        testButton.setEnabled(!busy && outputOk && MCPatcherUtils.getMinecraftPath().equals(MCPatcherUtils.getDefaultGameDir()));
        patchButton.setEnabled(!busy && origOk && !output.equals(orig));
        undoButton.setEnabled(!busy && origOk && !output.equals(orig));
        tabbedPane.setEnabled(!busy);

        updateActiveTab();
    }

    private void updateActiveTab() {
        if (tabbedPane.getSelectedIndex() != TAB_OPTIONS) {
            for (Mod mod : MCPatcher.modList.getAll()) {
                if (mod.configPanel != null) {
                    try {
                        mod.configPanel.save();
                    } catch (Throwable e) {
                        Logger.log(e);
                    }
                }
            }
        }
        switch (tabbedPane.getSelectedIndex()) {
            case TAB_OPTIONS:
                updateOptionsPanel();
                break;

            case TAB_CLASS_MAP:
                showClassMaps();
                break;

            case TAB_PATCH_SUMMARY:
                showPatchResults();
                break;

            default:
                break;
        }
    }

    private void updateOptionsPanel() {
        optionsPanel.removeAll();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        for (Mod mod : MCPatcher.modList.getAll()) {
            try {
                if (mod.configPanel == null) {
                    continue;
                }
                mod.configPanel.load();
                JPanel panel = mod.configPanel.getPanel();
                if (panel == null) {
                    continue;
                }
                String name = mod.configPanel.getPanelName();
                if (name == null) {
                    name = mod.getName();
                }
                if (panel.getParent() != null) {
                    panel.getParent().remove(panel);
                }
                panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), name));
                optionsPanel.add(panel);
                optionsPanel.add(Box.createRigidArea(new Dimension(1, 16)));
            } catch (Throwable e) {
                Logger.log(e);
            }
        }
        optionsPanel.validate();
    }

    private void showClassMaps() {
        classMap.setText("");
        JTextAreaPrintStream out = new JTextAreaPrintStream(classMap);
        MCPatcher.showClassMaps(out);
        out.close();
    }

    private void showPatchResults() {
        patchResults.setText("");
        JTextAreaPrintStream out = new JTextAreaPrintStream(patchResults);
        MCPatcher.showPatchResults(out);
        out.close();
    }

    public void setModList(final ModList modList) {
        modTable.setModel(new TableModel() {
            public int getRowCount() {
                return modList == null ? 0 : modList.getVisible().size();
            }

            public int getColumnCount() {
                return 2;
            }

            public String getColumnName(int columnIndex) {
                return null;
            }

            public Class<?> getColumnClass(int columnIndex) {
                return Mod.class;
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                return (modList != null && rowIndex >= 0 && rowIndex < modList.getVisible().size()) ? modList.getVisible().get(rowIndex) : null;
            }

            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            }

            public void addTableModelListener(TableModelListener l) {
            }

            public void removeTableModelListener(TableModelListener l) {
            }
        });
        modTable.getColumnModel().getColumn(0).setCellRenderer(new ModCheckBoxRenderer());
        modTable.getColumnModel().getColumn(1).setCellRenderer(new ModTextRenderer());
        modTable.doLayout();
        redrawModList();
    }

    public void updateModList() {
        setBusy(true);
        setStatusText("Analyzing %s...", MCPatcher.minecraft.getInputFile().getName());
        runWorker(new Runnable() {
            public void run() {
                try {
                    MCPatcher.getApplicableMods();
                    if (MCPatcher.minecraft.isModded()) {
                        JOptionPane.showMessageDialog(frame,
                            "Your minecraft.jar appears to be an older version or is already modded.\n" +
                                "This may work fine, but if the game crashes or you have problems patching:\n" +
                                " - Re-download the game by using Force Update in the launcher.\n" +
                                " - Delete the backup minecraft-" + MCPatcher.minecraft.getVersion() + ".jar\n" +
                                " - If you are using other mods, try applying them after running MCPatcher.",
                            "Warning", JOptionPane.WARNING_MESSAGE
                        );
                    }
                } catch (InterruptedException e) {
                } catch (IOException e) {
                    Logger.log(e);
                    showCorruptJarError();
                }
                redrawModList();
                setBusy(false);
            }
        });
    }

    public void redrawModList() {
        TableModel model = modTable.getModel();
        int rows = model.getRowCount();
        int cols = model.getColumnCount();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                model.setValueAt(null, i, j);
            }
        }
    }

    private class ModCheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        private boolean widthSet = false;

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Mod mod = (Mod) value;
            if (!table.isEnabled() || !mod.okToApply()) {
                setBackground(table.getBackground());
                setForeground(MOD_BUSY_COLOR);
            } else if (row == table.getSelectedRow()) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            setSelected(mod.isEnabled());
            setEnabled(table.isEnabled() && mod.okToApply());
            if (!widthSet) {
                TableColumn col = table.getColumnModel().getColumn(column);
                double width = getPreferredSize().getWidth();
                col.setMinWidth((int) width);
                col.setMaxWidth((int) (1.5 * width));
                col.setPreferredWidth((int) (1.5 * width));
                widthSet = true;
            }
            return this;
        }
    }

    private class ModTextRenderer extends JLabel implements TableCellRenderer {
        private HashMap<Integer, Integer> rowSize = new HashMap<Integer, Integer>();

        private String htmlEscape(String s) {
            return s == null ? "" : s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Mod mod = (Mod) value;

            setText(String.format(MOD_DESC_FORMAT,
                Math.max(frameWidth - 75, 350),
                htmlEscape(mod.getName()),
                htmlEscape(mod.getVersion()),
                htmlEscape(mod.getDescription())
            ));
            if (!table.isEnabled() || !mod.okToApply()) {
                setBackground(table.getBackground());
                setForeground(MOD_BUSY_COLOR);
            } else if (row == table.getSelectedRow()) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            setEnabled(table.isEnabled() && mod.okToApply());
            if (!rowSize.containsKey(row)) {
                int h = (int) getPreferredSize().getHeight();
                rowSize.put(row, h);
                table.setRowHeight(row, h);
            }

            ArrayList<String> errors = mod.getErrors();
            StringBuilder sb = new StringBuilder();
            if (!table.isEnabled()) {
            } else if (errors.size() == 0) {
                String author = htmlEscape(mod.getAuthor());
                String website = htmlEscape(mod.getWebsite());
                if (author.length() > 0 || website.length() > 0) {
                    sb.append("<html>");
                    if (mod.getAuthor().length() > 0) {
                        sb.append(String.format("Author: %s<br>", author));
                    }
                    if (mod.getWebsite().length() > 0) {
                        sb.append(String.format("Website: <a href=\"%1$s\">%1$s</a><br>", website));
                    }
                    sb.append("</html>");
                }
            } else {
                sb.append("<html><b>");
                sb.append(htmlEscape(mod.getName()));
                sb.append(" cannot be applied:</b><br>");
                for (String s : errors) {
                    sb.append("&nbsp;");
                    sb.append(s);
                    sb.append("<br>");
                }
                sb.append("</html>");
            }
            setToolTipText(sb.length() == 0 ? null : sb.toString());

            setOpaque(true);
            return this;
        }
    }

    private class CopyToClipboardListener implements ActionListener {
        JTextArea textArea;

        public CopyToClipboardListener(JTextArea textArea) {
            this.textArea = textArea;
        }

        public void actionPerformed(ActionEvent e) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection("[code]\n" + textArea.getText() + "[/code]\n"), null
            );
        }
    }
}
