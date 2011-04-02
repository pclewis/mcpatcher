import com.pclewis.mcpatcher.MCPatcherUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class TextureUtils {
    public static Minecraft minecraft;

    private static boolean animatedFire;
    private static boolean animatedLava;
    private static boolean animatedWater;
    private static boolean customLava;
    private static boolean customWater;
    private static boolean customPortal;

    public static final int LAVA_STILL_TEXTURE_INDEX = 14 * 16 + 13;  // Block.lavaStill.blockIndexInTexture
    public static final int LAVA_FLOWING_TEXTURE_INDEX = LAVA_STILL_TEXTURE_INDEX + 1; // Block.lavaMoving.blockIndexInTexture
    public static final int WATER_STILL_TEXTURE_INDEX = 12 * 16 + 13; // Block.waterStill.blockIndexInTexture
    public static final int WATER_FLOWING_TEXTURE_INDEX = WATER_STILL_TEXTURE_INDEX + 1; // Block.waterMoving.blockIndexInTexture
    public static final int PORTAL_TEXTURE_INDEX = 0 * 16 + 14; // Block.portal.blockIndexInTexture

    private static HashMap<String, Integer> expectedColumns = new HashMap<String, Integer>();

    private static TexturePackBase lastTexturePack = null;
    private static HashMap<String, BufferedImage> cache = new HashMap<String, BufferedImage>();
    private static boolean fromCustom = false;

    static {
        animatedFire = MCPatcherUtils.getBoolean("HDTexture", "animatedFire", true);
        animatedLava = MCPatcherUtils.getBoolean("HDTexture", "animatedLava", true);
        animatedWater = MCPatcherUtils.getBoolean("HDTexture", "animatedWater", true);
        customLava = MCPatcherUtils.getBoolean("HDTexture", "customLava", true);
        customWater = MCPatcherUtils.getBoolean("HDTexture", "customWater", true);
        customPortal = MCPatcherUtils.getBoolean("HDTexture", "customPortal", true);
        MCPatcherUtils.saveProperties();

        TextureUtils.expectedColumns.put("/gui/items.png", 16);
        TextureUtils.expectedColumns.put("/misc/dial.png", 1);
        TextureUtils.expectedColumns.put("/custom_lava_still.png", 1);
        TextureUtils.expectedColumns.put("/custom_lava_flowing.png", 1);
        TextureUtils.expectedColumns.put("/custom_water_still.png", 1);
        TextureUtils.expectedColumns.put("/custom_water_flowing.png", 1);
        TextureUtils.expectedColumns.put("/custom_portal.png", 1);
    }

    public static boolean setTileSize() {
        int size = getTileSize();
        File options = MCPatcherUtils.getMinecraftPath("options.txt");
        BufferedReader br = null;
        if (options.exists()) {
            try {
                br = new BufferedReader(new FileReader(options));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.matches("^skin:.*")) {
                        line = line.substring(5);
                        MCPatcherUtils.log("\nchanging skin to %s", line);
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
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
    }

    public static void refreshTextureFX(java.util.List<TextureFX> textureList) {
        MCPatcherUtils.log("refreshTextureFX()");

        textureList.clear();

        if (animatedFire) {
            textureList.add(new Fire(0));
            textureList.add(new Fire(1));
        }
        textureList.add(new Compass(minecraft));
        textureList.add(new Watch(minecraft));

        TexturePackBase selectedTexturePack = getSelectedTexturePack();
        boolean isDefault = (selectedTexturePack == null || selectedTexturePack instanceof TexturePackDefault);

        if (!isDefault && customLava) {
            textureList.add(new CustomAnimation(LAVA_STILL_TEXTURE_INDEX, 0, 1, "lava_still", -1, -1));
            textureList.add(new CustomAnimation(LAVA_FLOWING_TEXTURE_INDEX, 0, 2, "lava_flowing", -1, -1));
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

        if (!isDefault && customPortal && hasResource("/custom_portal.png")) {
            textureList.add(new CustomAnimation(PORTAL_TEXTURE_INDEX, 0, 1, "portal", -1, -1));
        } else {
            textureList.add(new Portal());
        }

        for (TextureFX t : textureList) {
            t.onTick();
        }

        refreshColorizer(ColorizerFoliage.colorBuffer, "/misc/foliagecolor.png");
        refreshColorizer(ColorizerGrass.colorBuffer, "/misc/grasscolor.png");

        System.gc();
    }

    public static TexturePackBase getSelectedTexturePack() {
        return minecraft == null ? null :
            minecraft.texturePackList == null ? null :
                minecraft.texturePackList.selectedTexturePack;
    }

    public static ByteBuffer getByteBuffer(ByteBuffer buffer, byte[] data) {
        buffer.clear();
        final int have = buffer.capacity();
        final int needed = data.length;
        if (needed > have || have >= 4 * needed) {
            MCPatcherUtils.log("resizing gl buffer from 0x%x to 0x%x", have, needed);
            buffer = GLAllocation.createDirectByteBuffer(needed);
        }
        buffer.put(data);
        buffer.position(0).limit(needed);
        TileSize.int_glBufferSize = needed;
        return buffer;
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
            fromCustom = false;
        } else {
            fromCustom = !(texturePack instanceof TexturePackDefault);
        }
        return is;
    }

    public static InputStream getResourceAsStream(String resource) {
        return getResourceAsStream(getSelectedTexturePack(), resource);
    }

    public static BufferedImage getResourceAsBufferedImage(TexturePackBase texturePack, String resource) throws IOException {
        BufferedImage image = null;
        boolean cached = false;

        if (texturePack == lastTexturePack) {
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
                    try {
                        is.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }

        if (image == null) {
            throw new IOException(resource + " image is null");
        }

        if (!cached && texturePack != lastTexturePack) {
            MCPatcherUtils.log("clearing texture cache (%d items)", cache.size());
            cache.clear();
        }
        MCPatcherUtils.log("opened %s %dx%d from %s",
            resource, image.getWidth(), image.getHeight(),
            (cached ? "cache" : fromCustom ? "custom texture pack" : "default texture pack")
        );
        if (!cached) {
            Integer i = expectedColumns.get(resource);
            if (i != null && image.getWidth() != i * TileSize.int_size) {
                image = resizeImage(image, i * TileSize.int_size);
            }
            lastTexturePack = texturePack;
            cache.put(resource, image);
        }

        return image;
    }

    public static BufferedImage getResourceAsBufferedImage(String resource) throws IOException {
        return getResourceAsBufferedImage(getSelectedTexturePack(), resource);
    }

    public static int getTileSize(TexturePackBase texturePack) {
        int size = 16;
        try {
            size = getResourceAsBufferedImage(texturePack, "/terrain.png").getWidth() / 16;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    public static int getTileSize() {
        return getTileSize(getSelectedTexturePack());
    }

    public static boolean hasResource(TexturePackBase texturePack, String resource) {
        InputStream is = getResourceAsStream(texturePack, resource);
        boolean has = (is != null);
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
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
