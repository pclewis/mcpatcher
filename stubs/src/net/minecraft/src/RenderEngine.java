package net.minecraft.src;

import net.minecraft.client.Minecraft;

import java.util.List;

abstract public class RenderEngine {
    public TexturePackList texturePackList;
    public List<TextureFX> textureFXList;

    abstract public int getTexture(String s);

    abstract public void createTextureFromBytes(int[] rgb, int width, int height, int texture);

    abstract public void setTileSize(Minecraft minecraft);
}
