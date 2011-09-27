package com.pclewis.mcpatcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;

class MainMenu {
    private MainForm mainForm;

    JMenuBar menuBar;

    JMenu file;
    JMenuItem origFile;
    JMenuItem outputFile;
    JMenuItem exit;

    JMenu mods;
    JMenuItem addMod;
    JMenuItem removeMod;
    JMenuItem moveUp;
    JMenuItem moveDown;
    JMenuItem save;
    JMenuItem load;

    JMenu game;
    JMenuItem patch;
    JMenuItem unpatch;
    JMenuItem test;

    MainMenu(MainForm mainForm1) {
        mainForm = mainForm1;

        menuBar = new JMenuBar();

        file = new JMenu("File");
        file.setMnemonic('F');
        menuBar.add(file);

        origFile = new JMenuItem("Select input file...");
        copyActionListener(origFile, mainForm.origBrowseButton);
        file.add(origFile);

        outputFile = new JMenuItem("Select output file...");
        copyActionListener(outputFile, mainForm.outputBrowseButton);
        file.add(outputFile);

        file.addSeparator();

        exit = new JMenuItem("Exit");
        exit.setMnemonic('x');
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                    new WindowEvent(mainForm.frame, WindowEvent.WINDOW_CLOSING)
                );
            }
        });
        file.add(exit);

        mods = new JMenu("Mods");
        mods.setMnemonic('M');
        menuBar.add(mods);

        addMod = new JMenuItem("Add...");
        copyActionListener(addMod, mainForm.addButton);
        mods.add(addMod);

        removeMod = new JMenuItem("Remove");
        copyActionListener(removeMod, mainForm.removeButton);
        mods.add(removeMod);

        mods.addSeparator();

        moveUp = new JMenuItem("Move up");
        copyActionListener(moveUp, mainForm.upButton);
        mods.add(moveUp);

        moveDown = new JMenuItem("Move down");
        copyActionListener(moveDown, mainForm.downButton);
        mods.add(moveDown);

        mods.addSeparator();

        save = new JMenuItem("Save profile...");
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        mods.add(save);

        load = new JMenu("Select profile");
        mods.add(load);

        game = new JMenu("Game");
        game.setMnemonic('G');
        menuBar.add(game);

        patch = new JMenuItem("Patch");
        copyActionListener(patch, mainForm.patchButton);
        game.add(patch);

        unpatch = new JMenuItem("Unpatch");
        copyActionListener(unpatch, mainForm.undoButton);
        game.add(unpatch);

        game.addSeparator();

        test = new JMenuItem("Test Minecraft");
        copyActionListener(test, mainForm.testButton);
        game.add(test);

        updateControls(true);
    }

    private static void copyActionListener(JMenuItem item, final JButton button) {
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (button.isEnabled()) {
                    for (ActionListener listener : button.getActionListeners()) {
                        listener.actionPerformed(e);
                    }
                }
            }
        });
    }

    void updateControls(boolean busy) {
        file.setEnabled(!busy);
        mods.setEnabled(!busy);
        game.setEnabled(!busy);

        origFile.setEnabled(mainForm.origBrowseButton.isEnabled());
        outputFile.setEnabled(mainForm.outputBrowseButton.isEnabled());
        addMod.setEnabled(mainForm.addButton.isEnabled());
        removeMod.setEnabled(mainForm.removeButton.isEnabled());
        moveUp.setEnabled(mainForm.upButton.isEnabled());
        moveDown.setEnabled(mainForm.downButton.isEnabled());
        patch.setEnabled(mainForm.patchButton.isEnabled());
        unpatch.setEnabled(mainForm.undoButton.isEnabled());
        test.setEnabled(mainForm.testButton.isEnabled());

        load.removeAll();
        if (!busy && MCPatcherUtils.config != null) {
            ArrayList<String> profiles = MCPatcherUtils.config.getProfiles();
            ButtonGroup buttonGroup = new ButtonGroup();
            final String currentProfile = MCPatcherUtils.config.getConfigValue(Config.TAG_SELECTED_PROFILE);
            for (final String profile : profiles) {
                JRadioButtonMenuItem item = new JRadioButtonMenuItem(profile, profile.equals(currentProfile));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        MCPatcherUtils.config.selectProfile(profile);
                        if (Config.isDefaultProfile(currentProfile) && Config.isDefaultProfile(profile)) {
                            String version = profile.replaceFirst("^Minecraft ", "");
                            File jar = MCPatcherUtils.getMinecraftPath("bin", "minecraft-" + version + ".jar");
                            if (jar.exists()) {
                                try {
                                    MCPatcher.setMinecraft(jar, false);
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                        MCPatcher.getAllMods();
                        mainForm.updateModList();
                    }
                });
                buttonGroup.add(item);
                load.add(item);
            }
        }
        if (load.getSubElements().length == 0) {
            JMenuItem item = new JMenuItem("(none)");
            item.setEnabled(false);
            load.add(item);
        } else {
            load.setEnabled(true);
        }
    }
}
