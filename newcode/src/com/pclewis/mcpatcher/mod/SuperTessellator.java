package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Tessellator;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class SuperTessellator extends Tessellator {
    private static int defaultBufferSize;

    private final HashMap<Integer, Tessellator> children = new HashMap<Integer, Tessellator>();

    boolean needCopy;

    public SuperTessellator(int bufferSize) {
        super(bufferSize);
        MCPatcherUtils.info("new %s(%d)", getClass().getSimpleName(), bufferSize);
        defaultBufferSize = bufferSize;
    }

    Tessellator getTessellator(int texture) {
        if (needCopy) {
            for (Tessellator t : children.values()) {
                copyFields(t, t.texture, false);
            }
            needCopy = false;
        }
        Tessellator newTessellator = children.get(texture);
        if (newTessellator == null) {
            MCPatcherUtils.info("new tessellator for texture %d", texture);
            newTessellator = new Tessellator(defaultBufferSize / 16);
            copyFields(newTessellator, texture, true);
            children.put(texture, newTessellator);
        }
        if (isDrawing && !newTessellator.isDrawing) {
            newTessellator.startDrawing(drawMode);
        } else if (!isDrawing && newTessellator.isDrawing) {
            newTessellator.reset();
        }
        return newTessellator;
    }

    void clearTessellators() {
        children.clear();
    }

    private void copyFields(Tessellator newTessellator, int texture, boolean isNew) {
        int saveBufferSize = newTessellator.bufferSize;
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
        newTessellator.texture = texture;
        newTessellator.reset();
        newTessellator.isDrawing = false;
        if (isDrawing) {
            newTessellator.startDrawing(drawMode);
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
