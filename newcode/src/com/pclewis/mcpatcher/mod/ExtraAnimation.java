package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.src.TexturePackBase;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ExtraAnimation extends CustomAnimation {
    private static final String CLASS_NAME = ExtraAnimation.class.getName();
    
    private static final ArrayList<ExtraAnimation> animations = new ArrayList<ExtraAnimation>();
    private static TexturePackBase lastTexturePack;

    private final int textureID;
    private final ByteBuffer imageData;
    private int frame;
    private final int numFrames;
    private final int x;
    private final int y;
    private final int w;
    private final int h;

    public static void updateAll() {
        checkUpdate();
        for (ExtraAnimation animation : animations) {
            animation.update();
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

    public static void add(String textureName, String srcName, int x, int y, int w, int h) {
        if (x < 0 || y < 0 || w <= 0 || h <= 0) {
            MCPatcherUtils.error("%s: %s invalid dimensions x=%d,y=%d,w=%d,h=%h", CLASS_NAME, srcName, x, y, w, h);
            return;
        }
        BufferedImage image = MCPatcherUtils.readImage(lastTexturePack.getInputStream(textureName));
        if (image == null) {
            MCPatcherUtils.error("%s: %s not found", CLASS_NAME, textureName);
            return;
        }
        if (x + w >= image.getWidth() || y + h >= image.getHeight()) {
            MCPatcherUtils.error("%s: %s invalid dimensions x=%d,y=%d,w=%d,h=%h", CLASS_NAME, srcName, x, y, w, h);
            return;
        }
        int textureID = MCPatcherUtils.getMinecraft().renderEngine.getTexture(textureName);
        if (textureID <= 0) {
            MCPatcherUtils.error("%s: invalid id %d for texture %s", CLASS_NAME, textureID, textureName);
            return;
        }
        image = MCPatcherUtils.readImage(lastTexturePack.getInputStream(srcName));
        if (image == null) {
            MCPatcherUtils.error("%s: %s not found", CLASS_NAME, srcName);
            return;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width != w || height % h != 0) {
            MCPatcherUtils.error("%s: %s dimensions %dx%d do not match %dx%d", CLASS_NAME, srcName, width, height, w, h);
            return;
        }
        ByteBuffer imageData = ByteBuffer.allocate(width * height * 4);
        int[] argb = new int[width * height];
        byte[] rgba = new byte[width * height * 4];
        image.getRGB(0, 0, width, height, argb, 0, width);
        CustomAnimation.ARGBtoRGBA(argb, rgba);
        imageData.put(rgba);
        int numFrames = height / h;
        animations.add(new ExtraAnimation(textureID, imageData, x, y, w, h, numFrames));
        MCPatcherUtils.log("new %s %s %dx%d -> %s @ %d,%d (%d frames)", CLASS_NAME, srcName, w, h, textureName, x, y, numFrames);
    }

    private ExtraAnimation(int textureID, ByteBuffer imageData, int x, int y, int w, int h, int numFrames) {
        this.textureID = textureID;
        this.imageData = imageData;
        this.numFrames = numFrames;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    private void update() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) imageData.position(w * h * frame));
        frame = (frame + 1) % numFrames;
    }
}
