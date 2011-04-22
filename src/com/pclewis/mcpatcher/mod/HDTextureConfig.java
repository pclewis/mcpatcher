package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import com.pclewis.mcpatcher.ModConfigPanel;

import javax.swing.*;
import java.awt.*;
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
        waterCombo.addItem("Default");
        waterCombo.addItem("Not Animated");
        waterCombo.addItem("Custom Animated");
        waterCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    switch (waterCombo.getSelectedIndex()) {
                        default:
                            MCPatcherUtils.set(MOD_CFG_NAME, "customWater", false);
                            MCPatcherUtils.set(MOD_CFG_NAME, "animatedWater", true);
                            break;

                        case 1:
                            MCPatcherUtils.set(MOD_CFG_NAME, "customWater", false);
                            MCPatcherUtils.set(MOD_CFG_NAME, "animatedWater", false);
                            break;

                        case 2:
                            MCPatcherUtils.set(MOD_CFG_NAME, "customWater", true);
                            MCPatcherUtils.set(MOD_CFG_NAME, "animatedWater", true);
                            break;
                    }
                }
            }
        });

        lavaCombo.addItem("Default");
        lavaCombo.addItem("Not Animated");
        lavaCombo.addItem("Custom Animated");
        lavaCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    switch (lavaCombo.getSelectedIndex()) {
                        default:
                            MCPatcherUtils.set(MOD_CFG_NAME, "customLava", false);
                            MCPatcherUtils.set(MOD_CFG_NAME, "animatedLava", true);
                            break;

                        case 1:
                            MCPatcherUtils.set(MOD_CFG_NAME, "customLava", false);
                            MCPatcherUtils.set(MOD_CFG_NAME, "animatedLava", false);
                            break;

                        case 2:
                            MCPatcherUtils.set(MOD_CFG_NAME, "customLava", true);
                            MCPatcherUtils.set(MOD_CFG_NAME, "animatedLava", true);
                            break;
                    }
                }
            }
        });

        fireCombo.addItem("Default");
        fireCombo.addItem("Not Animated");
        fireCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    switch (fireCombo.getSelectedIndex()) {
                        default:
                            MCPatcherUtils.set(MOD_CFG_NAME, "animatedFire", true);
                            break;

                        case 1:
                            MCPatcherUtils.set(MOD_CFG_NAME, "animatedFire", false);
                            break;
                    }
                }
            }
        });

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
        if (MCPatcherUtils.getBoolean(MOD_CFG_NAME, "customWater", true)) {
            waterCombo.setSelectedIndex(2);
        } else if (MCPatcherUtils.getBoolean(MOD_CFG_NAME, "animatedWater", true)) {
            waterCombo.setSelectedIndex(0);
        } else {
            waterCombo.setSelectedIndex(1);
        }

        if (MCPatcherUtils.getBoolean(MOD_CFG_NAME, "customLava", true)) {
            lavaCombo.setSelectedIndex(2);
        } else if (MCPatcherUtils.getBoolean(MOD_CFG_NAME, "animatedLava", true)) {
            lavaCombo.setSelectedIndex(0);
        } else {
            lavaCombo.setSelectedIndex(1);
        }

        if (MCPatcherUtils.getBoolean(MOD_CFG_NAME, "animatedFire", true)) {
            fireCombo.setSelectedIndex(0);
        } else {
            fireCombo.setSelectedIndex(1);
        }

        if (MCPatcherUtils.getBoolean(MOD_CFG_NAME, "customPortal", true)) {
            portalCombo.setSelectedIndex(1);
        } else {
            portalCombo.setSelectedIndex(0);
        }
    }

    @Override
    public void save() {
    }
}
