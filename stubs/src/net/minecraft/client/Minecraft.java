package net.minecraft.client;

import net.minecraft.src.*;

import java.io.File;

public class Minecraft {
    public TexturePackList texturePackList;
    public RenderEngine renderEngine;
    public GameSettings gameSettings;
    public FontRenderer fontRenderer;
    public FontRenderer alternateFontRenderer;
    public World theWorld;
    public EntityPlayerSP thePlayer;
    public int displayWidth;
    public int displayHeight;

    public static File getAppDir(String s) {
        return null;
    }
}
