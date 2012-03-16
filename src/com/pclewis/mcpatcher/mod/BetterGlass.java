package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.BadBytecode;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class BetterGlass extends Mod {
    private static final String GL11_CLASS = "org.lwjgl.opengl.GL11";

    private static final MethodRef glEnable = new MethodRef(GL11_CLASS, "glEnable", "(I)V");
    private static final MethodRef getRenderBlockPass = new MethodRef("Block", "getRenderBlockPass", "()I");

    public BetterGlass(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.BETTER_GLASS;
        author = "MCPatcher";
        description = "Enables partial transparency for glass blocks.";
        version = "1.0";
        defaultEnabled = false;

        classMods.add(new BlockMod());
        classMods.add(new BlockBreakableMod());
        classMods.add(new BlockGlassMod());
        classMods.add(new BlockPaneMod());
        classMods.add(new RenderBlocksMod());
        classMods.add(new WorldRendererMod());
        classMods.add(new EntityRendererMod());
        classMods.add(new RenderGlobalMod());
        classMods.add(new ItemRendererMod());
        classMods.add(new RenderItemMod());
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
            addBlockSignature("BlockGlass");
            addBlockSignature("BlockPane");
        }
    }
    
    private class BlockBreakableMod extends ClassMod {
        BlockBreakableMod() {
            parentClass = "Block";
            prerequisiteClasses.add("BlockGlass");
        }
    }

    private class BlockGlassMod extends ClassMod {
        BlockGlassMod() {
            parentClass = "BlockBreakable";
            prerequisiteClasses.add("Block");

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "change glass block render pass";
                }

                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().equals("()I")) {
                        return buildExpression(
                            ICONST_0
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ICONST_2
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "getRenderBlockPass", "()I")));
        }
    }
    
    private class BlockPaneMod extends ClassMod {
        BlockPaneMod() {
            parentClass = "Block";
            prerequisiteClasses.add(parentClass);
            
            patches.add(new AddMethodPatch("getRenderBlockPass", "()I") {
                @Override
                public byte[] generateMethod() throws BadBytecode, IOException {
                    return buildCode(
                        ICONST_2,
                        IRETURN
                    );
                }
            });
        }
    }

    private class RenderBlocksMod extends BaseMod.RenderBlocksMod {
        RenderBlocksMod() {
            final MethodRef renderBlockAsItem = new MethodRef(getDeobfClass(), "renderBlockAsItem", "(LBlock;IF)V");
            final MethodRef getRenderType = new MethodRef("Block", "getRenderType", "()I");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // k = block.getRenderType();
                        ALOAD_1,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        ISTORE, BinaryRegex.capture(BinaryRegex.any()),

                        // if (k == 0 || k == 16)
                        ILOAD, BinaryRegex.backReference(2),
                        IFEQ, BinaryRegex.any(2),
                        ILOAD, BinaryRegex.backReference(2),
                        push(16),
                        IF_ICMPNE, BinaryRegex.any(2)
                    );
                }
            }
                .setMethod(renderBlockAsItem)
                .addXref(1, getRenderType)
            );
            
            memberMappers.add(new MethodMapper("renderBlockPane", "(LBlockPane;III)Z"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "enable culling on glass panes";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        push(2884), /*GL_CULL_FACE*/
                        reference(INVOKESTATIC, glEnable)
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "renderBlockPane", "(LBlockPane;III)Z")));
        }
    }

    private class WorldRendererMod extends ClassMod {
        WorldRendererMod() {
            final MethodRef updateRenderer = new MethodRef(getDeobfClass(), "updateRenderer", "()V");
            final FieldRef skipRenderPass = new FieldRef(getDeobfClass(), "skipRenderPass", "[Z");

            classSignatures.add(new ConstSignature(new MethodRef(GL11_CLASS, "glNewList", "(II)V")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // j3 = block.getRenderBlockPass();
                        BytecodeMatcher.anyALOAD,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        ISTORE, BinaryRegex.capture(BinaryRegex.any()),
                        
                        // ...
                        BinaryRegex.any(0, 30),

                        // if (j3 != i2)
                        ILOAD, BinaryRegex.backReference(2),
                        ILOAD, BinaryRegex.any(),
                        IF_ICMPEQ, BinaryRegex.any(2),

                        // flag = true;
                        ICONST_1,
                        ISTORE, BinaryRegex.any()
                    );
                }
            }
                .setMethod(updateRenderer)
                .addXref(1, getRenderBlockPass)
            );
            
            memberMappers.add(new FieldMapper("skipRenderPass", "[Z"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "increase render passes from 2 to 3 (init)";
                }

                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            ICONST_2,
                            NEWARRAY, T_BOOLEAN,
                            reference(PUTFIELD, skipRenderPass)
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        ICONST_3,
                        NEWARRAY, T_BOOLEAN,
                        reference(PUTFIELD, skipRenderPass)
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "increase render passes from 2 to 3 (loop)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.capture(BytecodeMatcher.anyILOAD),
                        ICONST_2,
                        BinaryRegex.capture(BinaryRegex.build(
                            BinaryRegex.subset(new byte[]{(byte) IF_ICMPLT, (byte) IF_ICMPGE}, true),
                            BinaryRegex.any(2)
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        ICONST_3,
                        getCaptureGroup(2)
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "increase render passes from 2 to 3 (&&)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // return skipRenderPass[0] && skipRenderPass[1]; 
                        ALOAD_0,
                        reference(GETFIELD, skipRenderPass),
                        ICONST_0,
                        BALOAD,
                        IFEQ, BinaryRegex.any(2),
                        ALOAD_0,
                        reference(GETFIELD, skipRenderPass),
                        ICONST_1,
                        BALOAD,
                        IFEQ, BinaryRegex.any(2),
                        ICONST_1,
                        BinaryRegex.or(
                            BinaryRegex.build(IRETURN),
                            BinaryRegex.build(
                                GOTO, BinaryRegex.any(2)
                            )
                        ),
                        ICONST_0,
                        IRETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // return skipRenderPass[0] && skipRenderPass[1] && skipRenderPass[2]; 
                        ALOAD_0,
                        reference(GETFIELD, skipRenderPass),
                        ICONST_0,
                        BALOAD,
                        IFEQ, branch("A"),
                        ALOAD_0,
                        reference(GETFIELD, skipRenderPass),
                        ICONST_1,
                        BALOAD,
                        IFEQ, branch("A"),
                        ALOAD_0,
                        reference(GETFIELD, skipRenderPass),
                        ICONST_2,
                        BALOAD,
                        IFEQ, branch("A"),
                        ICONST_1,
                        IRETURN,
                        label("A"),
                        ICONST_0,
                        IRETURN
                    );
                }
            });
        }
    }

    private class EntityRendererMod extends ClassMod {
        final MethodRef renderWorld = new MethodRef(getDeobfClass(), "renderWorld", "(FJ)V");
        final FieldRef loop = new FieldRef(getDeobfClass(), "betterGrassLoop", "I");

        EntityRendererMod() {
            final MethodRef glBlendFunc = new MethodRef(GL11_CLASS, "glBlendFunc", "(II)V");
            final MethodRef glDisable = new MethodRef(GL11_CLASS, "glDisable", "(I)V");
            final MethodRef sortAndRender = new MethodRef("RenderGlobal", "sortAndRender", "(LEntityLiving;ID)I");
            final MethodRef renderAllRenderLists = new MethodRef("RenderGlobal", "renderAllRenderLists", "(ID)V");

            classSignatures.add(new ConstSignature("/terrain.png"));
            classSignatures.add(new ConstSignature("/environment/snow.png"));
            classSignatures.add(new ConstSignature("ambient.weather.rain"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(770),
                        push(771),
                        reference(INVOKESTATIC, glBlendFunc)
                    );
                }
            }.setMethod(renderWorld));
            
            patches.add(new AddFieldPatch("betterGrassLoop", "I"));
            
            addRenderPassPatch(renderAllRenderLists);
            addRenderPassPatch(sortAndRender);

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "add new render pass";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.capture(BinaryRegex.build(
                            // GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                            push(770),
                            push(771),
                            reference(INVOKESTATIC, glBlendFunc)
                        )),
                        
                        // ...
                        BinaryRegex.capture(BinaryRegex.build(
                            BinaryRegex.any(0, 1000)
                        )),

                        // setupFog(f);
                        // GL11.glDisable(GL_FOG);
                        BinaryRegex.capture(BinaryRegex.build(
                            ALOAD_0,
                            FLOAD_1,
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL),
                            push(2912),
                            reference(INVOKESTATIC, glDisable)
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        
                        // loop = 1;
                        ALOAD_0,
                        ICONST_1,
                        reference(PUTFIELD, loop),
                        
                        // if (loop >= 3) goto B;
                        label("A"),
                        ALOAD_0,
                        reference(GETFIELD, loop),
                        ICONST_3,
                        IF_ICMPGE, branch("B"),

                        // ...
                        getCaptureGroup(2),
                        
                        // loop++;
                        ALOAD_0,
                        DUP,
                        reference(GETFIELD, loop),
                        ICONST_1,
                        IADD,
                        reference(PUTFIELD, loop),

                        GOTO, branch("A"),

                        label("B"),
                        getCaptureGroup(3)
                    );
                }
            }.targetMethod(renderWorld));
        }
        
        private void addRenderPassPatch(final MethodRef method) {
            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render pass 1 -> i (" + method.getName() + ")";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.capture(BinaryRegex.build(
                            ALOAD, 5,
                            BinaryRegex.any(0, 2)
                        )),
                        ICONST_1,
                        BinaryRegex.capture(BinaryRegex.build(
                            FLOAD_1,
                            F2D,
                            reference(INVOKEVIRTUAL, method)
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        ALOAD_0,
                        reference(GETFIELD, loop),
                        getCaptureGroup(2)
                    );
                }
            }.targetMethod(renderWorld));
        }
    }

    private class RenderGlobalMod extends ClassMod {
        RenderGlobalMod() {
            final MethodRef sortAndRender = new MethodRef(getDeobfClass(), "sortAndRender", "(LEntityLiving;ID)I");
            final MethodRef renderAllRenderLists = new MethodRef(getDeobfClass(), "renderAllRenderLists", "(ID)V");
            
            classSignatures.add(new ConstSignature("smoke"));
            classSignatures.add(new ConstSignature("/environment/clouds.png"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().equals("()V")) {
                        return null;
                    } else {
                        return buildExpression(
                            reference(INVOKESTATIC, new MethodRef("java/util/Arrays", "sort", "([Ljava/lang/Object;Ljava/util/Comparator;)V"))
                        );
                    }
                }
            }.setMethod(sortAndRender));
            
            memberMappers.add(new MethodMapper("renderAllRenderLists", "(ID)V"));
        }
    }
    
    private class ItemRendererMod extends ClassMod {
        ItemRendererMod() {
            final MethodRef renderItem = new MethodRef(getDeobfClass(), "renderItem", "(LEntityLiving;LItemStack;I)V");
            final FieldRef mc = new FieldRef(getDeobfClass(), "mc", "LMinecraft;");
            final FieldRef renderEngine = new FieldRef("Minecraft", "renderEngine", "LRenderEngine;");
            final MethodRef getTexture = new MethodRef("RenderEngine", "getTexture", "(Ljava/lang/String;)I");
            final FieldRef itemID = new FieldRef("ItemStack", "itemID", "I");
            final MethodRef glBindTexture = new MethodRef(GL11_CLASS, "glBindTexture", "(II)V");

            classSignatures.add(new ConstSignature("/terrain.png"));
            classSignatures.add(new ConstSignature("/gui/items.png"));
            classSignatures.add(new ConstSignature("%blur%/misc/glint.png"));
            classSignatures.add(new ConstSignature("/misc/water.png"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(3553), // GL_TEXTURE_2D
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.captureReference(GETFIELD),
                        push("/terrain.png"),
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        reference(INVOKESTATIC, glBindTexture)
                    );
                }
            }
                .setMethod(renderItem)
                .addXref(1, mc)
                .addXref(2, renderEngine)
                .addXref(3, getTexture)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BytecodeMatcher.anyReference(GETSTATIC),
                        ALOAD_2,
                        BytecodeMatcher.captureReference(GETFIELD),
                        AALOAD
                    );
                }
            }
                .setMethod(renderItem)
                .addXref(1, itemID)
            );

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "enable alpha transparency for held items";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(3553), // GL_TEXTURE_2D
                        ALOAD_0,
                        reference(GETFIELD, mc),
                        reference(GETFIELD, renderEngine),
                        push("/terrain.png"),
                        reference(INVOKEVIRTUAL, getTexture),
                        reference(INVOKESTATIC, glBindTexture)
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // if (Block.blocksList[itemstack.itemID].getRenderBlockPass() != 0) {
                        reference(GETSTATIC, new FieldRef("Block", "blocksList", "[LBlock;")),
                        ALOAD_2,
                        reference(GETFIELD, itemID),
                        AALOAD,
                        reference(INVOKEVIRTUAL, getRenderBlockPass),
                        IFEQ, branch("A"),

                        // GL11.glEnable(3042 /*GL_BLEND*/);
                        push(3042), // GL_BLEND
                        reference(INVOKESTATIC, glEnable),

                        // }
                        label("A")
                    );
                }
            });
        }
    }
    
    private class RenderItemMod extends ClassMod {
        RenderItemMod() {
            final MethodRef doRenderItem = new MethodRef(getDeobfClass(), "doRenderItem", "(LEntityItem;DDDFF)V");
            final MethodRef drawItemIntoGui = new MethodRef(getDeobfClass(), "drawItemIntoGui", "(LFontRenderer;LRenderEngine;IIIII)V");
            
            classSignatures.add(new ConstSignature("/terrain.png"));
            classSignatures.add(new ConstSignature("/gui/items.png"));
            classSignatures.add(new ConstSignature("%blur%/misc/glint.png"));
            classSignatures.add(new ConstSignature("/misc/water.png").negate(true));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ALOAD_0,
                        BytecodeMatcher.anyReference(GETFIELD),
                        push(187L),
                        reference(INVOKEVIRTUAL, new MethodRef("java/util/Random", "setSeed", "(J)V"))
                    );
                }
            }.setMethod(doRenderItem));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(210.0f),
                        push(1.0f),
                        push(0.0f),
                        push(0.0f),
                        reference(INVOKESTATIC, new MethodRef(GL11_CLASS, "glRotatef", "(FFFF)V"))
                    );
                }
            }.setMethod(drawItemIntoGui));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "enable alpha transparency for glass item entities";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/terrain.png"),
                        BytecodeMatcher.anyReference(INVOKEVIRTUAL)
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // if (Block.blocksList[itemstack.itemID].getRenderBlockPass() != 0) {
                        reference(GETSTATIC, new FieldRef("Block", "blocksList", "[LBlock;")),
                        ALOAD, 10,
                        reference(GETFIELD, new FieldRef("ItemStack", "itemID", "I")),
                        AALOAD,
                        reference(INVOKEVIRTUAL, getRenderBlockPass),
                        IFEQ, branch("A"),

                        // GL11.glEnable(3042);
                        push(3042), // GL_BLEND
                        reference(INVOKESTATIC, glEnable),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(doRenderItem));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "enable alpha transparency for glass blocks in gui";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/terrain.png")
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        // if (Block.blocksList[i].getRenderBlockPass() != 0) {
                        reference(GETSTATIC, new FieldRef("Block", "blocksList", "[LBlock;")),
                        ILOAD_3,
                        AALOAD,
                        reference(INVOKEVIRTUAL, getRenderBlockPass),
                        IFEQ, branch("A"),

                        // GL11.glEnable(3042);
                        push(3042), // GL_BLEND
                        reference(INVOKESTATIC, glEnable),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(drawItemIntoGui));
        }
    }
}
