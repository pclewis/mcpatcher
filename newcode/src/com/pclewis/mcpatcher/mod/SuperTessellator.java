package com.pclewis.mcpatcher.mod;

import net.minecraft.src.Tessellator;

import java.util.HashMap;

public class SuperTessellator extends Tessellator {
    private final HashMap<Integer, Tessellator> children = new HashMap<Integer, Tessellator>();

    public SuperTessellator(int bufferSize) {
        super(bufferSize);
    }

    Tessellator getTessellator(int texture) {
        if (texture == CTMUtils.terrainTexture) {
            return this;
        } else if (children.containsKey(texture)) {
            return children.get(texture);
        } else {
            Tessellator newTessellator = new Tessellator(0x200000);
            children.put(texture, newTessellator);
            return newTessellator;
        }
    }

    void clearTessellators() {
        children.clear();
    }

    @Override
    public void reset() {
        super.reset();
        for (Tessellator t : children.values()) {
            t.reset();
        }
    }

    @Override
    public int draw() {
        int result = super.draw();
        for (Tessellator t : children.values()) {
            result += t.draw();
        }
        return result;
    }

    @Override
    public void startDrawing(int drawMode) {
        super.startDrawing(drawMode);
        for (Tessellator t : children.values()) {
            t.startDrawing(drawMode);
        }
    }
}
