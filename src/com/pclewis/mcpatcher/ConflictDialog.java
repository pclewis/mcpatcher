package com.pclewis.mcpatcher;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class ConflictDialog {
    private JPanel contentPane;
    private JTextArea conflictsText;

    ConflictDialog(HashMap<String, ArrayList<Mod>> conflicts) {
        HashMap<ArrayList<Mod>, ArrayList<String>> c = new HashMap<ArrayList<Mod>, ArrayList<String>>();
        for (Map.Entry<String, ArrayList<Mod>> entry : conflicts.entrySet()) {
            String filename = entry.getKey();
            ArrayList<Mod> mods = entry.getValue();
            ArrayList<String> fileList = c.get(mods);
            if (fileList == null) {
                fileList = new ArrayList<String>();
                c.put(mods, fileList);
            }
            fileList.add(filename);
        }
        StringBuilder message = new StringBuilder();
        for (Map.Entry<ArrayList<Mod>, ArrayList<String>> entry : c.entrySet()) {
            ArrayList<Mod> mods = entry.getKey();
            ArrayList<String> filenames = entry.getValue();
            Collections.sort(filenames);
            for (Mod mod : mods) {
                message.append(mod.getName()).append('\n');
            }
            for (String filename : filenames) {
                message.append("    ").append(filename).append('\n');
            }
            message.append('\n');
        }
        String text = message.toString().trim();
        conflictsText.setRows(Math.max(6, Math.min(24, text.split("\n").length)));
        conflictsText.setText(text);
    }

    int getResult() {
        JOptionPane pane = new JOptionPane(contentPane, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
        JDialog dialog = pane.createDialog("Mod conflict detected");
        dialog.setModal(true);
        dialog.setResizable(true);
        dialog.setVisible(true);
        Object result = pane.getValue();
        return result instanceof Integer ? (Integer) result : JOptionPane.NO_OPTION;
    }
}
