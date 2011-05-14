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

    public ZipTreeDialog(final ZipFile zipFile, String prefix) {
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
                for (ZipEntry entry : Collections.list(zipFile.entries())) {
                    String name = entry.getName();
                    if (entry.isDirectory() && name.startsWith(parent)) {
                        String suffix = name.substring(parent.length()).replaceFirst("/$", "");
                        if (!suffix.equals("") && !suffix.contains("/")) {
                            list.add(new ZipTreeNode(suffix, name));
                        }
                    }
                }
                return list;
            }
        });
        tree.setSelectionPath(new TreePath("/" + prefix));
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
            return compareTo(node.path);
        }

        public int compareTo(String path) {
            return this.path.compareTo(path);
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

    String getPrefix() {
        return prefix;
    }
}
