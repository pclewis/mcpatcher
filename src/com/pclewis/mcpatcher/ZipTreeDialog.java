package com.pclewis.mcpatcher;

import javax.swing.*;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipTreeDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTree tree;

    private String prefix;

    public ZipTreeDialog(final ZipFile zipFile) {
        prefix = "";

        setContentPane(contentPane);
        setTitle("Select folder to add to minecraft.jar");
        setMinimumSize(new Dimension(384, 384));
        setResizable(true);
        setModal(true);
        pack();
        getRootPane().setDefaultButton(buttonOK);

        // Some zip files are missing directory entries, so construct the list of directories here
        final ArrayList<String> dirs = new ArrayList<String>();
        for (ZipEntry entry : Collections.list(zipFile.entries())) {
            String name = entry.getName();
            if (!entry.isDirectory()) {
                name = name.replaceFirst("/?[^/]+$", "/");
            }
            if (!dirs.contains(name)) {
                dirs.add(name);
            }
        }
        Collections.sort(dirs);

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

        tree.setModel(new TreeModel() {
            public Object getRoot() {
                return new ZipTreeNode("/", "");
            }

            public Object getChild(Object parent, int index) {
                return getChildren(parent).get(index);
            }

            public int getChildCount(Object parent) {
                return getChildren(parent).size();
            }

            public boolean isLeaf(Object node) {
                return false;
            }

            public void valueForPathChanged(TreePath path, Object newValue) {
            }

            public int getIndexOfChild(Object parent, Object child) {
                return getChildren(parent).indexOf((ZipTreeNode) child);
            }

            public void addTreeModelListener(TreeModelListener l) {
            }

            public void removeTreeModelListener(TreeModelListener l) {
            }

            private ArrayList<ZipTreeNode> getChildren(Object object) {
                String parent = ((ZipTreeNode) object).path;
                ArrayList<ZipTreeNode> list = new ArrayList<ZipTreeNode>();
                for (String name : dirs) {
                    if (name.startsWith(parent)) {
                        String suffix = name.substring(parent.length()).replaceFirst("/$", "");
                        if (!suffix.equals("") && !suffix.contains("/")) {
                            list.add(new ZipTreeNode(suffix, name));
                        }
                    }
                }
                return list;
            }
        });
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        tree.setSelectionRow(0);
    }

    static class ZipTreeNode {
        String label;
        String path;

        ZipTreeNode(String label, String path) {
            this.label = label;
            this.path = path;
        }

        public String toString() {
            return label;
        }

        public int compareTo(ZipTreeNode node) {
            return this.path.compareTo(node.path);
        }

        public boolean equals(ZipTreeNode node) {
            return compareTo(node) == 0;
        }
    }

    private void onOK() {
        TreePath path = tree.getSelectionPath();
        prefix = ((ZipTreeNode) path.getLastPathComponent()).path;
        dispose();
    }

    private void onCancel() {
        prefix = null;
        dispose();
    }

    boolean hasSubfolders() {
        if (tree.getModel().getChildCount(tree.getModel().getRoot()) > 0) {
            return true;
        } else {
            prefix = "";
            return false;
        }
    }

    String getPrefix() {
        return prefix;
    }
}
