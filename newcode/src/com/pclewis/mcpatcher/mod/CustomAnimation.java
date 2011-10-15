package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.TextureFX;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

public class CustomAnimation extends TextureFX {
    private Delegate delegate;

    static private Random rand = new Random();

    public CustomAnimation(int tileNumber, int tileImage, int tileSize, String name, int minScrollDelay, int maxScrollDelay) {
        super(tileNumber);

        this.tileNumber = tileNumber;
        this.tileImage = tileImage;
        this.tileSize = tileSize;

        BufferedImage custom = null;
        String imageName = (tileImage == 0 ? "/terrain.png" : "/gui/items.png");
        try {
            String customSrc = "/custom_" + name + ".png";
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
            delegate = new Strip(custom);
        }
    }

    static private void ARGBtoRGBA(int[] src, byte[] dest) {
        for (int i = 0; i < src.length; ++i) {
            int v = src[i];
            dest[(i * 4) + 3] = (byte) ((v >> 24) & 0xff);
            dest[(i * 4) + 0] = (byte) ((v >> 16) & 0xff);
            dest[(i * 4) + 1] = (byte) ((v >> 8) & 0xff);
            dest[(i * 4) + 2] = (byte) ((v >> 0) & 0xff);
        }
    }

    @Override
    public void onTick() {
        delegate.onTick();
    }

    private interface Delegate {
        public void onTick();
    }

    private class Tile implements Delegate {
        private final int allButOneRow;
        private final int oneRow;
        private final int minScrollDelay;
        private final int maxScrollDelay;
        private final boolean isScrolling;
        private final byte[] temp;

        private int timer;

        Tile(String imageName, int tileNumber, int minScrollDelay, int maxScrollDelay) {
            oneRow = TileSize.int_size * 4;
            allButOneRow = (TileSize.int_size - 1) * oneRow;
            this.minScrollDelay = minScrollDelay;
            this.maxScrollDelay = maxScrollDelay;
            isScrolling = (this.minScrollDelay >= 0);
            if (isScrolling) {
                temp = new byte[oneRow];
            } else {
                temp = null;
            }

            BufferedImage tiles;
            try {
                tiles = TextureUtils.getResourceAsBufferedImage(imageName);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            int tileX = (tileNumber % 16) * TileSize.int_size;
            int tileY = (tileNumber / 16) * TileSize.int_size;
            int imageBuf[] = new int[TileSize.int_numPixels];
            tiles.getRGB(tileX, tileY, TileSize.int_size, TileSize.int_size, imageBuf, 0, TileSize.int_size);
            ARGBtoRGBA(imageBuf, imageData);
        }

        public void onTick() {
            if (isScrolling && (maxScrollDelay <= 0 || --timer <= 0)) {
                if (maxScrollDelay > 0) {
                    timer = rand.nextInt(maxScrollDelay - minScrollDelay + 1) + minScrollDelay;
                }
                System.arraycopy(imageData, allButOneRow, temp, 0, oneRow);
                System.arraycopy(imageData, 0, imageData, oneRow, allButOneRow);
                System.arraycopy(temp, 0, imageData, 0, oneRow);
            }
        }
    }

    private class Strip implements Delegate {
        private final int oneFrame;
        private final byte[] src;
        private final int numFrames;

        private int currentFrame;

        Strip(BufferedImage custom) {
            oneFrame = TileSize.int_size * TileSize.int_size * 4;
            numFrames = custom.getHeight() / custom.getWidth();
            int imageBuf[] = new int[custom.getWidth() * custom.getHeight()];
            custom.getRGB(0, 0, custom.getWidth(), custom.getHeight(), imageBuf, 0, TileSize.int_size);
            src = new byte[imageBuf.length * 4];
            ARGBtoRGBA(imageBuf, src);
        }

        public void onTick() {
            if (++currentFrame >= numFrames) {
                currentFrame = 0;
            }
            System.arraycopy(src, currentFrame * oneFrame, imageData, 0, oneFrame);
        }
    }
}
