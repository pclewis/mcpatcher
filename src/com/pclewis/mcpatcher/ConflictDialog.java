package com.pclewis.mcpatcher;

import javax.swing.*;

class ConflictDialog extends JDialog {
    private JPanel contentPane;
    private JTextArea conflicts;

    ConflictDialog(String s) {
        setContentPane(contentPane);
        setModal(true);
        conflicts.setText(s);
    }
}
