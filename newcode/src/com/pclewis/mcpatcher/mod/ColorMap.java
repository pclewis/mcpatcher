package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.WorldChunkManager;

final class ColorMap {
    private static final int COLORMAP_SIZE = 256;
    private static final float COLORMAP_SCALE = COLORMAP_SIZE - 1;

    private int[] map;
    private int mapDefault;
    
    static int getX(double temperature, double rainfall) {
        return (int) (COLORMAP_SCALE * (1.0 - temperature));
    }
    
    static int getY(double temperature, double rainfall) {
        return (int) (COLORMAP_SCALE * (1.0 - rainfall * temperature));
    }
    
    static float getBlockMetaKey(int blockID, int metadata) {
        return blockID + (metadata & 0xff) / 256.0f;
    }

    ColorMap(boolean useCustom, String filename, int defaultColor) {
        mapDefault = defaultColor;
        if (!useCustom) {
            return;
        }
        map = MCPatcherUtils.getImageRGB(MCPatcherUtils.readImage(Colorizer.lastTexturePack.getInputStream(filename)));
        if (map == null) {
            return;
        }
        if (map.length != COLORMAP_SIZE * COLORMAP_SIZE) {
            System.out.printf("ERROR: %s must be %dx%d\n", filename, COLORMAP_SIZE, COLORMAP_SIZE);
            map = null;
            return;
        }
        mapDefault = colorize(0xffffff, 0.5, 1.0);
        MCPatcherUtils.log("using %s, default color %06x", filename, mapDefault);
    }

    boolean isCustom() {
        return map != null;
    }

    int colorize() {
        return mapDefault;
    }

    int colorize(int defaultColor) {
        return map == null ? defaultColor : mapDefault;
    }

    int colorize(int defaultColor, double temperature, double rainfall) {
        if (map == null) {
            return defaultColor;
        } else {
            return map[COLORMAP_SIZE * getY(temperature, rainfall) + getX(temperature, rainfall)];
        }
    }

    int colorize(int defaultColor, WorldChunkManager chunkManager, int i, int j, int k) {
        return colorize(defaultColor, chunkManager.getTemperature(i, j, k), chunkManager.getRainfall(i, j));
    }
}
