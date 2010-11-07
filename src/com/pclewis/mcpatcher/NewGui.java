package com.pclewis.mcpatcher;

import com.jidesoft.swing.CheckBoxList;
import com.jidesoft.swing.JideBoxLayout;
import com.jidesoft.swing.MultilineLabel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.LinkedList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class NewGui implements ActionListener {
    private static Logger logger = Logger.getLogger("com.pclewis.mcpatcher.NewGui");

    private JPanel mainPanel;
    private JButton configureButton;
    private JButton uninstallButton;
    private JButton minecraftFolderButton;
    private JButton runMinecraftButton;
    private JButton patchButton;
    private JButton installNewButton;
    private JButton checkForUpdateButton;
    private JPanel infoPanel;
    private JProgressBar progressBar1;
    private JTextArea logArea;
    private CheckBoxList modList;
    private JFrame frame;
    ModListModel modListModel;
    LinkedList<Mod> mods;

    public NewGui() {
        this( new JFrame("Minecraft Patcher") );
        frame.setContentPane(this.getMainPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        minecraftFolderButton.addActionListener(this);
        ((DefaultCaret)logArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        Logger.getLogger("com.pclewis.mcpatcher").addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logArea.append(String.format("%tT %s %s: %s\n", record.getMillis(), record.getLevel(),
                        record.getSourceClassName(), record.getMessage()));
            }

            @Override public void flush() { }

            @Override public void close() throws SecurityException { }
        });

        modListModel = new ModListModel();
        modList.setModel(modListModel);

        modList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if(!e.getValueIsAdjusting()) {
                    ModInfo info = modListModel.getMod(modList.getSelectedIndex()).getModInfo();
                    logger.info(info.toString());
                    clearInfo();
                    setDescription(info.description(), info.author(), info.version());

                    // repack and redisplay
                    infoPanel.setPreferredSize(infoPanel.getPreferredSize());
                    infoPanel.validate();
                }
            }
        });
    }

    public NewGui(JFrame frame) {
        this.frame = frame;
        createMenu();
    }

    public void show() {
        frame.setLocationRelativeTo( null );
        frame.setVisible(true);
    }

    public void addMod(Mod mod) {
        modListModel.addElement( mod );
    }

    public void setDescription(String description, String author, String version) {
        addInfo("Description", description);
        addInfo("Author", author);
        addInfo("Version", version);
    }

    public void addInfo(String labelText, String text) {
        infoPanel.remove( infoPanel.getComponentCount() - 1 );
        JLabel label = new JLabel(labelText, JLabel.LEFT);
        Font f = label.getFont();
        label.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
        infoPanel.add(label, JideBoxLayout.FLEXIBLE);

        MultilineLabel ml = new MultilineLabel(text);
        ml.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 0));
        ml.setFont(f.deriveFont(f.getStyle() & ~Font.BOLD));
        infoPanel.add(ml, JideBoxLayout.FLEXIBLE);
        infoPanel.add(new JLabel(""), JideBoxLayout.VARY);
    }

    public void clearInfo() {
        infoPanel.removeAll();
        infoPanel.add(new JLabel(""), JideBoxLayout.VARY);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void createUIComponents() {
        infoPanel = new JPanel();
        infoPanel.setLayout(new JideBoxLayout(infoPanel, JideBoxLayout.PAGE_AXIS));
        infoPanel.add( new JLabel(""), JideBoxLayout.VARY );
    }


    private void addMenuItem(JMenu menu, String title, Icon icon, int mnemonic) {
        JMenuItem item = new JMenuItem(title, icon);
        item.setMnemonic(mnemonic);
        item.addActionListener(this);
        menu.add(item);
    }

    private void addRadioItem(JMenu menu, ButtonGroup bg, String title, String action, Icon icon, int mnemonic, boolean selected) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(title, icon);
        item.setMnemonic(mnemonic);
        item.setActionCommand(action);
        item.addActionListener(this);
        menu.add(item);
        bg.add(item);
        item.setSelected(selected);
    }

    private void addLogLevel(JMenu menu, ButtonGroup bg, Level level, int mnemonic) {
        addRadioItem(menu, bg, level.getLocalizedName(), "log_level_" + level.getName(),
                null, mnemonic, level == Level.SEVERE);
    }

    private void createMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);

        addMenuItem(menu, "Open Config", UIManager.getIcon("Tree.openIcon"), KeyEvent.VK_O);
        addMenuItem(menu, "Save Config", UIManager.getIcon("FileView.floppyDriveIcon"), KeyEvent.VK_S);
        menuBar.add(menu);

        menu = new JMenu("Advanced");
        menu.setMnemonic(KeyEvent.VK_A);

        addMenuItem(menu, "Create Moddable Jar", null, KeyEvent.VK_C );
        JMenu subMenu = new JMenu("Log Level");
        ButtonGroup bg = new ButtonGroup();
        addLogLevel(subMenu, bg, Level.SEVERE,  KeyEvent.VK_S);
        addLogLevel(subMenu, bg, Level.WARNING, KeyEvent.VK_W);
        addLogLevel(subMenu, bg, Level.INFO,    KeyEvent.VK_I);
        addLogLevel(subMenu, bg, Level.CONFIG,  KeyEvent.VK_C);
        addLogLevel(subMenu, bg, Level.FINE,    KeyEvent.VK_F);
        addLogLevel(subMenu, bg, Level.FINER,   KeyEvent.VK_R);
        addLogLevel(subMenu, bg, Level.FINEST,  KeyEvent.VK_T);
        menu.add(subMenu);
        menuBar.add(menu);

        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);

        addMenuItem(menu, "Report Bug", null, KeyEvent.VK_B );
        addMenuItem(menu, "Log Window", null, KeyEvent.VK_L );
        addMenuItem(menu, "Check For Updates", null, KeyEvent.VK_C );
        menu.addSeparator();
        addMenuItem(menu, "About", null, KeyEvent.VK_A );
        menuBar.add(menu);
        
        frame.setJMenuBar(menuBar);
    }

    private static void openMinecraftFolder() {
        ProcessBuilder pb = new ProcessBuilder(Util.isWindows() ? "explorer" : "open",
                new File(Util.getMinecraftPath()).getAbsolutePath());
        try {
            Process p = pb.start();
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Can't open folder", e);
        }
    }

    private static void runMinecraft() {
        // FIXME
    }

    private void setLogLevel(Level level) {
        for(Handler h : Logger.getLogger("").getHandlers()) {
            h.setLevel(level);
        }
        Logger.getLogger("com.pclewis").setLevel(level);
        logger.config("Set log level to " + level.getName());
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if(command!=null) {
            if(command.equals("mc_folder")) {
                openMinecraftFolder();
            } else if (command.equals("mc_run")) {
                runMinecraft();
            } else if (command.startsWith("log_level_")) {
                setLogLevel( Level.parse(command.replace("log_level_", "")) );
            }
        }
    }
}
