package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Tessellator;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

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
        Tessellator newTessellator = children.get(texture);
        if (newTessellator == null) {
            MCPatcherUtils.info("new tessellator for texture %d", texture);
            newTessellator = new Tessellator(defaultBufferSize);
            children.put(texture, newTessellator);
        }
        if (isDrawing && !newTessellator.isDrawing) {
            newTessellator.startDrawing(drawMode);
        } else if (!isDrawing && newTessellator.isDrawing) {
            newTessellator.reset();
        }
        newTessellator.hasBrightness = hasBrightness;
        newTessellator.brightness = brightness;
        newTessellator.isColorDisabled = isColorDisabled;
        newTessellator.hasColor = hasColor;
        newTessellator.color = color;
        newTessellator.hasNormals = hasNormals;
        newTessellator.normal = normal;
        newTessellator.hasTexture = hasTexture;
        newTessellator.textureU = textureU;
        newTessellator.textureV = textureV;
        newTessellator.setTranslation(xOffset, yOffset, zOffset);
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
        int curTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int result = super.draw();
        for (Map.Entry<Integer, Tessellator> entry : children.entrySet()) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, entry.getKey());
            result += entry.getValue().draw();
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, curTexture);
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
