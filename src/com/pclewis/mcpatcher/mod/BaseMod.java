package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BaseMod extends Mod {
    public BaseMod() {
        name = "__Base";
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.0";
        configPanel = new Config();

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcher.UTILS_CLASS));
    }

    public class Config extends ModConfigPanel {
        private JPanel panel;
        private JTextField heapSizeText;
        private JCheckBox debugCheckBox;

        Config() {
            debugCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(null, MCPatcherUtils.TAG_DEBUG, debugCheckBox.isSelected());
                }
            });
        }

        @Override
        public JPanel getPanel() {
            return panel;
        }

        @Override
        public String getPanelName() {
            return "General options";
        }

        @Override
        public void load() {
            heapSizeText.setText("" + MCPatcherUtils.getInt(MCPatcherUtils.TAG_JAVA_HEAP_SIZE, 1024));
            debugCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.TAG_DEBUG, false));
        }

        @Override
        public void save() {
            try {
                MCPatcherUtils.set(null, MCPatcherUtils.TAG_JAVA_HEAP_SIZE, Integer.parseInt(heapSizeText.getText()));
            } catch (Exception e) {
            }
        }
    }
}
