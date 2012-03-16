package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.src.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TextureUtils {
    private static final boolean animatedFire = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "animatedFire", true);
    private static final boolean animatedLava = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "animatedLava", true);
    private static final boolean animatedWater = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "animatedWater", true);
    private static final boolean animatedPortal = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "animatedPortal", true);
    private static final boolean customFire = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "customFire", true);
    private static final boolean customLava = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "customLava", true);
    private static final boolean customWater = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "customWater", true);
    private static final boolean customPortal = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "customPortal", true);
    private static final boolean customOther = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "customOther", true);

    private static final boolean reclaimGLMemory = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "reclaimGLMemory", false);
    private static final boolean autoRefreshTextures = MCPatcherUtils.getBoolean(MCPatcherUtils.HD_TEXTURES, "autoRefreshTextures", false);

    public static final int LAVA_STILL_TEXTURE_INDEX = 14 * 16 + 13;  // Block.lavaStill.blockIndexInTexture
    public static final int LAVA_FLOWING_TEXTURE_INDEX = LAVA_STILL_TEXTURE_INDEX + 1; // Block.lavaMoving.blockIndexInTexture
    public static final int WATER_STILL_TEXTURE_INDEX = 12 * 16 + 13; // Block.waterStill.blockIndexInTexture
    public static final int WATER_FLOWING_TEXTURE_INDEX = WATER_STILL_TEXTURE_INDEX + 1; // Block.waterMoving.blockIndexInTexture
    public static final int FIRE_E_W_TEXTURE_INDEX = 1 * 16 + 15; // Block.fire.blockIndexInTexture;
    public static final int FIRE_N_S_TEXTURE_INDEX = FIRE_E_W_TEXTURE_INDEX + 16;
    public static final int PORTAL_TEXTURE_INDEX = 0 * 16 + 14; // Block.portal.blockIndexInTexture

    static TexturePackBase lastTexturePack;
    private static long textureChangeDetectTimer;

    public static void checkUpdate() {
        checkTexturePackFileChange();
        TexturePackBase selectedTexturePack = MCPatcherUtils.getMinecraft().texturePackList.selectedTexturePack;
        if (lastTexturePack != selectedTexturePack) {
            lastTexturePack = selectedTexturePack;
            TileSize.refresh();
            refreshTextureFX(MCPatcherUtils.getMinecraft().renderEngine.textureFXList);
            refreshCustomAnimations();
            refreshFontRenderer();
            refreshColorizers();
            System.gc();
        }
    }

    private static void refreshFontRenderer(Minecraft minecraft, FontRenderer fontRenderer, String filename) {
        boolean saveUnicode = fontRenderer.isUnicode;
        fontRenderer.initialize(minecraft.gameSettings, filename, minecraft.renderEngine);
        fontRenderer.isUnicode = saveUnicode;
    }

    private static void refreshFontRenderer() {
        MCPatcherUtils.log("refreshFontRenderer()");
        Minecraft minecraft = MCPatcherUtils.getMinecraft();
        refreshFontRenderer(minecraft, minecraft.fontRenderer, "/font/default.png");
        if (minecraft.alternateFontRenderer != minecraft.fontRenderer) {
            refreshFontRenderer(minecraft, minecraft.alternateFontRenderer, "/font/alternate.png");
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
            textureFX instanceof Portal) {
            return null;
        }
        MCPatcherUtils.warn("attempting to refresh unknown animation %s", textureFX.getClass().getName());
        Minecraft minecraft = MCPatcherUtils.getMinecraft();
        Class<? extends TextureFX> textureFXClass = textureFX.getClass();
        TileSize tileSize = TileSize.getTileSize(textureFX);
        for (int i = 0; i < 3; i++) {
            Constructor<? extends TextureFX> constructor;
            try {
                switch (i) {
                    case 0:
                        constructor = textureFXClass.getConstructor(Minecraft.class, Integer.TYPE);
                        return constructor.newInstance(minecraft, tileSize);

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
        if (textureFX.imageData.length < tileSize.int_numBytes) {
            MCPatcherUtils.log("resizing %s buffer from %d to %d bytes",
                textureFXClass.getName(), textureFX.imageData.length, tileSize.int_numBytes
            );
            textureFX.imageData = new byte[tileSize.int_numBytes];
        }
        try {
            Method refreshMethod = textureFXClass.getDeclaredMethod("refresh", Minecraft.class, Integer.TYPE);
            refreshMethod.invoke(textureFX, minecraft, tileSize);
        } catch (NoSuchMethodException e) {
        } catch (IllegalAccessException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return textureFX;
    }

    private static void refreshTextureFX(java.util.List<TextureFX> textureList) {
        MCPatcherUtils.log("refreshTextureFX()");

        ArrayList<TextureFX> savedTextureFX = new ArrayList<TextureFX>();
        for (TextureFX t : textureList) {
            TextureFX fx = refreshTextureFX(t);
            if (fx != null) {
                savedTextureFX.add(fx);
            }
        }
        CustomAnimation.clear();
        textureList.clear();

        Minecraft minecraft = MCPatcherUtils.getMinecraft();
        textureList.add(new Compass(minecraft));
        textureList.add(new Watch(minecraft));

        TexturePackBase selectedTexturePack = getSelectedTexturePack();
        boolean isDefault = (selectedTexturePack == null || selectedTexturePack instanceof TexturePackDefault);

        if (!isDefault && customLava) {
            CustomAnimation.addStripOrTile("/terrain.png", "lava_still", LAVA_STILL_TEXTURE_INDEX, 1, -1, -1);
            CustomAnimation.addStripOrTile("/terrain.png", "lava_flowing", LAVA_FLOWING_TEXTURE_INDEX, 2, 3, 6);
        } else if (animatedLava) {
            textureList.add(new StillLava());
            textureList.add(new FlowLava());
        }

        if (!isDefault && customWater) {
            CustomAnimation.addStripOrTile("/terrain.png", "water_still", WATER_STILL_TEXTURE_INDEX, 1, -1, -1);
            CustomAnimation.addStripOrTile("/terrain.png", "water_flowing", WATER_FLOWING_TEXTURE_INDEX, 2, 0, 0);
        } else if (animatedWater) {
            textureList.add(new StillWater());
            textureList.add(new FlowWater());
        }

        if (!isDefault && customFire && hasResource("/anim/custom_fire_e_w.png") && hasResource("/anim/custom_fire_n_s.png")) {
            CustomAnimation.addStrip("/terrain.png", "fire_n_s", FIRE_N_S_TEXTURE_INDEX, 1);
            CustomAnimation.addStrip("/terrain.png", "fire_e_w", FIRE_E_W_TEXTURE_INDEX, 1);
        } else if (animatedFire) {
            textureList.add(new Fire(0));
            textureList.add(new Fire(1));
        }

        if (!isDefault && customPortal && hasResource("/anim/custom_portal.png")) {
            CustomAnimation.addStrip("/terrain.png", "portal", PORTAL_TEXTURE_INDEX, 1);
        } else if (animatedPortal) {
            textureList.add(new Portal());
        }

        for (TextureFX t : savedTextureFX) {
            textureList.add(t);
        }

        for (TextureFX t : textureList) {
            t.onTick();
        }
    }

    private static void addOtherTextureFX(String textureName, String imageName) {
        for (int tileNum = 0; tileNum < 256; tileNum++) {
            String resource = "/anim/custom_" + imageName + "_" + tileNum + ".png";
            if (hasResource(resource)) {
                CustomAnimation.addStrip(textureName, imageName + "_" + tileNum, tileNum, 1);
            }
        }
    }

    private static void refreshCustomAnimations() {
        if (!customOther) {
            return;
        }

        addOtherTextureFX("/terrain.png", "terrain");
        addOtherTextureFX("/gui/items.png", "item");

        TexturePackBase selectedTexturePack = getSelectedTexturePack();
        if (selectedTexturePack instanceof TexturePackCustom) {
            TexturePackCustom custom = (TexturePackCustom) selectedTexturePack;
            for (ZipEntry entry : Collections.list(custom.zipFile.entries())) {
                String name = "/" + entry.getName();
                if (name.startsWith("/anim/") && name.endsWith(".properties") && !isCustomTerrainItemResource(name)) {
                    InputStream inputStream = null;
                    try {
                        inputStream = custom.zipFile.getInputStream(entry);
                        Properties properties = new Properties();
                        properties.load(inputStream);
                        CustomAnimation.addStrip(properties);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        MCPatcherUtils.close(inputStream);
                    }
                }
            }
        } else if (selectedTexturePack.getClass().getSimpleName().equals("TexturePackFolder")) {
            File folder = null;
            try {
                for (Field field : selectedTexturePack.getClass().getFields()) {
                    if (field.getType().equals(File.class)) {
                        folder = (File) field.get(selectedTexturePack);
                        break;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (folder != null) {
                folder = new File(folder, "anim");
                if (folder.isDirectory()) {
                    for (File file : folder.listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".properties") && !isCustomTerrainItemResource("/anim/" + name);
                        }
                    })) {
                        InputStream inputStream = null;
                        try {
                            inputStream = new FileInputStream(file);
                            Properties properties = new Properties();
                            properties.load(inputStream);
                            CustomAnimation.addStrip(properties);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            MCPatcherUtils.close(inputStream);
                        }
                    }
                }
            }
        }

        CustomAnimation.updateAll();
    }

    private static void refreshColorizers() {
        if (ColorizerWater.colorBuffer != ColorizerFoliage.colorBuffer) {
            refreshColorizer(ColorizerWater.colorBuffer, "/misc/watercolor.png");
        }
        refreshColorizer(ColorizerGrass.colorBuffer, "/misc/grasscolor.png");
        refreshColorizer(ColorizerFoliage.colorBuffer, "/misc/foliagecolor.png");
    }

    public static TexturePackBase getSelectedTexturePack() {
        Minecraft minecraft = MCPatcherUtils.getMinecraft();
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
        return buffer;
    }

    private static boolean isRequiredResource(String resource) {
        return !(resource.startsWith("/custom_") ||
            resource.startsWith("/anim/custom_") ||
            resource.equals("/terrain_nh.png") ||
            resource.equals("/terrain_s.png") ||
            resource.matches("^/font/.*\\.properties$") ||
            resource.matches("^/mob/.*\\d+.png$")
        );
    }
    
    private static boolean isCustomTerrainItemResource(String resource) {
        resource = resource.replaceFirst("^/anim", "").replaceFirst("\\.(png|properties)$", "");
        return resource.equals("/custom_lava_still") ||
            resource.equals("/custom_lava_flowing") ||
            resource.equals("/custom_water_still") ||
            resource.equals("/custom_water_flowing") ||
            resource.equals("/custom_fire_n_s") ||
            resource.equals("/custom_fire_e_w") ||
            resource.equals("/custom_portal") ||
            resource.matches("^/custom_(terrain|item)_\\d+$");
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
        if (is == null && resource.startsWith("/anim/custom_")) {
            is = getResourceAsStream(texturePack, resource.substring(5));
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

    public static BufferedImage getResourceAsBufferedImage(TexturePackBase texturePack, String resource) {
        if (texturePack == null) {
            return null;
        }
        BufferedImage image = MCPatcherUtils.readImage(texturePack.getInputStream(resource));
        if (image != null) {
            MCPatcherUtils.log("opened %s %dx%d from %s",
                resource, image.getWidth(), image.getHeight(), getTexturePackName(texturePack)
            );
            if (resource.matches("^/mob/.*_eyes\\d*\\.png$")) {
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
            else if (resource.equals("/misc/dial.png")) {
                image = resizeImage(image, TileSize.tileSizes[1].int_size);
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

    static boolean hasResource(TexturePackBase texturePack, String resource) {
        InputStream is = getResourceAsStream(texturePack, resource);
        boolean has = (is != null);
        MCPatcherUtils.close(is);
        return has;
    }

    static boolean hasResource(String s) {
        return hasResource(getSelectedTexturePack(), s);
    }

    static BufferedImage resizeImage(BufferedImage image, int width) {
        if (width == image.getWidth()) {
            return image;
        }
        int height = image.getHeight() * width / image.getWidth();
        MCPatcherUtils.log("  resizing to %dx%d", width, height);
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = newImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, width, height, null);
        return newImage;
    }

    private static void refreshColorizer(int[] colorBuffer, String resource) {
        try {
            BufferedImage image = getResourceAsBufferedImage(resource);
            if (image != null && image.getWidth() == 256 && image.getHeight() == 256) {
                image.getRGB(0, 0, 256, 256, colorBuffer, 0, 256);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void openTexturePackFile(TexturePackCustom pack) {
        if (!autoRefreshTextures || pack.zipFile == null) {
            return;
        }
        InputStream input = null;
        OutputStream output = null;
        ZipFile newZipFile = null;
        try {
            pack.lastModified = pack.file.lastModified();
            pack.tmpFile = File.createTempFile("tmpmc", ".zip");
            pack.tmpFile.deleteOnExit();
            MCPatcherUtils.close(pack.zipFile);
            input = new FileInputStream(pack.file);
            output = new FileOutputStream(pack.tmpFile);
            byte[] buffer = new byte[65536];
            while (true) {
                int nread = input.read(buffer);
                if (nread <= 0) {
                    break;
                }
                output.write(buffer, 0, nread);
            }
            MCPatcherUtils.close(input);
            MCPatcherUtils.close(output);
            newZipFile = new ZipFile(pack.tmpFile);
            pack.origZip = pack.zipFile;
            pack.zipFile = newZipFile;
            newZipFile = null;
            MCPatcherUtils.log("copied %s to %s, lastModified = %d", pack.file.getPath(), pack.tmpFile.getPath(), pack.lastModified);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MCPatcherUtils.close(input);
            MCPatcherUtils.close(output);
            MCPatcherUtils.close(newZipFile);
        }
    }

    public static void closeTexturePackFile(TexturePackCustom pack) {
        if (pack.origZip != null) {
            MCPatcherUtils.close(pack.zipFile);
            pack.zipFile = pack.origZip;
            pack.origZip = null;
            pack.tmpFile.delete();
            MCPatcherUtils.log("deleted %s", pack.tmpFile.getPath());
            pack.tmpFile = null;
        }
    }
    
    private static void checkTexturePackFileChange() {
        long now = System.currentTimeMillis();
        if (!autoRefreshTextures || now - textureChangeDetectTimer < 500L) {
            return;
        }
        textureChangeDetectTimer = now;
        TexturePackList list = MCPatcherUtils.getMinecraft().texturePackList;
        if (!(list.selectedTexturePack instanceof TexturePackCustom)) {
            return;
        }
        TexturePackCustom pack = (TexturePackCustom) list.selectedTexturePack;
        long lastModified = pack.file.lastModified();
        if (lastModified == pack.lastModified || lastModified == 0 || pack.lastModified == 0) {
            return;
        }
        MCPatcherUtils.log("%s lastModified changed from %d to %d", pack.file.getPath(), pack.lastModified, lastModified);
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(pack.file);
        } catch (IOException e) {
            // file is still being written
            return;
        } finally {
            MCPatcherUtils.close(zipFile);
        }
        pack.closeTexturePackFile();
        list.updateAvailableTexturePacks();
        for (TexturePackBase tp : list.availableTexturePacks()) {
            if (!(tp instanceof TexturePackCustom)) {
                continue;
            }
            TexturePackCustom tpc = (TexturePackCustom) tp;
            if (tpc.file.equals(pack.file)) {
                MCPatcherUtils.log("setting new texture pack");
                list.selectedTexturePack = list.defaultTexturePack;
                list.setTexturePack(tpc);
                return;
            }
        }
        MCPatcherUtils.log("selected texture pack not found after refresh, switching to default");
        list.setTexturePack(list.defaultTexturePack);
    }
}
