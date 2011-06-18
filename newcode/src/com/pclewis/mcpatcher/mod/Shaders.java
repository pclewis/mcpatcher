// Code written by daxnitro.  Do what you want with it but give me some credit if you use it in whole or in part.

package com.pclewis.mcpatcher.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.src.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.*;

import java.io.*;
import java.nio.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class Shaders {

    private Shaders() {
    }

    private static void initShaders() {
        System.out.println("initShaders()");

        entityAttrib = -1;
        baseProgram = setupProgram("/shaders/base.vsh", "#define _ENABLE_GL_TEXTURE_2D\n", "/shaders/base.fsh", "#define _ENABLE_GL_TEXTURE_2D\n");
        baseProgramNoT2D = setupProgram("/shaders/base.vsh", "", "/shaders/base.fsh", "");
        baseProgramBM = setupProgram("/shaders/base.vsh", "#define _ENABLE_GL_TEXTURE_2D\n#define _ENABLE_BUMP_MAPPING\n", "/shaders/base.fsh", "#define _ENABLE_GL_TEXTURE_2D\n#define _ENABLE_BUMP_MAPPING\n");

        finalProgram = setupProgram("/shaders/final.vsh", "", "/shaders/final.fsh", "");
    }

    private static void destroyShaders() {
        System.out.println("destroyShaders()");

        if (baseProgram != 0) {
            ARBShaderObjects.glDeleteObjectARB(baseProgram);
            baseProgram = 0;
        }

        if (baseProgramNoT2D != 0) {
            ARBShaderObjects.glDeleteObjectARB(baseProgramNoT2D);
            baseProgramNoT2D = 0;
        }

        if (baseProgramBM != 0) {
            ARBShaderObjects.glDeleteObjectARB(baseProgramBM);
            baseProgramBM = 0;
        }

        if (finalProgram != 0) {
            ARBShaderObjects.glDeleteObjectARB(finalProgram);
            finalProgram = 0;
        }
    }

    private static int setupProgram(String vShaderPath, String vPrefix, String fShaderPath, String fPrefix) {
        System.out.printf("setupProgram(%s, %s, %s, %s)\n", vShaderPath, vPrefix, fShaderPath, fPrefix);

        int program = ARBShaderObjects.glCreateProgramObjectARB();

        int vShader = 0;
        int fShader = 0;

        if (program != 0) {
            vShader = createVertShader(vShaderPath, vPrefix);
            fShader = createFragShader(fShaderPath, fPrefix);
        }

        if (vShader != 0 || fShader != 0) {
            if (vShader != 0) {
                ARBShaderObjects.glAttachObjectARB(program, vShader);
            }
            if (fShader != 0) {
                ARBShaderObjects.glAttachObjectARB(program, fShader);
            }
            if (entityAttrib >= 0) {
                ARBVertexShader.glBindAttribLocationARB(program, entityAttrib, "mc_Entity");
            }
            ARBShaderObjects.glLinkProgramARB(program);
            ARBShaderObjects.glValidateProgramARB(program);
            printLogInfo(program);
        } else {
            program = 0;
        }

        return program;
    }

    public static void useProgram(int program) {
        ARBShaderObjects.glUseProgramObjectARB(program);
        activeProgram = program;
        if (program != 0) {
            int sampler1U = ARBShaderObjects.glGetUniformLocationARB(program, "sampler1");
            ARBShaderObjects.glUniform1iARB(sampler1U, 1);
            int sampler2U = ARBShaderObjects.glGetUniformLocationARB(program, "sampler2");
            ARBShaderObjects.glUniform1iARB(sampler2U, 2);
            int fogMode = GL11.glGetInteger(GL11.GL_FOG_MODE);
            int fogModeU = ARBShaderObjects.glGetUniformLocationARB(program, "fogMode");
            ARBShaderObjects.glUniform1iARB(fogModeU, fogMode);
            int renderTypeU = ARBShaderObjects.glGetUniformLocationARB(program, "renderType");
            ARBShaderObjects.glUniform1iARB(renderTypeU, renderType);
            int sunPositionU = ARBShaderObjects.glGetUniformLocationARB(program, "sunPosition");
            ARBShaderObjects.glUniform3fARB(sunPositionU, sunPosition[0], sunPosition[1], sunPosition[2]);
            int moonPositionU = ARBShaderObjects.glGetUniformLocationARB(program, "moonPosition");
            ARBShaderObjects.glUniform3fARB(moonPositionU, moonPosition[0], moonPosition[1], moonPosition[2]);
            int itemId;
            ItemStack stack = mc.thePlayer.inventory.getCurrentItem();
            if (stack != null && (itemId = stack.itemID) < lightSources.length && lightSources[itemId] != null) {
                int itemIdU = ARBShaderObjects.glGetUniformLocationARB(program, "heldLight.itemId");
                ARBShaderObjects.glUniform1iARB(itemIdU, itemId);
                int magnitudeU = ARBShaderObjects.glGetUniformLocationARB(program, "heldLight.magnitude");
                ARBShaderObjects.glUniform1fARB(magnitudeU, lightSources[itemId].magnitude);
                int specularU = ARBShaderObjects.glGetUniformLocationARB(program, "heldLight.specular");
                ARBShaderObjects.glUniform4fARB(specularU, lightSources[itemId].specular[0], lightSources[itemId].specular[1], lightSources[itemId].specular[2], lightSources[itemId].specular[3]);
            } else {
                int itemIdU = ARBShaderObjects.glGetUniformLocationARB(program, "heldLight.itemId");
                ARBShaderObjects.glUniform1iARB(itemIdU, -1);
                int magnitudeU = ARBShaderObjects.glGetUniformLocationARB(program, "heldLight.magnitude");
                ARBShaderObjects.glUniform1fARB(magnitudeU, 0.0F);
            }
            int worldTimeU = ARBShaderObjects.glGetUniformLocationARB(program, "worldTime");
            ARBShaderObjects.glUniform1iARB(worldTimeU, (int) (mc.theWorld.worldInfo.getWorldTime() % 24000L));
            int aspectRatioU = ARBShaderObjects.glGetUniformLocationARB(program, "aspectRatio");
            ARBShaderObjects.glUniform1fARB(aspectRatioU, (float) renderWidth / (float) renderHeight);
            int displayWidthU = ARBShaderObjects.glGetUniformLocationARB(program, "displayWidth");
            ARBShaderObjects.glUniform1fARB(displayWidthU, (float) renderWidth);
            int displayHeightU = ARBShaderObjects.glGetUniformLocationARB(program, "displayHeight");
            ARBShaderObjects.glUniform1fARB(displayHeightU, (float) renderHeight);
            int nearU = ARBShaderObjects.glGetUniformLocationARB(program, "near");
            ARBShaderObjects.glUniform1fARB(nearU, 0.05F);
            int farU = ARBShaderObjects.glGetUniformLocationARB(program, "far");
            ARBShaderObjects.glUniform1fARB(farU, 256 >> mc.gameSettings.renderDistance);
        }
    }

    private static BufferedReader getResource(String filename) throws IOException {
        InputStream inputStream = (Shaders.class).getResourceAsStream(filename);
        if (inputStream == null) {
            File file = new File(new File(Minecraft.getAppDir("minecraft"), "shaders"), filename);
            if (file.exists()) {
                inputStream = new FileInputStream(file);
                if (inputStream == null) {
                    System.out.printf("failed to open %s\n", filename);
                    return null;
                } else {
                    System.out.printf("opened %s\n", file.getAbsolutePath());
                }
            }
        } else {
            System.out.printf("opened %s from minecraft.jar\n", filename);
        }
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    private static int createVertShader(String filename, String prefixCode) {
        int vertShader = ARBShaderObjects.glCreateShaderObjectARB(ARBVertexShader.GL_VERTEX_SHADER_ARB);
        if (vertShader == 0) {
            return 0;
        }
        String vertexCode = prefixCode;
        String line;
        try {
            BufferedReader reader = getResource(filename);
            while ((line = reader.readLine()) != null) {
                if (line.matches("#version .*")) {
                    vertexCode = line + "\n" + vertexCode;
                } else {
                    if (line.matches("attribute [_a-zA-Z0-9]+ mc_Entity.*")) {
                        entityAttrib = 10;
                    }
                    vertexCode += line + "\n";
                }
            }
        } catch (Exception e) {
            return 0;
        }
        ARBShaderObjects.glShaderSourceARB(vertShader, vertexCode);
        ARBShaderObjects.glCompileShaderARB(vertShader);
        printLogInfo(vertShader);
        return vertShader;
    }

    private static int createFragShader(String filename, String prefixCode) {
        int fragShader = ARBShaderObjects.glCreateShaderObjectARB(ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
        if (fragShader == 0) {
            return 0;
        }
        String fragCode = prefixCode;
        String line;
        try {
            BufferedReader reader = getResource(filename);
            while ((line = reader.readLine()) != null) {
                if (line.matches("#version .*")) {
                    fragCode = line + "\n" + fragCode;
                } else {
                    fragCode += line + "\n";
                }
            }
        } catch (Exception e) {
            return 0;
        }
        ARBShaderObjects.glShaderSourceARB(fragShader, fragCode);
        ARBShaderObjects.glCompileShaderARB(fragShader);
        printLogInfo(fragShader);
        return fragShader;
    }

    private static boolean printLogInfo(int obj) {
        IntBuffer iVal = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB, iVal);

        int length = iVal.get();
        if (length > 1) {
            ByteBuffer infoLog = BufferUtils.createByteBuffer(length);
            iVal.flip();
            ARBShaderObjects.glGetInfoLogARB(obj, iVal, infoLog);
            byte[] infoBytes = new byte[length];
            infoLog.get(infoBytes);
            String out = new String(infoBytes);
            System.out.println("Info log:\n" + out);
            return false;
        }
        return true;
    }

    private static int createTexture(int width, int height, boolean depth) throws java.lang.OutOfMemoryError {
        int textureId = GL11.glGenTextures();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        if (depth) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4 * 4);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, buffer);
        } else {
            ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        }

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);

        return textureId;
    }

    public static void processScene(float red, float green, float blue) {
        if (!mc.gameSettings.anaglyph && finalProgram != 0) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, baseTextureId);
            GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 0, 0, renderWidth, renderHeight, 0);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTextureId);
            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture2Id);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            useProgram(finalProgram);
            GL11.glClearColor(red, green, blue, 0.0F);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glOrtho(0, renderWidth, renderHeight, 0, -1, 1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 1);
            GL11.glVertex3f(0, 0, 0);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex3f(0, renderHeight, 0);
            GL11.glTexCoord2f(1, 0);
            GL11.glVertex3f(renderWidth, renderHeight, 0);
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex3f(renderWidth, 0, 0);
            GL11.glEnd();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            useProgram(0);
        }
    }

    public static void updateDisplay(Minecraft mc) {
        if (useFSAA && pixels != null) {
            pixels.rewind();
            GL11.glReadPixels(0, 0, renderWidth, renderHeight, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, pixels);
            try {
                Display.makeCurrent();
            } catch (LWJGLException e) {
                e.printStackTrace();
            }
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, displayTextureId);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, renderWidth, renderHeight, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, pixels);
            GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
            GL11.glOrtho(0, mc.displayWidth, mc.displayHeight, 0, -1, 1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glLoadIdentity();
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 1);
            GL11.glVertex3f(0, 0, 0);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex3f(0, mc.displayHeight, 0);
            GL11.glTexCoord2f(1, 0);
            GL11.glVertex3f(mc.displayWidth, mc.displayHeight, 0);
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex3f(mc.displayWidth, 0, 0);
            GL11.glEnd();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            Display.update();
            try {
                pbuffer.makeCurrent();
            } catch (LWJGLException e) {
                e.printStackTrace();
            }
        } else {
            Display.update();
        }
    }

    public static void setUpBuffers() {
        System.out.println("setUpBuffers()");

        if (mc != null) {
            setUpBuffers(mc);
        }
    }

    public static void setUpBuffers(Minecraft mc) {
        System.out.println("setUpBuffers(minecraft)");

        Shaders.mc = mc;
        if (!isInitialized) {
            initOptions();
            initLightSources();

            if (useMSAA) {
                // Use MSAA
                try {
                    Display.destroy();
                    Display.create(new PixelFormat().withSamples(msaaSamples));
                    useMSAA = false;
                } catch (LWJGLException e) {
                    e.printStackTrace();
                    try {
                        Display.create();
                    } catch (LWJGLException e2) {
                        e2.printStackTrace();
                    }
                }
            }

            initShaders();

            isInitialized = true;
        }

        if (useFSAA) {
            // Use FSAA
            try {
                pbuffer = new Pbuffer(mc.displayWidth * fsaaAmount, mc.displayHeight * fsaaAmount, new PixelFormat(), null, null);
                renderWidth = pbuffer.getWidth();
                renderHeight = pbuffer.getHeight();
                pbuffer.makeCurrent();
                pixels = BufferUtils.createByteBuffer(renderWidth * renderHeight * 3);
            } catch (LWJGLException e) {
                e.printStackTrace();
            }
            GL11.glDeleteTextures(displayTextureId);
            displayTextureId = createTexture(renderWidth, renderHeight, false);
        } else {
            try {
                renderWidth = mc.displayWidth;
                renderHeight = mc.displayHeight;
                Display.makeCurrent();
            } catch (LWJGLException e) {
                e.printStackTrace();
            }
        }

        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glClearDepth(1.0D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(515);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(516, 0.1F);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GL11.glDeleteTextures(baseTextureId);
        baseTextureId = createTexture(renderWidth, renderHeight, false);
        GL11.glDeleteTextures(depthTextureId);
        depthTextureId = createTexture(renderWidth, renderHeight, false);
        GL11.glDeleteTextures(depthTexture2Id);
        depthTexture2Id = createTexture(renderWidth, renderHeight, false);
    }

    public static void viewport(int x, int y, int width, int height) {
        if (useFSAA) {
            GL11.glViewport(x * fsaaAmount, y * fsaaAmount, width * fsaaAmount, height * fsaaAmount);
        } else {
            GL11.glViewport(x, y, width, height);
        }
    }

    public static void copyDepthTexture(int texture) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, 0, 0, renderWidth, renderHeight, 0);
    }

    public static void bindTexture(int activeTexture, int texture) {
        GL13.glActiveTexture(activeTexture);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    public static void setRenderType(int type) {
        renderType = type;
        if (activeProgram != 0) {
            int renderTypeU = ARBShaderObjects.glGetUniformLocationARB(activeProgram, "renderType");
            ARBShaderObjects.glUniform1iARB(renderTypeU, renderType);
        }
    }

    public static void setCelestialPosition() {
        // This is called when the current matrix is the modelview matrix based on the celestial angle.
        // The sun is at (0, 100, 0), and the moon is at (0, -100, 0).
        FloatBuffer modelView = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asFloatBuffer();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        float[] mv = new float[16];
        modelView.get(mv, 0, 16);
        float[] sunPos = multiplyMat4xVec4(mv, new float[]{0.0F, 100.0F, 0.0F, 0.0F});
        sunPosition = sunPos;
        float[] moonPos = multiplyMat4xVec4(mv, new float[]{0.0F, -100.0F, 0.0F, 0.0F});
        moonPosition = moonPos;
    }

    private static float[] multiplyMat4xVec4(float[] ta, float[] tb) {
        float[] mout = new float[4];
        mout[0] = ta[0] * tb[0] + ta[4] * tb[1] + ta[8] * tb[2] + ta[12] * tb[3];
        mout[1] = ta[1] * tb[0] + ta[5] * tb[1] + ta[9] * tb[2] + ta[13] * tb[3];
        mout[2] = ta[2] * tb[0] + ta[6] * tb[1] + ta[10] * tb[2] + ta[14] * tb[3];
        mout[3] = ta[3] * tb[0] + ta[7] * tb[1] + ta[11] * tb[2] + ta[15] * tb[3];
        return mout;
    }

    private static void initLightSources() {
        lightSources = new LightSource[Item.itemsList.length];
        for (int i = 0; i < Item.itemsList.length; ++i) {
            if (Item.itemsList[i] == null) {
                continue;
            }
            if (i < Block.blocksList.length && Block.blocksList[i] != null) {
                lightSources[i] = new LightSource(Block.lightValue[i]);
                if (i == 50 /* wood torch */) {
                    lightSources[i].setSpecular(1.0F, 0.9F, 0.5F, 1.0F);
                } else if (i == 76 /* redstone torch (active) */) {
                    lightSources[i].setSpecular(1.0F, 0.0F, 0.0F, 1.0F);
                } else if (i == 89 /* light stone */) {
                    lightSources[i].setSpecular(1.0F, 1.0F, 0.8F, 1.0F);
                }
            } else if (i == 327 /* lava bucket */) {
                lightSources[i] = new LightSource(Block.lightValue[Block.blocksList[11 /* lava still */].blockID]);
                lightSources[i].setSpecular(1.0F, 0.5F, 0.0F, 1.0F);
            } else if (i == 326 /* water bucket */) {
                lightSources[i] = new LightSource(2.0F);
                lightSources[i].setSpecular(0.0F, 0.0F, 0.3F, 1.0F);
            }
        }
    }

    private static void initOptions() {
        options.clear();

        // Anti-aliasing option
        if (Pbuffer.PBUFFER_SUPPORTED > 0 || GLContext.getCapabilities().GL_ARB_multisample) {
            options.add(new Option("SHADERS_AA_KEY", 0) {
                public String getString() {
                    switch (getValue()) {
                        case 5000:
                            return "Anti-Aliasing: 2x MSAA";
                        case 5001:
                            return "Anti-Aliasing: 4x MSAA";
                        case 5002:
                            return "Anti-Aliasing: 8x MSAA";
                        case 5003:
                            return "Anti-Aliasing: 16x MSAA";
                        case 10000:
                            return "Anti-Aliasing: 2x FSAA";
                        default:
                            return "Anti-Aliasing: None";
                    }
                }

                public void apply() {
                    useMSAA = false;
                    useFSAA = false;
                    switch (getValue()) {
                        case 5000:
                            useMSAA = true;
                            msaaSamples = 2;
                            break;
                        case 5001:
                            useMSAA = true;
                            msaaSamples = 4;
                            break;
                        case 5002:
                            useMSAA = true;
                            msaaSamples = 8;
                            break;
                        case 5003:
                            useMSAA = true;
                            msaaSamples = 16;
                            break;
                        case 10000:
                            useFSAA = true;
                            break;
                    }
                    if (Pbuffer.PBUFFER_SUPPORTED == 0) {
                        useFSAA = false;
                    }
                    if (!GLContext.getCapabilities().GL_ARB_multisample) {
                        useMSAA = false;
                    }
                    if (isInitialized) {
                        setUpBuffers();
                    }
                }

                public void buttonClick() {
                    switch (getValue()) {
                        case 0:
                            if (GLContext.getCapabilities().GL_ARB_multisample) {
                                setValue(5000);
                                break;
                            }
                        case 5000:
                            if (GLContext.getCapabilities().GL_ARB_multisample) {
                                setValue(5001);
                                break;
                            }
                        case 5001:
                            if (GLContext.getCapabilities().GL_ARB_multisample) {
                                setValue(5002);
                                break;
                            }
                        case 5002:
                            if (GLContext.getCapabilities().GL_ARB_multisample) {
                                setValue(5003);
                                break;
                            }
                        case 5003:
                            if (Pbuffer.PBUFFER_SUPPORTED > 0) {
                                setValue(10000);
                                break;
                            }
                        case 10000:
                            setValue(0);
                            break;
                        default:
                            setValue(0);
                    }
                    if (showRestartAlert && GLContext.getCapabilities().GL_ARB_multisample) {
                        showRestartAlert = false;
                    }
                }
            });
        }
        // End anti-aliasing option

        options.add(new Option("SHADERS_VSYNC_KEY", 0) {
            public String getString() {
                if (getValue() == 1) {
                    return "V-Sync: On";
                }
                return "V-Sync: Off";
            }

            public void apply() {
                Display.setVSyncEnabled(getValue() == 1);
            }

            public void buttonClick() {
                if (getValue() == 1) {
                    setValue(0);
                } else {
                    setValue(1);
                }
            }
        });

        for (int i = 0; i < options.size(); ++i) {
            options.get(i).apply();
        }
    }

    public static void addVideoSettings(List<GuiButton> controlList, int width, int height, int existingButtons) {
        for (int n = 0; n < options.size(); ++n, ++existingButtons) {
            controlList.add(new GuiSmallButton(13370200 + n, (width / 2 - 155) + (existingButtons % 2) * 160, height / 6 + 24 * (existingButtons >> 1), options.get(n).getString()));
        }
    }

    private static class LightSource {
        public LightSource(float magnitude) {
            this.magnitude = magnitude;
        }

        public void setSpecular(float r, float g, float b, float a) {
            specular[0] = r;
            specular[1] = g;
            specular[2] = b;
            specular[3] = a;
        }

        public float magnitude = 0.0F;
        public float[] specular = {1.0F, 1.0F, 1.0F, 1.0F};
    }

    public static void actionPerformed(GuiButton guibutton) {
        int vsid = guibutton.id - 13370200;
        if (vsid >= 0 && vsid < options.size()) {
            Option option = options.get(vsid);
            option.buttonClick();
            option.apply();
            guibutton.displayString = option.getString();
        }
    }

    public static void initBuffer(int size) {
        shadersBuffer = GLAllocation.createDirectByteBuffer(size * 2);
        shadersShortBuffer = shadersBuffer.asShortBuffer();
        shadersData = new short[]{-1, 0};
    }

    public static void clearBuffer() {
        shadersBuffer.clear();
    }

    public static void setEntity(int i, int j, int k) {
        shadersData[0] = (short) i;
        shadersData[1] = (short) (j + k * 16);
    }

    public static void drawGLArrays(int mode, int first, int count) {
        if (entityAttrib >= 0) {
            ARBVertexProgram.glEnableVertexAttribArrayARB(Shaders.entityAttrib);
            ARBVertexProgram.glVertexAttribPointerARB(Shaders.entityAttrib, 2, false, false, 4, (ShortBuffer) shadersShortBuffer.position(0));
        }
        GL11.glDrawArrays(mode, first, count);
        if (entityAttrib >= 0) {
            ARBVertexProgram.glDisableVertexAttribArrayARB(Shaders.entityAttrib);
        }
    }

    private static class Option {
        public Option(String key, int def) {
            this.key = key;
            this.def = def;
        }

        public int getValue() {
            Preferences prefs = Preferences.userNodeForPackage(Shaders.class);
            return prefs.getInt(key, def);
        }

        public void setValue(int value) {
            Preferences prefs = Preferences.userNodeForPackage(Shaders.class);
            prefs.putInt(key, value);
        }

        public String getString() {
            return "";
        }

        public void buttonClick() {
        }

        public void apply() {
        }

        private String key;
        private int def;
    }

    public static boolean isInitialized = false;

    public static boolean showRestartAlert = true;

    public static int activeProgram = 0;

    public static int baseProgram = 0;
    public static int baseProgramNoT2D = 0;
    public static int baseProgramBM = 0;

    public static int finalProgram = 0;

    public static int entityAttrib = -1;

    public static boolean useMSAA = false;
    public static int msaaSamples = 4;

    public static boolean useFSAA = false;
    public static int fsaaAmount = 2;

    private static Pbuffer pbuffer;
    private static ByteBuffer pixels;
    private static int renderWidth = 0;
    private static int renderHeight = 0;

    private static int renderType = 0; // RENDER_TYPE_UNKNOWN

    private static float[] sunPosition = new float[3];
    private static float[] moonPosition = new float[3];

    private static LightSource[] lightSources;

    private static ArrayList<Option> options = new ArrayList<Option>();

    public static int displayTextureId = 0;
    public static int baseTextureId = 0;
    public static int depthTextureId = 0;
    public static int depthTexture2Id = 0;

    private static ByteBuffer shadersBuffer;
    private static ShortBuffer shadersShortBuffer;
    private static short[] shadersData;

    public static Minecraft mc = null;

    public static final int RENDER_TYPE_UNKNOWN = 0;
    public static final int RENDER_TYPE_TERRAIN = 1;
}
