package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.glu.GLU;

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

    private static int shaderProgram;
    private static int fragShader;
    private static int texture1Location;
    private static int texture2Location;
    private static int blendLocation;

    private static final int SKY_TEXTURE_UNIT = 2;
    private static final int STAR_TEXTURE_UNIT = SKY_TEXTURE_UNIT + 1;
    private static final String SHADER_SOURCE =
        "/* Simple shader to blend two textures */\n" +
            "uniform sampler2D texture1;\n" +
            "uniform sampler2D texture2;\n" +
            "uniform float blending;\n" +
            "void main() {\n" +
            "    vec4 color1 = texture2D(texture1, gl_TexCoord[0].st);\n" +
            "    vec4 color2 = texture2D(texture2, gl_TexCoord[0].st);\n" +
            "    gl_FragColor = (1.0 - blending) * color1 + blending * color2;\n" +
            "}\n";

    static {
        /*
        shaderProgram = GL20.glCreateProgram();
        fragShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragShader, SHADER_SOURCE);
        GL20.glCompileShader(fragShader);
        GL20.glAttachShader(shaderProgram, fragShader);
        GL20.glLinkProgram(shaderProgram);

        texture1Location = GL20.glGetUniformLocation(shaderProgram, "texture1");
        texture2Location = GL20.glGetUniformLocation(shaderProgram, "texture2");
        blendLocation = GL20.glGetUniformLocation(shaderProgram, "blending");

        MCPatcherUtils.info(
            "shaderProgram = %d, fragShader = %d, blendLocation = %d, texture1Location = %d, texture2Location = %d",
            shaderProgram, fragShader, blendLocation, texture1Location, texture2Location
        );
        */
    }

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
                h = hasTexture(getSkyTexture()) & hasTexture(getStarTexture());
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
        return active;
    }

    public static boolean renderStars() {
        if (active) {
            Tessellator tessellator = Tessellator.instance;

            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            if (shaderProgram > 0) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + SKY_TEXTURE_UNIT);
                renderEngine.bindTexture(renderEngine.getTexture(getSkyTexture()));
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + STAR_TEXTURE_UNIT);
                renderEngine.bindTexture(renderEngine.getTexture(getStarTexture()));

                GL20.glUseProgram(shaderProgram);

                GL20.glUniform1i(texture1Location, SKY_TEXTURE_UNIT);
                GL20.glUniform1i(texture2Location, STAR_TEXTURE_UNIT);
                GL20.glUniform1f(blendLocation, celestialAngle);
            } else {
                renderEngine.bindTexture(renderEngine.getTexture(getStarTexture()));
            }

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

            if (shaderProgram > 0) {
                GL20.glUseProgram(0);
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
            }

            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        }
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

    private static boolean hasTexture(String s) {
        if (MCPatcherUtils.readImage(lastTexturePack.getInputStream(s)) == null) {
            MCPatcherUtils.info("texture %s NOT FOUND", s);
            return false;
        } else {
            MCPatcherUtils.info("texture %s FOUND", s);
            return true;
        }
    }

    private static String getSkyTexture() {
        return "/terrain/world" + worldType + "/sky.png";
    }

    private static String getStarTexture() {
        return "/terrain/world" + worldType + "/stars1.png";
    }

    private static void checkGLError() {
        int error = GL11.glGetError();
        if (error != 0) {
            throw new RuntimeException("GL error: " + GLU.gluErrorString(error));
        }
    }
}
