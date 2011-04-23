package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import com.pclewis.mcpatcher.ModConfigPanel;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class HDTextureConfig extends ModConfigPanel {
    private static final String MOD_CFG_NAME = "HDTexture";

    private JPanel panel;
    private JComboBox waterCombo;
    private JComboBox lavaCombo;
    private JComboBox fireCombo;
    private JComboBox portalCombo;

    HDTextureConfig() {
        waterCombo.addItemListener(new AnimComboListener(waterCombo, "Water"));
        lavaCombo.addItemListener(new AnimComboListener(lavaCombo, "Lava"));
        fireCombo.addItemListener(new AnimComboListener(fireCombo, "Fire"));

        portalCombo.addItem("Default");
        portalCombo.addItem("Custom Animated");
        portalCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    switch (portalCombo.getSelectedIndex()) {
                        default:
                            MCPatcherUtils.set(MOD_CFG_NAME, "customPortal", false);
                            break;

                        case 1:
                            MCPatcherUtils.set(MOD_CFG_NAME, "customPortal", true);
                            break;
                    }
                }
            }
        });
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    public void load() {
        ((AnimComboListener) (waterCombo.getItemListeners()[0])).load();
        ((AnimComboListener) (lavaCombo.getItemListeners()[0])).load();
        ((AnimComboListener) (fireCombo.getItemListeners()[0])).load();

        if (MCPatcherUtils.getBoolean(MOD_CFG_NAME, "customPortal", true)) {
            portalCombo.setSelectedIndex(1);
        } else {
            portalCombo.setSelectedIndex(0);
        }
    }

    @Override
    public void save() {
    }

    private static class AnimComboListener implements ItemListener {
        private static final int OPT_DEFAULT = 0;
        private static final int OPT_NOT_ANIMATED = 1;
        private static final int OPT_CUSTOM_ANIMATED = 2;

        final private JComboBox comboBox;
        final private String animatedTag;
        final private String customTag;

        public AnimComboListener(JComboBox comboBox, String tag) {
            this.comboBox = comboBox;
            customTag = "custom" + tag;
            animatedTag = "animated" + tag;
            comboBox.addItem("Default");
            comboBox.addItem("Not Animated");
            comboBox.addItem("Custom Animated");
        }

        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                boolean custom;
                boolean anim;
                switch (comboBox.getSelectedIndex()) {
                    case OPT_DEFAULT:
                        custom = false;
                        anim = true;
                        break;

                    case OPT_NOT_ANIMATED:
                        custom = false;
                        anim = false;
                        break;

                    case OPT_CUSTOM_ANIMATED:
                        custom = true;
                        anim = true;
                        break;

                    default:
                        return;
                }
                MCPatcherUtils.set(MOD_CFG_NAME, customTag, custom);
                MCPatcherUtils.set(MOD_CFG_NAME, animatedTag, anim);
            }
        }

        public void load() {
            if (MCPatcherUtils.getBoolean(MOD_CFG_NAME, customTag, true)) {
                comboBox.setSelectedIndex(OPT_CUSTOM_ANIMATED);
            } else if (MCPatcherUtils.getBoolean(MOD_CFG_NAME, animatedTag, true)) {
                comboBox.setSelectedIndex(OPT_DEFAULT);
            } else {
                comboBox.setSelectedIndex(OPT_NOT_ANIMATED);
            }
        }
    }
}
