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
        }
        Tessellator oldTessellator = Tessellator.instance;
        Tessellator newTessellator = children.get(texture);
        if (newTessellator == null) {
            newTessellator = new Tessellator(0x200000);
            children.put(texture, newTessellator);
        } else {
            return newTessellator;
        }
        if (oldTessellator.isDrawing) {
            newTessellator.startDrawing(oldTessellator.drawMode);
        }
        if (oldTessellator.hasBrightness) {
            newTessellator.setBrightness(oldTessellator.brightness);
        }
        newTessellator.isColorDisabled = oldTessellator.isColorDisabled;
        newTessellator.hasColor = oldTessellator.hasColor;
        newTessellator.color = oldTessellator.color;
        newTessellator.hasBrightness = oldTessellator.hasBrightness;
        newTessellator.brightness = oldTessellator.brightness;
        newTessellator.hasNormals = oldTessellator.hasNormals;
        if (newTessellator.hasTexture) {
            newTessellator.setTextureUV(oldTessellator.textureU, oldTessellator.textureV);
        }
        newTessellator.setTranslation(oldTessellator.xOffset, oldTessellator.yOffset, oldTessellator.zOffset);
        return newTessellator;
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
