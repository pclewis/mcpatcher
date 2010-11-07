package com.pclewis.mcpatcher;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;

public class ModListModel extends AbstractListModel {
    List<Mod> items = new LinkedList<Mod>();

    public int getSize() {
        return items.size();
    }

    public Object getElementAt(int index) {
        return items.get(index).getModInfo().name();
    }

    public Mod getMod(int index) {
        return items.get(index);
    }

    public void addElement(Mod mod) {
        items.add(mod);
    }
}
