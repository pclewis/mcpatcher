package net.minecraft.src;

import net.minecraft.client.Minecraft;

import java.awt.image.BufferedImage;

abstract public class RenderEngine {
    public TexturePackList texturePackList;

    abstract public int getTexture(String s);

    abstract public void bindTexture(int texture);

    abstract public void createTextureFromBytes(int[] rgb, int width, int height, int texture);

    abstract public int allocateAndSetupTexture(BufferedImage image);

    abstract public void reloadTextures(Minecraft minecraft);
}
