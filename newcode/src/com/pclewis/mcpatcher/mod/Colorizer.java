package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
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

    public static final int COLOR_MAP_SWAMP_GRASS = 0;
    public static final int COLOR_MAP_SWAMP_FOLIAGE = 1;
    public static final int COLOR_MAP_PINE = 2;
    public static final int COLOR_MAP_BIRCH = 3;
    public static final int COLOR_MAP_FOLIAGE = 4;
    public static final int COLOR_MAP_WATER = 5;
    public static final int COLOR_MAP_UNDERWATER = 6;
    public static final int COLOR_MAP_FOG0 = 7;
    public static final int COLOR_MAP_SKY0 = 8;
    
    private static final String[] COLOR_MAPS = new String[]{
        "/misc/swampgrasscolor.png",
        "/misc/swampfoliagecolor.png",
        "/misc/pinecolor.png",
        "/misc/birchcolor.png",
        "/misc/foliagecolor.png",
        "/misc/watercolorX.png",
        "/misc/underwatercolor.png",
        "/misc/fogcolor0.png",
        "/misc/skycolor0.png",
    };
    public static final String PALETTE_BLOCK_KEY = "palette.block.";

    private static Properties properties;
    private static int[][] colorMaps; // bitmaps from COLOR_MAPS
    private static int[] colorMapDefault; // default value (x=127, y=127) from each color map
    private static int[] lilypadColor; // lilypad
    private static float[] waterBaseColor; // drop.water
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

    private static final boolean useDropColors = MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "drop", true);
    public static float[] waterColor;

    private static final boolean useEggColors = MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "egg", true);
    private static final HashMap<Integer, String> entityNamesByID = new HashMap<Integer, String>();
    private static final HashMap<Integer, Integer> spawnerEggShellColors = new HashMap<Integer, Integer>(); // egg.shell.*
    private static final HashMap<Integer, Integer> spawnerEggSpotColors = new HashMap<Integer, Integer>(); // egg.spots.*

    private static final int fogBlendRadius = MCPatcherUtils.getInt(MCPatcherUtils.CUSTOM_COLORS, "fogBlendRadius", 7);
    private static final float fogBlendScale = 1.0f / ((2 * fogBlendRadius + 1) * (2 * fogBlendRadius + 1));

    private static final ArrayList<BiomeGenBase> biomes = new ArrayList<BiomeGenBase>();

    public static float redstoneWireRed;
    public static float redstoneWireGreen;
    public static float redstoneWireBlue;

    public static float lavaDropRed;
    public static float lavaDropGreen;
    public static float lavaDropBlue;
    
    private static Entity fogCamera;
    private static WorldChunkManager fogChunkManager;
    public static final float[] setColor = new float[3];

    private static boolean colorMapIndexValid(int index) {
        return index >= 0 && index < colorMaps.length && colorMaps[index] != null;
    }

    public static int colorizeBiome(int origColor, int index, double temperature, double rainfall) {
        checkUpdate();
        if (!colorMapIndexValid(index)) {
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
    
    public static int colorizeWater(WorldChunkManager chunkManager, int i, int k) {
        return colorizeBiome(chunkManager.getBiomeGenAt(i, k).waterColorMultiplier, COLOR_MAP_WATER, chunkManager, i, 64, k);
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

    public static int colorizeSpawnerEgg(int origColor, int entityID, int spots) {
        if (!useEggColors) {
            return origColor;
        }
        checkUpdate();
        Integer value = null;
        HashMap<Integer, Integer> eggMap = (spots == 0 ? spawnerEggShellColors : spawnerEggSpotColors);
        if (eggMap.containsKey(entityID)) {
            value = eggMap.get(entityID);
        } else if (entityNamesByID.containsKey(entityID)) {
            String name = entityNamesByID.get(entityID);
            if (name != null) {
                int[] tmp = new int[]{origColor};
                loadIntColor((spots == 0 ? "egg.shell." : "egg.spots.") + name, tmp, 0);
                eggMap.put(entityID, tmp[0]);
                value = tmp[0];
            }
        }
        return value == null ? origColor : value;
    }

    public static void setColorF(int color) {
        intToFloat3(color, setColor);
    }

    public static int getWaterBottleColor() {
        checkUpdate();
        return waterBottleColor[0];
    }

    public static int getLilyPadColor() {
        checkUpdate();
        return lilypadColor[0];
    }

    public static int getItemColorFromDamage(int origColor, int blockID, int damage) {
        if (blockID == 8 || blockID == 9) {
            return colorizeBiome(origColor, COLOR_MAP_WATER);
        } else {
            return origColor;
        }
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
                    rgb[k] = clamp(sunrgb[3 * s + k] + torchrgb[3 * t + k]);
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

    public static boolean computeWaterColor(WorldChunkManager chunkManager, double x, double y, double z) {
        checkUpdate();
        if (useDropColors) {
            int rgb = colorizeBiome(0xffffff, COLOR_MAP_WATER, chunkManager, (int) x, (int) y, (int) z);
            float[] multiplier = new float[3];
            intToFloat3(rgb, multiplier);
            for (int i = 0; i < 3; i++) {
                waterColor[i] = multiplier[i] * waterBaseColor[i];
            }
            return true;
        } else {
            return false;
        }
    }

    public static void computeWaterColor() {
        checkUpdate();
        int rgb = colorizeBiome(0xffffff, COLOR_MAP_WATER);
        intToFloat3(rgb, waterColor);
    }
    
    public static void colorizeWaterBlockGL(int blockID) {
        if (blockID == 8 || blockID == 9) {
            computeWaterColor();
            GL11.glColor4f(waterColor[0], waterColor[1], waterColor[2], 1.0f);
        }
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
    
    public static void setupForFog(WorldChunkManager chunkManager, Entity entity) {
        fogChunkManager = chunkManager;
        fogCamera = entity;
    }

    public static boolean computeFogColor(int index) {
        checkUpdate();
        if (!colorMapIndexValid(index) || fogChunkManager == null || fogCamera == null) {
            return false;
        }
        float[] f = new float[3];
        int x = (int) fogCamera.posX;
        int y = (int) fogCamera.posY;
        int z = (int) fogCamera.posZ;
        setColor[0] = 0.0f;
        setColor[1] = 0.0f;
        setColor[2] = 0.0f;
        for (int i = -fogBlendRadius; i <= fogBlendRadius; i++) {
            for (int j = -fogBlendRadius; j <= fogBlendRadius; j++) {
                int rgb = colorizeBiome(0xffffff, index, fogChunkManager, x + i, y, z + j);
                intToFloat3(rgb, f);
                setColor[0] += f[0] * fogBlendScale;
                setColor[1] += f[1] * fogBlendScale;
                setColor[2] += f[2] * fogBlendScale;
            }
        }
        return true;
    }
    
    public static boolean computeSkyColor(World world) {
        return world.worldProvider.worldType == 0 && computeFogColor(COLOR_MAP_SKY0);
    }

    public static void setupBiome(BiomeGenBase biome) {
        MCPatcherUtils.log("setupBiome #%d \"%s\" %06x", biome.biomeID, biome.biomeName, biome.waterColorMultiplier);
        biomes.add(biome);
    }

    public static void setupPotion(Potion potion) {
        //System.out.printf("potion.%s=%06x\n", potion.name, potion.color);
        MCPatcherUtils.log("setupPotion #%d \"%s\" %06x", potion.id, potion.name, potion.color);
        potion.origColor = potion.color;
        potions.add(potion);
    }

    public static void setupSpawnerEgg(String entityName, int entityID, int defaultShellColor, int defaultSpotColor) {
        //System.out.printf("egg.shell.%s=%06x\n", entityName, defaultShellColor);
        //System.out.printf("egg.spots.%s=%06x\n", entityName, defaultSpotColor);
        MCPatcherUtils.log("setupSpawnerEgg #%d \"%s\" %06x %06x", entityID, entityName, defaultShellColor, defaultSpotColor);
        entityNamesByID.put(entityID, entityName);
    }

    public static void checkUpdate() {
        if (lastTexturePack == MCPatcherUtils.getMinecraft().texturePackList.selectedTexturePack) {
            return;
        }
        lastTexturePack = MCPatcherUtils.getMinecraft().texturePackList.selectedTexturePack;

        properties = new Properties();
        colorMaps = new int[COLOR_MAPS.length + Block.blocksList.length][];
        colorMapDefault = new int[colorMaps.length];
        for (int i = 0; i < colorMapDefault.length; i++) {
            colorMapDefault[i] = 0xffffff;
        }
        colorMapDefault[COLOR_MAP_SWAMP_GRASS] = 0x4e4e4e;
        colorMapDefault[COLOR_MAP_SWAMP_FOLIAGE] = 0x4e4e4e;
        colorMapDefault[COLOR_MAP_PINE] = 0x619961;
        colorMapDefault[COLOR_MAP_BIRCH] = 0x80a755;
        colorMapDefault[COLOR_MAP_FOLIAGE] = 0x48b518;
        colorMapDefault[COLOR_MAP_WATER] = 0xffffff;
        colorMapDefault[COLOR_MAP_UNDERWATER] = 0x050533;
        colorMapDefault[COLOR_MAP_FOG0] = 0xc0d8ff;
        colorMapDefault[COLOR_MAP_SKY0] = 0xffffff;
        lilypadColor = new int[]{0x208030};
        waterBaseColor = new float[]{0.2f, 0.3f, 1.0f};
        waterColor = new float[]{0.2f, 0.3f, 1.0f};
        lavaDropColor = null;
        waterBottleColor = new int[]{0x385dc6};
        redstoneColor = null;
        stemColors = null;
        lightmaps.clear();
        spawnerEggShellColors.clear();
        spawnerEggSpotColors.clear();
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

        if (MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "water", true)) {
            loadColorMap(COLOR_MAP_WATER);
            loadColorMap(COLOR_MAP_UNDERWATER);
        }
        if (MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "fog", true)) {
            loadColorMap(COLOR_MAP_FOG0);
            loadColorMap(COLOR_MAP_SKY0);
        }
        if (MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "potion", true)) {
            for (Potion potion : potions) {
                loadIntColor(potion.name, potion);
            }
            loadIntColor("potion.water", waterBottleColor, 0);
        }
        if (MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "swamp", true)) {
            loadColorMap(COLOR_MAP_SWAMP_GRASS);
            loadColorMap(COLOR_MAP_SWAMP_FOLIAGE);
            loadIntColor("lilypad", lilypadColor, 0);
        }
        if (MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "tree", true)) {
            loadColorMap(COLOR_MAP_PINE);
            loadColorMap(COLOR_MAP_BIRCH);
            loadColorMap(COLOR_MAP_FOLIAGE);
            colorMaps[COLOR_MAP_FOLIAGE] = null;
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
                                    if (id >= 0 && id < colorMaps.length - COLOR_MAPS.length) {
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
            loadFloatColor("drop.water", waterBaseColor);
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
