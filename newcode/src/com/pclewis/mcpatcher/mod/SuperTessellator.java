package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Tessellator;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
        Tessellator newTessellator = children.get(texture);
        if (newTessellator == null) {
            MCPatcherUtils.info("new tessellator for texture %d", texture);
            newTessellator = new Tessellator(defaultBufferSize / 16);
            newTessellator.texture = texture;
            copyFields(newTessellator, true);
            children.put(texture, newTessellator);
        } else {
            copyFields(newTessellator, false);
        }
        return newTessellator;
    }

    void clearTessellators() {
        children.clear();
    }

    private void copyFields(Tessellator newTessellator, boolean isNew) {
        int saveBufferSize = newTessellator.bufferSize;
        int saveVertexCount = newTessellator.vertexCount;
        int saveAddedVertices = newTessellator.addedVertices;
        int saveRawBufferIndex = newTessellator.rawBufferIndex;
        int saveTexture = newTessellator.texture;
        for (Field f : Tessellator.class.getDeclaredFields()) {
            Class<?> type = f.getType();
            int modifiers = f.getModifiers();
            if (!Modifier.isStatic(modifiers) && type.isPrimitive()) {
                f.setAccessible(true);
                try {
                    Object value = f.get(this);
                    if (isNew) {
                        MCPatcherUtils.debug("  copy %s %s %s = %s", Modifier.toString(modifiers), type.toString(), f.getName(), value.toString());
                    }
                    f.set(newTessellator, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        newTessellator.bufferSize = saveBufferSize;
        newTessellator.vertexCount = saveVertexCount;
        newTessellator.addedVertices = saveAddedVertices;
        newTessellator.rawBufferIndex = saveRawBufferIndex;
        newTessellator.texture = saveTexture;
        if (isDrawing && !newTessellator.isDrawing) {
            newTessellator.startDrawing(drawMode);
        } else if (!isDrawing && newTessellator.isDrawing) {
            newTessellator.reset();
        }
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
        int total = 0;
        for (Tessellator t : children.values()) {
            total += t.draw();
        }
        return total + super.draw();
    }

    @Override
    public void startDrawing(int drawMode) {
        super.startDrawing(drawMode);
        for (Tessellator t : children.values()) {
            t.startDrawing(drawMode);
        }
    }
}
