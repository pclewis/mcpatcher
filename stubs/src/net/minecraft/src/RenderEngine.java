package net.minecraft.src;

import net.minecraft.client.Minecraft;

import java.awt.image.BufferedImage;

public class RenderEngine {
    public TexturePackList texturePackList;
    public boolean clampTexture;
    public boolean blurTexture;

    public int getTexture(String s) {
        return -1;
    }

    public void bindTexture(int texture) {
    }

    public void deleteTexture(int texture) {
    }

    public void createTextureFromBytes(int[] rgb, int width, int height, int texture) {
    }

    public int[] readTextureImageData(String s) {
        return null;
    }

    public int allocateAndSetupTexture(BufferedImage image) {
        return -1;
    }

    public void setupTexture(BufferedImage image, int texture) {
    }

    public void reloadTextures(Minecraft minecraft) {
    }
}
