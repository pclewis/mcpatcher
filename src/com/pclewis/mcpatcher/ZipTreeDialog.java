package com.pclewis.mcpatcher;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipTreeDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel treePanel;

    private JTree tree;

    private ZipFile zipFile;
    private String prefix;

    public ZipTreeDialog(ZipFile zipFile, String prefix) {
        this.zipFile = zipFile;
        this.prefix = prefix;

        setContentPane(contentPane);
        setTitle("Select folder to add to minecraft.jar");
        setMinimumSize(new Dimension(384, 384));
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

    private DefaultMutableTreeNode addPath(HashMap<String, DefaultMutableTreeNode> map, String path) {
        path = ("" + path).replaceFirst("/$", "");
        DefaultMutableTreeNode node = map.get(path);
        if (node != null) {
            return node;
        }
        if (path.equals("")) {
            node = new DefaultMutableTreeNode("/");
        } else {
            String dir = path.replaceFirst("[^/]+$", "");
            String baseName = path.replaceFirst(".*/", "");
            System.out.printf("%s - %s\n", dir, baseName);
            node = new DefaultMutableTreeNode(baseName);
            addPath(map, dir).add(node);
        }
        map.put(path, node);
        return node;
    }

    private void updateTree() {
        HashMap<String, DefaultMutableTreeNode> map = new HashMap<String, DefaultMutableTreeNode>();
        tree = new JTree(addPath(map, ""));
        for (ZipEntry entry : Collections.list(zipFile.entries())) {
            if (entry.isDirectory()) {
                addPath(map, entry.getName());
            }
        }
        tree.setRootVisible(true);
        tree.setExpandsSelectedPaths(true);
        tree.setSelectionPath(new TreePath("/"));
        treePanel.removeAll();
        treePanel.add(tree);
        treePanel.validate();
    }
}
