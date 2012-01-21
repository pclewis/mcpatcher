// Bytecode patches adapted from Hooks.java in daxnitro's original glsl mod.
// http://nitrous.daxnitro.com/repo/
// daxnitro [at] gmail.com

package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class GLSLShader extends Mod {
    private static final String DISPLAY_CLASS = "org.lwjgl.opengl.Display";
    private static final String GL11_CLASS = "org.lwjgl.opengl.GL11";

    public GLSLShader(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.GLSL_SHADERS;
        author = "daxnitro";
        description = "Adds graphical shaders to the game.  Based on daxnitro's mod.";
        version = "2.0";
        website = "http://nitrous.daxnitro.com/repo/";
        defaultEnabled = false;

        classMods.add(new MinecraftMod());
        classMods.add(new BaseMod.BlockMod());
        classMods.add(new EntityRendererMod());
        classMods.add(new EntityLivingMod());
        classMods.add(new RenderGlobalMod());
        classMods.add(new TessellatorMod());
        /*
        classMods.add(new GLViewportMod());
        classMods.add(new BaseMod.GLAllocationMod());
        classMods.add(new RenderEngineMod());
        classMods.add(new RenderLivingMod());
        classMods.add(new EntityRendererMod());
        classMods.add(new ItemMod());
        classMods.add(new BlockMod());
        classMods.add(new GameSettingsMod());
        classMods.add(new FrustrumMod());
        classMods.add(new EnumOptionsMod());
        classMods.add(new GuiButtonMod());
        classMods.add(new GuiSmallButtonMod());
        classMods.add(new GuiScreenMod());
        classMods.add(new GuiVideoSettingsMod());
        classMods.add(new RenderBlocksMod());
        classMods.add(new EntityPlayerMod());
        classMods.add(new EntityPlayerSPMod());
        classMods.add(new InventoryPlayerMod());
        classMods.add(new ItemStackMod());
        classMods.add(new WorldMod());
        classMods.add(new WorldInfoMod());
        classMods.add(new WorldRendererMod());
        */

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.SHADERS_CLASS));
    }

    private class MinecraftMod extends BaseMod.MinecraftMod {
        MinecraftMod() {
            //memberMappers.add(new FieldMapper("renderEngine", "LRenderEngine;"));
            //memberMappers.add(new FieldMapper("gameSettings", "LGameSettings;"));
            //memberMappers.add(new FieldMapper("thePlayer", "LEntityPlayerSP;"));
            memberMappers.add(new FieldMapper("entityRenderer", "LEntityRenderer;"));
            //memberMappers.add(new FieldMapper("theWorld", "LWorld;"));

            memberMappers.add(new FieldMapper(new String[]{
                "displayWidth",
                "displayHeight"
            }, "I")
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
                .accessFlag(AccessFlag.FINAL, false)
            );

            memberMappers.add(new MethodMapper("getAppDir", "(Ljava/lang/String;)Ljava/io/File;")
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "call setUpBuffers on init";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return BinaryRegex.or(
                        BinaryRegex.build(reference(methodInfo, INVOKESTATIC, new MethodRef(DISPLAY_CLASS, "create", "()V"))),
                        BinaryRegex.build(reference(methodInfo, INVOKESTATIC, new MethodRef(DISPLAY_CLASS, "create", "(Lorg/lwjgl/opengl/PixelFormat;)V")))
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "setUpBuffers", "(LMinecraft;)V"))
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "Display.update -> Shaders.updateDisplay";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(DISPLAY_CLASS, "update", "()V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "updateDisplay", "(LMinecraft;)V"))
                    );
                }
            });

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "call setUpBuffers on resize";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.getDescriptor().equals("(II)V") && (methodInfo.getAccessFlags() & AccessFlag.PRIVATE) != 0) {
                        return buildExpression(
                            RETURN
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "setUpBuffers", "(LMinecraft;)V"))
                    );
                }
            });
        }
    }

    private class EntityRendererMod extends ClassMod {
        EntityRendererMod() {
            classSignatures.add(new ConstSignature("/terrain.png"));
            classSignatures.add(new ConstSignature("/environment/snow.png"));
            classSignatures.add(new ConstSignature("ambient.weather.rain"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 2918), // GL_FOG_COLOR
                        ALOAD_0,
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        push(methodInfo, 1.0f),
                        BytecodeMatcher.anyReference(INVOKESPECIAL),
                        reference(methodInfo, INVOKESTATIC, new MethodRef("org/lwjgl/opengl/GL11", "glFog", "(ILjava/nio/FloatBuffer;)V"))
                    );
                }
            }
                .addXref(1, new FieldRef(getDeobfClass(), "fogColorRed", "F"))
                .addXref(2, new FieldRef(getDeobfClass(), "fogColorGreen", "F"))
                .addXref(3, new FieldRef(getDeobfClass(), "fogColorBlue", "F"))
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 5889), // GL_PROJECTION
                        reference(methodInfo, INVOKESTATIC, new MethodRef("org/lwjgl/opengl/GL11", "glMatrixMode", "(I)V")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef("org/lwjgl/opengl/GL11", "glLoadIdentity", "()V"))
                    );
                }
            }.setMethod(new MethodRef(getDeobfClass(), "setupCameraTransform", "(FI)V")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, "/environment/snow.png")
                    );
                }
            }.setMethod(new MethodRef(getDeobfClass(), "renderRainSnow", "(F)V")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 0.6666667f),
                        FSTORE, BinaryRegex.capture(BinaryRegex.any()),
                        push(methodInfo, 1.0f),
                        FLOAD, BinaryRegex.backReference(1),
                        push(methodInfo, 1.0f),
                        reference(methodInfo, INVOKESTATIC, new MethodRef("org/lwjgl/opengl/GL11", "glScalef", "(FFF)V"))
                    );
                }
            }.setMethod(new MethodRef(getDeobfClass(), "renderHand", "(FI)V")));

            memberMappers.add(new FieldMapper("mc", "LMinecraft;"));
            memberMappers.add(new MethodMapper("renderWorld", "(FJ)V"));
            memberMappers.add(new MethodMapper(new String[]{"disableLightmap", "enableLightmap"}, "(D)V"));
        }
    }

    private class EntityLivingMod extends ClassMod {
        EntityLivingMod() {
            classSignatures.add(new ConstSignature("/mob/char.png"));
            classSignatures.add(new ConstSignature("bubble"));
        }
    }

    private class RenderGlobalMod extends ClassMod {
        RenderGlobalMod() {
            classSignatures.add(new ConstSignature("smoke"));
            classSignatures.add(new ConstSignature("/environment/clouds.png"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BytecodeMatcher.anyFLOAD,
                        push(methodInfo, 0.2f),
                        FMUL,
                        push(methodInfo, 0.04f),
                        FADD,
                        BytecodeMatcher.anyFLOAD,
                        push(methodInfo, 0.2f),
                        FMUL,
                        push(methodInfo, 0.04f),
                        FADD,
                        BytecodeMatcher.anyFLOAD,
                        push(methodInfo, 0.6f),
                        FMUL,
                        push(methodInfo, 0.1f),
                        FADD,
                        reference(methodInfo, INVOKESTATIC, new MethodRef("org/lwjgl/opengl/GL11", "glColor3f", "(FFF)V"))
                    );
                }
            }.setMethod(new MethodRef(getDeobfClass(), "renderSky", "(F)V")));

            memberMappers.add(new MethodMapper("sortAndRender", "(LEntityLiving;ID)I"));
            memberMappers.add(new MethodMapper("renderAllRenderLists", "(ID)V"));

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "call Shaders.setCelestialPosition";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("World", "getStarBrightness", "(F)F"))
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "setCelestialPosition", "()V"))
                    );
                }
            });
        }
    }

    private class TessellatorMod extends ClassMod {
        TessellatorMod() {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, "Not tesselating!")
                    );
                }
            }.setMethod(new MethodRef(getDeobfClass(), "draw", "()I")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "clear", "()Ljava/nio/Buffer;"))
                    );
                }
            }.setMethod(new MethodRef(getDeobfClass(), "reset", "()V")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // hasNormals = true;
                        ALOAD_0,
                        ICONST_1,
                        BytecodeMatcher.captureReference(PUTFIELD),

                        //  byte0 = (byte) (int) (f * 127.0f);
                        FLOAD_1,
                        push(methodInfo, 127.0F),
                        FMUL,
                        F2I,
                        I2B,
                        BytecodeMatcher.anyISTORE,

                        // byte1 = (byte) (int) (f1 * 127.0f);
                        FLOAD_2,
                        push(methodInfo, 127.0F),
                        FMUL,
                        F2I,
                        I2B,
                        BytecodeMatcher.anyISTORE,

                        // byte2 = (byte) (int) (f2 * 127.0f);
                        FLOAD_3,
                        push(methodInfo, 127.0F),
                        FMUL,
                        F2I,
                        I2B,
                        BytecodeMatcher.anyISTORE,

                        ALOAD_0,
                        BinaryRegex.any(0, 30),
                        BytecodeMatcher.captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "setNormal", "(FFF)V"))
                .addXref(1, new FieldRef(getDeobfClass(), "hasNormals", "Z"))
                .addXref(2, new FieldRef(getDeobfClass(), "normal", "I"))
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // addedVertices++;
                        BinaryRegex.begin(),
                        ALOAD_0,
                        DUP,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ICONST_1,
                        IADD,
                        BytecodeMatcher.anyReference(PUTFIELD),

                        // if (drawMode == 7)
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        push(methodInfo, 7),
                        IF_ICMPNE, BinaryRegex.any(2)
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "addVertex", "(DDD)V"))
                .addXref(1, new FieldRef(getDeobfClass(), "addedVertices", "I"))
                .addXref(2, new FieldRef(getDeobfClass(), "drawMode", "I"))
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ICONST_0,
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java/nio/IntBuffer", "put", "([III)Ljava/nio/IntBuffer;"))
                    );
                }
            }
                .addXref(1, new FieldRef(getDeobfClass(), "rawBuffer", "[I"))
                .addXref(2, new FieldRef(getDeobfClass(), "rawBufferIndex", "I"))
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD_0,
                        BytecodeMatcher.anyReference(GETFIELD),
                        push(methodInfo, 7),
                        IF_ICMPNE, BinaryRegex.any(2),
                        BytecodeMatcher.captureReference(GETSTATIC),
                        IFEQ, BinaryRegex.any(2)
                    );
                }
            }.addXref(1, new FieldRef(getDeobfClass(), "convertQuadsToTriangles", "Z")));

            memberMappers.add(new FieldMapper("instance", "LTessellator;").accessFlag(AccessFlag.STATIC, true));

            patches.add(new MakeMemberPublicPatch(new FieldRef("Tessellator", "rawBuffer", "[I")));
            patches.add(new MakeMemberPublicPatch(new FieldRef("Tessellator", "convertQuadsToTriangles", "Z")));
            patches.add(new MakeMemberPublicPatch(new FieldRef("Tessellator", "hasNormals", "Z")));
            patches.add(new MakeMemberPublicPatch(new FieldRef("Tessellator", "rawBufferIndex", "I")));
            patches.add(new MakeMemberPublicPatch(new FieldRef("Tessellator", "addedVertices", "I")));
            patches.add(new MakeMemberPublicPatch(new FieldRef("Tessellator", "drawMode", "I")));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "initialize shadersData";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            RETURN
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Shaders.initBuffer(i);
                        ILOAD_1,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "initBuffer", "(I)V"))
                    );
                }
            });

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "clear shadersBuffer";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef("Tessellator", "byteBuffer", "Ljava/nio/ByteBuffer;")),
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java.nio.ByteBuffer", "clear", "()Ljava/nio/Buffer;"))
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "clearBuffer", "()V"))
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "fix normal calculation";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        FLOAD_1,
                        push(methodInfo, 128.0f),
                        FMUL,
                        F2I,
                        I2B,
                        ISTORE, 4
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        FLOAD_1,
                        push(methodInfo, 127.0f),
                        FMUL,
                        F2I,
                        I2B,
                        ISTORE, 4
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "glDrawArrays -> Shaders.drawGLArrays";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(GL11_CLASS, "glDrawArrays", "(III)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "drawGLArrays", "(III)V"))
                    );
                }
            });

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "call Shaders.addVertex";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ALOAD_0,
                        DUP,
                        GETFIELD, BinaryRegex.capture(BinaryRegex.any(2)),
                        ICONST_1,
                        IADD,
                        PUTFIELD, BinaryRegex.backReference(1)
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "addVertex", "(LTessellator;)V"))
                    );
                }
            });
        }
    }

    /*
    private class GLViewportMod extends ClassMod {
        GLViewportMod() {
            global = true;

            classSignatures.add(new ConstSignature(new MethodRef(GL11_CLASS, "glViewport", "(IIII)V")));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "GL11.glViewport -> Shaders.viewport";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(GL11_CLASS, "glViewport", "(IIII)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "viewport", "(IIII)V"))
                    );
                }
            });
        }
    }

    private class RenderEngineMod extends ClassMod {
        RenderEngineMod() {
            classSignatures.add(new ConstSignature(new MethodRef(GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V")));

            classSignatures.add(new FixedBytecodeSignature(
                BinaryRegex.begin(),
                ALOAD_0,
                BytecodeMatcher.captureReference(GETFIELD),
                BytecodeMatcher.captureReference(GETFIELD),
                ASTORE_2,
                ALOAD_0
            )
                .addXref(1, new FieldRef("RenderEngine", "texturePackList", "LTexturePackList;"))
                .addXref(2, new FieldRef("TexturePackList", "selectedTexturePack", "LTexturePackBase;"))
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD_0,
                        ALOAD_0,
                        ALOAD_2,
                        ALOAD_1,
                        BIPUSH, 7,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java/lang/String", "substring", "(I)Ljava/lang/String;")),
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        BytecodeMatcher.anyReference(INVOKESPECIAL),
                        BytecodeMatcher.anyReference(INVOKESPECIAL)
                    );
                }
            }.addXref(1, new MethodRef("TexturePackBase", "getInputStream", "(Ljava/lang/String;)Ljava/io/InputStream;")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.getDescriptor().equals("()V") && !methodInfo.isConstructor()) {
                        return buildExpression(
                            push(methodInfo, "%clamp%")
                        );
                    } else {
                        return null;
                    }
                }
            }.setMethodName("refreshTextures"));

            memberMappers.add(new MethodMapper("getTexture", "(Ljava/lang/String;)I"));

            patches.add(new MakeMemberPublicPatch(new FieldRef("RenderEngine", "texturePackList", "LTexturePackList;")));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "call Shaders.refreshTextures";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        RETURN
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "refreshTextures", "()V"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "refreshTextures", "()V")));
        }
    }

    private class RenderLivingMod extends ClassMod {
        RenderLivingMod() {
            classSignatures.add(new ConstSignature("deadmau5"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 514),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(GL11_CLASS, "glDepthFunc", "(I)V"))
                    );
                }
            });

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "apply baseProgramNoT2D";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 514),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(GL11_CLASS, "glDepthFunc", "(I)V"))
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.SHADERS_CLASS, "baseProgramNoT2D", "I")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "useProgram", "(I)V"))
                    );
                }
            });

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "apply baseProgram";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 515),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(GL11_CLASS, "glDepthFunc", "(I)V"))
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.SHADERS_CLASS, "baseProgram", "I")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "useProgram", "(I)V"))
                    );
                }
            });
        }
    }

    private class EntityRendererMod extends ClassMod {
        EntityRendererMod() {
            classSignatures.add(new ConstSignature("ambient.weather.rain"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java.util.Random", "nextGaussian", "()D"))
                    );
                }
            }.setMethodName("renderRainSnow"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(GL11_CLASS, "glColorMask", "(ZZZZ)V"))
                    );
                }
            }.setMethodName("renderWorld"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 2918), // GL_FOG_COLOR
                        ALOAD_0,
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        FCONST_1,
                        BytecodeMatcher.anyReference(INVOKESPECIAL),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(GL11_CLASS, "glFog", "(ILjava/nio/FloatBuffer;)V"))
                    );
                }
            }
                .addXref(1, new FieldRef("EntityRenderer", "fogColorRed", "F"))
                .addXref(2, new FieldRef("EntityRenderer", "fogColorGreen", "F"))
                .addXref(3, new FieldRef("EntityRenderer", "fogColorBlue", "F"))
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // float f10 = fogColor2 + (fogColor1 - fogColor2) * f;
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ALOAD_0,
                        BinaryRegex.backReference(1),
                        FSUB,
                        FLOAD_1,
                        FMUL,
                        FADD,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                        // fogColorRed *= f10;
                        ALOAD_0,
                        DUP
                    );
                }
            }
                .addXref(1, new FieldRef("EntityRenderer", "fogColor2", "F"))
                .addXref(2, new FieldRef("EntityRenderer", "fogColor1", "F"))
            );

            if (haveLightmaps) {
                classSignatures.add(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression(MethodInfo methodInfo) {
                        return buildExpression(
                            FCONST_1,
                            INVOKEVIRTUAL, BinaryRegex.any(2),
                            push(methodInfo, 0.95f),
                            FMUL,
                            push(methodInfo, 0.05f),
                            FADD,
                            BytecodeMatcher.anyFSTORE
                        );
                    }
                }.setMethodName("updateLightmap"));
            }

            memberMappers.add(new FieldMapper("mc", "LMinecraft;"));

            memberMappers.add(new MethodMapper(
                new String[]{
                    "setupCameraTransform",
                    "renderFirstPersonEffects"
                },
                "(FI)V"
            ));

            if (haveLightmaps) {
                memberMappers.add(new MethodMapper(
                    new String[]{
                        "disableLightmap",
                        "enableLightmap"
                    },
                    "(D)V"
                ).accessFlag(AccessFlag.PUBLIC, true));
            }

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "call Shaders.processScene";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(BinaryRegex.or(
                        buildExpression(reference(methodInfo, INVOKEVIRTUAL, new MethodRef("EntityRenderer", "renderRainSnow", "(F)V"))),
                        buildExpression(reference(methodInfo, INVOKEVIRTUAL, new MethodRef("EntityRenderer", "renderWorld", "(FJ)V")))
                    ));
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef("EntityRenderer", "fogColorRed", "F")),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef("EntityRenderer", "fogColorGreen", "F")),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef("EntityRenderer", "fogColorBlue", "F")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "processScene", "(FFF)V"))
                    );
                }
            });

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "apply baseProgram";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, NEW, new ClassRef("Frustrum")),
                        DUP,
                        reference(methodInfo, INVOKESPECIAL, new MethodRef("Frustrum", "<init>", "()V")),
                        BytecodeMatcher.anyASTORE
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.SHADERS_CLASS, "baseProgram", "I")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "useProgram", "(I)V"))
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "apply baseProgram, baseProgramBM";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return BinaryRegex.capture(BinaryRegex.build(
                        BytecodeMatcher.anyALOAD,
                        BytecodeMatcher.anyALOAD,
                        BinaryRegex.subset(new byte[]{ICONST_0, ICONST_1}, true),
                        BytecodeMatcher.anyFLOAD,
                        F2D,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("RenderGlobal", "sortAndRender", "(LEntityLiving;ID)I"))
                    ));
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Shaders.bindTerrainMaps();
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "bindTerrainMaps", "()V")),

                        // Shaders.setRenderType(1);
                        ICONST_1,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "setRenderType", "(I)V")),

                        // Shaders.useProgram(Shaders.baseProgramBM);
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.SHADERS_CLASS, "baseProgramBM", "I")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "useProgram", "(I)V")),

                        getCaptureGroup(1),

                        // Shaders.setRenderType(0);
                        ICONST_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "setRenderType", "(I)V")),

                        // Shaders.useProgram(Shaders.baseProgram);
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.SHADERS_CLASS, "baseProgram", "I")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "useProgram", "(I)V"))
                    );
                }
            });

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "copy depth texture (clouds)";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("RenderGlobal", "renderClouds", "(F)V"))
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Shaders.copyDepthTexture(Shaders.depthTextureId);
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.SHADERS_CLASS, "depthTextureId", "I")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "copyDepthTexture", "(I)V"))
                    );
                }
            });

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "copy depth texture (camera)";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKESPECIAL, new MethodRef("EntityRenderer", "renderFirstPersonEffects", "(FI)V"))
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Shaders.copyDepthTexture(Shaders.depthTexture2Id);
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.SHADERS_CLASS, "depthTexture2Id", "I")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "copyDepthTexture", "(I)V")),

                        // Shaders.useProgram(0);
                        ICONST_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "useProgram", "(I)V"))
                    );
                }
            });
        }
    }

    private class ItemMod extends ClassMod {
        ItemMod() {
            classSignatures.add(new ConstSignature("CONFLICT @ "));

            memberMappers.add(new FieldMapper("itemsList", "[LItem;") {
                @Override
                public boolean match(FieldInfo fieldInfo) {
                    if (fieldInfo.getDescriptor().matches("^\\[L[a-z]+;")) {
                        descriptor = fieldInfo.getDescriptor();
                        return super.match(fieldInfo);
                    } else {
                        return false;
                    }
                }
            });
        }
    }

    private class BlockMod extends ClassMod {
        BlockMod() {
            classSignatures.add(new ConstSignature(" is already occupied by "));

            memberMappers.add(new FieldMapper("blocksList", "[LBlock;") {
                @Override
                public boolean match(FieldInfo fieldInfo) {
                    if (fieldInfo.getDescriptor().matches("^\\[L[a-z]+;")) {
                        descriptor = fieldInfo.getDescriptor();
                        return super.match(fieldInfo);
                    } else {
                        return false;
                    }
                }
            });

            memberMappers.add(new FieldMapper("blockID", "I")
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
                .accessFlag(AccessFlag.FINAL, true)
            );

            memberMappers.add(new FieldMapper(new String[]{
                "lightOpacity",
                "lightValue"
            }, "[I")
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
                .accessFlag(AccessFlag.FINAL, true)
            );
        }
    }

    private class GameSettingsMod extends ClassMod {
        GameSettingsMod() {
            classSignatures.add(new ConstSignature("key.forward"));

            memberMappers.add(new FieldMapper(new String[]{
                "renderDistance"
            }, "I")
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
                .accessFlag(AccessFlag.FINAL, false)
            );

            memberMappers.add(new FieldMapper(new String[]{
                "invertMouse",
                "viewBobbing",
                "anaglyph"
            }, "Z")
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
                .accessFlag(AccessFlag.FINAL, false)
            );
        }
    }

    private class FrustrumMod extends ClassMod {
        FrustrumMod() {
            interfaces = new String[]{"ICamera"};

            classSignatures.add(new FixedBytecodeSignature(
                BinaryRegex.begin(),
                ALOAD_0,
                GETFIELD, BinaryRegex.any(2),

                DLOAD_1,
                ALOAD_0,
                GETFIELD, BinaryRegex.capture(BinaryRegex.any(2)),
                DSUB,

                DLOAD_3,
                ALOAD_0,
                GETFIELD, BinaryRegex.capture(BinaryRegex.any(2)),
                DSUB,

                DLOAD, 5,
                ALOAD_0,
                GETFIELD, BinaryRegex.capture(BinaryRegex.any(2)),
                DSUB,

                DLOAD, 7,
                ALOAD_0,
                GETFIELD, BinaryRegex.backReference(1),
                DSUB,

                DLOAD, 9,
                ALOAD_0,
                GETFIELD, BinaryRegex.backReference(2),
                DSUB,

                DLOAD, 11,
                ALOAD_0,
                GETFIELD, BinaryRegex.backReference(3),
                DSUB,

                INVOKEVIRTUAL, BinaryRegex.any(2),
                IRETURN
            ));
        }
    }

    private class EnumOptionsMod extends ClassMod {
        EnumOptionsMod() {
            classSignatures.add(new ConstSignature("INVERT_MOUSE"));
        }
    }

    private class GuiButtonMod extends ClassMod {
        GuiButtonMod() {
            classSignatures.add(new ConstSignature("/gui/gui.png"));
            classSignatures.add(new ConstSignature(0xffa0a0a0));

            memberMappers.add(new FieldMapper("displayString", "Ljava/lang/String;"));

            memberMappers.add(new FieldMapper(new String[]{
                "width",
                "height",
                "xPosition",
                "yPosition",
                "id"
            }, "I")
                .accessFlag(AccessFlag.STATIC, false)
            );
        }
    }

    private class GuiSmallButtonMod extends ClassMod {
        GuiSmallButtonMod() {
            classSignatures.add(new FixedBytecodeSignature(
                SIPUSH, 0, 150,
                BIPUSH, 20,
                ALOAD, 5
            ));

            classSignatures.add(new FixedBytecodeSignature(
                BinaryRegex.begin(),
                ALOAD_0,
                ILOAD_1,
                ILOAD_2,
                ILOAD_3,
                ILOAD, 4,
                ILOAD, 5,
                ALOAD, 6,
                INVOKESPECIAL, BinaryRegex.any(2),
                ALOAD_0,
                ACONST_NULL,
                PUTFIELD, BinaryRegex.any(2),
                RETURN,
                BinaryRegex.end()
            ));
        }
    }

    private class GuiScreenMod extends ClassMod {
        GuiScreenMod() {
            classSignatures.add(new ConstSignature("/gui/background.png"));
            classSignatures.add(new ConstSignature("random.click"));

            memberMappers.add(new MethodMapper("setWorldAndResolution", "(LMinecraft;II)V"));
            memberMappers.add(new FieldMapper("controlList", "Ljava/util/List;"));

            memberMappers.add(new FieldMapper(new String[]{
                "width",
                "height"
            }, "I")
                .accessFlag(AccessFlag.PUBLIC, true)
            );
        }
    }

    private class GuiVideoSettingsMod extends ClassMod {
        GuiVideoSettingsMod() {
            classSignatures.add(new ConstSignature("options.videoTitle"));
            classSignatures.add(new ConstSignature("gui.done"));

            classSignatures.add(new FixedBytecodeSignature(
                SIPUSH, 0, 200,
                IF_ICMPNE
            ).setMethodName("actionPerformed"));

            memberMappers.add(new MethodMapper("initGui", "()V"));
            memberMappers.add(new FieldMapper("options", "[LEnumOptions;"));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "add buttons to video options";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        RETURN,
                        BinaryRegex.end()
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Shaders.addVideoSettings(super.controlList, super.width, super.height, field_22108_k.length);
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef("GuiScreen", "controlList", "Ljava/util/List;")),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef("GuiScreen", "width", "I")),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef("GuiScreen", "height", "I")),
                        reference(methodInfo, GETSTATIC, new FieldRef("GuiVideoSettings", "options", "[LEnumOptions;")),
                        ARRAYLENGTH,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "addVideoSettings", "(Ljava/util/List;III)V"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "initGui", "()V")));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "handle gui button press";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        RETURN,
                        BinaryRegex.end()
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Shaders.actionPerformed(guibutton);
                        ALOAD_1,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "actionPerformed", "(LGuiButton;)V"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "actionPerformed", "(LGuiButton;)V")));
        }
    }

    private class RenderBlocksMod extends BaseMod.RenderBlocksMod {
        String[] faceMethods = new String[6];

        RenderBlocksMod() {
            setupBlockFace(0, "Bottom", 0, -1, 0);
            setupBlockFace(1, "Top", 0, 1, 0);
            setupBlockFace(2, "East", 0, 0, -1);
            setupBlockFace(3, "West", 0, 0, 1);
            setupBlockFace(4, "North", -1, 0, 0);
            setupBlockFace(5, "South", 1, 0, 0);

            memberMappers.add(new MethodMapper(faceMethods, "(LBlock;DDDI)V"));
            memberMappers.add(new MethodMapper("renderBlockByRenderType", "(LBlock;III)Z"));
        }

        private void setupBlockFace(int index, final String direction, final int x, final int y, final int z) {
            String methodName = "render" + direction + "Face";
            faceMethods[index] = methodName;
            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "set normal when rendering block " + direction.toLowerCase() + " face";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BinaryRegex.begin(),
                        reference(methodInfo, GETSTATIC, new FieldRef("Tessellator", "instance", "LTessellator;")),
                        ASTORE, BinaryRegex.capture(BinaryRegex.any())
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD, getCaptureGroup(1),
                        push(methodInfo, x),
                        push(methodInfo, y),
                        push(methodInfo, z),
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("Tessellator", "setNormal", "(FFF)V"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), methodName, "(LBlock;DDDI)V")));
        }
    }

    private class EntityPlayerMod extends ClassMod {
        EntityPlayerMod() {
            classSignatures.add(new ConstSignature("humanoid"));
            classSignatures.add(new ConstSignature("/mob/char.png"));

            memberMappers.add(new FieldMapper("inventory", "LInventoryPlayer;"));
        }
    }

    private class EntityPlayerSPMod extends ClassMod {
        EntityPlayerSPMod() {
            parentClass = "EntityPlayer";

            classSignatures.add(new ConstSignature("http://s3.amazonaws.com/MinecraftSkins/"));
            classSignatures.add(new ConstSignature("portal.trigger"));
        }
    }

    private class InventoryPlayerMod extends ClassMod {
        InventoryPlayerMod() {
            classSignatures.add(new ConstSignature("Inventory"));
            classSignatures.add(new ConstSignature("Slot"));

            memberMappers.add(new MethodMapper("getCurrentItem", "()LItemStack;"));
        }
    }

    private class ItemStackMod extends ClassMod {
        ItemStackMod() {
            classSignatures.add(new ConstSignature("id"));
            classSignatures.add(new ConstSignature("Count"));
            classSignatures.add(new ConstSignature("Damage"));

            memberMappers.add(new FieldMapper(new String[]{
                "stackSize",
                "animationsToGo",
                "itemID"
            }, "I")
                .accessFlag(AccessFlag.PUBLIC, true)
            );
        }
    }

    private class WorldMod extends BaseMod.WorldMod {
        WorldMod() {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        FCONST_1,
                        BytecodeMatcher.anyFLOAD,
                        push(methodInfo, (float) Math.PI),
                        FMUL,
                        FCONST_2,
                        FMUL,
                        INVOKESTATIC, BinaryRegex.any(2),
                        FCONST_2,
                        FMUL,
                        push(methodInfo, 0.75F),
                        FADD,
                        FSUB
                    );
                }
            }.setMethodName("getStarBrightness"));

            classSignatures.add(new FixedBytecodeSignature(
                BinaryRegex.begin(),
                ALOAD_0,
                ILOAD_1,
                ILOAD_2,
                ILOAD_3,
                ICONST_1,
                INVOKEVIRTUAL, BinaryRegex.any(2),
                IRETURN,
                BinaryRegex.end()
            ).setMethodName("getBlockLightValue"));

            memberMappers.add(new FieldMapper("worldInfo", "LWorldInfo;"));

            patches.add(new MakeMemberPublicPatch(new FieldRef("World", "worldInfo", "LWorldInfo;")));
        }
    }

    private class WorldInfoMod extends ClassMod {
        WorldInfoMod() {
            classSignatures.add(new ConstSignature("RandomSeed"));
            classSignatures.add(new ConstSignature("SpawnX"));

            memberMappers.add(new MethodMapper(new String[]{
                "getRandomSeed",
                "getWorldTime"
            }, "()J"));
        }
    }

    private class WorldRendererMod extends ClassMod {
        WorldRendererMod() {
            classSignatures.add(new ConstSignature(new MethodRef(GL11_CLASS, "glNewList", "(II)V")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 1.000001F)
                    );
                }
            }.setMethodName("updateRenderer"));

            memberMappers.add(new FieldMapper("worldObj", "LWorld;"));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "call Shaders.setEntity";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ILOAD, 13,
                        ALOAD, 10,
                        BinaryRegex.capture(BinaryRegex.build(
                            ALOAD, 19
                        )),
                        BinaryRegex.capture(BinaryRegex.build(
                            ILOAD, 17,
                            ILOAD, 15,
                            ILOAD, 16
                        )),
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("RenderBlocks", "renderBlockByRenderType", "(LBlock;III)Z")),
                        IOR,
                        ISTORE, 13
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Shaders.setEntity(block1.blockID, worldObj.getBlockLightValue(i4, l3, k3), Block.lightValue[block1.blockID]);
                        getCaptureGroup(1), // block1
                        reference(methodInfo, GETFIELD, new FieldRef("Block", "blockID", "I")),

                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef("WorldRenderer", "worldObj", "LWorld;")),
                        getCaptureGroup(2), // i4, l3, k3
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("World", "getBlockLightValue", "(III)I")),

                        reference(methodInfo, GETSTATIC, new FieldRef("Block", "lightValue", "[I")),
                        getCaptureGroup(1), // block1
                        reference(methodInfo, GETFIELD, new FieldRef("Block", "blockID", "I")),
                        IALOAD,

                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "setEntity", "(III)V"))
                    );
                }
            });

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "clear Shaders.setEntity";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        RETURN,
                        BinaryRegex.end()
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Shaders.setEntity(-1, 0, 0);
                        ICONST_M1,
                        ICONST_0,
                        ICONST_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "setEntity", "(III)V"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "updateRenderer", "()V")));
        }
    }
    */
}
