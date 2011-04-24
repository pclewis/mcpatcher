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

    private AnimationComboListener[] comboListeners;

    HDTextureConfig() {
        comboListeners = new AnimationComboListener[4];
        comboListeners[0] = new AnimationComboListener(waterCombo, "Water");
        comboListeners[1] = new AnimationComboListener(lavaCombo, "Lava");
        comboListeners[2] = new AnimationComboListener(fireCombo, "Fire");
        comboListeners[3] = new AnimationComboListener(portalCombo, "Portal");

        waterCombo.addItemListener(comboListeners[0]);
        lavaCombo.addItemListener(comboListeners[1]);
        fireCombo.addItemListener(comboListeners[2]);
        portalCombo.addItemListener(comboListeners[3]);
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    public void load() {
        for (AnimationComboListener listener : comboListeners) {
            listener.load();
        }
    }

    @Override
    public void save() {
    }

    private static class AnimationComboListener implements ItemListener {
        private static final int OPT_DEFAULT = 0;
        private static final int OPT_NOT_ANIMATED = 1;
        private static final int OPT_CUSTOM_ANIMATED = 2;

        final private JComboBox comboBox;
        final private String animatedTag;
        final private String customTag;

        public AnimationComboListener(JComboBox comboBox, String tag) {
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
