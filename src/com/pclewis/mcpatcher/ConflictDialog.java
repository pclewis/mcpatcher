package com.pclewis.mcpatcher;

import javax.swing.*;
import java.util.*;

class ConflictDialog {
    private JPanel contentPane;
    private JTextArea conflictsText;

    static String getText(HashMap<String, ArrayList<Mod>> conflicts) {
        HashMap<ArrayList<Mod>, ArrayList<String>> conflictGroups = new HashMap<ArrayList<Mod>, ArrayList<String>>();
        for (Map.Entry<String, ArrayList<Mod>> entry : conflicts.entrySet()) {
            String filename = entry.getKey();
            ArrayList<Mod> mods = entry.getValue();
            ArrayList<String> fileList = conflictGroups.get(mods);
            if (fileList == null) {
                fileList = new ArrayList<String>();
                conflictGroups.put(mods, fileList);
            }
            fileList.add(filename);
        }
        StringBuilder message = new StringBuilder();
        ArrayList<Map.Entry<ArrayList<Mod>, ArrayList<String>>> conflictEntries = new ArrayList<Map.Entry<ArrayList<Mod>, ArrayList<String>>>();
        conflictEntries.addAll(conflictGroups.entrySet());
        Collections.sort(conflictEntries, new Comparator<Map.Entry<ArrayList<Mod>, ArrayList<String>>>() {
            public int compare(Map.Entry<ArrayList<Mod>, ArrayList<String>> o1, Map.Entry<ArrayList<Mod>, ArrayList<String>> o2) {
                ArrayList<Mod> a1 = o1.getKey();
                ArrayList<Mod> a2 = o2.getKey();
                for (int i = 0; i < a1.size() && i < a2.size(); i++) {
                    Mod mod1 = a1.get(i);
                    Mod mod2 = a2.get(i);
                    if (mod1 != mod2) {
                        return mod1.getName().compareTo(mod2.getName());
                    }
                }
                return a1.size() - a2.size();
            }
        });
        for (Map.Entry<ArrayList<Mod>, ArrayList<String>> entry : conflictEntries) {
            ArrayList<Mod> mods = entry.getKey();
            ArrayList<String> filenames = entry.getValue();
            Collections.sort(filenames);
            for (Mod mod : mods) {
                message.append(mod.getName()).append('\n');
            }
            for (String filename : filenames) {
                message.append("    ").append(filename).append('\n');
            }
            message.append("Only the files from ").append(mods.get(mods.size() - 1).getName()).append(" will be used.\n\n");
        }
        return message.toString().trim();
    }

    ConflictDialog(HashMap<String, ArrayList<Mod>> conflicts) {
        String text = getText(conflicts);
        conflictsText.setRows(Math.max(6, Math.min(24, text.split("\n").length + 1)));
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
