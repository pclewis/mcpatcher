package net.minecraft.client;

import net.minecraft.src.*;

import java.io.File;

public class Minecraft {
    public TexturePackList texturePackList;
    public RenderEngine renderEngine;
    public GameSettings gameSettings;
    public FontRenderer fontRenderer;
    public FontRenderer alternateFontRenderer;
    public EntityLiving renderViewEntity;
    public EntityRenderer entityRenderer;
    public int displayWidth;
    public int displayHeight;
    public MovingObjectPosition objectMouseOver;
    public RenderGlobal renderGlobal;
    public GuiScreen currentScreen;
    public Timer timer;
    public Profiler mcProfiler;

    public static File getMinecraftDir() {
        return null;
    }

    public static File getAppDir(String s) {
        return null;
    }

    public World getWorld() {
        return null;
    }

    public EntityPlayerSP getPlayer() {
        return null;
    }
}
