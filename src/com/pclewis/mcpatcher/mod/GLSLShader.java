// Bytecode patches adapted from Hooks.java in daxnitro's original glsl mod.
// http://nitrous.daxnitro.com/repo/
// daxnitro [at] gmail.com

package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.BadBytecode;

import java.io.IOException;

import static com.pclewis.mcpatcher.BinaryRegex.*;
import static com.pclewis.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class GLSLShader extends Mod {
    private final boolean haveNewWorld;

    public GLSLShader(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.GLSL_SHADERS;
        author = "daxnitro";
        description = "Adds graphical shaders to the game.  Based on daxnitro's mod.";
        version = "2.0";
        website = "http://daxnitro.wikia.com/wiki/Shaders_2.0";
        defaultEnabled = false;

        haveNewWorld = minecraftVersion.compareTo("12w18a") >= 0;

        classMods.add(new MinecraftMod(minecraftVersion));
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
        if (haveNewWorld) {
            classMods.add(new EntityPlayerSPSubMod());
        }
        classMods.add(new InventoryPlayerMod(minecraftVersion));
        classMods.add(new ItemStackMod());
        classMods.add(new WorldMod());
        if (haveNewWorld) {
            classMods.add(new BaseMod.WorldServerMod(minecraftVersion));
            classMods.add(new BaseMod.WorldServerMPMod(minecraftVersion));
        }
        classMods.add(new WorldRendererMod());

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.SHADERS_CLASS));
    }

    private class MinecraftMod extends BaseMod.MinecraftMod {
        MinecraftMod(MinecraftVersion minecraftVersion) {
            final FieldRef thePlayer = new FieldRef(getDeobfClass(), "thePlayer", "LEntityPlayerSP;");
            final FieldRef playerSub = new FieldRef(getDeobfClass(), "playerSub", "LEntityPlayerSPSub;");
            final MethodRef getPlayer = new MethodRef(getDeobfClass(), "getPlayer", "()LEntityPlayerSP;");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (Keyboard.getEventKey() == 63) {
                        reference(INVOKESTATIC, new MethodRef("org/lwjgl/input/Keyboard", "getEventKey", "()I")),
                        push(63),
                        IF_ICMPNE, any(2),

                        // gameSettings.thirdPersonView++;
                        ALOAD_0,
                        anyReference(GETFIELD),
                        DUP,
                        captureReference(GETFIELD),
                        ICONST_1,
                        IADD,
                        anyReference(PUTFIELD)
                    );
                }
            }.addXref(1, new FieldRef("GameSettings", "thirdPersonView", "I")));

            addWorldGetter(minecraftVersion);

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;")));
            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "gameSettings", "LGameSettings;")));
            if (haveNewWorld) {
                memberMappers.add(new FieldMapper(playerSub));
            } else {
                memberMappers.add(new FieldMapper(thePlayer));
            }
            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "entityRenderer", "LEntityRenderer;")));
            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "renderViewEntity", "LEntityLiving;")));

            memberMappers.add(new FieldMapper(
                new FieldRef(getDeobfClass(), "displayWidth", "I"),
                new FieldRef(getDeobfClass(), "displayHeight", "I")
            )
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
                .accessFlag(AccessFlag.FINAL, false)
            );

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getAppDir", "(Ljava/lang/String;)Ljava/io/File;"))
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );

            patches.add(new AddMethodPatch(getPlayer) {
                @Override
                public byte[] generateMethod() throws BadBytecode, IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, haveNewWorld ? playerSub : thePlayer),
                        ARETURN
                    );
                }
            });
        }
    }

    private class RenderEngineMod extends ClassMod {
        RenderEngineMod() {
            classSignatures.add(new ConstSignature(new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().equals("()V") && !getMethodInfo().isConstructor()) {
                        return buildExpression(
                            push("%clamp%")
                        );
                    } else {
                        return null;
                    }
                }
            }.setMethodName("refreshTextures"));

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getTexture", "(Ljava/lang/String;)I")));
        }
    }

    private class RenderGlobalMod extends ClassMod {
        RenderGlobalMod() {
            final MethodRef renderSky = new MethodRef(getDeobfClass(), "renderSky", "(F)V");
            final String worldClass = haveNewWorld ? "WorldServerMP" : "World";
            final FieldRef worldObj = new FieldRef(getDeobfClass(), "worldObj", "L" + worldClass + ";");

            classSignatures.add(new ConstSignature("smoke"));
            classSignatures.add(new ConstSignature("/environment/clouds.png"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        anyFLOAD,
                        push(0.2f),
                        FMUL,
                        push(0.04f),
                        FADD,
                        anyFLOAD,
                        push(0.2f),
                        FMUL,
                        push(0.04f),
                        FADD,
                        anyFLOAD,
                        push(0.6f),
                        FMUL,
                        push(0.1f),
                        FADD,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glColor3f", "(FFF)V"))
                    );
                }
            }.setMethod(renderSky));

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "sortAndRender", "(LEntityLiving;ID)I")));
            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "renderAllRenderLists", "(ID)V")));
            memberMappers.add(new FieldMapper(worldObj));

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "call setCelestialPosition";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // f18 = (float)(worldObj.getStarBrightness(par1) * d);
                        ALOAD_0,
                        reference(GETFIELD, worldObj),
                        FLOAD_1,
                        reference(INVOKEVIRTUAL, new MethodRef(worldClass, "getStarBrightness", "(F)F")),
                        anyFLOAD,
                        FMUL,
                        anyFSTORE
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "setCelestialPosition", "()V"))
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
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, name, "(I)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, name + "Wrapper", "(I)V"))
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
                public String getMatchExpression() {
                    return buildExpression(
                        push(514),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glDepthFunc", "(I)V"))
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
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, name, "(I)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, name + "Wrapper", "(I)V"))
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
                public String getMatchExpression() {
                    return buildExpression(
                        push(2918), // GL_FOG_COLOR
                        ALOAD_0,
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        push(1.0f),
                        anyReference(INVOKESPECIAL),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glFog", "(ILjava/nio/FloatBuffer;)V"))
                    );
                }
            }
                .addXref(1, fogColorRed)
                .addXref(2, fogColorGreen)
                .addXref(3, fogColorBlue)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(5889), // GL_PROJECTION
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glMatrixMode", "(I)V")),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glLoadIdentity", "()V"))
                    );
                }
            }.setMethod(setupCameraTransform));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/environment/snow.png")
                    );
                }
            }.setMethod(renderRainSnow));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // GL11.glClear(256);
                        push(256),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glClear", "(I)V")),

                        // renderHand(f, i);
                        ALOAD_0,
                        FLOAD_1,
                        anyILOAD,
                        captureReference(INVOKESPECIAL)
                    );
                }
            }
                .setMethod(renderWorld)
                .addXref(1, renderHand)
            );

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "mc", "LMinecraft;")));
            memberMappers.add(new MethodMapper(renderWorld));
            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "disableLightmap", "(D)V"), new MethodRef(getDeobfClass(), "enableLightmap", "(D)V")));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "call beginRender";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, new FieldRef(getDeobfClass(), "mc", "LMinecraft;")),
                        FLOAD_1,
                        LLOAD_2,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "beginRender", "(LMinecraft;FJ)V"))
                    );
                }
            }.targetMethod(renderWorld));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "call endRender";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        RETURN
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "endRender", "()V"))
                    );
                }
            }.targetMethod(renderWorld));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "call setClearColor / setCamera";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        FLOAD_1,
                        anyILOAD,
                        reference(INVOKESPECIAL, setupCameraTransform)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // Shaders.setClearColor(fogColorRed, fogColorGreen, fogColorBlue);
                        ALOAD_0,
                        reference(GETFIELD, fogColorRed),
                        ALOAD_0,
                        reference(GETFIELD, fogColorGreen),
                        ALOAD_0,
                        reference(GETFIELD, fogColorBlue),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "setClearColor", "(FFF)V")),

                        // ... original code ...
                        getMatch(),

                        // Shaders.setCamera(f);
                        FLOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "setCamera", "(F)V"))
                    );
                }
            }.targetMethod(renderWorld));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "wrap terrain and water rendering";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEVIRTUAL, new MethodRef("RenderGlobal", "sortAndRender", "(LEntityLiving;ID)I"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "sortAndRenderWrapper", "(LRenderGlobal;LEntityLiving;ID)I"))
                    );
                }
            }.targetMethod(renderWorld));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "wrap water rendering";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // (vanilla) renderglobal.renderAllRenderLists(1, par1);
                        // (w/ Better Glass) renderglobal.renderAllRenderLists(this.loop, par1);
                        anyALOAD,
                        or(
                            build(ICONST_1),
                            build(
                                ALOAD_0,
                                anyReference(GETFIELD)
                            )
                        ),
                        FLOAD_1,
                        F2D,
                        reference(INVOKEVIRTUAL, new MethodRef("RenderGlobal", "renderAllRenderLists", "(ID)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // Shaders.beginWater(); ...; Shaders.endWater();
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "beginWater", "()V")),
                        getMatch(),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "endWater", "()V"))
                    );
                }
            }.targetMethod(renderWorld));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "wrap weather rendering";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        FLOAD_1,
                        reference(INVOKEVIRTUAL, renderRainSnow)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // Shaders.beginWeather(); ...; Shaders.endWeather();
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "beginWeather", "()V")),
                        getMatch(),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "endWeather", "()V"))
                    );
                }
            }.targetMethod(renderWorld));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "wrap hand rendering";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        FLOAD_1,
                        anyILOAD,
                        reference(INVOKESPECIAL, renderHand)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // Shaders.beginHand(); ...; Shaders.endHand();
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "beginHand", "()V")),
                        getMatch(),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "endHand", "()V"))
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
                public String getMatchExpression() {
                    return buildExpression(
                        RETURN
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, name, "()V"))
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
            memberMappers.add(new FieldMapper(
                new FieldRef(getDeobfClass(), "lightOpacity", "[I"),
                new FieldRef(getDeobfClass(), "lightValue", "[I")
            )
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
                public String getMatchExpression() {
                    return buildExpression(
                        push("Not tesselating!")
                    );
                }
            }.setMethod(draw));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "clear", "()Ljava/nio/Buffer;"))
                    );
                }
            }.setMethod(reset));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // hasNormals = true;
                        ALOAD_0,
                        ICONST_1,
                        captureReference(PUTFIELD),

                        //  byte0 = (byte) (int) (f * 127.0f);
                        FLOAD_1,
                        push(127.0F),
                        FMUL,
                        F2I,
                        I2B,
                        anyISTORE,

                        // byte1 = (byte) (int) (f1 * 127.0f);
                        FLOAD_2,
                        push(127.0F),
                        FMUL,
                        F2I,
                        I2B,
                        anyISTORE,

                        // byte2 = (byte) (int) (f2 * 127.0f);
                        FLOAD_3,
                        push(127.0F),
                        FMUL,
                        F2I,
                        I2B,
                        anyISTORE,

                        ALOAD_0,
                        any(0, 30),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "setNormal", "(FFF)V"))
                .addXref(1, hasNormals)
                .addXref(2, new FieldRef(getDeobfClass(), "normal", "I"))
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // addedVertices++;
                        begin(),
                        ALOAD_0,
                        DUP,
                        captureReference(GETFIELD),
                        ICONST_1,
                        IADD,
                        anyReference(PUTFIELD),

                        // if (drawMode == 7)
                        ALOAD_0,
                        captureReference(GETFIELD),
                        push(7),
                        IF_ICMPNE, any(2)
                    );
                }
            }
                .setMethod(addVertex)
                .addXref(1, addedVertices)
                .addXref(2, drawMode)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ICONST_0,
                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/IntBuffer", "put", "([III)Ljava/nio/IntBuffer;"))
                    );
                }
            }
                .addXref(1, rawBuffer)
                .addXref(2, rawBufferIndex)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        anyReference(GETFIELD),
                        push(7),
                        IF_ICMPNE, any(2),
                        captureReference(GETSTATIC),
                        IFEQ, any(2)
                    );
                }
            }.addXref(1, convertQuadsToTriangles));

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "instance", "LTessellator;")).accessFlag(AccessFlag.STATIC, true));

            patches.add(new AddFieldPatch(new FieldRef(getDeobfClass(), "shadersBuffer", "Ljava.nio.ByteBuffer;")));
            patches.add(new AddFieldPatch(new FieldRef(getDeobfClass(), "shadersShortBuffer", "Ljava.nio.ShortBuffer;")));
            patches.add(new AddFieldPatch(shadersData));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "initialize shadersData";
                }

                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            RETURN
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // shadersData = new short[]{-1, 0};
                        ALOAD_0,
                        ICONST_2,
                        NEWARRAY, T_SHORT,
                        DUP,
                        ICONST_0,
                        ICONST_M1,
                        SASTORE,
                        reference(PUTFIELD, shadersData),

                        // shadersBuffer = GLAllocation.createDirectByteBuffer(i / 8 * 4);
                        ALOAD_0,
                        ILOAD_1,
                        push(8),
                        IDIV,
                        ICONST_4,
                        IMUL,
                        reference(INVOKESTATIC, new MethodRef("GLAllocation", "createDirectByteBuffer", "(I)Ljava/nio/ByteBuffer;")),
                        reference(PUTFIELD, shadersBuffer),

                        // shadersShortBuffer = shadersBuffer.asShortBuffer();
                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, shadersBuffer),
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "asShortBuffer", "()Ljava/nio/ShortBuffer;")),
                        reference(PUTFIELD, shadersShortBuffer)
                    );
                }
            });

            patches.add(new AddMethodPatch(new MethodRef(getDeobfClass(), "setEntity", "(I)V")) {
                @Override
                public byte[] generateMethod() throws BadBytecode, IOException {
                    return buildCode(
                        // if (Shaders.entityAttrib >= 0) {
                        reference(GETSTATIC, new FieldRef(MCPatcherUtils.SHADERS_CLASS, "entityAttrib", "I")),
                        IFLT, branch("A"),

                        // shadersData[0] = i;
                        ALOAD_0,
                        reference(GETFIELD, shadersData),
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
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, shadersBuffer),
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "clear", "()Ljava/nio/Buffer;")),
                        POP
                    );
                }
            }.targetMethod(reset));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "wrap glDrawArrays";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glDrawArrays", "(III)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, shadersShortBuffer),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SHADERS_CLASS, "glDrawArraysWrapper", "(IIILjava/nio/ShortBuffer;)V"))
                    );
                }
            }.targetMethod(draw));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "call Shaders.addVertex";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // if (drawMode == 7
                        ALOAD_0,
                        reference(GETFIELD, drawMode),
                        push(7),
                        IF_ICMPNE, branch("A"),

                        // && convertQuadsToTriangles
                        reference(GETSTATIC, convertQuadsToTriangles),
                        IFEQ, branch("A"),

                        // && (addedVertices + 1) % 4 == 0
                        ALOAD_0,
                        reference(GETFIELD, addedVertices),
                        ICONST_1,
                        IADD,
                        ICONST_4,
                        IREM,
                        IFNE, branch("A"),

                        // && hasNormals) {
                        ALOAD_0,
                        reference(GETFIELD, hasNormals),
                        IFEQ, branch("A"),

                        getRawBufferCode(6, -18),
                        getShaderBufferCode(),

                        getRawBufferCode(14, -2),
                        getShaderBufferCode(),

                        // }
                        label("A"),
                        getShaderBufferCode()
                    );
                }

                private byte[] getRawBufferCode(int offset1, int offset2) throws IOException {
                    return buildCode(
                        // rawBuffer[rawBufferIndex + offset1] = rawBuffer[rawBufferIndex + offset2];
                        ALOAD_0,
                        reference(GETFIELD, rawBuffer),
                        ALOAD_0,
                        reference(GETFIELD, rawBufferIndex),
                        push(offset1),
                        IADD,
                        ALOAD_0,
                        reference(GETFIELD, rawBuffer),
                        ALOAD_0,
                        reference(GETFIELD, rawBufferIndex),
                        push(offset2),
                        IADD,
                        IALOAD,
                        IASTORE
                    );
                }

                private byte[] getShaderBufferCode() throws IOException {
                    return buildCode(
                        // shadersBuffer.putShort(shadersData[0]).putShort(shadersData[1]);
                        ALOAD_0,
                        reference(GETFIELD, shadersBuffer),
                        ALOAD_0,
                        reference(GETFIELD, shadersData),
                        ICONST_0,
                        SALOAD,
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "putShort", "(S)Ljava/nio/ByteBuffer;")),
                        ALOAD_0,
                        reference(GETFIELD, shadersData),
                        ICONST_1,
                        SALOAD,
                        reference(INVOKEVIRTUAL, new MethodRef("java/nio/ByteBuffer", "putShort", "(S)Ljava/nio/ByteBuffer;")),
                        POP
                    );
                }
            }.targetMethod(addVertex));
        }
    }

    private class RenderBlocksMod extends BaseMod.RenderBlocksMod {
        private final MethodRef[] faceMethods = new MethodRef[6];

        RenderBlocksMod() {
            setupBlockFace(0, "Bottom", 0, -1, 0);
            setupBlockFace(1, "Top", 0, 1, 0);
            setupBlockFace(2, "North", 0, 0, -1);
            setupBlockFace(3, "South", 0, 0, 1);
            setupBlockFace(4, "West", -1, 0, 0);
            setupBlockFace(5, "East", 1, 0, 0);

            memberMappers.add(new MethodMapper(faceMethods));
            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "renderBlockByRenderType", "(LBlock;III)Z")));
        }

        private void setupBlockFace(int face, final String direction, final int x, final int y, final int z) {
            faceMethods[face] = new MethodRef(getDeobfClass(), "render" + direction + "Face", "(LBlock;DDDI)V");

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "set normal when rendering block " + direction.toLowerCase() + " face";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        reference(GETSTATIC, new FieldRef("Tessellator", "instance", "LTessellator;")),
                        ASTORE, capture(any())
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        ALOAD, getCaptureGroup(1),
                        push((float) x),
                        push((float) y),
                        push((float) z),
                        reference(INVOKEVIRTUAL, new MethodRef("Tessellator", "setNormal", "(FFF)V"))
                    );
                }
            }.targetMethod(faceMethods[face]));
        }
    }

    private class EntityPlayerMod extends ClassMod {
        EntityPlayerMod() {
            classSignatures.add(new ConstSignature("humanoid"));
            classSignatures.add(new ConstSignature("/mob/char.png"));

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "inventory", "LInventoryPlayer;")));
        }
    }

    private class EntityPlayerSPMod extends ClassMod {
        EntityPlayerSPMod() {
            parentClass = "EntityPlayer";

            classSignatures.add(new OrSignature(
                new ConstSignature("http://s3.amazonaws.com/MinecraftSkins/"), // 1.2
                new ConstSignature("http://skins.minecraft.net/MinecraftSkins/") // 1.3
            ));
            classSignatures.add(new ConstSignature("portal.trigger"));
        }
    }

    private class EntityPlayerSPSubMod extends ClassMod {
        EntityPlayerSPSubMod() {
            parentClass = "EntityPlayerSP";

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(-999.0),
                        push(-999.0)
                    );
                }
            });
        }
    }

    private class InventoryPlayerMod extends ClassMod {
        InventoryPlayerMod(MinecraftVersion minecraftVersion) {
            if (minecraftVersion.compareTo("12w04a") >= 0) {
                classSignatures.add(new ConstSignature("container.inventory"));
            } else {
                classSignatures.add(new ConstSignature("Inventory"));
            }
            classSignatures.add(new ConstSignature("Slot"));

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getCurrentItem", "()LItemStack;")));
        }
    }

    private class ItemStackMod extends ClassMod {
        ItemStackMod() {
            classSignatures.add(new ConstSignature("id"));
            classSignatures.add(new ConstSignature("Count"));
            classSignatures.add(new ConstSignature("Damage"));

            memberMappers.add(new FieldMapper(
                new FieldRef(getDeobfClass(), "stackSize", "I"),
                new FieldRef(getDeobfClass(), "animationsToGo", "I"),
                new FieldRef(getDeobfClass(), "itemID", "I")
            )
                .accessFlag(AccessFlag.PUBLIC, true)
            );
        }
    }

    private class WorldMod extends BaseMod.WorldMod {
        WorldMod() {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        FCONST_1,
                        anyFLOAD,
                        push((float) Math.PI),
                        FMUL,
                        FCONST_2,
                        FMUL,
                        anyReference(INVOKESTATIC),
                        FCONST_2,
                        FMUL,
                        or(
                            build(push(0.75f)), // pre-12w21a
                            build(push(0.25f))  // 12w21a+
                        ),
                        FADD,
                        FSUB
                    );
                }
            }.setMethod(new MethodRef(getDeobfClass(), "getStarBrightness", "(F)F")));

            classSignatures.add(new FixedBytecodeSignature(
                begin(),
                ALOAD_0,
                ILOAD_1,
                ILOAD_2,
                ILOAD_3,
                ICONST_1,
                anyReference(INVOKEVIRTUAL),
                IRETURN,
                end()
            ).setMethod(new MethodRef(getDeobfClass(), "getBlockLightValue", "(III)I")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // entity.lastTickPosX = entity.posX;
                        ALOAD_1,
                        ALOAD_1,
                        captureReference(GETFIELD),
                        captureReference(PUTFIELD),

                        // entity.lastTickPosY = entity.posY;
                        ALOAD_1,
                        ALOAD_1,
                        captureReference(GETFIELD),
                        captureReference(PUTFIELD),

                        // entity.lastTickPosZ = entity.posZ;
                        ALOAD_1,
                        ALOAD_1,
                        captureReference(GETFIELD),
                        captureReference(PUTFIELD)
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
                public String getMatchExpression() {
                    return buildExpression(
                        // f1 = getCelestialAngle(f);
                        begin(),
                        ALOAD_0,
                        FLOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        FSTORE_2,

                        // return f1 * 3.141593f * 2.0f;
                        FLOAD_2,
                        push((float) Math.PI),
                        FMUL,
                        push(2.0f),
                        FMUL,
                        FRETURN,
                        end()
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "getCelestialAngleRadians", "(F)F"))
                .addXref(1, new MethodRef(getDeobfClass(), "getCelestialAngle", "(F)F"))
            );

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getSeed", "()J"), new MethodRef(getDeobfClass(), "getWorldTime", "()J")));
        }
    }

    private class WorldRendererMod extends ClassMod {
        WorldRendererMod() {
            final MethodRef updateRenderer = new MethodRef(getDeobfClass(), "updateRenderer", "()V");

            classSignatures.add(new ConstSignature(new MethodRef(MCPatcherUtils.GL11_CLASS, "glNewList", "(II)V")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(1.000001F)
                    );
                }
            }.setMethod(updateRenderer));

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "worldObj", "LWorld;")));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "call Tessellator.setEntity";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        anyILOAD,
                        anyALOAD,
                        capture(anyALOAD),
                        anyILOAD,
                        anyILOAD,
                        anyILOAD,
                        reference(INVOKEVIRTUAL, new MethodRef("RenderBlocks", "renderBlockByRenderType", "(LBlock;III)Z")),
                        IOR,
                        anyISTORE
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // Tessellator.instance.setEntity(block1.blockID);
                        reference(GETSTATIC, new FieldRef("Tessellator", "instance", "LTessellator;")),
                        getCaptureGroup(1), // block1
                        reference(GETFIELD, new FieldRef("Block", "blockID", "I")),
                        reference(INVOKEVIRTUAL, new MethodRef("Tessellator", "setEntity", "(I)V"))
                    );
                }
            }.targetMethod(updateRenderer));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "clear Tessellator.setEntity";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        RETURN,
                        end()
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // Tessellator.instance.setEntity(-1);
                        reference(GETSTATIC, new FieldRef("Tessellator", "instance", "LTessellator;")),
                        ICONST_M1,
                        reference(INVOKEVIRTUAL, new MethodRef("Tessellator", "setEntity", "(I)V"))
                    );
                }
            }.targetMethod(updateRenderer));
        }
    }
}
