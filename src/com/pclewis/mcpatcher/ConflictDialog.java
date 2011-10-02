package com.pclewis.mcpatcher;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class ConflictDialog extends JDialog {
    private JPanel contentPane;
    private JTextArea conflictsText;

    ConflictDialog(HashMap<String, ArrayList<Mod>> conflicts) {
        setContentPane(contentPane);
        setModal(true);
        setResizable(true);
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
        conflictsText.setText(message.toString().trim());
    }
}
