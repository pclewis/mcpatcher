package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.TextureFX;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

public class CustomAnimation extends TextureFX {
    private int frame;
    private int numFrames;
    private byte[] src;
    private byte[] temp;
    private int minScrollDelay = -1;
    private int maxScrollDelay = -1;
    private int timer = -1;
    private boolean isScrolling;

    static private Random rand = new Random();

    public CustomAnimation(int tileNumber, int tileImage, int tileSize, String name, int minScrollDelay, int maxScrollDelay) {
        super(tileNumber);

        this.tileNumber = tileNumber;
        this.tileImage = tileImage;
        this.tileSize = tileSize;
        this.minScrollDelay = minScrollDelay;
        this.maxScrollDelay = maxScrollDelay;
        this.isScrolling = (minScrollDelay >= 0);

        BufferedImage custom = null;
        String customSrc = "custom_" + name + ".png";
        try {
            custom = TextureUtils.getResourceAsBufferedImage("/" + customSrc);
        } catch (IOException ex) {
        }
        MCPatcherUtils.log("new CustomAnimation %s, src=%s, buffer size=0x%x, tile=%d",
            name, (custom == null ? "terrain.png" : customSrc), imageData.length, this.tileNumber
        );

        if (custom == null) {
            BufferedImage tiles;
            try {
                tiles = TextureUtils.getResourceAsBufferedImage("/terrain.png");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            int tileX = (tileNumber % 16) * TileSize.int_size;
            int tileY = (tileNumber / 16) * TileSize.int_size;
            int imageBuf[] = new int[TileSize.int_numPixels];
            tiles.getRGB(tileX, tileY, TileSize.int_size, TileSize.int_size, imageBuf, 0, TileSize.int_size);
            ARGBtoRGBA(imageBuf, imageData);
            if (isScrolling) {
                temp = new byte[TileSize.int_size * 4];
            }
        } else {
            numFrames = custom.getHeight() / custom.getWidth();
            int imageBuf[] = new int[custom.getWidth() * custom.getHeight()];
            custom.getRGB(0, 0, custom.getWidth(), custom.getHeight(), imageBuf, 0, TileSize.int_size);
            this.src = new byte[imageBuf.length * 4];
            ARGBtoRGBA(imageBuf, this.src);
        }
    }

    static private void ARGBtoRGBA(int[] src, byte[] dest) {
        for (int i = 0; i < src.length; ++i) {
            int v = src[i];
            dest[(i * 4) + 3] = (byte) ((v >> 24) & 0xFF);
            dest[(i * 4) + 0] = (byte) ((v >> 16) & 0xFF);
            dest[(i * 4) + 1] = (byte) ((v >> 8) & 0xFF);
            dest[(i * 4) + 2] = (byte) ((v >> 0) & 0xFF);
        }
    }

    @Override
    public void onTick() {
        if (src != null) {
            if (++frame >= numFrames) {
                frame = 0;
            }
            System.arraycopy(src, (frame * (TileSize.int_size * TileSize.int_size * 4)), imageData, 0, TileSize.int_size * TileSize.int_size * 4);
        } else if (isScrolling) {
            if (maxScrollDelay <= 0 || --this.timer <= 0) {
                if (maxScrollDelay > 0) {
                    timer = rand.nextInt(maxScrollDelay - minScrollDelay + 1) + minScrollDelay;
                }
                System.arraycopy(imageData, (TileSize.int_size - 1) * TileSize.int_size * 4, temp, 0, TileSize.int_size * 4);
                System.arraycopy(imageData, 0, imageData, TileSize.int_size * 4, TileSize.int_size * (TileSize.int_size - 1) * 4);
                System.arraycopy(temp, 0, imageData, 0, TileSize.int_size * 4);
            }
        }
    }
}
