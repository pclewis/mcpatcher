package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.TexturePackBase;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

public class CustomAnimation {
    private static final String CLASS_NAME = CustomAnimation.class.getName();

    private static Random rand = new Random();
    private static final ArrayList<CustomAnimation> animations = new ArrayList<CustomAnimation>();
    private static TexturePackBase lastTexturePack;

    private Delegate delegate;

    public static void updateAll() {
        checkUpdate();
        for (CustomAnimation animation : animations) {
            animation.delegate.update();
        }
    }

    private static void checkUpdate() {
        TexturePackBase selectedTexturePack = MCPatcherUtils.getMinecraft().texturePackList.selectedTexturePack;
        if (selectedTexturePack == lastTexturePack) {
            return;
        }
        lastTexturePack = selectedTexturePack;
        animations.clear();
    }
    
    static void add(String textureName, String name, int tileNumber, int minScrollDelay, int maxScrollDelay) {
        String srcName = "/custom_" + name + ".png";
        BufferedImage srcImage = MCPatcherUtils.readImage(lastTexturePack.getInputStream(srcName));
        if (srcImage == null) {
            delegate = new Tile(textureName, tileNumber, minScrollDelay, maxScrollDelay);
        } else {
            add(textureName, srcName, srcImage, (tileNumber % 16) * TileSize.int_size, (tileNumber / 16) * TileSize.int_size, TileSize.int_size, TileSize.int_size);
        }
    }

    static void add(String textureName, String srcName, int x, int y, int w, int h) {
        BufferedImage srcImage = MCPatcherUtils.readImage(lastTexturePack.getInputStream(srcName));
        if (srcImage == null) {
            return;
        }
        add(textureName, srcName, srcImage, x, y, w, h);
    }
    
    static boolean add(String textureName, String srcName, BufferedImage srcImage, int x, int y, int w, int h) {
        if (x < 0 || y < 0 || w <= 0 || h <= 0) {
            MCPatcherUtils.error("%s: %s invalid dimensions x=%d,y=%d,w=%d,h=%h", CLASS_NAME, srcName, x, y, w, h);
            return false;
        }
        BufferedImage destImage = MCPatcherUtils.readImage(lastTexturePack.getInputStream(textureName));
        if (destImage == null) {
            MCPatcherUtils.error("%s: %s not found", CLASS_NAME, textureName);
            return false;
        }
        if (x + w >= destImage.getWidth() || y + h >= destImage.getHeight()) {
            MCPatcherUtils.error("%s: %s invalid dimensions x=%d,y=%d,w=%d,h=%h", CLASS_NAME, srcName, x, y, w, h);
            return false;
        }
        int textureID = MCPatcherUtils.getMinecraft().renderEngine.getTexture(textureName);
        if (textureID <= 0) {
            MCPatcherUtils.error("%s: invalid id %d for texture %s", CLASS_NAME, textureID, textureName);
            return false;
        }
        int width = srcImage.getWidth();
        int height = srcImage.getHeight();
        if (width != w || height % h != 0) {
            MCPatcherUtils.error("%s: %s dimensions %dx%d do not match %dx%d", CLASS_NAME, srcName, width, height, w, h);
            return false;
        }
        ByteBuffer imageData = ByteBuffer.allocate(width * height * 4);
        int[] argb = new int[width * height];
        byte[] rgba = new byte[width * height * 4];
        srcImage.getRGB(0, 0, width, height, argb, 0, width);
        ARGBtoRGBA(argb, rgba);
        imageData.put(rgba);
        int numFrames = height / h;
        animations.add(new CustomAnimation(textureID, imageData, x, y, w, h, numFrames));
        MCPatcherUtils.log("new %s %s %dx%d -> %s @ %d,%d (%d frames)", CLASS_NAME, srcName, w, h, textureName, x, y, numFrames);
        return true;
    }

    private CustomAnimation(int textureID, ByteBuffer imageData, int x, int y, int w, int h, int numFrames) {
        this.textureID = textureID;
        this.imageData = imageData;
        this.numFrames = numFrames;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    CustomAnimation(int tileNumber, int tileImage, int tileSize, String name, int minScrollDelay, int maxScrollDelay) {
        this.tileNumber = tileNumber;
        this.tileImage = tileImage;
        this.tileSize = tileSize;

        BufferedImage custom = null;
        String imageName = (tileImage == 0 ? "/terrain.png" : "/gui/items.png");
        String customSrc = "/anim/custom_" + name + ".png";
        try {
            custom = TextureUtils.getResourceAsBufferedImage(customSrc);
            if (custom != null) {
                imageName = customSrc;
            }
        } catch (IOException ex) {
        }
        MCPatcherUtils.log("new CustomAnimation %s, src=%s, buffer size=0x%x, tile=%d",
            name, imageName, imageData.length, this.tileNumber
        );

        if (custom == null) {
            delegate = new Tile(imageName, tileNumber, minScrollDelay, maxScrollDelay);
        } else {
            Properties properties = null;
            InputStream inputStream = null;
            try {
                inputStream = TextureUtils.getResourceAsStream(customSrc.replaceFirst("\\.png$", ".properties"));
                if (inputStream != null) {
                    properties = new Properties();
                    properties.load(inputStream);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                MCPatcherUtils.close(inputStream);
            }
            delegate = new Strip(custom, properties);
        }
    }

    static void ARGBtoRGBA(int[] src, byte[] dest) {
        for (int i = 0; i < src.length; ++i) {
            int v = src[i];
            dest[(i * 4) + 3] = (byte) ((v >> 24) & 0xff);
            dest[(i * 4) + 0] = (byte) ((v >> 16) & 0xff);
            dest[(i * 4) + 1] = (byte) ((v >> 8) & 0xff);
            dest[(i * 4) + 2] = (byte) ((v >> 0) & 0xff);
        }
    }

    private interface Delegate {
        public void update();
    }

    private class Tile implements Delegate {
        private final int textureID;
        private final ByteBuffer imageData;
        private final int x;
        private final int y;
        private final int w;
        private final int h;

        private final int minScrollDelay;
        private final int maxScrollDelay;
        private final boolean isScrolling;

        private int currentFrame;
        private int currentDelay;

        Tile(String imageName, int tileNumber, int tileSize, int minScrollDelay, int maxScrollDelay) throws IOException {
            this.minScrollDelay = minScrollDelay;
            this.maxScrollDelay = maxScrollDelay;
            isScrolling = (this.minScrollDelay >= 0);

            BufferedImage tiles = TextureUtils.getResourceAsBufferedImage(imageName);
            textureID = MCPatcherUtils.getMinecraft().renderEngine.getTexture(imageName);

            x = (tileNumber % 16) * TileSize.int_size;
            y = (tileNumber / 16) * TileSize.int_size;
            w = TileSize.int_size * tileSize;
            h = TileSize.int_size * tileSize;

            int rgbInt[] = new int[w * h];
            byte rgbByte[] = new byte[w * h * 4];
            tiles.getRGB(x, y, w, h, rgbInt, 0, w * h);

            ARGBtoRGBA(rgbInt, rgbByte);
            imageData = ByteBuffer.allocate(TileSize.int_numBytes);
            imageData.put(rgbByte);
        }

        public void update() {
            if (isScrolling && (maxScrollDelay <= 0 || --currentDelay <= 0)) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y + h - currentFrame, w, currentFrame, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) imageData.position(4 * w * currentFrame));
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, w, h - currentFrame, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) imageData.position(4 * w * (h - currentFrame)));
                currentFrame = (currentFrame + 1) % h;
                if (maxScrollDelay > 0) {
                    currentDelay = rand.nextInt(maxScrollDelay - minScrollDelay + 1) + minScrollDelay;
                }
            }
        }
    }

    private class Strip implements Delegate {
        private final int textureID;
        private final ByteBuffer imageData;
        private final int x;
        private final int y;
        private final int w;
        private final int h;
        private final int numTiles;

        private int[] tileOrder;
        private int[] tileDelay;
        private int numFrames;
        private int currentFrame;
        private int currentDelay;

        Strip(int textureID, ByteBuffer imageData, int x, int y, int w, int h, int width, int height, Properties properties) {
            this.textureID = textureID;
            this.imageData = imageData;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            numFrames = numTiles = height / h;
            loadProperties(properties);
            currentFrame = -1;
        }

        private void loadProperties(Properties properties) {
            loadTileOrder(properties);
            if (tileOrder == null) {
                tileOrder = new int[numFrames];
                for (int i = 0; i < numFrames; i++) {
                    tileOrder[i] = i % numTiles;
                }
            }
            tileDelay = new int[numFrames];
            for (int i = 0; i < numFrames; i++) {
                tileDelay[i] = 1;
            }
            loadTileDelay(properties);
        }

        private void loadTileOrder(Properties properties) {
            if (properties == null) {
                return;
            }
            int i = 0;
            for (; getIntValue(properties, "tile.", i) != null; i++) {
            }
            if (i > 0) {
                numFrames = i;
                tileOrder = new int[numFrames];
                for (i = 0; i < numFrames; i++) {
                    tileOrder[i] = Math.abs(getIntValue(properties, "tile.", i)) % numTiles;
                }
            }
        }

        private void loadTileDelay(Properties properties) {
            if (properties == null) {
                return;
            }
            for (int i = 0; i < numFrames; i++) {
                Integer value = getIntValue(properties, "duration.", i);
                if (value != null) {
                    tileDelay[i] = Math.max(value, 1);
                }
            }
        }

        private Integer getIntValue(Properties properties, String prefix, int index) {
            try {
                String value = properties.getProperty(prefix + index);
                if (value != null && value.matches("^\\d+$")) {
                    return Integer.parseInt(value);
                }
            } catch (NumberFormatException e) {
            }
            return null;
        }

        public void update() {
            if (--currentDelay > 0) {
                return;
            }
            if (++currentFrame >= numFrames) {
                currentFrame = 0;
            }
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) imageData.position(w * h * 4 * currentFrame));
            currentDelay = tileDelay[currentFrame];
        }
    }
}
