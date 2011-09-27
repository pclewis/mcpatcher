package com.pclewis.mcpatcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

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
        origFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        file.add(origFile);

        outputFile = new JMenuItem("Select output file...");
        outputFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
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
        addMod.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        mods.add(addMod);

        removeMod = new JMenuItem("Remove");
        removeMod.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        mods.add(removeMod);

        mods.addSeparator();

        moveUp = new JMenuItem("Move up");
        moveUp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        mods.add(moveUp);

        moveDown = new JMenuItem("Move down");
        moveDown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        mods.add(moveDown);

        mods.addSeparator();

        save = new JMenuItem("Save profile...");
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        mods.add(save);

        load = new JMenuItem("Load profile...");
        load.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        mods.add(load);

        game = new JMenu("Game");
        game.setMnemonic('G');
        menuBar.add(game);

        patch = new JMenuItem("Patch");
        patch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        game.add(patch);

        unpatch = new JMenuItem("Unpatch");
        unpatch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        game.add(unpatch);

        test = new JMenuItem("Test Minecraft");
        test.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        game.add(test);
    }

    void updateControls(boolean busy) {
        file.setEnabled(!busy);
        mods.setEnabled(!busy);
        game.setEnabled(!busy);

        origFile.setEnabled(mainForm.origBrowseButton.isEnabled());
        outputFile.setEnabled(mainForm.outputBrowseButton.isEnabled());
        patch.setEnabled(mainForm.patchButton.isEnabled());
        unpatch.setEnabled(mainForm.undoButton.isEnabled());
        test.setEnabled(mainForm.testButton.isEnabled());
    }
}
