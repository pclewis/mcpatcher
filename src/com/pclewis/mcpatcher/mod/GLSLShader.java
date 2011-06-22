// Bytecode patches adapted from Hooks.java in daxnitro's original glsl mod.
// http://nitrous.daxnitro.com/repo/
// daxnitro [at] gmail.com

package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class GLSLShader extends Mod {
    private static final String class_Shaders = "com.pclewis.mcpatcher.mod.Shaders";
    private static final String class_Display = "org.lwjgl.opengl.Display";
    private static final String class_GL11 = "org.lwjgl.opengl.GL11";

    public GLSLShader() {
        name = MCPatcherUtils.GLSL_SHADERS;
        description = "Adds graphical shaders to the game.  Based on daxnitro's mod.";
        version = "1.0";
        website = "http://nitrous.daxnitro.com/repo/";
        defaultEnabled = false;

        classMods.add(new MinecraftMod());
        classMods.add(new GLViewportMod());
        classMods.add(new HDTexture.GLAllocationMod());
        classMods.add(new RenderEngineMod());
        classMods.add(new RenderGlobalMod());
        classMods.add(new RenderLivingMod());
        classMods.add(new EntityRendererMod());
        classMods.add(new EntityLivingMod());
        classMods.add(new ItemMod());
        classMods.add(new BlockMod());
        classMods.add(new GameSettingsMod());
        classMods.add(new FrustrumMod());
        classMods.add(new EnumOptionsMod());
        classMods.add(new GuiButtonMod());
        classMods.add(new GuiSmallButtonMod());
        classMods.add(new GuiScreenMod());
        classMods.add(new GuiVideoSettingsMod());
        classMods.add(new TessellatorMod());
        classMods.add(new RenderBlocksMod());
        classMods.add(new EntityPlayerMod());
        classMods.add(new EntityPlayerSPMod());
        classMods.add(new InventoryPlayerMod());
        classMods.add(new ItemStackMod());
        classMods.add(new WorldMod());
        classMods.add(new WorldInfoMod());
        classMods.add(new WorldRendererMod());

        filesToAdd.add("com/pclewis/mcpatcher/mod/Shaders.class");
        filesToAdd.add("com/pclewis/mcpatcher/mod/Shaders$1.class");
        filesToAdd.add("com/pclewis/mcpatcher/mod/Shaders$2.class");
        filesToAdd.add("com/pclewis/mcpatcher/mod/Shaders$LightSource.class");
        filesToAdd.add("com/pclewis/mcpatcher/mod/Shaders$Option.class");
    }

    private class MinecraftMod extends ClassMod {
        public MinecraftMod() {
            classSignatures.add(new FilenameSignature("net/minecraft/client/Minecraft.class"));

            memberMappers.add(new FieldMapper("renderEngine", "LRenderEngine;"));
            memberMappers.add(new FieldMapper("gameSettings", "LGameSettings;"));
            memberMappers.add(new FieldMapper("thePlayer", "LEntityPlayerSP;"));
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

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "call setUpBuffers on init";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Display, "create", "()V"))
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "setUpBuffers", "(LMinecraft;)V"))
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Display, "update", "()V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "updateDisplay", "(LMinecraft;)V"))
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "setUpBuffers", "(LMinecraft;)V"))
                    );
                }
            });
        }
    }

    private class GLViewportMod extends ClassMod {
        public GLViewportMod() {
            global = true;

            classSignatures.add(new ConstSignature(new MethodRef(class_GL11, "glViewport", "(IIII)V")));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "GL11.glViewport -> Shaders.viewport";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_GL11, "glViewport", "(IIII)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "viewport", "(IIII)V"))
                    );
                }
            });
        }
    }

    private class RenderEngineMod extends ClassMod {
        public RenderEngineMod() {
            classSignatures.add(new ConstSignature(new MethodRef(class_GL11, "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V")));

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

            memberMappers.add(new MethodMapper("refreshTextures", "()V") {
                @Override
                public boolean match(MethodInfo methodInfo) {
                    if (!super.match(methodInfo) || methodInfo.getName().startsWith("<")) {
                        return false;
                    }
                    BytecodeMatcher bm = new BytecodeMatcher(
                        push(methodInfo, "%clamp%")
                    );
                    return bm.match(methodInfo);
                }
            });

            memberMappers.add(new MethodMapper("getTexture", "(Ljava/lang/String;)I"));

            patches.add(new MakeMemberPublicPatch(new FieldRef("RenderEngine", "texturePackList", "LTexturePackList;")));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "call Shaders.refreshTextures";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    JavaRef refreshTextures = map(new MethodRef("RenderEngine", "refreshTextures", "()V"));
                    if (methodInfo.getDescriptor().equals(refreshTextures.getType()) &&
                        methodInfo.getName().equals(refreshTextures.getName())) {
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "refreshTextures", "()V"))
                    );
                }
            });
        }
    }

    private class RenderGlobalMod extends ClassMod {
        public RenderGlobalMod() {
            classSignatures.add(new ConstSignature("smoke"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, "/environment/clouds.png")
                    );
                }
            }.setMethodName("renderClouds"));

            memberMappers.add(new MethodMapper("clipRenderersByFrustrum", "(LICamera;F)V"));
            memberMappers.add(new MethodMapper("sortAndRender", "(LEntityLiving;ID)I"));

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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "setCelestialPosition", "()V"))
                    );
                }
            });
        }
    }

    private class RenderLivingMod extends ClassMod {
        public RenderLivingMod() {
            classSignatures.add(new ConstSignature("deadmau5"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 514),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_GL11, "glDepthFunc", "(I)V"))
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_GL11, "glDepthFunc", "(I)V"))
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, GETSTATIC, new FieldRef(class_Shaders, "baseProgramNoT2D", "I")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "useProgram", "(I)V"))
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_GL11, "glDepthFunc", "(I)V"))
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, GETSTATIC, new FieldRef(class_Shaders, "baseProgram", "I")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "useProgram", "(I)V"))
                    );
                }
            });
        }
    }

    private class EntityRendererMod extends ClassMod {
        public EntityRendererMod() {
            classSignatures.add(new ConstSignature("ambient.weather.rain"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java.util.Random", "nextGaussian", "()D"))
                    );
                }
            }.setMethodName("renderWorld1"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_GL11, "glColorMask", "(ZZZZ)V"))
                    );
                }
            }.setMethodName("renderWorld2"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 2915 /*GL_FOG_START*/),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        push(methodInfo, 0.25f),
                        FMUL,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_GL11, "glFogf", "(IF)V"))
                    );
                }
            }.addXref(1, new FieldRef("EntityRenderer", "farPlaneDistance", "F")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 2918 /* GL_FOG_COLOR */),
                        ALOAD_0,
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        FCONST_1,
                        BytecodeMatcher.anyReference(INVOKESPECIAL),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_GL11, "glFog", "(ILjava/nio/FloatBuffer;)V"))
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

            memberMappers.add(new FieldMapper("mc", "LMinecraft;"));

            memberMappers.add(new MethodMapper(
                new String[]{
                    "setupCameraTransform",
                    "renderFirstPersonEffects" /* func_4135_b */
                },
                "(FI)V"
            ));

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "call Shaders.processScene";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(BinaryRegex.or(
                        buildExpression(reference(methodInfo, INVOKEVIRTUAL, new MethodRef("EntityRenderer", "renderWorld1", "(F)V"))),
                        buildExpression(reference(methodInfo, INVOKEVIRTUAL, new MethodRef("EntityRenderer", "renderWorld2", "(FJ)V")))
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "processScene", "(FFF)V"))
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
                        reference(methodInfo, GETSTATIC, new FieldRef(class_Shaders, "baseProgram", "I")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "useProgram", "(I)V"))
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "bindTerrainMaps", "()V")),

                        // Shaders.setRenderType(1);
                        ICONST_1,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "setRenderType", "(I)V")),

                        // Shaders.useProgram(Shaders.baseProgramBM);
                        reference(methodInfo, GETSTATIC, new FieldRef(class_Shaders, "baseProgramBM", "I")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "useProgram", "(I)V")),

                        getCaptureGroup(1),

                        // Shaders.setRenderType(0);
                        ICONST_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "setRenderType", "(I)V")),

                        // Shaders.useProgram(Shaders.baseProgram);
                        reference(methodInfo, GETSTATIC, new FieldRef(class_Shaders, "baseProgram", "I")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "useProgram", "(I)V"))
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
                        reference(methodInfo, GETSTATIC, new FieldRef(class_Shaders, "depthTextureId", "I")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "copyDepthTexture", "(I)V"))
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
                        reference(methodInfo, GETSTATIC, new FieldRef(class_Shaders, "depthTexture2Id", "I")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "copyDepthTexture", "(I)V")),

                        // Shaders.useProgram(0);
                        ICONST_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "useProgram", "(I)V"))
                    );
                }
            });
        }
    }

    private class EntityLivingMod extends ClassMod {
        public EntityLivingMod() {
            classSignatures.add(new ConstSignature("/mob/char.png"));
            classSignatures.add(new ConstSignature("bubble"));
        }
    }

    private class ItemMod extends ClassMod {
        public ItemMod() {
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
        public BlockMod() {
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
        public GameSettingsMod() {
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
        public FrustrumMod() {
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
            ) {
                @Override
                public void afterMatch(ClassFile classFile, MethodInfo methodInfo) {
                    classMap.addClassMap("ICamera", classFile.getInterfaces()[0]);
                }
            });
        }
    }

    private class EnumOptionsMod extends ClassMod {
        public EnumOptionsMod() {
            classSignatures.add(new ConstSignature("INVERT_MOUSE"));
        }
    }

    private class GuiButtonMod extends ClassMod {
        public GuiButtonMod() {
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
        public GuiSmallButtonMod() {
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
        public GuiScreenMod() {
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
        public GuiVideoSettingsMod() {
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
                    JavaRef initGui = classMap.map(new MethodRef("GuiVideoSettings", "initGui", "()V"));
                    if (methodInfo.getName().equals(initGui.getName()) &&
                        methodInfo.getDescriptor().equals(initGui.getType())) {
                        return buildExpression(
                            RETURN,
                            BinaryRegex.end()
                        );
                    } else {
                        return null;
                    }
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "addVideoSettings", "(Ljava/util/List;III)V"))
                    );
                }
            });

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "handle gui button press";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    JavaRef actionPerformed = classMap.map(new MethodRef("GuiVideoSettings", "actionPerformed", "(LGuiButton;)V"));
                    if (methodInfo.getName().equals(actionPerformed.getName()) &&
                        methodInfo.getDescriptor().equals(actionPerformed.getType())) {
                        return buildExpression(
                            RETURN,
                            BinaryRegex.end()
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Shaders.actionPerformed(guibutton);
                        ALOAD_1,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "actionPerformed", "(LGuiButton;)V"))
                    );
                }
            });
        }
    }

    private class TessellatorMod extends ClassMod {
        public TessellatorMod() {
            classSignatures.add(new ConstSignature("Not tesselating!"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        FLOAD_1,
                        push(methodInfo, 128.0F),
                        FMUL,
                        F2I,
                        I2B,
                        BytecodeMatcher.anyISTORE,

                        FLOAD_2,
                        push(methodInfo, 127.0F),
                        FMUL,
                        F2I,
                        I2B,
                        BytecodeMatcher.anyISTORE,

                        FLOAD_3,
                        push(methodInfo, 127.0F),
                        FMUL,
                        F2I,
                        I2B,
                        BytecodeMatcher.anyISTORE
                    );
                }

                @Override
                public void afterMatch(ClassFile classFile, MethodInfo methodInfo) {
                    String deobfClass = getDeobfClass();
                    classMap.addClassMap(deobfClass, classFile.getName());
                    classMap.addMethodMap(deobfClass, "setNormal", methodInfo.getName());
                    for (Object o : classFile.getFields()) {
                        FieldInfo fieldInfo = (FieldInfo) o;
                        if ((fieldInfo.getAccessFlags() & AccessFlag.STATIC) != 0 &&
                            fieldInfo.getDescriptor().equals("L" + classFile.getName() + ";")) {
                            classMap.addFieldMap(deobfClass, "instance", fieldInfo.getName());
                            break;
                        }
                    }
                }
            });

            memberMappers.add(new FieldMapper("byteBuffer", "Ljava/nio/ByteBuffer;"));

            memberMappers.add(new FieldMapper("rawBuffer", "[I")
                .accessFlag(AccessFlag.PRIVATE, true)
                .accessFlag(AccessFlag.STATIC, false)
            );

            memberMappers.add(new FieldMapper("convertQuadsToTriangles", "Z")
                .accessFlag(AccessFlag.PRIVATE, true)
                .accessFlag(AccessFlag.STATIC, true)
            );

            memberMappers.add(new FieldMapper(new String[]{
                null,
                null,
                "hasNormals"
            }, "Z")
                .accessFlag(AccessFlag.PRIVATE, true)
                .accessFlag(AccessFlag.STATIC, false)
            );

            memberMappers.add(new FieldMapper(new String[]{
                null,
                null,
                "rawBufferIndex",
                "addedVertices",
                "drawMode"
            }, "I")
                .accessFlag(AccessFlag.PRIVATE, true)
                .accessFlag(AccessFlag.STATIC, false)
            );

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
                    if (methodInfo.getName().equals("<init>")) {
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "initBuffer", "(I)V"))
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "clearBuffer", "()V"))
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_GL11, "glDrawArrays", "(III)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "drawGLArrays", "(III)V"))
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "addVertex", "(LTessellator;)V"))
                    );
                }
            });
        }
    }

    private class RenderBlocksMod extends ClassMod {
        public RenderBlocksMod() {
            classSignatures.add(new ConstSignature(0.1875D));
            classSignatures.add(new ConstSignature(0.01D));

            final String[] faceMethods = {
                "renderBottomFace",
                "renderTopFace",
                "renderEastFace",
                "renderWestFace",
                "renderNorthFace",
                "renderSouthFace"
            };

            memberMappers.add(new MethodMapper(faceMethods, "(LBlock;DDDI)V"));
            memberMappers.add(new MethodMapper("renderBlockByRenderType", "(LBlock;III)Z"));

            patches.add(new BytecodePatch.InsertAfter() {
                private final float[] x = new float[]{0, 0, 0, 0, -1, 1};
                private final float[] y = new float[]{-1, 1, 0, 0, 0, 0};
                private final float[] z = new float[]{0, 0, -1, 1, 0, 0};

                @Override
                public String getDescription() {
                    return "set normal when rendering block face";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.getDescriptor().equals(classMap.mapTypeString("(LBlock;DDDI)V"))) {
                        return buildExpression(
                            BinaryRegex.begin(),
                            reference(methodInfo, GETSTATIC, new FieldRef("Tessellator", "instance", "LTessellator;")),
                            ASTORE, BinaryRegex.capture(BinaryRegex.any())
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    for (int match = 0; match < faceMethods.length; match++) {
                        if (methodInfo.getName().equals(classMap.map(new MethodRef("RenderBlocks", faceMethods[match], "(LBlock;DDDI)V")).getName())) {
                            return buildCode(
                                ALOAD, getCaptureGroup(1),
                                push(methodInfo, x[match]),
                                push(methodInfo, y[match]),
                                push(methodInfo, z[match]),
                                reference(methodInfo, INVOKEVIRTUAL, new MethodRef("Tessellator", "setNormal", "(FFF)V"))
                            );
                        }
                    }
                    return null;
                }
            });
        }
    }

    private class EntityPlayerMod extends ClassMod {
        public EntityPlayerMod() {
            classSignatures.add(new ConstSignature("humanoid"));
            classSignatures.add(new ConstSignature("/mob/char.png"));

            memberMappers.add(new FieldMapper("inventory", "LInventoryPlayer;"));
        }

        @Override
        public boolean mapClassMembers(String filename, ClassFile classFile) throws Exception {
            if (super.mapClassMembers(filename, classFile)) {
                mod.getClassMap().addInheritance("EntityPlayer", "EntityPlayerSP");
                return true;
            } else {
                return false;
            }
        }
    }

    private class EntityPlayerSPMod extends ClassMod {
        public EntityPlayerSPMod() {
            classSignatures.add(new ConstSignature("http://s3.amazonaws.com/MinecraftSkins/"));
            classSignatures.add(new ConstSignature("portal.trigger"));
        }
    }

    private class InventoryPlayerMod extends ClassMod {
        public InventoryPlayerMod() {
            classSignatures.add(new ConstSignature("Inventory"));
            classSignatures.add(new ConstSignature("Slot"));

            memberMappers.add(new MethodMapper("getCurrentItem", "()LItemStack;"));
        }
    }

    private class ItemStackMod extends ClassMod {
        public ItemStackMod() {
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

    private class WorldMod extends ClassMod {
        public WorldMod() {
            classSignatures.add(new ConstSignature("More than "));
            classSignatures.add(new ConstSignature(" updates, aborting lighting updates"));

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
        public WorldInfoMod() {
            classSignatures.add(new ConstSignature("RandomSeed"));
            classSignatures.add(new ConstSignature("SpawnX"));

            memberMappers.add(new MethodMapper(new String[]{
                "getRandomSeed",
                "getWorldTime"
            }, "()J"));
        }
    }

    private class WorldRendererMod extends ClassMod {
        public WorldRendererMod() {
            classSignatures.add(new ConstSignature(new MethodRef(class_GL11, "glNewList", "(II)V")));

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

                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "setEntity", "(III)V"))
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
                    JavaRef updateRenderer = classMap.map(new MethodRef("WorldRenderer", "updateRenderer", "()V"));
                    if (methodInfo.getDescriptor().equals(updateRenderer.getType()) &&
                        methodInfo.getName().equals(updateRenderer.getName())) {
                        return buildExpression(
                            RETURN,
                            BinaryRegex.end()
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Shaders.setEntity(-1, 0, 0);
                        ICONST_M1,
                        ICONST_0,
                        ICONST_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "setEntity", "(III)V"))
                    );
                }
            });
        }
    }
}
