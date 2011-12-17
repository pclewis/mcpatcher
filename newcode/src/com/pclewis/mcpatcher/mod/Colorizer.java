package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Colorizer {
    private static final String COLOR_PROPERTIES = "/color.properties";
    private static final String LIGHTMAP_FORMAT = "/environment/lightmap%d.png";
    private static final String REDSTONE_COLORS = "/misc/redstonecolor.png";
    private static final String STEM_COLORS = "/misc/stemcolor.png";
    private static final String LAVA_DROP_COLORS = "/misc/lavadropcolor.png";
    private static final String[] COLOR_MAPS = new String[]{
        "/misc/swampgrasscolor.png",
        "/misc/swampfoliagecolor.png",
        "/misc/pinecolor.png",
        "/misc/birchcolor.png",
        "/misc/foliagecolor.png",
        "/misc/watercolorX.png"
    };
    public static final String PALETTE_BLOCK_KEY = "palette.block.";

    private static Properties properties;
    private static int[][] colorMaps; // bitmaps from COLOR_MAPS
    private static int[] colorMapDefault; // default value (x=127, y=127) from each color map
    private static int[] lilypadColor; // lilypad
    private static float[] waterDropBaseColor; // drop.water
    private static float[] lavaDropColor; // /misc/lavadropcolor.png
    private static int[] waterBottleColor; // potion.water
    private static float[][] redstoneColor; // /misc/redstonecolor.png
    private static int[] stemColors; // /misc/stemcolor.png

    private static ArrayList<Potion> potions = new ArrayList<Potion>();
    private static TexturePackBase lastTexturePack;

    private static final int LIGHTMAP_SIZE = 16;
    private static final float LIGHTMAP_SCALE = LIGHTMAP_SIZE - 1;

    private static final int COLORMAP_SIZE = 256;
    private static final float COLORMAP_SCALE = COLORMAP_SIZE - 1;

    private static final boolean useLightmaps = MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "lightmaps", true);
    private static HashMap<Integer, BufferedImage> lightmaps = new HashMap<Integer, BufferedImage>();
    private static int lightMethod;

    private static final boolean useDropColors = MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "drop", true);
    public static float[] waterDropColor;

    private static final boolean useEggColors = MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "egg", true);
    private static final HashMap<Integer, String> entityNamesByID = new HashMap<Integer, String>(); 
    private static final HashMap<Integer, Integer> spawnerEggColors = new HashMap<Integer, Integer>(); // egg.*

    public static float redstoneWireRed;
    public static float redstoneWireGreen;
    public static float redstoneWireBlue;

    public static float lavaDropRed;
    public static float lavaDropGreen;
    public static float lavaDropBlue;

    public static int colorizeBiome(int origColor, int index, double temperature, double rainfall) {
        checkUpdate();
        if (colorMaps[index] == null) {
            return origColor;
        } else {
            int x = (int) (COLORMAP_SCALE * (1.0 - temperature));
            int y = (int) (COLORMAP_SCALE * (1.0 - rainfall * temperature));
            return colorMaps[index][COLORMAP_SIZE * y + x];
        }
    }

    public static int colorizeBiome(int origColor, int index) {
        checkUpdate();
        return colorMapDefault[index];
    }

    public static int colorizeBiome(int origColor, int index, WorldChunkManager chunkManager, int i, int j, int k) {
        checkUpdate();
        return colorizeBiome(origColor, index, chunkManager.getTemperature(i, j, k), chunkManager.getRainfall(i, k));
    }

    public static int colorizeStem(int origColor, int blockMetadata) {
        checkUpdate();
        if (stemColors == null) {
            return origColor;
        } else {
            return stemColors[blockMetadata & 0x7];
        }
    }

    public static int colorizeBlock(Block block, WorldChunkManager chunkManager, int i, int j, int k) {
        return colorizeBiome(0xffffff, COLOR_MAPS.length + block.blockID, chunkManager, i, j, k);
    }
    
    public static int colorizeBlock(Block block) {
        return colorizeBiome(0xffffff, COLOR_MAPS.length + block.blockID);
    }
    
    public static int colorizeSpawnerEgg(int origColor, int entityID) {
        if (!useEggColors) {
            return origColor;
        }
        checkUpdate();
        Integer value = null;
        if (spawnerEggColors.containsKey(entityID)) {
            value = spawnerEggColors.get(entityID);
        } else if (entityNamesByID.containsKey(entityID)) {
            String name = entityNamesByID.get(entityID);
            if (name != null) {
                int[] tmp = new int[]{origColor};
                loadIntColor("egg." + name, tmp, 0);
                spawnerEggColors.put(entityID, tmp[0]);
                value = tmp[0];
            }
        }
        return value == null ? origColor : value;
    }

    public static int getWaterBottleColor() {
        checkUpdate();
        return waterBottleColor[0];
    }

    public static int getLilyPadColor() {
        checkUpdate();
        return lilypadColor[0];
    }

    public static boolean computeLightmap(EntityRenderer renderer, World world) {
        if (world == null || !useLightmaps) {
            return false;
        }
        checkUpdate();
        int worldType = world.worldProvider.worldType;
        String name = String.format(LIGHTMAP_FORMAT, worldType);
        BufferedImage image;
        if (lightmaps.containsKey(worldType)) {
            image = lightmaps.get(worldType);
        } else {
            image = MCPatcherUtils.readImage(lastTexturePack.getInputStream(name));
            lightmaps.put(worldType, image);
            if (image == null) {
                MCPatcherUtils.log("using default lighting for world %d", worldType);
            } else {
                MCPatcherUtils.log("using %s", name);
            }
        }
        if (image == null) {
            return false;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (height != 2 * LIGHTMAP_SIZE) {
            System.out.printf("ERROR: %s must be exactly %d pixels high\n", name, 2 * LIGHTMAP_SIZE);
            lightmaps.put(worldType, null);
            return false;
        }
        int[] origMap = new int[width * height];
        image.getRGB(0, 0, width, height, origMap, 0, width);
        int[] newMap = new int[LIGHTMAP_SIZE * LIGHTMAP_SIZE];
        float sun = clamp(world.lightningFlash > 0 ? 1.0f : 7.0f / 6.0f * (world.getSunAngle(1.0f) - 0.2f)) * (width - 1);
        float torch = clamp(renderer.torchFlickerX + 0.5f) * (width - 1);
        float gamma = clamp(MCPatcherUtils.getMinecraft().gameSettings.gammaSetting);
        float[] sunrgb = new float[3 * LIGHTMAP_SIZE];
        float[] torchrgb = new float[3 * LIGHTMAP_SIZE];
        float[] rgb = new float[3];
        for (int i = 0; i < LIGHTMAP_SIZE; i++) {
            interpolate(origMap, i * width, sun, sunrgb, 3 * i);
            interpolate(origMap, (i + LIGHTMAP_SIZE) * width, torch, torchrgb, 3 * i);
        }
        for (int s = 0; s < LIGHTMAP_SIZE; s++) {
            for (int t = 0; t < LIGHTMAP_SIZE; t++) {
                for (int k = 0; k < 3; k++) {
                    float a = sunrgb[3 * s + k];
                    float b = torchrgb[3 * t + k];
                    float result;
                    switch (lightMethod) {
                        case 1:
                            result = Math.max(a, b);
                            break;

                        case 2:
                            result = a + b - a * b;
                            break;

                        case 3:
                            result = (a * b) / (a + b + 0.00001f);
                            break;
                        
                        case 4:
                            result = (float) Math.sqrt(a * a + b * b);
                            break;

                        default:
                            result = a + b;
                            break;
                    }
                    rgb[k] = clamp(result);
                }
                if (gamma != 0.0f) {
                    for (int k = 0; k < 3; k++) {
                        float tmp = 1.0f - rgb[k];
                        tmp = 1.0f - tmp * tmp * tmp * tmp;
                        rgb[k] = gamma * tmp + (1.0f - gamma) * rgb[k];
                    }
                }
                newMap[s * LIGHTMAP_SIZE + t] = 0xff000000 | float3ToInt(rgb);
            }
        }
        MCPatcherUtils.getMinecraft().renderEngine.createTextureFromBytes(newMap, LIGHTMAP_SIZE, LIGHTMAP_SIZE, renderer.lightmapTexture);
        return true;
    }

    public static boolean computeRedstoneWireColor(int current) {
        checkUpdate();
        if (redstoneColor == null) {
            return false;
        } else {
            float[] f = redstoneColor[Math.max(Math.min(current, 15), 0)];
            redstoneWireRed = f[0];
            redstoneWireGreen = f[1];
            redstoneWireBlue = f[2];
            return true;
        }
    }

    public static boolean computeWaterDropColor(WorldChunkManager chunkManager, double x, double y, double z) {
        checkUpdate();
        if (useDropColors) {
            int rgb = colorizeBiome(0xffffff, 5, chunkManager, (int) x, (int) y, (int) z);
            float[] multiplier = new float[3];
            intToFloat3(rgb, multiplier);
            for (int i = 0; i < 3; i++) {
                waterDropColor[i] = multiplier[i] * waterDropBaseColor[i];
            }
            return true;
        } else {
            return false;
        }
    }
    
    public static void computeWaterDropColor() {
        checkUpdate();
        int rgb = colorizeBiome(0xffffff, 5);
        intToFloat3(rgb, waterDropColor);
    }
    
    public static boolean computeLavaDropColor(int age) {
        checkUpdate();
        if (lavaDropColor == null) {
            return false;
        } else {
            int offset = 3 * Math.max(Math.min(lavaDropColor.length / 3 - 1, age), 0);
            lavaDropRed = lavaDropColor[offset];
            lavaDropGreen = lavaDropColor[offset + 1];
            lavaDropBlue = lavaDropColor[offset + 2];
            return true;
        }
    }

    public static void setupPotion(Potion potion) {
        MCPatcherUtils.log("setupPotion #%d \"%s\" %06x", potion.id, potion.name, potion.color);
        potion.origColor = potion.color;
        potions.add(potion);
    }
    
    public static void setupSpawnerEgg(int entityID, String entityName) {
        int defaultColor = ((64 + (entityID * 0x24faef & 0xc0)) << 16 | (64 + (entityID * 0x3692f & 0xc0)) << 8 | (64 + (entityID * 0x3b367 & 0xc0))) & 0xffffff;
        //System.out.printf("egg.%s=%06x\n", entityName, defaultColor);
        MCPatcherUtils.log("setupSpawnerEgg #%d \"%s\" %06x", entityID, entityName, defaultColor);
        entityNamesByID.put(entityID, entityName);
    }

    public static void checkUpdate() {
        if (lastTexturePack == MCPatcherUtils.getMinecraft().texturePackList.selectedTexturePack) {
            return;
        }
        lastTexturePack = MCPatcherUtils.getMinecraft().texturePackList.selectedTexturePack;

        properties = new Properties();
        colorMaps = new int[COLOR_MAPS.length + 256][];
        colorMapDefault = new int[colorMaps.length];
        int colorIndex = 0;
        colorMapDefault[colorIndex++] = 0x4e4e4e;
        colorMapDefault[colorIndex++] = 0x4e4e4e;
        colorMapDefault[colorIndex++] = 0x619961;
        colorMapDefault[colorIndex++] = 0x80a755;
        colorMapDefault[colorIndex++] = 0x48b518;
        colorMapDefault[colorIndex++] = 0xffffff;
        for ( ; colorIndex < colorMapDefault.length; colorIndex++) {
            colorMapDefault[colorIndex] = 0xffffff;
        }
        lilypadColor = new int[]{0x208030};
        waterDropBaseColor = new float[]{0.2f, 0.3f, 1.0f};
        waterDropColor = new float[]{0.2f, 0.3f, 1.0f};
        lavaDropColor = null;
        waterBottleColor = new int[]{0x385dc6};
        redstoneColor = null;
        stemColors = null;
        lightmaps.clear();
        lightMethod = 0;
        spawnerEggColors.clear();
        for (Potion potion : potions) {
            potion.color = potion.origColor;
        }

        InputStream inputStream = null;
        try {
            inputStream = lastTexturePack.getInputStream(COLOR_PROPERTIES);
            if (inputStream != null) {
                MCPatcherUtils.log("reloading %s", COLOR_PROPERTIES);
                properties.load(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MCPatcherUtils.close(inputStream);
        }
        try {
            lightMethod = Integer.parseInt(properties.getProperty("light.method"));
        } catch (Throwable e) {
        }
        System.out.printf("light.method=%d\n", lightMethod);

        if (MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "water", true)) {
            loadColorMap(5);
        }
        if (MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "potion", true)) {
            for (Potion potion : potions) {
                loadIntColor(potion.name, potion);
            }
            loadIntColor("potion.water", waterBottleColor, 0);
        }
        if (MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "swamp", true)) {
            loadColorMap(0);
            loadColorMap(1);
            loadIntColor("lilypad", lilypadColor, 0);
        }
        if (MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "tree", true)) {
            loadColorMap(2);
            loadColorMap(3);
            loadColorMap(4);
            colorMaps[4] = null;
        }
        if (MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "otherBlocks", true)) {
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                    String key = (String) entry.getKey();
                    String value = (String) entry.getValue();
                    if (key.startsWith(PALETTE_BLOCK_KEY)) {
                        key = key.substring(PALETTE_BLOCK_KEY.length()).trim();
                        int[] rgb = loadColorMap(key);
                        if (rgb != null) {
                            for (String idString : value.split("\\s+")) {
                                try {
                                    int id = Integer.parseInt(idString);
                                    if (id >= 0 && id < 256) {
                                        int index = COLOR_MAPS.length + id;
                                        colorMaps[index] = rgb;
                                        colorMapDefault[index] = colorizeBiome(colorMapDefault[index], index, 0.5, 1.0);
                                        MCPatcherUtils.log("using %s for block %d, default color %06x", key, id, colorMapDefault[index]);
                                    }
                                } catch (NumberFormatException e) {
                                }
                            }
                        }
                    }
                }
            }
        }
        if (useDropColors) {
            loadFloatColor("drop.water", waterDropBaseColor);
            int[] rgb = MCPatcherUtils.getImageRGB(MCPatcherUtils.readImage(lastTexturePack.getInputStream(LAVA_DROP_COLORS)));
            if (rgb != null) {
                lavaDropColor = new float[3 * rgb.length];
                for (int i = 0; i < rgb.length; i++) {
                    intToFloat3(rgb[i], lavaDropColor, 3 * i);
                }
            }
        }

        if (MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "redstone", true)) {
            int[] rgb = MCPatcherUtils.getImageRGB(MCPatcherUtils.readImage(lastTexturePack.getInputStream(REDSTONE_COLORS)));
            if (rgb != null && rgb.length >= 16) {
                redstoneColor = new float[16][];
                for (int i = 0; i < 16; i++) {
                    float[] f = new float[3];
                    intToFloat3(rgb[i], f);
                    redstoneColor[i] = f;
                }
            }
        }

        if (MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "stem", true)) {
            int[] rgb = MCPatcherUtils.getImageRGB(MCPatcherUtils.readImage(lastTexturePack.getInputStream(STEM_COLORS)));
            if (rgb != null && rgb.length >= 8) {
                stemColors = rgb;
            }
        }
    }

    private static void loadIntColor(String key, Potion potion) {
        //System.out.printf("%s=%06x\n", key, potion.color);
        String value = properties.getProperty(key, "");
        if (!value.equals("")) {
            try {
                potion.color = Integer.parseInt(value, 16);
            } catch (NumberFormatException e) {
            }
        }
    }

    private static void loadIntColor(String key, int[] color, int index) {
        //System.out.printf("%s=%06x\n", key, color[index]);
        String value = properties.getProperty(key, "");
        if (!value.equals("")) {
            try {
                color[index] = Integer.parseInt(value, 16);
            } catch (NumberFormatException e) {
            }
        }
    }

    private static void loadFloatColor(String key, float[] color) {
        //System.out.printf("%s=%06x\n", key, float3ToInt(color));
        String value = properties.getProperty(key, "");
        if (!value.equals("")) {
            try {
                intToFloat3(Integer.parseInt(value, 16), color);
            } catch (NumberFormatException e) {
            }
        }
    }

    private static int[] loadColorMap(String filename) {
        int[] rgb = MCPatcherUtils.getImageRGB(MCPatcherUtils.readImage(lastTexturePack.getInputStream(filename)));
        if (rgb == null) {
            return null;
        }
        if (rgb.length != COLORMAP_SIZE * COLORMAP_SIZE) {
            System.out.printf("ERROR: %s must be %dx%d\n", filename, COLORMAP_SIZE, COLORMAP_SIZE);
            return null;
        }
        return rgb;
    }

    private static void loadColorMap(int index) {
        int rgb[] = loadColorMap(COLOR_MAPS[index]);
        if (rgb != null) {
            colorMaps[index] = rgb;
            colorMapDefault[index] = colorizeBiome(colorMapDefault[index], index, 0.5, 1.0);
            MCPatcherUtils.log("using %s, default color %06x", COLOR_MAPS[index], colorMapDefault[index]);
        }
    }

    private static void intToFloat3(int rgb, float[] f, int offset) {
        f[offset] = (float) (rgb & 0xff0000) / (float) 0xff0000;
        f[offset + 1] = (float) (rgb & 0xff00) / (float) 0xff00;
        f[offset + 2] = (float) (rgb & 0xff) / (float) 0xff;
    }

    private static void intToFloat3(int rgb, float[] f) {
        intToFloat3(rgb, f, 0);
    }

    private static int float3ToInt(float[] f, int offset) {
        return ((int) (255.0f * f[offset])) << 16 | ((int) (255.0f * f[offset + 1])) << 8 | (int) (255.0f * f[offset + 2]);
    }

    private static int float3ToInt(float[] f) {
        return float3ToInt(f, 0);
    }

    private static float clamp(float f) {
        if (f < 0.0f) {
            return 0.0f;
        } else if (f > 1.0f) {
            return 1.0f;
        } else {
            return f;
        }
    }

    private static void clamp(float[] f) {
        for (int i = 0; i < f.length; i++) {
            f[i] = clamp(f[i]);
        }
    }

    private static void interpolate(int[] map, int offset1, float x, float[] rgb, int offset2) {
        int x0 = (int) Math.floor(x);
        int x1 = (int) Math.ceil(x);
        if (x0 == x1) {
            intToFloat3(map[offset1 + x0], rgb, offset2);
        } else {
            float xf = x - x0;
            float xg = 1.0f - xf;
            float[] rgb0 = new float[3];
            float[] rgb1 = new float[3];
            intToFloat3(map[offset1 + x0], rgb0);
            intToFloat3(map[offset1 + x1], rgb1);
            for (int i = 0; i < 3; i++) {
                rgb[offset2 + i] = xg * rgb0[i] + xf * rgb1[i];
            }
        }
    }
}
