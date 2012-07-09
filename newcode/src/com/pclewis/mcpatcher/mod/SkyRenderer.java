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
    private static RenderEngine renderEngine;
    private static double worldTime;
    private static float celestialAngle;
    private static float rainStrength;

    private static final HashMap<Integer, ArrayList<Layer>> worldSkies = new HashMap<Integer, ArrayList<Layer>>();
    private static ArrayList<Layer> currentSkies;
    private static TexturePackBase lastTexturePack;

    public static boolean active;

    public static void setup(World world, RenderEngine renderEngine, float partialTick, float celestialAngle) {
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
                for (int i = -1; ; i++) {
                    String prefix = "/terrain/sky" + (i < 0 ? "" : "" + worldType) + "/sky" + i;
                    Layer layer = Layer.create(prefix);
                    if (layer == null) {
                        if (i > 0) {
                            break;
                        }
                    } else if (layer.valid) {
                        MCPatcherUtils.info("loaded %s.properties", prefix);
                        currentSkies.add(layer);
                    }
                }
            }
            active = !currentSkies.isEmpty();
            if (active) {
                SkyRenderer.renderEngine = renderEngine;
                worldTime = world.getWorldTime() + partialTick;
                rainStrength = 1.0f - world.getRainStrength(partialTick);
                SkyRenderer.celestialAngle = celestialAngle;
            }
        }
    }

    public static void renderAll() {
        if (active) {
            Tessellator tessellator = Tessellator.instance;
            for (Layer layer : currentSkies) {
                layer.render(tessellator);
            }
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, rainStrength);
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
        private static final double TOD_OFFSET = -0.25;

        private static final double SKY_DISTANCE = 100.0;

        private static final int METHOD_ADD = 1;
        private static final int METHOD_SUBTRACT = 2;
        private static final int METHOD_MULTIPLY = 3;
        private static final int METHOD_DODGE = 4;
        private static final int METHOD_BURN = 5;
        private static final int METHOD_SCREEN = 6;
        private static final int METHOD_REPLACE = 7;

        private String prefix;
        private Properties properties;
        private String texture;
        private boolean rotate;
        private int blendMethod;

        private double a;
        private double b;
        private double c;

        boolean valid;

        static Layer create(String prefix) {
            InputStream is = null;
            try {
                is = lastTexturePack.getInputStream(prefix + ".properties");
                if (is != null) {
                    Properties properties = new Properties();
                    properties.load(is);
                    return new Layer(prefix, properties);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                MCPatcherUtils.close(is);
            }
            return null;
        }

        private Layer(String prefix, Properties properties) {
            this.prefix = prefix;
            this.properties = properties;
            valid = true;
            valid = (readTexture() && readRotation() & readBlendingMethod() && readFadeTimers());
        }

        private boolean readTexture() {
            texture = properties.getProperty("source", prefix + ".png");
            if (MCPatcherUtils.readImage(lastTexturePack.getInputStream(texture)) == null) {
                return addError("source texture %s not found", texture);
            }
            return true;
        }

        private boolean readRotation() {
            rotate = Boolean.parseBoolean(properties.getProperty("rotate", "true"));
            return true;
        }

        private boolean readBlendingMethod() {
            String value = properties.getProperty("blend", "add").trim().toLowerCase();
            if (value.equals("add")) {
                blendMethod = METHOD_ADD;
            } else if (value.equals("subtract")) {
                blendMethod = METHOD_SUBTRACT;
            } else if (value.equals("multiply")) {
                blendMethod = METHOD_MULTIPLY;
            } else if (value.equals("dodge")) {
                blendMethod = METHOD_DODGE;
            } else if (value.equals("burn")) {
                blendMethod = METHOD_BURN;
            } else if (value.equals("screen")) {
                blendMethod = METHOD_SCREEN;
            } else if (value.equals("replace")) {
                blendMethod = METHOD_REPLACE;
            } else {
                return addError("unknown blend method %s", value);
            }
            return true;
        }

        private boolean readFadeTimers() {
            int startFadeIn = parseTime(properties, "startFadeIn");
            int endFadeIn = parseTime(properties, "endFadeIn");
            int endFadeOut = parseTime(properties, "endFadeOut");
            if (!valid) {
                return false;
            }
            while (endFadeIn <= startFadeIn) {
                endFadeIn += SECS_PER_DAY;
            }
            while (endFadeOut <= endFadeIn) {
                endFadeOut += SECS_PER_DAY;
            }
            if (endFadeOut - startFadeIn >= SECS_PER_DAY) {
                return addError("fade times must fall within a 24 hour period");
            }
            int startFadeOut = startFadeIn + endFadeOut - endFadeIn;

            // f(x) = a cos x + b sin x + c
            // f(s0) = 0
            // f(s1) = 1
            // f(e1) = 0
            // Solve for a, b, c using Cramer's rule.
            double s0 = normalize(startFadeIn, SECS_PER_DAY, TOD_OFFSET);
            double s1 = normalize(endFadeIn, SECS_PER_DAY, TOD_OFFSET);
            double e0 = normalize(startFadeOut, SECS_PER_DAY, TOD_OFFSET);
            double e1 = normalize(endFadeOut, SECS_PER_DAY, TOD_OFFSET);
            double det = Math.cos(s0) * Math.sin(s1) + Math.cos(e1) * Math.sin(s0) + Math.cos(s1) * Math.sin(e1) -
                Math.cos(s0) * Math.sin(e1) - Math.cos(s1) * Math.sin(s0) - Math.cos(e1) * Math.sin(s1);
            if (det == 0.0) {
                return addError("determinant is 0");
            }
            a = (Math.sin(e1) - Math.sin(s0)) / det;
            b = (Math.cos(s0) - Math.cos(e1)) / det;
            c = (Math.cos(e1) * Math.sin(s0) - Math.cos(s0) * Math.sin(e1)) / det;

            MCPatcherUtils.info("%s.properties: y = %f cos x + %f sin x + %f", prefix, a, b, c);
            MCPatcherUtils.info("  at %f: %f", s0, f(s0));
            MCPatcherUtils.info("  at %f: %f", s1, f(s1));
            MCPatcherUtils.info("  at %f: %f", e0, f(e0));
            MCPatcherUtils.info("  at %f: %f", e1, f(e1));
            return true;
        }

        private boolean addError(String format, Object... params) {
            MCPatcherUtils.error(prefix + ".properties: " + format, params);
            valid = false;
            return false;
        }

        private int parseTime(Properties properties, String key) {
            String s = properties.getProperty(key, "").trim();
            if ("".equals(s)) {
                addError("missing value for %s", key);
                return -1;
            }
            String[] t = s.split(":");
            if (t.length >= 2) {
                try {
                    int hh = Integer.parseInt(t[0].trim());
                    int mm = Integer.parseInt(t[1].trim());
                    int ss;
                    if (t.length >= 3) {
                        ss = Integer.parseInt(t[2].trim());
                    } else {
                        ss = 0;
                    }
                    return (60 * 60 * hh + 60 * mm + ss) % SECS_PER_DAY;
                } catch (NumberFormatException e) {
                }
            }
            addError("invalid %s time %s", key, s);
            return -1;
        }

        private static double normalize(double time, int period, double offset) {
            return 2.0 * Math.PI * (time / period + offset);
        }

        private double f(double x) {
            return a * Math.cos(x) + b * Math.sin(x) + c;
        }

        boolean render(Tessellator tessellator) {
            double x = normalize(worldTime, TICKS_PER_DAY, 0.0);
            float brightness = (float) f(x) * rainStrength;
            if (brightness <= 0.0f) {
                return false;
            }
            if (brightness > 1.0f) {
                brightness = 1.0f;
            }

            setBlendingMethod();
            GL11.glColor4f(1.0f, 1.0f, 1.0f, brightness);

            renderEngine.bindTexture(renderEngine.getTexture(texture));

            GL11.glPushMatrix();

            if (rotate) {
                GL11.glRotatef(celestialAngle * 360.0f, 1.0f, 0.0f, 0.0f);
            }

            // north
            GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
            GL11.glRotatef(-90.0f, 0.0f, 0.0f, 1.0f);
            drawTile(tessellator, 4);

            // top
            GL11.glPushMatrix();
            GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
            drawTile(tessellator, 1);
            GL11.glPopMatrix();

            // bottom
            GL11.glPushMatrix();
            GL11.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
            drawTile(tessellator, 0);
            GL11.glPopMatrix();

            // west
            GL11.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
            drawTile(tessellator, 5);

            // south
            GL11.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
            drawTile(tessellator, 2);

            // east
            GL11.glRotatef(90.0f, 0.0f, 0.0f, 1.0f);
            drawTile(tessellator, 3);

            GL11.glPopMatrix();

            return true;
        }

        private static void drawTile(Tessellator tessellator, int tile) {
            double tileX = (tile % 3) / 3.0;
            double tileY = (tile / 3) / 2.0;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(-SKY_DISTANCE, -SKY_DISTANCE, -SKY_DISTANCE, tileX, tileY);
            tessellator.addVertexWithUV(-SKY_DISTANCE, -SKY_DISTANCE, SKY_DISTANCE, tileX, tileY + 0.5);
            tessellator.addVertexWithUV(SKY_DISTANCE, -SKY_DISTANCE, SKY_DISTANCE, tileX + 1.0 / 3.0, tileY + 0.5);
            tessellator.addVertexWithUV(SKY_DISTANCE, -SKY_DISTANCE, -SKY_DISTANCE, tileX + 1.0 / 3.0, tileY);
            tessellator.draw();
        }

        private void setBlendingMethod() {
            switch (blendMethod) {
                case METHOD_ADD:
                    GL11.glDisable(GL11.GL_ALPHA_TEST);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                    break;

                case METHOD_SUBTRACT:
                    GL11.glDisable(GL11.GL_ALPHA_TEST);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ZERO);
                    break;

                case METHOD_MULTIPLY:
                    GL11.glDisable(GL11.GL_ALPHA_TEST);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_ZERO, GL11.GL_SRC_COLOR);
                    break;

                case METHOD_DODGE:
                    GL11.glDisable(GL11.GL_ALPHA_TEST);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                    break;

                case METHOD_BURN:
                    GL11.glDisable(GL11.GL_ALPHA_TEST);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR);
                    break;

                case METHOD_SCREEN:
                    GL11.glDisable(GL11.GL_ALPHA_TEST);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR);
                    break;

                case METHOD_REPLACE:
                    GL11.glEnable(GL11.GL_ALPHA_TEST);
                    GL11.glDisable(GL11.GL_BLEND);
                    break;

                default:
                    break;
            }

            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
    }
}
