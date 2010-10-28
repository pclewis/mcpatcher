package com.pclewis.mcpatcher;

import com.jidesoft.swing.JideBoxLayout;
import com.jidesoft.swing.MultilineLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class NewGui implements ActionListener {
    private JPanel mainPanel;
    private JButton configureButton;
    private JButton uninstallButton;
    private JButton minecraftFolderButton;
    private JButton runMinecraftButton;
    private JButton patchButton;
    private JButton installNewButton;
    private JButton checkForUpdateButton;
    private JPanel infoPanel;
    private JFrame frame;

    public NewGui() {
        this( new JFrame("Minecraft Patcher") );
        frame.setContentPane(this.getMainPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
    }

    public NewGui(JFrame frame) {
        this.frame = frame;
        createMenu();
    }

    public void show() {
        frame.setLocationRelativeTo( null );
        frame.setVisible(true);
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

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void createUIComponents() {
        infoPanel = new JPanel();
        infoPanel.setLayout(new JideBoxLayout(infoPanel, JideBoxLayout.PAGE_AXIS));
        infoPanel.add( new JLabel(""), JideBoxLayout.VARY );
    }



    
    private void addMenuItem( JMenu menu, String title, Icon icon, int mnemonic) {
        JMenuItem item = new JMenuItem(title, icon);
        item.setMnemonic(mnemonic);
        item.addActionListener(this);
        menu.add(item);
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



    public void actionPerformed(ActionEvent e) {

    }
}
