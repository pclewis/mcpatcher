package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;

final public class TileSize {
    static final TileSize[] tileSizes;

    public final String textureName;
    public final int textureID;

    public final int columns;

    public final int int_size;
    public final int int_sizeMinus1;
    public final int int_sizeHalf;
    public final int int_numPixels;
    public final int int_numBytes;
    public final int int_numPixelsMinus1;
    public final int int_compassNeedleMin;
    public final int int_compassNeedleMax;
    public final int int_compassCrossMin;
    public final int int_compassCrossMax;
    public final int int_flameHeight;
    public final int int_flameHeightMinus1;
    public final int int_flameArraySize;

    public final float float_size;
    public final float float_sizeMinus1;
    public final float float_sizeMinus0_01;
    public final float float_sizeHalf;
    public final float float_size16;
    public final float float_reciprocal;
    public final float float_texNudge;
    public final float float_flameNudge;

    public final double double_size;
    public final double double_sizeMinus1;
    public final double double_compassCenterMin;
    public final double double_compassCenterMax;

    static {
        tileSizes = new TileSize[2];
        refresh();
    }

    static void refresh() {
        tileSizes[0] = new TileSize("/terrain.png", 16);
        tileSizes[1] = new TileSize("/gui/items.png", 16);
    }

    public static TileSize getTileSize(TextureFX textureFX) {
        if (textureFX instanceof Compass || textureFX instanceof Watch) {
            // we must handle these directly because getTileSize is called before tileImage is set to 1
            return tileSizes[1];
        } else {
            return tileSizes[textureFX.tileImage];
        }
    }

    public static TileSize getTileSize(ItemRenderer itemRenderer) {
        return tileSizes[1];
    }

    public static TileSize getTileSize(int index) {
        return tileSizes[index];
    }

    public static TileSize getTileSize(String textureName) {
        for (TileSize tileSize : tileSizes) {
            if (tileSize.textureName.equals(textureName)) {
                return tileSize;
            }
        }
        return null;
    }

    private TileSize(String textureName, int columns) {
        this.textureName = textureName;
        this.columns = columns;
        int size;
        RenderEngine renderEngine = MCPatcherUtils.getMinecraft().renderEngine;
        if (renderEngine == null) {
            textureID = -1;
            size = 16;
        } else {
            textureID = renderEngine.getTexture(textureName);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
            size = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH) / columns;
        }

        int_size = size;
        int_sizeMinus1 = size - 1;
        int_sizeHalf = size / 2;
        int_numPixels = size * size;
        int_numBytes = 4 * int_numPixels;
        int_numPixelsMinus1 = int_numPixels - 1;
        int_compassNeedleMin = size / -2;
        int_compassNeedleMax = size;
        int_compassCrossMin = size / -4;
        int_compassCrossMax = size / 4;
        int_flameHeight = size + 4;
        int_flameHeightMinus1 = int_flameHeight - 1;
        int_flameArraySize = size * int_flameHeight;

        float_size = (float) int_size;
        float_sizeMinus1 = float_size - 1.0f;
        float_sizeMinus0_01 = float_size - 0.01f;
        float_sizeHalf = float_size / 2.0f;
        float_size16 = float_size * 16.0f;
        float_reciprocal = 1.0f / float_size;
        float_texNudge = 1.0f / (float_size * float_size * 2.0f);
        if (size < 64) {
            float_flameNudge = 1.0f + 0.96f / float_size;
        } else {
            float_flameNudge = 1.0f + 1.28f / float_size;
        }

        double_size = (double) int_size;
        double_sizeMinus1 = double_size - 1.0;
        double_compassCenterMin = double_size / 2.0 - 0.5;
        double_compassCenterMax = double_size / 2.0 + 0.5;
    }

    private void dump() {
        MCPatcherUtils.log("%s: %s", TileSize.class.getSimpleName(), textureName);
        for (Field f : TileSize.class.getDeclaredFields()) {
            if (f.getName().contains("_")) {
                try {
                    MCPatcherUtils.log("%s = %s", f.getName(), f.get(this));
                } catch (Exception e) {
                    MCPatcherUtils.log("%s: %s", f.getName(), e.toString());
                }
            }
        }
    }
}
