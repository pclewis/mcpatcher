package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;

import java.io.IOException;

import static com.pclewis.mcpatcher.BinaryRegex.*;
import static com.pclewis.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BetterGlass extends Mod {
    private static final int EXTRA_PASSES = 2;

    private static final MethodRef glEnable = new MethodRef(MCPatcherUtils.GL11_CLASS, "glEnable", "(I)V");
    private static final MethodRef glDisable = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDisable", "(I)V");
    private static final MethodRef glDepthMask = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDepthMask", "(Z)V");
    private static final MethodRef glShadeModel = new MethodRef(MCPatcherUtils.GL11_CLASS, "glShadeModel", "(I)V");
    private static final MethodRef glCallList = new MethodRef(MCPatcherUtils.GL11_CLASS, "glCallList", "(I)V");
    private static final MethodRef getRenderBlockPass = new MethodRef("Block", "getRenderBlockPass", "()I");
    private static final MethodRef enableLightmap = new MethodRef("EntityRenderer", "enableLightmap", "(D)V");
    private static final MethodRef disableLightmap = new MethodRef("EntityRenderer", "disableLightmap", "(D)V");

    public BetterGlass(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.BETTER_GLASS;
        author = "MCPatcher";
        description = "Enables partial transparency for glass blocks.";
        version = "1.9";

        addDependency(BaseTexturePackMod.NAME);
        addDependency(MCPatcherUtils.CONNECTED_TEXTURES);

        addClassMod(new BaseMod.MinecraftMod());
        addClassMod(new BaseMod.BlockMod());
        addClassMod(new BaseMod.IBlockAccessMod());
        addClassMod(new WorldRendererMod());
        addClassMod(new EntityRendererMod());
        addClassMod(new RenderGlobalMod());
        addClassMod(new RenderBlocksMod());

        addClassFile(MCPatcherUtils.RENDER_PASS_CLASS);
        addClassFile(MCPatcherUtils.RENDER_PASS_CLASS + "$1");
        addClassFile(MCPatcherUtils.RENDER_PASS_CLASS + "$2");
    }

    private class WorldRendererMod extends ClassMod {
        private int loopRegister;

        WorldRendererMod() {
            final MethodRef updateRenderer = new MethodRef(getDeobfClass(), "updateRenderer", "()V");
            final FieldRef glRenderList = new FieldRef(getDeobfClass(), "glRenderList", "I");
            final FieldRef skipRenderPass = new FieldRef(getDeobfClass(), "skipRenderPass", "[Z");
            final MethodRef startPass = new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "start", "(I)V");
            final MethodRef canRenderInPass1 = new MethodRef("forge/ForgeHooksClient", "canRenderInPass", "(LBlock;I)Z");
            final MethodRef canRenderInPass2 = new MethodRef("Block", "canRenderInPass", "(I)Z");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // j3 = block.getRenderBlockPass();
                        anyALOAD,
                        captureReference(INVOKEVIRTUAL),
                        ISTORE, capture(any()),

                        // ...
                        any(0, 30),

                        // if (j3 != i2)
                        ILOAD, backReference(2),
                        ILOAD, any(),
                        IF_ICMPEQ, any(2),

                        // flag = true;
                        push(1),
                        ISTORE, any()
                    );
                }
            }
                .setMethod(updateRenderer)
                .addXref(1, getRenderBlockPass)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        push(2),
                        IADD,
                        reference(INVOKESTATIC, glCallList)
                    );
                }
            }.addXref(1, glRenderList));

            addMemberMapper(new FieldMapper(skipRenderPass));

            addPatch(new RenderPassPatch("init") {
                @Override
                protected String getPrefix() {
                    return buildExpression(
                        ALOAD_0
                    );
                }

                @Override
                protected String getSuffix() {
                    return buildExpression(
                        NEWARRAY, T_BOOLEAN,
                        reference(PUTFIELD, skipRenderPass)
                    );
                }
            }.matchConstructorOnly(true));

            addPatch(new RenderPassPatch("loop") {
                @Override
                protected String getPrefix() {
                    return buildExpression(
                        anyILOAD
                    );
                }

                @Override
                protected String getSuffix() {
                    return buildExpression(
                        IF_ICMPLT_or_IF_ICMPGE, any(2)
                    );
                }
            });

            addPatch(new RenderPassPatch("occlusion") {
                @Override
                protected String getPrefix() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, glRenderList)
                    );
                }

                @Override
                protected String getSuffix() {
                    return buildExpression(
                        IADD
                    );
                }
            });

            addPatch(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "pre render pass";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0),
                        ISTORE, capture(any()),
                        push(0),
                        ISTORE, any(),
                        push(0),
                        ISTORE, any()
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    loopRegister = (getCaptureGroup(1)[0] & 0xff) - 1;
                    Logger.log(Logger.LOG_CONST, "loop register %d", loopRegister);
                    return buildCode(
                        ILOAD, loopRegister,
                        reference(INVOKESTATIC, startPass)
                    );
                }
            }.targetMethod(updateRenderer));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "prevent early loop exit";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (!var12) {
                        ILOAD, loopRegister + 1,
                        IFNE, any(2),

                        // break;
                        GOTO, any(2)

                        // }
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                    );
                }
            }.targetMethod(updateRenderer));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "increase render passes from 2 to " + (2 + EXTRA_PASSES) + " (&&)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // return skipRenderPass[0] && skipRenderPass[1];
                        ALOAD_0,
                        reference(GETFIELD, skipRenderPass),
                        push(0),
                        BALOAD,
                        IFEQ, any(2),
                        ALOAD_0,
                        reference(GETFIELD, skipRenderPass),
                        push(1),
                        BALOAD,
                        IFEQ, any(2),
                        push(1),
                        or(
                            build(IRETURN),
                            build(
                                GOTO, any(2)
                            )
                        ),
                        push(0),
                        IRETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // return RenderPass.skipAllRenderPasses(skipRenderPass);
                        ALOAD_0,
                        reference(GETFIELD, skipRenderPass),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "skipAllRenderPasses", "([Z)Z")),
                        IRETURN
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up extra render pass";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEVIRTUAL, getRenderBlockPass)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "getBlockRenderPass", "(LBlock;)I"))
                    );
                }
            }.targetMethod(updateRenderer));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up extra render pass (forge)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(or(
                        build(reference(INVOKESTATIC, canRenderInPass1)),
                        build(reference(INVOKEVIRTUAL, canRenderInPass2))
                    ));
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        DUP2,
                        getMatch(),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "canRenderInPass", "(LBlock;IZ)Z"))
                    );
                }
            }.targetMethod(updateRenderer));
        }

        abstract private class RenderPassPatch extends BytecodePatch {
            private final String tag;

            RenderPassPatch(String tag) {
                this.tag = tag;
            }

            @Override
            public String getDescription() {
                return "increase render passes from 2 to " + (2 + EXTRA_PASSES) + " (" + tag + ")";
            }

            @Override
            public final String getMatchExpression() {
                return buildExpression(
                    lookBehind(getPrefix(), true),
                    push(2),
                    lookAhead(getSuffix(), true)
                );
            }

            @Override
            public final byte[] getReplacementBytes() throws IOException {
                return buildCode(
                    push(2 + EXTRA_PASSES)
                );
            }

            abstract protected String getPrefix();

            abstract protected String getSuffix();
        }
    }

    private class EntityRendererMod extends ClassMod {
        EntityRendererMod() {
            final MethodRef renderWorld = new MethodRef(getDeobfClass(), "renderWorld", "(FJ)V");
            final MethodRef renderRainSnow = new MethodRef(getDeobfClass(), "renderRainSnow", "(F)V");
            final MethodRef sortAndRender = new MethodRef("RenderGlobal", "sortAndRender", "(LEntityLiving;ID)I");
            final MethodRef doRenderPass = new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "doRenderPass", "(LRenderGlobal;LEntityLiving;ID)V");

            addClassSignature(new ConstSignature("/terrain.png"));
            addClassSignature(new ConstSignature("/environment/snow.png"));
            addClassSignature(new ConstSignature("ambient.weather.rain"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD, 5,
                        ALOAD, 4,
                        push(1),
                        FLOAD_1,
                        F2D,
                        captureReference(INVOKEVIRTUAL),
                        ISTORE, any()
                    );
                }
            }
                .setMethod(renderWorld)
                .addXref(1, sortAndRender)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(3553), // GL_TEXTURE_2D,
                        reference(INVOKESTATIC, glDisable)
                    );
                }
            }.setMethod(disableLightmap));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(3553), // GL_TEXTURE_2D,
                        reference(INVOKESTATIC, glEnable)
                    );
                }
            }.setMethod(enableLightmap));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/environment/snow.png")
                    );
                }
            }.setMethod(renderRainSnow));

            addPatch(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "set gl shade model";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (...)
                        IFEQ, any(2),

                        // GL11.glShadeModel(GL11.GL_SMOOTH);
                        push(7425),
                        reference(INVOKESTATIC, glShadeModel)
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // if (RenderPass.setAmbientOcclusion(...))
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "setAmbientOcclusion", "(Z)Z"))
                    );
                }
            });

            addPatch(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "do extra render pass 2";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD, 5,
                        ALOAD, 4,
                        push(0),
                        FLOAD_1,
                        F2D,
                        reference(INVOKEVIRTUAL, sortAndRender),
                        POP
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // RenderPass.doRenderPass(renderGlobal, camera, 2, par1);
                        ALOAD, 5,
                        ALOAD, 4,
                        push(2),
                        FLOAD_1,
                        F2D,
                        reference(INVOKESTATIC, doRenderPass)
                    );
                }
            });

            addPatch(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "do extra render pass 3";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // GL11.glDepthMask(true);
                        push(1),
                        reference(INVOKESTATIC, glDepthMask),

                        // GL11.glEnable(GL11.GL_CULL_FACE);
                        push(2884), // GL_CULL_FACE
                        reference(INVOKESTATIC, glEnable),

                        // GL11.glDisable(GL11.GL_BLEND);
                        push(3042), // GL_BLEND
                        reference(INVOKESTATIC, glDisable)
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // renderRainSnow(par1);
                        ALOAD_0,
                        FLOAD_1,
                        reference(INVOKEVIRTUAL, renderRainSnow),

                        // RenderPass.doRenderPass(renderGlobal, camera, 3, par1);
                        ALOAD, 5,
                        ALOAD, 4,
                        push(3),
                        FLOAD_1,
                        F2D,
                        reference(INVOKESTATIC, doRenderPass)
                    );
                }
            }.targetMethod(renderWorld));
        }
    }

    private class RenderGlobalMod extends ClassMod {
        RenderGlobalMod() {
            final FieldRef glRenderListBase = new FieldRef(getDeobfClass(), "glRenderListBase", "I");
            final MethodRef loadRenderers = new MethodRef(getDeobfClass(), "loadRenderers", "()V");
            final MethodRef renderAllRenderLists = new MethodRef(getDeobfClass(), "renderAllRenderLists", "(ID)V");
            final MethodRef generateDisplayLists = new MethodRef("GLAllocation", "generateDisplayLists", "(I)I");

            addClassSignature(new ConstSignature("smoke"));
            addClassSignature(new ConstSignature("/environment/clouds.png"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(3),
                        IMUL,
                        captureReference(INVOKESTATIC),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, generateDisplayLists)
                .addXref(2, glRenderListBase)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        anyILOAD,
                        push(16),
                        IDIV,
                        push(1),
                        IADD
                    );
                }
            }.setMethod(loadRenderers));

            addClassSignature(new OrSignature(
                // 1.4.3
                new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // var4 = this.allRenderLists;
                            ALOAD_0,
                            anyReference(GETFIELD),
                            ASTORE, capture(any()),

                            // var5 = var4.length;
                            ALOAD, backReference(1),
                            ARRAYLENGTH,
                            build(ISTORE, any())
                        );
                    }
                }.setMethod(renderAllRenderLists),
                // 1.4.4+
                new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // for (int var4 = 0; var4 < this.allRenderLists.length; var4++)
                            push(0),
                            ISTORE, capture(any()),
                            ILOAD, backReference(1),

                            ALOAD_0,
                            captureReference(GETFIELD),
                            ARRAYLENGTH,
                            IF_ICMPGE_or_IF_ICMPLT, any(2),

                            // this.allRenderLists[var4]...
                            ALOAD_0,
                            backReference(2),
                            ILOAD, backReference(1),
                            AALOAD
                        );
                    }
                }.setMethod(renderAllRenderLists)
            ));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "increase gl render lists per chunk from 3 to " + (3 + EXTRA_PASSES) + " (init)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(3),
                        lookAhead(build(
                            IMUL,
                            reference(INVOKESTATIC, generateDisplayLists)
                        ), true)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        push(3 + EXTRA_PASSES)
                    );
                }
            }.matchConstructorOnly(true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "increase gl render lists per chunk from 3 to " + (3 + EXTRA_PASSES) + " (loop)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        IINC, capture(any()), 3
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        IINC, getCaptureGroup(1), 3 + EXTRA_PASSES
                    );
                }
            }.targetMethod(loadRenderers));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up lightmap for extra render passes";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // mc.entityRenderer.enableLightmap(par2);
                        lookBehind(build(
                            ALOAD_0,
                            anyReference(GETFIELD),
                            anyReference(GETFIELD),
                            DLOAD_2
                        ), true),
                        reference(INVOKEVIRTUAL, enableLightmap)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ILOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "enableDisableLightmap", "(LEntityRenderer;DI)V"))
                    );
                }
            }.targetMethod(renderAllRenderLists));
        }
    }

    private class RenderBlocksMod extends BaseMod.RenderBlocksMod {
        RenderBlocksMod() {
            final MethodRef renderStandardBlockWithAmbientOcclusion = new MethodRef(getDeobfClass(), "renderStandardBlockWithAmbientOcclusion", "(LBlock;IIIFFF)Z");
            final FieldRef renderAllFaces = new FieldRef(getDeobfClass(), "renderAllFaces", "Z");
            final FieldRef blockAccess = new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;");
            final MethodRef shouldSideBeRendered = new MethodRef("Block", "shouldSideBeRendered", "(LIBlockAccess;IIII)Z");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0x0f000f)
                    );
                }
            }.setMethod(renderStandardBlockWithAmbientOcclusion));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        IFNE, any(2),
                        ALOAD_1,
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ILOAD_2,
                        ILOAD_3,
                        push(1),
                        ISUB,
                        ILOAD, 4,
                        push(0),
                        captureReference(INVOKEVIRTUAL),
                        IFEQ, any(2)
                    );
                }
            }
                .setMethod(renderStandardBlockWithAmbientOcclusion)
                .addXref(1, renderAllFaces)
                .addXref(2, blockAccess)
                .addXref(3, shouldSideBeRendered)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override AO block brightness for extra render passes";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // (flag ? f : 1.0f) * ...
                        IFEQ, any(2),
                        capture(anyFLOAD),
                        GOTO, any(2),
                        push(1.0f),
                        capture(or(
                            build(push(0.5f)),
                            build(push(0.6f)),
                            build(push(0.8f))
                        )),
                        FMUL
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // (flag ? f : 1.0f) * RenderPass.getAOBaseMultiplier(...)
                        IFEQ, branch("A"),
                        getCaptureGroup(1),
                        GOTO, branch("B"),
                        label("A"),
                        push(1.0f),
                        label("B"),
                        getCaptureGroup(2),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "getAOBaseMultiplier", "(F)F")),
                        FMUL
                    );
                }
            }.targetMethod(renderStandardBlockWithAmbientOcclusion));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render all sides of adjacent blocks";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEVIRTUAL, shouldSideBeRendered)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "shouldSideBeRendered", "(LBlock;LIBlockAccess;IIII)Z"))
                    );
                }
            });
        }
    }
}
