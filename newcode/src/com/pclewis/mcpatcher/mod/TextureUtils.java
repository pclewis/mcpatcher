package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.src.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TextureUtils {
    public static Minecraft minecraft;

    private static boolean animatedFire;
    private static boolean animatedLava;
    private static boolean animatedWater;
    private static boolean animatedPortal;
    private static boolean customFire;
    private static boolean customLava;
    private static boolean customWater;
    private static boolean customPortal;
    private static boolean customOther;

    public static final int LAVA_STILL_TEXTURE_INDEX = 14 * 16 + 13;  // Block.lavaStill.blockIndexInTexture
    public static final int LAVA_FLOWING_TEXTURE_INDEX = LAVA_STILL_TEXTURE_INDEX + 1; // Block.lavaMoving.blockIndexInTexture
    public static final int WATER_STILL_TEXTURE_INDEX = 12 * 16 + 13; // Block.waterStill.blockIndexInTexture
    public static final int WATER_FLOWING_TEXTURE_INDEX = WATER_STILL_TEXTURE_INDEX + 1; // Block.waterMoving.blockIndexInTexture
    public static final int FIRE_E_W_TEXTURE_INDEX = 1 * 16 + 15; // Block.fire.blockIndexInTexture;
    public static final int FIRE_N_S_TEXTURE_INDEX = FIRE_E_W_TEXTURE_INDEX + 16;
    public static final int PORTAL_TEXTURE_INDEX = 0 * 16 + 14; // Block.portal.blockIndexInTexture

    private static HashMap<String, Integer> expectedColumns = new HashMap<String, Integer>();

    private static boolean useTextureCache;
    private static boolean reclaimGLMemory;
    private static TexturePackBase lastTexturePack = null;
    private static HashMap<String, BufferedImage> cache = new HashMap<String, BufferedImage>();

    static {
        animatedFire = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "animatedFire", true);
        animatedLava = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "animatedLava", true);
        animatedWater = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "animatedWater", true);
        animatedPortal = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "animatedPortal", true);
        customFire = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "customFire", true);
        customLava = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "customLava", true);
        customWater = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "customWater", true);
        customPortal = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "customPortal", true);
        customOther = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "customOther", true);

        useTextureCache = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "useTextureCache", false);
        reclaimGLMemory = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "reclaimGLMemory", false);

        expectedColumns.put("/terrain.png", 16);
        expectedColumns.put("/gui/items.png", 16);
        expectedColumns.put("/misc/dial.png", 1);
        expectedColumns.put("/custom_lava_still.png", 1);
        expectedColumns.put("/custom_lava_flowing.png", 1);
        expectedColumns.put("/custom_water_still.png", 1);
        expectedColumns.put("/custom_water_flowing.png", 1);
        expectedColumns.put("/custom_fire_n_s.png", 1);
        expectedColumns.put("/custom_fire_e_w.png", 1);
        expectedColumns.put("/custom_portal.png", 1);
    }

    public static boolean setTileSize() {
        MCPatcherUtils.log("\nchanging skin to %s", getTexturePackName(getSelectedTexturePack()));
        int size = getTileSize();
        if (size == TileSize.int_size) {
            MCPatcherUtils.log("tile size %d unchanged", size);
            return false;
        } else {
            MCPatcherUtils.log("setting tile size to %d (was %d)", size, TileSize.int_size);
            TileSize.setTileSize(size);
            return true;
        }
    }

    public static void setFontRenderer() {
        MCPatcherUtils.log("setFontRenderer()");
        minecraft.fontRenderer.initialize(minecraft.gameSettings, "/font/default.png", minecraft.renderEngine);
        if (minecraft.alternateFontRenderer != minecraft.fontRenderer) {
            minecraft.alternateFontRenderer.initialize(minecraft.gameSettings, "/font/alternate.png", minecraft.renderEngine);
        }
    }

    public static void registerTextureFX(java.util.List<TextureFX> textureList, TextureFX textureFX) {
        TextureFX fx = refreshTextureFX(textureFX);
        if (fx != null) {
            MCPatcherUtils.log("registering new TextureFX class %s", textureFX.getClass().getName());
            textureList.add(fx);
            fx.onTick();
        }
    }

    private static TextureFX refreshTextureFX(TextureFX textureFX) {
        if (textureFX instanceof Compass ||
            textureFX instanceof Watch ||
            textureFX instanceof StillLava ||
            textureFX instanceof FlowLava ||
            textureFX instanceof StillWater ||
            textureFX instanceof FlowWater ||
            textureFX instanceof Fire ||
            textureFX instanceof Portal ||
            textureFX instanceof CustomAnimation) {
            return null;
        }
        System.out.printf("attempting to refresh unknown animation %s\n", textureFX.getClass().getName());
        Class<? extends TextureFX> textureFXClass = textureFX.getClass();
        for (int i = 0; i < 3; i++) {
            Constructor<? extends TextureFX> constructor;
            try {
                switch (i) {
                    case 0:
                        constructor = textureFXClass.getConstructor(Minecraft.class, Integer.TYPE);
                        return constructor.newInstance(minecraft, TileSize.int_size);

                    case 1:
                        constructor = textureFXClass.getConstructor(Minecraft.class);
                        return constructor.newInstance(minecraft);

                    case 2:
                        constructor = textureFXClass.getConstructor();
                        return constructor.newInstance();

                    default:
                        break;
                }
            } catch (NoSuchMethodException e) {
            } catch (IllegalAccessException e) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (textureFX.imageData.length != TileSize.int_numBytes) {
            MCPatcherUtils.log("resizing %s buffer from %d to %d bytes",
                textureFXClass.getName(), textureFX.imageData.length, TileSize.int_numBytes
            );
            textureFX.imageData = new byte[TileSize.int_numBytes];
        }
        return textureFX;
    }

    public static void refreshTextureFX(java.util.List<TextureFX> textureList) {
        MCPatcherUtils.log("refreshTextureFX()");

        ArrayList<TextureFX> savedTextureFX = new ArrayList<TextureFX>();
        for (TextureFX t : textureList) {
            TextureFX fx = refreshTextureFX(t);
            if (fx != null) {
                savedTextureFX.add(fx);
            }
        }
        textureList.clear();

        textureList.add(new Compass(minecraft));
        textureList.add(new Watch(minecraft));

        TexturePackBase selectedTexturePack = getSelectedTexturePack();
        boolean isDefault = (selectedTexturePack == null || selectedTexturePack instanceof TexturePackDefault);

        if (!isDefault && customLava) {
            textureList.add(new CustomAnimation(LAVA_STILL_TEXTURE_INDEX, 0, 1, "lava_still", -1, -1));
            textureList.add(new CustomAnimation(LAVA_FLOWING_TEXTURE_INDEX, 0, 2, "lava_flowing", 3, 6));
        } else if (animatedLava) {
            textureList.add(new StillLava());
            textureList.add(new FlowLava());
        }

        if (!isDefault && customWater) {
            textureList.add(new CustomAnimation(WATER_STILL_TEXTURE_INDEX, 0, 1, "water_still", -1, -1));
            textureList.add(new CustomAnimation(WATER_FLOWING_TEXTURE_INDEX, 0, 2, "water_flowing", 0, 0));
        } else if (animatedWater) {
            textureList.add(new StillWater());
            textureList.add(new FlowWater());
        }

        if (!isDefault && customFire && hasResource("/custom_fire_e_w.png") && hasResource("/custom_fire_n_s.png")) {
            textureList.add(new CustomAnimation(FIRE_N_S_TEXTURE_INDEX, 0, 1, "fire_n_s", 2, 4));
            textureList.add(new CustomAnimation(FIRE_E_W_TEXTURE_INDEX, 0, 1, "fire_e_w", 2, 4));
        } else if (animatedFire) {
            textureList.add(new Fire(0));
            textureList.add(new Fire(1));
        }

        if (!isDefault && customPortal && hasResource("/custom_portal.png")) {
            textureList.add(new CustomAnimation(PORTAL_TEXTURE_INDEX, 0, 1, "portal", -1, -1));
        } else if (animatedPortal) {
            textureList.add(new Portal());
        }

        if (customOther) {
            for (int tileImage = 0; tileImage < 2; tileImage++) {
                String imageName = (tileImage == 0 ? "terrain" : "item");
                for (int tileNum = 0; tileNum < 256; tileNum++) {
                    String resource = "/custom_" + imageName + "_" + tileNum + ".png";
                    if (hasResource(resource)) {
                        textureList.add(new CustomAnimation(tileNum, tileImage, 1, imageName + "_" + tileNum, 2, 4));
                    }
                }
            }
        }

        for (TextureFX t : savedTextureFX) {
            textureList.add(t);
        }

        for (TextureFX t : textureList) {
            t.onTick();
        }

        if (ColorizerWater.colorBuffer != ColorizerFoliage.colorBuffer) {
            refreshColorizer(ColorizerWater.colorBuffer, "/misc/watercolor.png");
        }
        refreshColorizer(ColorizerGrass.colorBuffer, "/misc/grasscolor.png");
        refreshColorizer(ColorizerFoliage.colorBuffer, "/misc/foliagecolor.png");

        System.gc();
    }

    public static TexturePackBase getSelectedTexturePack() {
        return minecraft == null ? null :
            minecraft.texturePackList == null ? null :
                minecraft.texturePackList.selectedTexturePack;
    }

    public static String getTexturePackName(TexturePackBase texturePack) {
        return texturePack == null ? "Default" : texturePack.texturePackFileName;
    }

    public static ByteBuffer getByteBuffer(ByteBuffer buffer, byte[] data) {
        buffer.clear();
        final int have = buffer.capacity();
        final int needed = data.length;
        if (needed > have || (reclaimGLMemory && have >= 4 * needed)) {
            //MCPatcherUtils.log("resizing gl buffer from 0x%x to 0x%x", have, needed);
            buffer = GLAllocation.createDirectByteBuffer(needed);
        }
        buffer.put(data);
        buffer.position(0).limit(needed);
        TileSize.int_glBufferSize = needed;
        return buffer;
    }

    public static boolean isRequiredResource(String resource) {
        return !(resource.startsWith("/custom_") ||
            resource.equals("/terrain_nh.png") ||
            resource.equals("/terrain_s.png") ||
            resource.matches("^/font/.*\\.properties$") ||
            resource.matches("^/mob/.*\\d+.png$")
        );
    }

    public static InputStream getResourceAsStream(TexturePackBase texturePack, String resource) {
        InputStream is = null;
        if (texturePack != null) {
            try {
                is = texturePack.getInputStream(resource);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (is == null) {
            is = TextureUtils.class.getResourceAsStream(resource);
        }
        if (is == null && isRequiredResource(resource)) {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
            MCPatcherUtils.warn("falling back on thread class loader for %s: %s",
                resource, (is == null ? "failed" : "success")
            );
        }
        return is;
    }

    public static InputStream getResourceAsStream(String resource) {
        return getResourceAsStream(getSelectedTexturePack(), resource);
    }

    public static BufferedImage getResourceAsBufferedImage(TexturePackBase texturePack, String resource) throws IOException {
        BufferedImage image = null;
        boolean cached = false;

        if (useTextureCache && texturePack == lastTexturePack) {
            image = cache.get(resource);
            if (image != null) {
                cached = true;
            }
        }

        if (image == null) {
            InputStream is = getResourceAsStream(texturePack, resource);
            if (is != null) {
                try {
                    image = ImageIO.read(is);
                } finally {
                    MCPatcherUtils.close(is);
                }
            }
        }

        if (image == null) {
            if (isRequiredResource(resource)) {
                throw new IOException(resource + " image is null");
            } else {
                return null;
            }
        }

        if (useTextureCache && !cached && texturePack != lastTexturePack) {
            MCPatcherUtils.log("clearing texture cache (%d items)", cache.size());
            cache.clear();
        }
        MCPatcherUtils.log("opened %s %dx%d from %s",
            resource, image.getWidth(), image.getHeight(), (cached ? "cache" : getTexturePackName(texturePack))
        );
        if (!cached) {
            Integer i;
            if (resource.matches("^/custom_\\w+_\\d+\\.png$")) {
                i = 1;
            } else {
                i = expectedColumns.get(resource);
            }
            if (i != null && image.getWidth() != i * TileSize.int_size) {
                image = resizeImage(image, i * TileSize.int_size);
            }
            if (useTextureCache) {
                lastTexturePack = texturePack;
                cache.put(resource, image);
            }
            if (resource.endsWith("_eyes.png")) {
                int p = 0;
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        int argb = image.getRGB(x, y);
                        if ((argb & 0xff000000) == 0 && argb != 0) {
                            image.setRGB(x, y, 0);
                            p++;
                        }
                    }
                }
                if (p > 0) {
                    MCPatcherUtils.log("  fixed %d transparent pixels", p, resource);
                }
            }
        }

        return image;
    }

    public static BufferedImage getResourceAsBufferedImage(String resource) throws IOException {
        return getResourceAsBufferedImage(getSelectedTexturePack(), resource);
    }

    public static BufferedImage getResourceAsBufferedImage(Object o1, Object o2, String resource) throws IOException {
        return getResourceAsBufferedImage(resource);
    }

    public static int getTileSize(TexturePackBase texturePack) {
        int size = 0;
        for (Map.Entry<String, Integer> entry : expectedColumns.entrySet()) {
            InputStream is = null;
            try {
                is = getResourceAsStream(texturePack, entry.getKey());
                if (is != null) {
                    BufferedImage bi = ImageIO.read(is);
                    int newSize = bi.getWidth() / entry.getValue();
                    MCPatcherUtils.log("  %s tile size is %d", entry.getKey(), newSize);
                    size = Math.max(size, newSize);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                MCPatcherUtils.close(is);
            }
        }
        return size > 0 ? size : 16;
    }

    public static int getTileSize() {
        return getTileSize(getSelectedTexturePack());
    }

    public static boolean hasResource(TexturePackBase texturePack, String resource) {
        InputStream is = getResourceAsStream(texturePack, resource);
        boolean has = (is != null);
        MCPatcherUtils.close(is);
        return has;
    }

    public static boolean hasResource(String s) {
        return hasResource(getSelectedTexturePack(), s);
    }

    private static BufferedImage resizeImage(BufferedImage image, int width) {
        int height = image.getHeight() * width / image.getWidth();
        MCPatcherUtils.log("  resizing to %dx%d", width, height);
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = newImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, width, height, null);
        return newImage;
    }

    private static void refreshColorizer(int[] colorBuffer, String resource) {
        try {
            BufferedImage bi = getResourceAsBufferedImage(resource);
            if (bi != null) {
                bi.getRGB(0, 0, 256, 256, colorBuffer, 0, 256);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setMinecraft(Minecraft minecraft) {
        TextureUtils.minecraft = minecraft;
    }

    public static Minecraft getMinecraft() {
        return minecraft;
    }
}
