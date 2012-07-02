package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;

public class SkyRenderer {
    private static final double DISTANCE = 100.0;
    private static final float[][] ROTATIONS = new float[][]{
        {0.0f, 1.0f, 0.0f, 0.0f},
        {90.0f, 1.0f, 0.0f, 0.0f},
        {-90.0f, 1.0f, 0.0f, 0.0f},
        {180.0f, 1.0f, 0.0f, 0.0f},
        {90.0f, 0.0f, 0.0f, 1.0f},
        {-90.0f, 0.0f, 0.0f, 1.0f},
    };

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
                boolean allFaces = true;
                for (int i = 0; i < ROTATIONS.length; i++) {
                    if (MCPatcherUtils.readImage(texturePack.getInputStream(getSkyTexture(i))) == null) {
                        allFaces = false;
                        break;
                    }
                }
                haveSkyBox.put(worldType, allFaces);
                h = haveSkyBox.get(worldType);
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

            //GL11.glDisable(GL11.GL_FOG);
            //GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            //GL11.glDepthMask(false);

            for (int i = 0; i < ROTATIONS.length; i++) {
                renderEngine.bindTexture(renderEngine.getTexture(getSkyTexture(i)));
                GL11.glPushMatrix();

                //GL11.glRotatef(celestialAngle * 360.0f, 0.0f, 0.0f, 1.0f);
                float[] r = ROTATIONS[i];
                GL11.glRotatef(r[0], r[1], r[2], r[3]);

                tessellator.startDrawingQuads();
                tessellator.addVertexWithUV(-DISTANCE, -DISTANCE, -DISTANCE, 0.0, 0.0);
                tessellator.addVertexWithUV(-DISTANCE, -DISTANCE, DISTANCE, 0.0, 1.0);
                tessellator.addVertexWithUV(DISTANCE, -DISTANCE, DISTANCE, 1.0, 1.0);
                tessellator.addVertexWithUV(DISTANCE, -DISTANCE, -DISTANCE, 1.0, 0.0);
                tessellator.draw();
                GL11.glPopMatrix();
            }

            //GL11.glDepthMask(true);
            //GL11.glEnable(GL11.GL_TEXTURE_2D);
            //GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        }
        return active;
    }

    public static boolean renderStars() {
        return active;
    }

    private static String getSkyTexture(int face) {
        return "/terrain/world" + worldType + "/sky" + face + ".png";
    }
}
