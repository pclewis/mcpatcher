package com.pclewis.mcpatcher;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipTreeDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JScrollPane treePane;

    private JTree tree;

    private ZipFile zipFile;
    private String prefix;

    public ZipTreeDialog(ZipFile zipFile, String prefix) {
        this.zipFile = zipFile;
        this.prefix = prefix;

        setContentPane(contentPane);
        setTitle("Select folder to add to minecraft.jar");
        setMinimumSize(new Dimension(256, 256));
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

        updateTree();
    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private void updateTree() {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode("/");
        treePane.removeAll();
        tree = new JTree(node);
        for (ZipEntry entry : Collections.list(zipFile.entries())) {
            if (!entry.isDirectory()) {
                continue;
            }
            String name = entry.getName().replaceFirst("/$", "");
            String baseName = name.replaceAll(".*/", "");
            Logger.log(Logger.LOG_GUI, "%s", name);
        }
        treePane.add(tree);
    }
}
