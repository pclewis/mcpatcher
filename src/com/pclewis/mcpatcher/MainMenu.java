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

    JMenu game;
    JMenuItem patch;
    JMenuItem unpatch;
    JMenuItem test;

    JMenu about;

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

        menuBar.add(Box.createHorizontalGlue());

        about = new JMenu("About");
        about.setMnemonic('A');
        about.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        menuBar.add(about);
    }

    void update(boolean busy) {
        menuBar.setEnabled(!busy);
    }
}
