package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;

public class SkyRenderer {
    private static final double DISTANCE = 100.0;

    private static RenderEngine renderEngine;
    private static float partialTick;
    private static float celestialAngle;
    private static int worldType;

    public static boolean active;

    private static final HashMap<Integer, Boolean> haveSkyBox = new HashMap<Integer, Boolean>();
    private static TexturePackBase lastTexturePack;

    public static void setup(World world, RenderEngine renderEngine, float partialTick) {
        Minecraft minecraft = MCPatcherUtils.getMinecraft();
        TexturePackBase texturePack = minecraft.texturePackList.getSelectedTexturePack();
        if (texturePack != lastTexturePack) {
            lastTexturePack = texturePack;
            haveSkyBox.clear();
        }
        if (texturePack instanceof TexturePackDefault || Keyboard.isKeyDown(Keyboard.KEY_ADD)) {
            active = false;
        } else {
            worldType = minecraft.getWorld().worldProvider.worldType;
            Boolean h = haveSkyBox.get(worldType);
            if (h == null) {
                h = MCPatcherUtils.readImage(texturePack.getInputStream(getSkyTexture())) != null;
                haveSkyBox.put(worldType, h);
            }
            active = h;
            if (active) {
                SkyRenderer.partialTick = partialTick;
                SkyRenderer.renderEngine = renderEngine;
                celestialAngle = world.getCelestialAngle(partialTick);
            }
        }
    }

    public static boolean renderSky() {
        if (active) {
            Tessellator tessellator = Tessellator.instance;

            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            renderEngine.bindTexture(renderEngine.getTexture(getSkyTexture()));

            GL11.glPushMatrix();

            // north
            GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
            GL11.glRotatef(-90.0f, 0.0f, 0.0f, 1.0f);
            drawTile(tessellator, 5);

            // top
            GL11.glPushMatrix();
            GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
            drawTile(tessellator, 1);
            GL11.glPopMatrix();

            // bottom
            GL11.glPushMatrix();
            GL11.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
            drawTile(tessellator, 9);
            GL11.glPopMatrix();

            // west
            GL11.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
            drawTile(tessellator, 6);

            // south
            GL11.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
            drawTile(tessellator, 7);

            // east
            GL11.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
            drawTile(tessellator, 4);

            GL11.glPopMatrix();

            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        }
        return active;
    }

    public static boolean renderStars() {
        return active;
    }

    private static void drawTile(Tessellator tessellator, int tile) {
        double tileX = (tile % 4) / 4.0;
        double tileY = (tile / 4) / 3.0;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(-DISTANCE, -DISTANCE, -DISTANCE, tileX, tileY);
        tessellator.addVertexWithUV(-DISTANCE, -DISTANCE, DISTANCE, tileX, tileY + 1.0 / 3.0);
        tessellator.addVertexWithUV(DISTANCE, -DISTANCE, DISTANCE, tileX + 0.25, tileY + 1.0 / 3.0);
        tessellator.addVertexWithUV(DISTANCE, -DISTANCE, -DISTANCE, tileX + 0.25, tileY);
        tessellator.draw();
    }

    private static String getSkyTexture() {
        return "/terrain/world" + worldType + "/sky.png";
    }
}
