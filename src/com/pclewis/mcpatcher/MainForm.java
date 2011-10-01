package com.pclewis.mcpatcher;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

class MainForm {
    private static final int TAB_MODS = 0;
    private static final int TAB_OPTIONS = 1;
    private static final int TAB_LOG = 2;
    private static final int TAB_CLASS_MAP = 3;
    private static final int TAB_PATCH_SUMMARY = 4;

    private static final Color MOD_BUSY_COLOR = new Color(192, 192, 192);
    private static final String MOD_DESC_FORMAT = "<html>" +
        "<table border=\"0\" cellspacing=\"0\" cellpadding=\"1\" style=\"padding-top: 2px; padding-bottom: 2px;\" width=\"%1$d\"><tr>" +
        "<td align=\"left\"><font size=\"5\">%2$s</font></td>" +
        "<td align=\"right\">%3$s</td>" +
        "</tr>" +
        "<tr><td colspan=\"2\" style=\"font-weight: normal; font-style: italic;\">%4$s%5$s</td></tr>" +
        "</table>" +
        "</html>";

    private JPanel mainPanel;
    JFrame frame;
    private int frameWidth = 518;

    private MainMenu mainMenu;

    private JTextField origField;
    JButton origBrowseButton;
    private JTextField outputField;
    JButton outputBrowseButton;
    JButton testButton;
    JButton patchButton;
    JButton undoButton;
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
    JButton upButton;
    JButton addButton;
    JButton downButton;
    JButton removeButton;

    private AddModDialog addModDialog;

    private boolean shift;

    private boolean busy = true;
    private Thread workerThread = null;

    public MainForm() {
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue() {
            private boolean reenter;

            protected void dispatchEvent(AWTEvent event) {
                try {
                    if (event instanceof KeyEvent && (event.getID() == KeyEvent.KEY_PRESSED || event.getID() == KeyEvent.KEY_RELEASED)) {
                        KeyEvent keyEvent = (KeyEvent) event;
                        if (keyEvent.getKeyCode() == KeyEvent.VK_SHIFT) {
                            shift = (event.getID() == KeyEvent.KEY_PRESSED);
                        }
                    }
                    super.dispatchEvent(event);
                } catch (Throwable e) {
                    Logger.log(Logger.LOG_MAIN);
                    Logger.log(Logger.LOG_MAIN, "Unexpected error while handling UI event %s", event.toString());
                    e.printStackTrace();
                    if (!reenter) {
                        reenter = true;
                        try {
                            setBusy(false);
                            tabbedPane.setSelectedIndex(TAB_LOG);
                            updateActiveTab();
                            cancelWorker();
                        } catch (Throwable e1) {
                            e.printStackTrace();
                        } finally {
                            reenter = false;
                        }
                    }
                }
            }
        });

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
                MCPatcher.saveProperties();
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
                        AddModDialog.modDir = null;
                        MCPatcher.saveProperties();
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
                    if (e.getClickCount() == 2) {
                        if (mod instanceof ExternalMod) {
                            boolean oldEnabled = mod.isEnabled();
                            addModDialog = new AddModDialog((ExternalMod) mod);
                            addModDialog.setLocationRelativeTo(frame);
                            addModDialog.setVisible(true);
                            Mod newMod = addModDialog.getMod();
                            if (newMod != null) {
                                newMod.setEnabled(oldEnabled);
                                int newRow = MCPatcher.modList.replace(mod, newMod);
                                modTable.addRowSelectionInterval(newRow, newRow);
                                AbstractTableModel model = (AbstractTableModel) modTable.getModel();
                                ModTextRenderer renderer = (ModTextRenderer) modTable.getColumnModel().getColumn(1).getCellRenderer();
                                renderer.resetRowHeights();
                                model.fireTableDataChanged();
                            }
                        }
                    }
                }
                super.mouseClicked(e);
            }
        });

        upButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = modTable.getSelectedRow();
                if (row >= 0) {
                    int newRow = MCPatcher.modList.moveUp(row, shift);
                    modTable.clearSelection();
                    AbstractTableModel model = (AbstractTableModel) modTable.getModel();
                    model.fireTableRowsUpdated(Math.min(row, newRow), Math.max(row, newRow));
                    ModTextRenderer renderer = (ModTextRenderer) modTable.getColumnModel().getColumn(1).getCellRenderer();
                    renderer.resetRowHeights();
                    modTable.addRowSelectionInterval(newRow, newRow);
                }
            }
        });

        downButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = modTable.getSelectedRow();
                if (row >= 0) {
                    int newRow = MCPatcher.modList.moveDown(row, shift);
                    modTable.clearSelection();
                    AbstractTableModel model = (AbstractTableModel) modTable.getModel();
                    model.fireTableRowsUpdated(Math.min(row, newRow), Math.max(row, newRow));
                    ModTextRenderer renderer = (ModTextRenderer) modTable.getColumnModel().getColumn(1).getCellRenderer();
                    renderer.resetRowHeights();
                    modTable.addRowSelectionInterval(newRow, newRow);
                }
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    addModDialog = new AddModDialog();
                    if (!addModDialog.showBrowseDialog()) {
                        return;
                    }
                    addModDialog.setLocationRelativeTo(frame);
                    addModDialog.setVisible(true);
                    Mod mod = addModDialog.getMod();
                    if (mod != null) {
                        int row = MCPatcher.modList.addFirst(mod);
                        mod.setEnabled(true);
                        modTable.clearSelection();
                        AbstractTableModel model = (AbstractTableModel) modTable.getModel();
                        model.fireTableRowsInserted(row, row);
                        ModTextRenderer renderer = (ModTextRenderer) modTable.getColumnModel().getColumn(1).getCellRenderer();
                        renderer.resetRowHeights();
                    }
                } catch (Throwable e1) {
                    Logger.log(e1);
                } finally {
                    hideDialog();
                    updateControls();
                }
            }

            private void hideDialog() {
                if (addModDialog != null) {
                    addModDialog.setVisible(false);
                    addModDialog.dispose();
                    addModDialog = null;
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = modTable.getSelectedRow();
                Mod mod = (Mod) modTable.getModel().getValueAt(row, 0);
                if (mod instanceof ExternalMod) {
                    MCPatcher.modList.remove(mod);
                    modTable.clearSelection();
                    AbstractTableModel model = (AbstractTableModel) modTable.getModel();
                    model.fireTableRowsDeleted(row, row);
                    if (row >= model.getRowCount()) {
                        row--;
                    }
                    modTable.addRowSelectionInterval(row, row);
                    ModTextRenderer renderer = (ModTextRenderer) modTable.getColumnModel().getColumn(1).getCellRenderer();
                    renderer.resetRowHeights();
                }
            }
        });

        patchButton.addActionListener(new ActionListener() {
            class PatchThread implements Runnable {
                public void run() {
                    boolean patchOk = true;
                    ArrayList<String> conflicts = MCPatcher.getConflicts();
                    if (conflicts.size() > 0) {
                        StringBuilder message = new StringBuilder();
                        for (String s : conflicts) {
                            message.append(s).append('\n');
                        }
                        ConflictDialog dialog = new ConflictDialog(message.toString());
                        int result = JOptionPane.showConfirmDialog(
                            frame, dialog.getContentPane(), "Mod conflict detected", JOptionPane.YES_NO_OPTION
                        );
                        if (result != JOptionPane.YES_OPTION) {
                            patchOk = false;
                        }
                    }
                    if (patchOk && !MCPatcher.patch()) {
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
                MCPatcher.saveProperties();
                runWorker(new MinecraftThread());
            }
        });

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateActiveTab();
            }
        });

        ((DefaultCaret) logText.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JTextAreaPrintStream output = new JTextAreaPrintStream(logText);
        System.setOut(output);
        System.setErr(output);
        copyLogButton.addActionListener(new CopyToClipboardListener(logText));

        ((DefaultCaret) classMap.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        copyClassMapButton.addActionListener(new CopyToClipboardListener(classMap));

        ((DefaultCaret) patchResults.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        copyPatchResultsButton.addActionListener(new CopyToClipboardListener(patchResults));

        mainMenu = new MainMenu(this);
        frame.setJMenuBar(mainMenu.menuBar);
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
        String currentProfile = MCPatcherUtils.config.getConfigValue(Config.TAG_SELECTED_PROFILE);
        if (currentProfile == null || currentProfile.equals("")) {
            frame.setTitle("MCPatcher " + MCPatcher.VERSION_STRING);
        } else {
            frame.setTitle("MCPatcher " + MCPatcher.VERSION_STRING + " [" + currentProfile + "]");
        }
        if (MCPatcher.minecraft == null) {
            origField.setText("");
            outputField.setText("");
        } else {
            origField.setText(MCPatcher.minecraft.getInputFile().getPath());
            outputField.setText(MCPatcher.minecraft.getOutputFile().getPath());
        }
        origField.setToolTipText(origField.getText());
        outputField.setToolTipText(outputField.getText());
        boolean outputSet = !outputField.getText().equals("");
        File orig = new File(origField.getText());
        File output = new File(outputField.getText());
        boolean origOk = orig.exists();
        boolean outputOk = output.exists();
        origBrowseButton.setEnabled(!busy);
        outputBrowseButton.setEnabled(!busy);
        modTable.setEnabled(!busy && origOk && outputSet);
        upButton.setEnabled(!busy);
        downButton.setEnabled(!busy);
        addButton.setEnabled(!busy);
        removeButton.setEnabled(!busy);
        refreshButton.setEnabled(!busy);
        testButton.setEnabled(!busy && outputOk && MCPatcherUtils.getMinecraftPath().equals(MCPatcherUtils.getDefaultGameDir()));
        patchButton.setEnabled(!busy && origOk && !output.equals(orig));
        undoButton.setEnabled(!busy && origOk && !output.equals(orig));
        tabbedPane.setEnabled(!busy);

        updateActiveTab();
        mainMenu.updateControls(busy);
    }

    private void updateActiveTab() {
        if (tabbedPane.getSelectedIndex() != TAB_OPTIONS) {
            saveOptions();
        }
        switch (tabbedPane.getSelectedIndex()) {
            case TAB_OPTIONS:
                loadOptions();
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

    private void saveOptions() {
        for (Mod mod : MCPatcher.modList.getAll()) {
            if (mod.configPanel != null) {
                try {
                    mod.configPanel.save();
                } catch (Throwable e) {
                    Logger.log(e);
                }
            }
        }
        MCPatcher.saveProperties();
    }

    private void loadOptions() {
        optionsPanel.removeAll();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        for (Mod mod : MCPatcher.modList.getAll()) {
            try {
                if (mod.configPanel == null) {
                    continue;
                }
                mod.loadOptions();
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
        modTable.setModel(new DefaultTableModel() {
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
                if (modList == null || rowIndex < 0) {
                    return null;
                } else {
                    Vector<Mod> visible = modList.getVisible();
                    return rowIndex < visible.size() ? visible.get(rowIndex) : null;
                }
            }

            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            }
        });
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
                            "Your minecraft.jar appears to be an older or preview version\n" +
                                "or is already modded.  This may work fine, but if the game\n" +
                                "crashes or you have problems patching:\n" +
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
        modTable.getColumnModel().getColumn(0).setCellRenderer(new ModCheckBoxRenderer());
        modTable.getColumnModel().getColumn(1).setCellRenderer(new ModTextRenderer());
        AbstractTableModel model = (AbstractTableModel) modTable.getModel();
        model.fireTableDataChanged();
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
            return s == null ? "" : s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        }

        public void resetRowHeights() {
            rowSize.clear();
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Mod mod = (Mod) value;

            setText(String.format(MOD_DESC_FORMAT,
                Math.max(frameWidth - 75, 350),
                htmlEscape(mod.getName()),
                htmlEscape(mod.getVersion()),
                (ModList.isExperimental(mod.getName()) ? "<font color=\"red\">(Experimental)</font> " : ""),
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
                new StringSelection("[spoiler][code]\n" + textArea.getText() + "[/code][/spoiler]\n"), null
            );
        }
    }
}
