package com.pclewis.mcpatcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

class MainMenu {
    private MainForm mainForm;

    JMenuBar menuBar;

    JMenu fileMenu;
    JMenuItem origFile;
    JMenuItem outputFile;
    JMenuItem exit;

    MainMenu(MainForm mainForm1) {
        mainForm = mainForm1;

        menuBar = new JMenuBar();

        fileMenu = new JMenu("File");

        origFile = new JMenuItem("Select input file");
        origFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });

        outputFile = new JMenuItem("Select output file");
        outputFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });

        exit = new JMenuItem("Exit");
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                    new WindowEvent(mainForm.frame, WindowEvent.WINDOW_CLOSING)
                );
            }
        });

        menuBar.add(fileMenu);

        fileMenu.add(origFile);
        fileMenu.add(outputFile);
        fileMenu.addSeparator();
        fileMenu.add(exit);
    }

    void update(boolean busy) {
        menuBar.setEnabled(!busy);
    }
}
