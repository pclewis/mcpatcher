package com.pclewis.mcpatcher;

import javax.swing.*;

/**
 * Class that defines a GUI configuration screen for the mod.
 */
abstract public class ModConfigPanel {
    /**
     * Called by MCPatcher to get the mod's top-level GUI panel.
     *
     * @return JPanel
     */
    abstract public JPanel getPanel();

    /**
     * Can be overridden to specify a different name to be used in the border around the
     * mod's configuration UI.  If null, mod.getName() is used.
     *
     * @return String name
     * @see com.pclewis.mcpatcher.Mod#getName()
     */
    public String getPanelName() {
        return null;
    }

    /**
     * Called by MCPatcher whenever the user switches <i>to</i> the Options panel.  Use this
     * to load the current settings into the UI.
     *
     * @see MCPatcherUtils#getString(String, String, Object)
     */
    abstract public void load();

    /**
     * Called by MCPatcher whenever the user switches <i>away from</i> the Options panel.  Use this
     * to save changes made in the UI.
     *
     * @see MCPatcherUtils#set(String, String, Object)
     */
    abstract public void save();
}
