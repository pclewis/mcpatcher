// Bytecode patches adapted from Hooks.java in daxnitro's original glsl mod.
// http://nitrous.daxnitro.com/repo/
// daxnitro [at] gmail.com

package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class GLSLShader extends Mod {
    private static final String GL11_CLASS = "org.lwjgl.opengl.GL11";

    public GLSLShader(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.GLSL_SHADERS;
        author = "daxnitro";
        description = "Adds graphical shaders to the game.  Based on daxnitro's mod.";
        version = "2.0";
        website = "http://daxnitro.wikia.com/wiki/Shaders_2.0";
        defaultEnabled = false;

        classMods.add(new MinecraftMod());
        classMods.add(new BaseMod.GLAllocationMod());
        classMods.add(new RenderEngineMod());
        classMods.add(new RenderGlobalMod());
        classMods.add(new RenderLivingMod());
        classMods.add(new EntityRendererMod());
        classMods.add(new EntityLivingMod());
        classMods.add(new BlockMod());
        classMods.add(new GameSettingsMod());
        classMods.add(new TessellatorMod());
        classMods.add(new RenderBlocksMod());
        classMods.add(new EntityPlayerMod());
        classMods.add(new EntityPlayerSPMod());
        classMods.add(new InventoryPlayerMod());
        classMods.add(new ItemStackMod());
        classMods.add(new WorldMod());
        classMods.add(new WorldRendererMod());

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.SHADERS_CLASS));
    }

    private class MinecraftMod extends BaseMod.MinecraftMod {
        MinecraftMod() {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // if (Keyboard.getEventKey() == 63) {
                        reference(methodInfo, INVOKESTATIC, new MethodRef("org/lwjgl/input/Keyboard", "getEventKey", "()I")),
                        push(methodInfo, 63),
                        IF_ICMPNE, BinaryRegex.any(2),

                        // gameSettings.thirdPersonView++;
                        ALOAD_0,
                        BytecodeMatcher.anyReference(GETFIELD),
                        DUP,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ICONST_1,
                        IADD,
                        BytecodeMatcher.anyReference(PUTFIELD)
                    );
                }
            }.addXref(1, new FieldRef("GameSettings", "thirdPersonView", "I")));

            memberMappers.add(new FieldMapper("renderEngine", "LRenderEngine;"));
            memberMappers.add(new FieldMapper("gameSettings", "LGameSettings;"));
            memberMappers.add(new FieldMapper("thePlayer", "LEntityPlayerSP;"));
            memberMappers.add(new FieldMapper("entityRenderer", "LEntityRenderer;"));
            memberMappers.add(new FieldMapper("renderViewEntity", "LEntityLiving;"));
            memberMappers.add(new FieldMapper("theWorld", "LWorld;"));

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
        }
    }

    private class RenderGlobalMod extends ClassMod {
        RenderGlobalMod() {
            final MethodRef renderSky = new MethodRef(getDeobfClass(), "renderSky", "(F)V");

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
            }.setMethod(renderSky));

            memberMappers.add(new MethodMapper("sortAndRender", "(LEntityLiving;ID)I"));
            memberMappers.add(new MethodMapper("renderAllRenderLists", "(ID)V"));
            memberMappers.add(new FieldMapper("worldObj", "LWorld;"));

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "call setCelestialPosition";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef("RenderGlobal", "worldObj", "LWorld;")),
                        FLOAD_1,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("World", "getStarBrightness", "(F)F")),
                        BytecodeMatcher.anyFLOAD,
                        FMUL,
                        BytecodeMatcher.anyFSTORE
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "setCelestialPosition", "()V"))
                    );
                }
            }.targetMethod(renderSky));

            addGLWrapper("glEnable");
            addGLWrapper("glDisable");
        }

        private void addGLWrapper(final String name) {
            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "wrap " + name;
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(GL11_CLASS, name, "(I)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, name + "Wrapper", "(I)V"))
                    );
                }
            });
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

            addGLWrapper("glEnable");
            addGLWrapper("glDisable");
        }

        private void addGLWrapper(final String name) {
            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "wrap " + name;
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(GL11_CLASS, name, "(I)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, name + "Wrapper", "(I)V"))
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

            final MethodRef setupCameraTransform = new MethodRef(getDeobfClass(), "setupCameraTransform", "(FI)V");
            final MethodRef renderWorld = new MethodRef(getDeobfClass(), "renderWorld", "(FJ)V");
            final MethodRef renderRainSnow = new MethodRef(getDeobfClass(), "renderRainSnow", "(F)V");
            final MethodRef renderHand = new MethodRef(getDeobfClass(), "renderHand", "(FI)V");
            final FieldRef fogColorRed = new FieldRef(getDeobfClass(), "fogColorRed", "F");
            final FieldRef fogColorGreen = new FieldRef(getDeobfClass(), "fogColorGreen", "F");
            final FieldRef fogColorBlue = new FieldRef(getDeobfClass(), "fogColorBlue", "F");

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
                .addXref(1, fogColorRed)
                .addXref(2, fogColorGreen)
                .addXref(3, fogColorBlue)
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
            }.setMethod(setupCameraTransform));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, "/environment/snow.png")
                    );
                }
            }.setMethod(renderRainSnow));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // GL11.glClear(256);
                        push(methodInfo, 256),
                        reference(methodInfo, INVOKESTATIC, new MethodRef("org/lwjgl/opengl/GL11", "glClear", "(I)V")),

                        // renderHand(f, i);
                        ALOAD_0,
                        FLOAD_1,
                        BytecodeMatcher.anyILOAD,
                        BytecodeMatcher.captureReference(INVOKESPECIAL)
                    );
                }
            }
                .setMethod(renderWorld)
                .addXref(1, renderHand)
            );

            memberMappers.add(new FieldMapper("mc", "LMinecraft;"));
            memberMappers.add(new MethodMapper("renderWorld", "(FJ)V"));
            memberMappers.add(new MethodMapper(new String[]{"disableLightmap", "enableLightmap"}, "(D)V"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "call beginRender";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BinaryRegex.begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef(getDeobfClass(), "mc", "LMinecraft;")),
                        FLOAD_1,
                        LLOAD_2,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "beginRender", "(LMinecraft;FJ)V"))
                    );
                }
            }.targetMethod(renderWorld));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "call endRender";
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "endRender", "()V"))
                    );
                }
            }.targetMethod(renderWorld));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "call setClearColor / setCamera";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD_0,
                        FLOAD_1,
                        BytecodeMatcher.anyILOAD,
                        reference(methodInfo, INVOKESPECIAL, setupCameraTransform)
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Shaders.setClearColor(fogColorRed, fogColorGreen, fogColorBlue);
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, fogColorRed),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, fogColorGreen),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, fogColorBlue),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "setClearColor", "(FFF)V")),

                        // ... original code ...
                        getMatch(),

                        // Shaders.setCamera(f);
                        FLOAD_1,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "setCamera", "(F)V"))
                    );
                }
            }.targetMethod(renderWorld));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "wrap terrain and water rendering";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BytecodeMatcher.anyALOAD,
                        BytecodeMatcher.anyALOAD,
                        BinaryRegex.capture(BinaryRegex.any()),
                        FLOAD_1,
                        F2D,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("RenderGlobal", "sortAndRender", "(LEntityLiving;ID)I")),
                        BinaryRegex.capture(BinaryRegex.or(BinaryRegex.build(POP), BytecodeMatcher.anyISTORE))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // if ($1 == 0) {
                        getCaptureGroup(1),
                        ICONST_0,
                        IF_ICMPNE, branch("A"),

                        // Shaders.beginTerrain(); ...; Shaders.endTerrain();
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "beginTerrain", "()V")),
                        getMatch(),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "endTerrain", "()V")),
                        GOTO, branch("C"),

                        label("A"),
                        // } else if ($1 == 1) {
                        getCaptureGroup(1),
                        ICONST_1,
                        IF_ICMPNE, branch("B"),

                        // Shaders.beginWater(); ...; Shaders.endWater();
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "beginWater", "()V")),
                        getMatch(),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "endWater", "()V")),
                        GOTO, branch("C"),

                        // } else {
                        label("B"),

                        // ...
                        getMatch(),

                        // }
                        label("C")
                    );
                }
            }.targetMethod(renderWorld));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "wrap water rendering";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BytecodeMatcher.anyALOAD,
                        ICONST_1,
                        FLOAD_1,
                        F2D,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("RenderGlobal", "renderAllRenderLists", "(ID)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Shaders.beginWater(); ...; Shaders.endWater();
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "beginWater", "()V")),
                        getMatch(),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "endWater", "()V"))
                    );
                }
            }.targetMethod(renderWorld));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "wrap weather rendering";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD_0,
                        FLOAD_1,
                        reference(methodInfo, INVOKEVIRTUAL, renderRainSnow)
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Shaders.beginWeather(); ...; Shaders.endWeather();
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "beginWeather", "()V")),
                        getMatch(),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "endWeather", "()V"))
                    );
                }
            }.targetMethod(renderWorld));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "wrap hand rendering";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD_0,
                        FLOAD_1,
                        BytecodeMatcher.anyILOAD,
                        reference(methodInfo, INVOKESPECIAL, renderHand)
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Shaders.beginHand(); ...; Shaders.endHand();
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "beginHand", "()V")),
                        getMatch(),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "endHand", "()V"))
                    );
                }
            }.targetMethod(renderWorld));

            addLightmapPatch("enableLightmap");
            addLightmapPatch("disableLightmap");
        }

        private void addLightmapPatch(final String name) {
            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "wrap " + name;
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, name, "()V"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), name, "(D)V")));
        }
    }

    private class EntityLivingMod extends ClassMod {
        EntityLivingMod() {
            parentClass = "Entity";

            classSignatures.add(new ConstSignature("/mob/char.png"));
            classSignatures.add(new ConstSignature("bubble"));
        }
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
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

    private class GameSettingsMod extends BaseMod.GameSettingsMod {
        GameSettingsMod() {
            mapOption("viewDistance", "renderDistance", "I");
        }
    }

    private class TessellatorMod extends ClassMod {
        TessellatorMod() {
            final MethodRef reset = new MethodRef(getDeobfClass(), "reset", "()V");
            final MethodRef draw = new MethodRef(getDeobfClass(), "draw", "()I");
            final MethodRef addVertex = new MethodRef(getDeobfClass(), "addVertex", "(DDD)V");
            final FieldRef drawMode = new FieldRef(getDeobfClass(), "drawMode", "I");
            final FieldRef addedVertices = new FieldRef(getDeobfClass(), "addedVertices", "I");
            final FieldRef convertQuadsToTriangles = new FieldRef(getDeobfClass(), "convertQuadsToTriangles", "Z");
            final FieldRef hasNormals = new FieldRef(getDeobfClass(), "hasNormals", "Z");
            final FieldRef rawBuffer = new FieldRef(getDeobfClass(), "rawBuffer", "[I");
            final FieldRef rawBufferIndex = new FieldRef(getDeobfClass(), "rawBufferIndex", "I");
            final FieldRef shadersBuffer = new FieldRef(getDeobfClass(), "shadersBuffer", "Ljava/nio/ByteBuffer;");
            final FieldRef shadersShortBuffer = new FieldRef(getDeobfClass(), "shadersShortBuffer", "Ljava/nio/ShortBuffer;");
            final FieldRef shadersData = new FieldRef(getDeobfClass(), "shadersData", "[S");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, "Not tesselating!")
                    );
                }
            }.setMethod(draw));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "clear", "()Ljava/nio/Buffer;"))
                    );
                }
            }.setMethod(reset));

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
                .addXref(1, hasNormals)
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
                .setMethod(addVertex)
                .addXref(1, addedVertices)
                .addXref(2, drawMode)
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
                .addXref(1, rawBuffer)
                .addXref(2, rawBufferIndex)
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
            }.addXref(1, convertQuadsToTriangles));

            memberMappers.add(new FieldMapper("instance", "LTessellator;").accessFlag(AccessFlag.STATIC, true));

            patches.add(new AddFieldPatch("shadersBuffer", "Ljava.nio.ByteBuffer;"));
            patches.add(new AddFieldPatch("shadersShortBuffer", "Ljava.nio.ShortBuffer;"));
            patches.add(new AddFieldPatch("shadersData", "[S"));

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
                        // shadersData = new short[]{-1, 0};
                        ALOAD_0,
                        ICONST_2,
                        NEWARRAY, T_SHORT,
                        DUP,
                        ICONST_0,
                        ICONST_M1,
                        SASTORE,
                        reference(methodInfo, PUTFIELD, shadersData),

                        // shadersBuffer = GLAllocation.createDirectByteBuffer(i / 8 * 4);
                        ALOAD_0,
                        ILOAD_1,
                        push(methodInfo, 8),
                        IDIV,
                        ICONST_4,
                        IMUL,
                        reference(methodInfo, INVOKESTATIC, new MethodRef("GLAllocation", "createDirectByteBuffer", "(I)Ljava/nio/ByteBuffer;")),
                        reference(methodInfo, PUTFIELD, shadersBuffer),

                        // shadersShortBuffer = shadersBuffer.asShortBuffer();
                        ALOAD_0,
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, shadersBuffer),
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "asShortBuffer", "()Ljava/nio/ShortBuffer;")),
                        reference(methodInfo, PUTFIELD, shadersShortBuffer)
                    );
                }
            });

            patches.add(new AddMethodPatch("setEntity", "(I)V") {
                @Override
                public byte[] generateMethod(ClassFile classFile, MethodInfo methodInfo) throws BadBytecode, IOException {
                    return buildCode(
                        // if (Shaders.entityAttrib >= 0) {
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.SHADERS_CLASS, "entityAttrib", "I")),
                        IFLT, branch("A"),

                        // shadersData[0] = i;
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, shadersData),
                        ICONST_0,
                        ILOAD_1,
                        I2S,
                        SASTORE,

                        // }
                        label("A"),
                        RETURN
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "clear shadersBuffer";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BinaryRegex.begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, shadersBuffer),
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "clear", "()Ljava/nio/Buffer;")),
                        POP
                    );
                }
            }.targetMethod(reset));

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
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, shadersShortBuffer),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "drawGLArrays", "(IIILjava/nio/ShortBuffer;)V"))
                    );
                }
            }.targetMethod(draw));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "call Shaders.addVertex";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BinaryRegex.begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // if (drawMode == 7
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, drawMode),
                        push(methodInfo, 7),
                        IF_ICMPNE, branch("A"),

                        // && convertQuadsToTriangles
                        reference(methodInfo, GETSTATIC, convertQuadsToTriangles),
                        IFEQ, branch("A"),

                        // && (addedVertices + 1) % 4 == 0
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, addedVertices),
                        ICONST_1,
                        IADD,
                        ICONST_4,
                        IREM,
                        IFNE, branch("A"),

                        // && hasNormals) {
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, hasNormals),
                        IFEQ, branch("A"),

                        getRawBufferCode(methodInfo, 6, -18),
                        getShaderBufferCode(methodInfo),

                        getRawBufferCode(methodInfo, 14, -2),
                        getShaderBufferCode(methodInfo),

                        // }
                        label("A"),
                        getShaderBufferCode(methodInfo)
                    );
                }

                private byte[] getRawBufferCode(MethodInfo methodInfo, int offset1, int offset2) throws IOException {
                    return buildCode(
                        // rawBuffer[rawBufferIndex + offset1] = rawBuffer[rawBufferIndex + offset2];
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, rawBuffer),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, rawBufferIndex),
                        push(methodInfo, offset1),
                        IADD,
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, rawBuffer),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, rawBufferIndex),
                        push(methodInfo, offset2),
                        IADD,
                        IALOAD,
                        IASTORE
                    );
                }

                private byte[] getShaderBufferCode(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // shadersBuffer.putShort(shadersData[0]).putShort(shadersData[1]);
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, shadersBuffer),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, shadersData),
                        ICONST_0,
                        SALOAD,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "putShort", "(S)Ljava/nio/ByteBuffer;")),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, shadersData),
                        ICONST_1,
                        SALOAD,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "putShort", "(S)Ljava/nio/ByteBuffer;")),
                        POP
                    );
                }
            }.targetMethod(addVertex));
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
                        push(methodInfo, (float) x),
                        push(methodInfo, (float) y),
                        push(methodInfo, (float) z),
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
                        BytecodeMatcher.anyReference(INVOKESTATIC),
                        FCONST_2,
                        FMUL,
                        push(methodInfo, 0.75f),
                        FADD,
                        FSUB
                    );
                }
            }.setMethod(new MethodRef(getDeobfClass(), "getStarBrightness", "(F)F")));

            classSignatures.add(new FixedBytecodeSignature(
                BinaryRegex.begin(),
                ALOAD_0,
                ILOAD_1,
                ILOAD_2,
                ILOAD_3,
                ICONST_1,
                BytecodeMatcher.anyReference(INVOKEVIRTUAL),
                IRETURN,
                BinaryRegex.end()
            ).setMethod(new MethodRef(getDeobfClass(), "getBlockLightValue", "(III)I")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // entity.lastTickPosX = entity.posX;
                        ALOAD_1,
                        ALOAD_1,
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.captureReference(PUTFIELD),

                        // entity.lastTickPosY = entity.posY;
                        ALOAD_1,
                        ALOAD_1,
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.captureReference(PUTFIELD),

                        // entity.lastTickPosZ = entity.posZ;
                        ALOAD_1,
                        ALOAD_1,
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.captureReference(PUTFIELD)
                    );
                }
            }
                .addXref(1, new FieldRef("Entity", "posX", "D"))
                .addXref(2, new FieldRef("Entity", "lastTickPosX", "D"))
                .addXref(3, new FieldRef("Entity", "posY", "D"))
                .addXref(4, new FieldRef("Entity", "lastTickPosY", "D"))
                .addXref(5, new FieldRef("Entity", "posZ", "D"))
                .addXref(6, new FieldRef("Entity", "lastTickPosZ", "D"))
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // f1 = getCelestialAngle(f);
                        BinaryRegex.begin(),
                        ALOAD_0,
                        FLOAD_1,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        FSTORE_2,

                        // return f1 * 3.141593f * 2.0f;
                        FLOAD_2,
                        push(methodInfo, (float) Math.PI),
                        FMUL,
                        push(methodInfo, 2.0f),
                        FMUL,
                        FRETURN,
                        BinaryRegex.end()
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "getCelestialAngleRadians", "(F)F"))
                .addXref(1, new MethodRef(getDeobfClass(), "getCelestialAngle", "(F)F"))
            );

            memberMappers.add(new MethodMapper(new String[]{"getSeed", "getWorldTime"}, "()J"));
        }
    }

    private class WorldRendererMod extends ClassMod {
        WorldRendererMod() {
            final MethodRef updateRenderer = new MethodRef(getDeobfClass(), "updateRenderer", "()V");

            classSignatures.add(new ConstSignature(new MethodRef(GL11_CLASS, "glNewList", "(II)V")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 1.000001F)
                    );
                }
            }.setMethod(updateRenderer));

            memberMappers.add(new FieldMapper("worldObj", "LWorld;"));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "call Tessellator.setEntity";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BytecodeMatcher.anyILOAD,
                        BytecodeMatcher.anyALOAD,
                        BinaryRegex.capture(BytecodeMatcher.anyALOAD),
                        BytecodeMatcher.anyILOAD,
                        BytecodeMatcher.anyILOAD,
                        BytecodeMatcher.anyILOAD,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("RenderBlocks", "renderBlockByRenderType", "(LBlock;III)Z")),
                        IOR,
                        BytecodeMatcher.anyISTORE
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Tessellator.instance.setEntity(block1.blockID);
                        reference(methodInfo, GETSTATIC, new FieldRef("Tessellator", "instance", "LTessellator;")),
                        getCaptureGroup(1), // block1
                        reference(methodInfo, GETFIELD, new FieldRef("Block", "blockID", "I")),
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("Tessellator", "setEntity", "(I)V"))
                    );
                }
            }.targetMethod(updateRenderer));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "clear Tessellator.setEntity";
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
                        // Tessellator.instance.setEntity(-1);
                        reference(methodInfo, GETSTATIC, new FieldRef("Tessellator", "instance", "LTessellator;")),
                        ICONST_M1,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("Tessellator", "setEntity", "(I)V"))
                    );
                }
            }.targetMethod(updateRenderer));
        }
    }
}
