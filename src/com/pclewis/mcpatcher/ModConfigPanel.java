package com.pclewis.mcpatcher;

import javax.swing.*;

abstract public class ModConfigPanel {
    abstract public JPanel getPanel();

    public String getPanelName() {
        return null;
    }

    abstract public void load();
    abstract public void save();
}
