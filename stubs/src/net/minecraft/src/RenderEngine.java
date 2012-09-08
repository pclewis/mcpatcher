package net.minecraft.src;

import net.minecraft.client.Minecraft;

import java.awt.image.BufferedImage;

public class RenderEngine {
    public TexturePackList texturePackList;

    public int getTexture(String s) {
        return -1;
    }

    public void bindTexture(int texture) {
    }

    public void deleteTexture(int texture) {
    }

    public void createTextureFromBytes(int[] rgb, int width, int height, int texture) {
    }

    public int allocateAndSetupTexture(BufferedImage image) {
        return -1;
    }

    public void reloadTextures(Minecraft minecraft) {
    }
}
