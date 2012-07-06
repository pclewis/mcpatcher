package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class SkyRenderer {
    private static final double DISTANCE = 100.0;

    private static RenderEngine renderEngine;
    private static double worldTime;

    private static final HashMap<Integer, ArrayList<Layer>> worldSkies = new HashMap<Integer, ArrayList<Layer>>();
    private static ArrayList<Layer> currentSkies;
    private static TexturePackBase lastTexturePack;

    public static boolean active;

    public static void setup(World world, RenderEngine renderEngine, float partialTick) {
        Minecraft minecraft = MCPatcherUtils.getMinecraft();
        TexturePackBase texturePack = minecraft.texturePackList.getSelectedTexturePack();
        if (texturePack != lastTexturePack) {
            lastTexturePack = texturePack;
            worldSkies.clear();
        }
        if (texturePack instanceof TexturePackDefault || Keyboard.isKeyDown(Keyboard.KEY_ADD)) {
            active = false;
        } else {
            int worldType = minecraft.getWorld().worldProvider.worldType;
            currentSkies = worldSkies.get(worldType);
            if (currentSkies == null) {
                currentSkies = new ArrayList<Layer>();
                worldSkies.put(worldType, currentSkies);
                for (int i = 1; ; i++) {
                    String prefix = "/terrain/sky" + worldType + "/sky" + i;
                    Layer layer = Layer.create(prefix);
                    if (layer == null) {
                        break;
                    } else {
                        currentSkies.add(layer);
                    }
                }
            }
            active = !currentSkies.isEmpty();
            if (active) {
                SkyRenderer.renderEngine = renderEngine;
                worldTime = world.getWorldTime() + partialTick;
            }
        }
    }

    public static void renderSky(boolean rotate) {
        Tessellator tessellator = Tessellator.instance;
        for (Layer layer : currentSkies) {
            if (layer.rotate == rotate) {
                layer.render(tessellator, worldTime);
            }
        }
    }

    private static void checkGLError() {
        int error = GL11.glGetError();
        if (error != 0) {
            throw new RuntimeException("GL error: " + GLU.gluErrorString(error));
        }
    }

    private static class Layer {
        private static final int SECS_PER_DAY = 24 * 60 * 60;
        private static final int TICKS_PER_DAY = 24000;

        String prefix;
        String texture;
        int startFadeIn;
        int endFadeIn;
        int startFadeOut;
        int endFadeOut;
        boolean rotate;
        boolean valid;

        double cos1;
        double sin1;
        double add1;

        static Layer create(String prefix) {
            Properties properties = null;
            InputStream is = null;
            try {
                is = lastTexturePack.getInputStream(prefix + ".properties");
                if (is != null) {
                    properties = new Properties();
                    properties.load(is);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                MCPatcherUtils.close(is);
            }
            Layer layer = new Layer(prefix, properties);
            if (layer.valid) {
                MCPatcherUtils.info("loaded %s.properties", prefix);
                return layer;
            } else {
                return null;
            }
        }

        private Layer(String prefix, Properties properties) {
            this.prefix = prefix;
            valid = true;
            if (properties == null) {
                valid = false;
                return;
            }
            texture = properties.getProperty("source", prefix + ".png");
            if (MCPatcherUtils.readImage(lastTexturePack.getInputStream(texture)) == null) {
                addError("texture %s not found", texture);
                return;
            }
            startFadeIn = parseTime(properties.getProperty("startFadeIn", "20:00"));
            endFadeIn = parseTime(properties.getProperty("endFadeIn", "22:00"));
            startFadeOut = parseTime(properties.getProperty("startFadeOut", "5:00"));
            endFadeOut = parseTime(properties.getProperty("endFadeOut", "7:00"));
            while (endFadeIn <= startFadeIn) {
                endFadeIn += SECS_PER_DAY;
            }
            while (startFadeOut <= endFadeIn) {
                startFadeOut += SECS_PER_DAY;
            }
            while (endFadeOut <= startFadeOut) {
                endFadeOut += SECS_PER_DAY;
            }
            if (endFadeOut - startFadeIn >= SECS_PER_DAY) {
                addError("%s.properties: fade times are incoherent", prefix);
                return;
            }
            double s0 = normalize(startFadeIn, SECS_PER_DAY);
            double s1 = normalize(endFadeIn, SECS_PER_DAY);
            double e1 = normalize(endFadeOut, SECS_PER_DAY);
            double det = Math.cos(s0) * Math.sin(s1) + Math.cos(e1) * Math.sin(s0) + Math.cos(s1) * Math.sin(e1) -
                Math.cos(s0) * Math.sin(e1) - Math.cos(s1) * Math.sin(s0) - Math.cos(e1) * Math.sin(s1);
            if (det == 0.0) {
                addError("%s.properties: determinant is 0", prefix);
                return;
            }
            cos1 = (Math.sin(e1) - Math.sin(s0)) / det;
            sin1 = (Math.cos(s0) - Math.cos(e1)) / det;
            add1 = (Math.cos(e1) * Math.sin(s0) - Math.cos(s0) * Math.sin(e1)) / det;
            MCPatcherUtils.info("%s.properties: y = %f cos x + %f sin x + %f", cos1, sin1, add1);
        }

        private void addError(String format, Object... params) {
            MCPatcherUtils.error(prefix + ".properties: " + format, params);
            valid = false;
        }

        private static int parseTime(String s) {
            String[] t = s.split(":");
            if (t.length >= 2) {
                try {
                    int hh = Integer.parseInt(t[0]);
                    int mm = Integer.parseInt(t[1]);
                    int ss;
                    if (t.length >= 3) {
                        ss = Integer.parseInt(t[2]);
                    } else {
                        ss = 0;
                    }
                    return (60 * 60 * hh + 60 * mm + ss) % SECS_PER_DAY;
                } catch (NumberFormatException e) {
                    MCPatcherUtils.error("invalid time %s", s);
                }
            }
            return -1;
        }

        private static double normalize(double t, int d) {
            return 2.0 * Math.PI * t / d;
        }

        boolean render(Tessellator tessellator, double worldTime) {
            double x = normalize(worldTime, TICKS_PER_DAY);
            float brightness = (float) (cos1 * Math.cos(x) + sin1 * Math.sin(x) + add1);
            if (brightness <= 0.0) {
                return false;
            }
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, brightness);

            renderEngine.bindTexture(renderEngine.getTexture(texture));

            drawBox(tessellator);

            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            return true;
        }

        private static void drawBox(Tessellator tessellator) {
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
    }
}
