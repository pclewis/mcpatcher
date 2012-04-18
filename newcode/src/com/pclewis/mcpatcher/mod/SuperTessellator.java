package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Tessellator;

import java.util.HashMap;

public class SuperTessellator extends Tessellator {
    private static int defaultBufferSize;

    private final HashMap<Integer, Tessellator> children = new HashMap<Integer, Tessellator>();

    public SuperTessellator(int bufferSize) {
        super(bufferSize);
        MCPatcherUtils.info("new %s(%d)", getClass().getSimpleName(), bufferSize);
        defaultBufferSize = bufferSize;
    }

    Tessellator getTessellator(int texture) {
        if (texture == CTMUtils.terrainTexture) {
            return this;
        }
        Tessellator oldTessellator = Tessellator.instance;
        Tessellator newTessellator = children.get(texture);
        if (newTessellator == null) {
            MCPatcherUtils.info("new tessellator for texture %d", texture);
            newTessellator = new Tessellator(defaultBufferSize);
            children.put(texture, newTessellator);
            if (oldTessellator.isDrawing) {
                newTessellator.startDrawing(oldTessellator.drawMode);
            }
        }
        newTessellator.hasBrightness = oldTessellator.hasBrightness;
        newTessellator.brightness = oldTessellator.brightness;
        newTessellator.isColorDisabled = oldTessellator.isColorDisabled;
        newTessellator.hasColor = oldTessellator.hasColor;
        newTessellator.color = oldTessellator.color;
        newTessellator.hasNormals = oldTessellator.hasNormals;
        newTessellator.normal = oldTessellator.normal;
        newTessellator.hasTexture = oldTessellator.hasTexture;
        newTessellator.textureU = oldTessellator.textureU;
        newTessellator.textureV = oldTessellator.textureV;
        newTessellator.setTranslation(oldTessellator.xOffset, oldTessellator.yOffset, oldTessellator.zOffset);
        return newTessellator;
    }

    void clearTessellators() {
        children.clear();
    }

    @Override
    public void reset() {
        MCPatcherUtils.info("reset()");
        super.reset();
        for (Tessellator t : children.values()) {
            t.reset();
        }
    }

    @Override
    public int draw() {
        MCPatcherUtils.info("draw()");
        int result = super.draw();
        for (Tessellator t : children.values()) {
            result += t.draw();
        }
        return result;
    }

    @Override
    public void startDrawing(int drawMode) {
        MCPatcherUtils.info("startDrawing(%d)", drawMode);
        super.startDrawing(drawMode);
        for (Tessellator t : children.values()) {
            t.startDrawing(drawMode);
        }
    }
}
