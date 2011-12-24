package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class CustomColors extends Mod {
    private boolean haveSpawnerEggs;

    public CustomColors(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.CUSTOM_COLORS;
        author = "MCPatcher";
        description = "Gives texture packs control over hardcoded colors in the game.";
        version = "1.1";

        if (minecraftVersion.compareTo("Beta 1.9 Prerelease 4") < 0) {
            addError("Requires Minecraft Beta 1.9 or newer.");
            return;
        }

        haveSpawnerEggs = minecraftVersion.compareTo("11w49a") >= 0 || minecraftVersion.compareTo("1.0.1") >= 0;

        configPanel = new ConfigPanel();

        classMods.add(new BaseMod.MinecraftMod().mapTexturePackList());
        classMods.add(new BaseMod.TexturePackListMod());
        classMods.add(new BaseMod.TexturePackBaseMod());
        classMods.add(new BaseMod.IBlockAccessMod());

        classMods.add(new BlockMod());

        classMods.add(new BiomeGenBaseMod());
        classMods.add(new BiomeGenSwampMod());
        classMods.add(new BlockFluidMod());
        classMods.add(new BlockCauldronMod());
        classMods.add(new BaseMod.ItemMod());
        classMods.add(new ItemBlockMod());
        classMods.add(new ItemRendererMod());

        classMods.add(new PotionMod());
        classMods.add(new PotionHelperMod());

        classMods.add(new ColorizerFoliageMod());
        classMods.add(new BlockLeavesMod());

        classMods.add(new WorldMod());
        classMods.add(new WorldChunkManagerMod());
        classMods.add(new EntityMod());
        classMods.add(new EntityFXMod());
        classMods.add(new EntityRainFXMod());
        classMods.add(new EntityDropParticleFXMod());
        classMods.add(new EntitySplashFXMod());
        classMods.add(new EntityBubbleFXMod());

        classMods.add(new EntityRendererMod());

        classMods.add(new BlockLilyPadMod());

        classMods.add(new BlockRedstoneWireMod());
        classMods.add(new RenderBlocksMod());
        classMods.add(new EntityReddustFXMod());

        classMods.add(new BlockStemMod());

        if (haveSpawnerEggs) {
            classMods.add(new EntityListMod());
            classMods.add(new ItemSpawnerEggMod());
        }

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.COLORIZER_CLASS));
    }

    private class ConfigPanel extends ModConfigPanel {
        private JCheckBox waterCheckBox;
        private JCheckBox swampCheckBox;
        private JCheckBox treeCheckBox;
        private JCheckBox potionCheckBox;
        private JCheckBox dropCheckBox;
        private JPanel panel;
        private JCheckBox lightmapCheckBox;
        private JCheckBox redstoneCheckBox;
        private JCheckBox stemCheckBox;
        private JCheckBox otherBlockCheckBox;
        private JCheckBox eggCheckBox;

        ConfigPanel() {
            waterCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "water", waterCheckBox.isSelected());
                }
            });

            swampCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "swamp", swampCheckBox.isSelected());
                }
            });

            treeCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "tree", treeCheckBox.isSelected());
                }
            });

            potionCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "potion", potionCheckBox.isSelected());
                }
            });

            dropCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "drop", dropCheckBox.isSelected());
                }
            });

            lightmapCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "lightmaps", lightmapCheckBox.isSelected());
                }
            });

            redstoneCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "redstone", redstoneCheckBox.isSelected());
                }
            });

            stemCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "stem", stemCheckBox.isSelected());
                }
            });

            eggCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "egg", eggCheckBox.isSelected());
                }
            });

            otherBlockCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(MCPatcherUtils.CUSTOM_COLORS, "otherBlocks", otherBlockCheckBox.isSelected());
                }
            });
        }

        @Override
        public JPanel getPanel() {
            return panel;
        }

        @Override
        public void load() {
            waterCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "water", true));
            swampCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "swamp", true));
            treeCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "tree", true));
            potionCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "potion", true));
            dropCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "drop", true));
            lightmapCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "lightmaps", true));
            redstoneCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "redstone", true));
            stemCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "stem", true));
            eggCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "egg", true));
            otherBlockCheckBox.setSelected(MCPatcherUtils.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "otherBlocks", true));
            eggCheckBox.setVisible(haveSpawnerEggs);
        }

        @Override
        public void save() {
        }
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
            final MethodRef getRenderColor = new MethodRef(getDeobfClass(), "getRenderColor", "(I)I");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.getDescriptor().equals(getRenderColor.getType())) {
                        return buildExpression(
                            BinaryRegex.begin(),
                            push(methodInfo, 0xffffff),
                            IRETURN,
                            BinaryRegex.end()
                        );
                    } else {
                        return null;
                    }
                }
            }.setMethod(getRenderColor));

            memberMappers.add(new FieldMapper("blockID", "I")
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
                .accessFlag(AccessFlag.FINAL, true)
            );

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override color multiplier for all blocks";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 0xffffff)
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_0,
                        ALOAD_1,
                        reference(methodInfo, INVOKEINTERFACE, new InterfaceMethodRef("IBlockAccess", "getWorldChunkManager", "()LWorldChunkManager;")),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBlock", "(LBlock;LWorldChunkManager;III)I"))
                    );
                }

            }.targetMethod(new MethodRef(getDeobfClass(), "colorMultiplier", "(LIBlockAccess;III)I")));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override default color for all blocks";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 0xffffff)
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBlock", "(LBlock;)I"))
                    );
                }
            }.targetMethod(getRenderColor));
        }
    }

    private class BiomeGenBaseMod extends ClassMod {
        BiomeGenBaseMod() {
            classSignatures.add(new ConstSignature("Ocean"));
            classSignatures.add(new ConstSignature("Plains"));
            classSignatures.add(new ConstSignature("Desert"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            push(methodInfo, 0xffffff),
                            BytecodeMatcher.captureReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            }.addXref(1, new FieldRef(getDeobfClass(), "waterColorMultiplier", "I")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            ILOAD_1,
                            BytecodeMatcher.captureReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            }.addXref(1, new FieldRef(getDeobfClass(), "biomeID", "I")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.getDescriptor().startsWith("(Ljava/lang/String;)")) {
                        return buildExpression(
                            BinaryRegex.begin(),
                            ALOAD_0,
                            ALOAD_1,
                            BytecodeMatcher.captureReference(PUTFIELD),
                            ALOAD_0,
                            ARETURN,
                            BinaryRegex.end()
                        );
                    } else {
                        return null;
                    }
                }
            }
                .setMethodName("setBiomeName")
                .addXref(1, new FieldRef(getDeobfClass(), "biomeName", "Ljava/lang/String;"))
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // d = iblockaccess.getWorldChunkManager().getTemperature(i, j, k);
                        ALOAD_1,
                        BytecodeMatcher.anyReference(INVOKEINTERFACE),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        F2D,
                        DSTORE, 5,

                        // d1 = iblockaccess.getWorldChunkManager().getRainfall(i, k);
                        ALOAD_1,
                        BytecodeMatcher.anyReference(INVOKEINTERFACE),
                        ILOAD_2,
                        ILOAD, 4,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        F2D,
                        DSTORE, 7
                    );
                }
            }
                .addXref(1, new MethodRef("WorldChunkManager", "getTemperature", "(III)F"))
                .addXref(2, new MethodRef("WorldChunkManager", "getRainfall", "(II)F"))
            );

            memberMappers.add(new MethodMapper(new String[]{"getGrassColor", "getFoliageColor"}, "(LIBlockAccess;III)I"));
            memberMappers.add(new FieldMapper("color", "I").accessFlag(AccessFlag.PUBLIC, true));
        }
    }

    private class BiomeGenSwampMod extends ClassMod {
        private static final int MAGIC1 = 0xfefefe;
        private static final int MAGIC2 = 0x4e0e4e;
        private static final int MAGIC3 = 0xe0ff70;

        BiomeGenSwampMod() {
            parentClass = "BiomeGenBase";

            classSignatures.add(new ConstSignature(MAGIC1));
            classSignatures.add(new ConstSignature(MAGIC2));
            classSignatures.add(new ConstSignature(MAGIC3));

            addSwampColorPatch(0, "Grass");
            addSwampColorPatch(1, "Foliage");
        }

        private void addSwampColorPatch(final int index, final String name) {
            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override swamp " + name.toLowerCase() + " color";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, MAGIC1),
                        IAND,
                        push(methodInfo, MAGIC2),
                        IADD,
                        ICONST_2,
                        IDIV
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        push(methodInfo, index),
                        DLOAD, 5,
                        DLOAD, 7,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBiome", "(IIDD)I"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "get" + name + "Color", "(LIBlockAccess;III)I")));
        }
    }

    private class BlockFluidMod extends ClassMod {
        BlockFluidMod() {
            classSignatures.add(new ConstSignature("splash"));
            classSignatures.add(new ConstSignature("liquid.water"));

            MethodRef colorMultiplier = new MethodRef(getDeobfClass(), "colorMultiplier", "(LIBlockAccess;III)I");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD_1,
                        BytecodeMatcher.anyReference(INVOKEINTERFACE),
                        ILOAD_2,
                        ILOAD, 4,
                        BytecodeMatcher.anyReference(INVOKEVIRTUAL),
                        ASTORE, 5,
                        ALOAD, 5,
                        BytecodeMatcher.anyReference(GETFIELD),
                        IRETURN
                    );
                }
            }.setMethod(colorMultiplier));

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override water color";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD, 5,
                        reference(methodInfo, GETFIELD, new FieldRef("BiomeGenBase", "waterColorMultiplier", "I"))
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        push(methodInfo, 5),
                        ALOAD_1,
                        reference(methodInfo, INVOKEINTERFACE, new InterfaceMethodRef("IBlockAccess", "getWorldChunkManager", "()LWorldChunkManager;")),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBiome", "(IILWorldChunkManager;III)I"))
                    );
                }
            }.targetMethod(colorMultiplier));
        }
    }

    private class BlockCauldronMod extends ClassMod {
        BlockCauldronMod() {
            parentClass = "Block";

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BinaryRegex.lookBehind(BinaryRegex.build(push(methodInfo, 138), BinaryRegex.any(0, 20)), true),
                        BinaryRegex.lookBehind(BinaryRegex.build(push(methodInfo, 154), BinaryRegex.any(0, 20)), true),
                        BinaryRegex.lookBehind(BinaryRegex.build(push(methodInfo, 155), BinaryRegex.any(0, 20)), true),
                        IRETURN,
                        BinaryRegex.end()
                    );
                }
            });
        }
    }

    private class ItemBlockMod extends ClassMod {
        ItemBlockMod() {
            parentClass = "Item";

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            ILOAD_1,
                            push(methodInfo, 256),
                            IADD,
                            BytecodeMatcher.anyReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            });

            memberMappers.add(new FieldMapper("blockID", "I").accessFlag(AccessFlag.PRIVATE, true));

            patches.add(new AddMethodPatch("getColorFromDamage", "(I)I") {
                @Override
                public byte[] generateMethod(ClassFile classFile, MethodInfo methodInfo) throws BadBytecode, IOException {
                    return buildCode(
                        ALOAD_0,
                        ILOAD_1,
                        reference(methodInfo, INVOKESPECIAL, new MethodRef("Item", "getColorFromDamage", "(I)I")),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef(getDeobfClass(), "blockID", "I")),
                        ILOAD_1,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "getItemColorFromDamage", "(III)I")),
                        IRETURN
                    );
                }
            });
        }
    }
    
    private class ItemRendererMod extends ClassMod {
        ItemRendererMod() {
            classSignatures.add(new ConstSignature("/terrain.png"));
            classSignatures.add(new ConstSignature("/gui/items.png"));
            classSignatures.add(new ConstSignature("%blur%/misc/glint.png"));
            classSignatures.add(new ConstSignature("/misc/mapbg.png"));
            
            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override water block color in third person";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BinaryRegex.lookBehind(BinaryRegex.build(
                            // if (itemstack.itemID > 256) {
                            ALOAD_2,
                            BytecodeMatcher.captureReference(GETFIELD),
                            push(methodInfo, 256),
                            IF_ICMPGE, BinaryRegex.any(2),

                            // ...
                            BinaryRegex.any(0, 300)
                        ), true),

                        // GL11.glTranslatef(-0.9375f, -0.0625f, 0.0f);
                        push(methodInfo, -0.9375f),
                        push(methodInfo, -0.0625f),
                        FCONST_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef("org/lwjgl/opengl/GL11", "glTranslatef", "(FFF)V"))
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_2,
                        getCaptureGroup(1),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeWaterBlockGL", "(I)V"))
                    );
                }
            });
        }
    }

    private class PotionMod extends ClassMod {
        PotionMod() {
            classSignatures.add(new ConstSignature("potion.moveSpeed"));
            classSignatures.add(new ConstSignature("potion.moveSlowdown"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.getDescriptor().startsWith("(Ljava/lang/String;)")) {
                        return buildExpression(
                            BinaryRegex.begin(),
                            ALOAD_0,
                            ALOAD_1,
                            BytecodeMatcher.captureReference(PUTFIELD),
                            ALOAD_0,
                            ARETURN,
                            BinaryRegex.end()
                        );
                    } else {
                        return null;
                    }
                }
            }
                .setMethodName("setPotionName")
                .addXref(1, new FieldRef(getDeobfClass(), "name", "Ljava/lang/String;"))
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            ILOAD_1,
                            BytecodeMatcher.captureReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            }.addXref(1, new FieldRef(getDeobfClass(), "id", "I")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            ILOAD_3,
                            BytecodeMatcher.captureReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            }.addXref(1, new FieldRef(getDeobfClass(), "color", "I")));

            patches.add(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "name", "Ljava/lang/String;")));
            patches.add(new AddFieldPatch("origColor", "I"));

            patches.add(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "color", "I")) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return super.getNewFlags(oldFlags) & ~AccessFlag.FINAL;
                }
            });

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "map potions by name";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD_0,
                        ARETURN,
                        BinaryRegex.end()
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "setupPotion", "(LPotion;)V"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "setPotionName", "(Ljava/lang/String;)LPotion;")));
        }
    }

    private class PotionHelperMod extends ClassMod {
        private static final int MAGIC = 0x385dc6;

        PotionHelperMod() {
            classSignatures.add(new ConstSignature("potion.prefix.mundane"));
            classSignatures.add(new ConstSignature(MAGIC));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override water bottle color";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // int i = 0x385dc6;
                        BinaryRegex.begin(),
                        push(methodInfo, MAGIC),
                        ISTORE_1
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // int i = Colorizer.getWaterBottleColor();
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "getWaterBottleColor", "()I")),
                        ISTORE_1
                    );
                }
            });
        }
    }

    private class ColorizerFoliageMod extends ClassMod {
        ColorizerFoliageMod() {
            setupColor(2, 0x619961, "Pine");
            setupColor(3, 0x80a755, "Birch");
            setupColor(4, 0x48b518, "Basic");
        }

        private void setupColor(final int index, final int color, final String name) {
            classSignatures.add(new ConstSignature(color));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override default " + name.toLowerCase() + " foliage color";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BinaryRegex.begin(),
                        push(methodInfo, color),
                        IRETURN,
                        BinaryRegex.end()
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        push(methodInfo, color),
                        push(methodInfo, index),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBiome", "(II)I")),
                        IRETURN
                    );
                }
            });
        }
    }

    private class BlockLeavesMod extends ClassMod {
        BlockLeavesMod() {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        BytecodeMatcher.captureReference(INVOKEINTERFACE),
                        ISTORE, 5,

                        ILOAD, 5,
                        ICONST_1,
                        IAND,
                        ICONST_1,
                        IF_ICMPNE, BinaryRegex.any(2),
                        BytecodeMatcher.captureReference(INVOKESTATIC),
                        IRETURN,

                        ILOAD, 5,
                        ICONST_2,
                        IAND,
                        ICONST_2,
                        IF_ICMPNE, BinaryRegex.any(2),

                        BytecodeMatcher.captureReference(INVOKESTATIC),
                        IRETURN,

                        ALOAD_1,
                        BytecodeMatcher.captureReference(INVOKEINTERFACE),
                        ILOAD_2,
                        ILOAD, 4,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        IRETURN,
                        BinaryRegex.end()
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "colorMultiplier", "(LIBlockAccess;III)I"))
                .addXref(1, new InterfaceMethodRef("IBlockAccess", "getBlockMetadata", "(III)I"))
                .addXref(2, new MethodRef("ColorizerFoliage", "getFoliageColorPine", "()I"))
                .addXref(3, new MethodRef("ColorizerFoliage", "getFoliageColorBirch", "()I"))
                .addXref(4, new InterfaceMethodRef("IBlockAccess", "getWorldChunkManager", "()LWorldChunkManager;"))
                .addXref(5, new MethodRef("WorldChunkManager", "getBiomeGenAt", "(II)LBiomeGenBase;"))
                .addXref(6, new MethodRef("BiomeGenBase", "getFoliageColor", "(LIBlockAccess;III)I"))
            );

            addFoliagePatch(2, "Pine");
            addFoliagePatch(3, "Birch");
        }

        private void addFoliagePatch(final int index, final String name) {
            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override biome " + name.toLowerCase() + " foliage color";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKESTATIC, new MethodRef("ColorizerFoliage", "getFoliageColor" + name, "()I"))
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        push(methodInfo, index),
                        ALOAD_1,
                        reference(methodInfo, INVOKEINTERFACE, new InterfaceMethodRef("IBlockAccess", "getWorldChunkManager", "()LWorldChunkManager;")),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBiome", "(IILWorldChunkManager;III)I"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "colorMultiplier", "(LIBlockAccess;III)I")));
        }
    }

    private class WorldMod extends BaseMod.WorldMod {
        WorldMod() {
            memberMappers.add(new MethodMapper("getWorldChunkManager", "()LWorldChunkManager;"));
        }
    }

    private class WorldChunkManagerMod extends ClassMod {
        WorldChunkManagerMod() {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ILOAD, 4,
                        ILOAD, 5,
                        IMUL,
                        NEWARRAY, T_FLOAT,
                        ASTORE_1
                    );
                }
            });

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BinaryRegex.begin(),
                        ILOAD_1,
                        ILOAD_3,
                        ISUB,
                        ICONST_2,
                        ISHR,
                        ISTORE, 6
                    );
                }
            });

            memberMappers.add(new MethodMapper("getBiomeGenAt", "(II)LBiomeGenBase;"));
        }
    }

    private class EntityMod extends ClassMod {
        EntityMod() {
            classSignatures.add(new ConstSignature("tilecrack_"));
            classSignatures.add(new ConstSignature("random.splash"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BinaryRegex.begin(),

                        // prevPosX = posX = d;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD_1,
                        DUP2_X1,
                        BytecodeMatcher.captureReference(PUTFIELD),
                        BytecodeMatcher.captureReference(PUTFIELD),

                        // prevPosY = posY = d1;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD_3,
                        DUP2_X1,
                        BytecodeMatcher.captureReference(PUTFIELD),
                        BytecodeMatcher.captureReference(PUTFIELD),

                        // prevPosZ = posZ = d2;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD, 5,
                        DUP2_X1,
                        BytecodeMatcher.captureReference(PUTFIELD),
                        BytecodeMatcher.captureReference(PUTFIELD)
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "setPositionAndRotation", "(DDDFF)V"))
                .addXref(1, new FieldRef(getDeobfClass(), "posX", "D"))
                .addXref(2, new FieldRef(getDeobfClass(), "prevPosX", "D"))
                .addXref(3, new FieldRef(getDeobfClass(), "posY", "D"))
                .addXref(4, new FieldRef(getDeobfClass(), "prevPosY", "D"))
                .addXref(5, new FieldRef(getDeobfClass(), "posZ", "D"))
                .addXref(6, new FieldRef(getDeobfClass(), "prevPosZ", "D"))
            );

            memberMappers.add(new FieldMapper("worldObj", "LWorld;"));
        }
    }

    private class EntityFXMod extends ClassMod {
        EntityFXMod() {
            parentClass = "Entity";

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            // setSize(0.2f, 0.2f);
                            ALOAD_0,
                            push(methodInfo, 0.2f),
                            push(methodInfo, 0.2f),
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL)
                        );
                    } else {
                        return null;
                    }
                }
            });

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            // particleRed = particleGreen = particleBlue = 1.0f;
                            ALOAD_0,
                            ALOAD_0,
                            ALOAD_0,
                            FCONST_1,
                            DUP_X1,
                            BytecodeMatcher.captureReference(PUTFIELD),
                            DUP_X1,
                            BytecodeMatcher.captureReference(PUTFIELD),
                            BytecodeMatcher.captureReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            }
                .addXref(1, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                .addXref(2, new FieldRef(getDeobfClass(), "particleGreen", "F"))
                .addXref(3, new FieldRef(getDeobfClass(), "particleRed", "F"))
            );
        }
    }

    abstract private class WaterFXMod extends ClassMod {
        void addWaterColorPatch(final String name, final float[] particleColors) {
            addWaterColorPatch(name, particleColors, particleColors);
        }

        void addWaterColorPatch(final String name, final float[] origColors, final float[] newColors) {
            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override " + name + " drop color";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (!methodInfo.isConstructor()) {
                        return null;
                    } else if (origColors == null) {
                        return buildExpression(
                            RETURN
                        );
                    } else {
                        return buildExpression(
                            // particleRed = r;
                            ALOAD_0,
                            push(methodInfo, origColors[0]),
                            reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                            // particleGreen = g;
                            ALOAD_0,
                            push(methodInfo, origColors[1]),
                            reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                            // particleBlue = b;
                            ALOAD_0,
                            push(methodInfo, origColors[2]),
                            reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                        );
                    }
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // if (Colorizer.computeWaterColor(worldObj.getWorldChunkManager(), i, j, k)) {
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef(getDeobfClass(), "worldObj", "LWorld;")),
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("World", "getWorldChunkManager", "()LWorldChunkManager;")),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef(getDeobfClass(), "posX", "D")),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef(getDeobfClass(), "posY", "D")),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef(getDeobfClass(), "posZ", "D")),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeWaterColor", "(LWorldChunkManager;DDD)Z")),
                        IFEQ, branch("A"),

                        // particleRed = Colorizer.waterColor[0];
                        ALOAD_0,
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "waterColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                        // particleGreen = Colorizer.waterColor[1];
                        ALOAD_0,
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "waterColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                        // particleBlue = Colorizer.waterColor[2];
                        ALOAD_0,
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "waterColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F")),
                        GOTO, branch("B"),

                        // } else {
                        label("A"),

                        newColors == null ? new byte[]{} : buildCode(
                            // particleRed = r;
                            ALOAD_0,
                            push(methodInfo, newColors[0]),
                            reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                            // particleGreen = g;
                            ALOAD_0,
                            push(methodInfo, newColors[1]),
                            reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                            // particleBlue = b;
                            ALOAD_0,
                            push(methodInfo, newColors[2]),
                            reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                        ),

                        // }
                        label("B"),
                        (origColors == null ? new byte[]{(byte) RETURN} : new byte[]{})
                    );
                }
            });
        }
    }

    private class EntityRainFXMod extends WaterFXMod {
        EntityRainFXMod() {
            parentClass = "EntityFX";

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            // 0.2f * (float) Math.random() + 0.1f
                            reference(methodInfo, INVOKESTATIC, new MethodRef("java/lang/Math", "random", "()D")),
                            D2F,
                            push(methodInfo, 0.2f),
                            FMUL,
                            push(methodInfo, 0.1f),
                            FADD,
                            F2D
                        );
                    } else {
                        return null;
                    }
                }
            });

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            // 19 + rand.nextInt(4)
                            push(methodInfo, 19),
                            ALOAD_0,
                            BytecodeMatcher.anyReference(GETFIELD),
                            ICONST_4,
                            reference(methodInfo, INVOKEVIRTUAL, new MethodRef("java/util/Random", "nextInt", "(I)I")),
                            IADD
                        );
                    } else {
                        return null;
                    }
                }
            });

            addWaterColorPatch("rain", new float[]{1.0f, 1.0f, 1.0f}, new float[]{0.2f, 0.3f, 1.0f});
        }
    }

    private class EntityDropParticleFXMod extends WaterFXMod {
        EntityDropParticleFXMod() {
            parentClass = "EntityFX";

            MethodRef onUpdate = new MethodRef(getDeobfClass(), "onUpdate", "()V");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // particleRed = 0.2f;
                        ALOAD_0,
                        push(methodInfo, 0.2f),
                        BytecodeMatcher.anyReference(PUTFIELD),

                        // particleGreen = 0.3f;
                        ALOAD_0,
                        push(methodInfo, 0.3f),
                        BytecodeMatcher.anyReference(PUTFIELD),

                        // particleBlue = 1.0f;
                        ALOAD_0,
                        push(methodInfo, 1.0f),
                        BytecodeMatcher.anyReference(PUTFIELD),

                        // ...
                        BinaryRegex.any(0, 30),

                        // 40 - age
                        push(methodInfo, 40),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ISUB
                    );
                }
            }
                .setMethod(onUpdate)
                .addXref(1, new FieldRef(getDeobfClass(), "timer", "I"))
            );

            addWaterColorPatch("water", new float[]{0.0f, 0.0f, 1.0f}, new float[]{0.2f, 0.3f, 1.0f});

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "remove water drop color update";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // particleRed = 0.2f;
                        ALOAD_0,
                        push(methodInfo, 0.2f),
                        reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                        // particleGreen = 0.3f;
                        ALOAD_0,
                        push(methodInfo, 0.3f),
                        reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                        // particleBlue = 1.0f;
                        ALOAD_0,
                        push(methodInfo, 1.0f),
                        reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode();
                }
            }.targetMethod(onUpdate));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override lava drop color";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // particleRed = 1.0f;
                        ALOAD_0,
                        push(methodInfo, 1.0f),
                        reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                        // particleGreen = 16.0f / (float)((40 - timer) + 16);
                        ALOAD_0,
                        push(methodInfo, 16.0f),
                        BinaryRegex.any(0, 20),
                        reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                        // particleBlue = 4.0f / (float)((40 - timer) + 8);
                        ALOAD_0,
                        push(methodInfo, 4.0f),
                        BinaryRegex.any(0, 20),
                        reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // if (Colorizer.computeLavaDropColor(40 - timer)) {
                        push(methodInfo, 40),
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef(getDeobfClass(), "timer", "I")),
                        ISUB,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeLavaDropColor", "(I)Z")),
                        IFEQ, branch("A"),

                        // particleRed = Colorizer.lavaDropRed;
                        ALOAD_0,
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "lavaDropRed", "F")),
                        reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleRed", "F")),

                        // particleGreen = Colorizer.lavaDropGreen;
                        ALOAD_0,
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "lavaDropGreen", "F")),
                        reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleGreen", "F")),

                        // particleBlue = Colorizer.lavaDropBlue;
                        ALOAD_0,
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "lavaDropBlue", "F")),
                        reference(methodInfo, PUTFIELD, new FieldRef(getDeobfClass(), "particleBlue", "F")),

                        // } else {
                        GOTO, branch("B"),

                        // ... original code ...
                        label("A"),
                        getMatch(),

                        // }
                        label("B")
                    );
                }
            }.targetMethod(onUpdate));
        }
    }
    
    private class EntitySplashFXMod extends WaterFXMod {
        EntitySplashFXMod() {
            parentClass = "EntityRainFX";

            classSignatures.add(new ConstSignature(0.04f));
            classSignatures.add(new ConstSignature(0.10000000000000001));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            ALOAD_0,
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL),
                            ICONST_1,
                            IADD,
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL)
                        );
                    } else {
                        return null;
                    }
                }
            });

            addWaterColorPatch("splash", null);
        }
    }

    private class EntityBubbleFXMod extends WaterFXMod {
        EntityBubbleFXMod() {
            parentClass = "EntityFX";

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            // setParticleTextureIndex(32);
                            ALOAD_0,
                            push(methodInfo, 32),
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL),

                            // setSize(0.02F, 0.02F);
                            ALOAD_0,
                            push(methodInfo, 0.02f),
                            push(methodInfo, 0.02f),
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL)
                        );
                    } else {
                        return null;
                    }
                }
            });

            addWaterColorPatch("bubble", new float[]{1.0f, 1.0f, 1.0f});
        }
    }

    private class BlockLilyPadMod extends ClassMod {
        private static final int MAGIC = 0x208030;

        BlockLilyPadMod() {
            classSignatures.add(new ConstSignature(MAGIC));
            classSignatures.add(new ConstSignature(0.5f));
            classSignatures.add(new ConstSignature(0.015625f));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override lily pad color";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, MAGIC)
                    );
                }

                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "getLilyPadColor", "()I"))
                    );
                }
            });
        }
    }

    private class EntityRendererMod extends ClassMod {
        EntityRendererMod() {
            classSignatures.add(new ConstSignature("ambient.weather.rain"));
            classSignatures.add(new ConstSignature("/terrain.png"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // sun = world.func_35464_b(1.0F) * 0.95F + 0.05F;
                        ALOAD_1,
                        FCONST_1,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        push(methodInfo, 0.95f),
                        FMUL,
                        push(methodInfo, 0.05f),
                        FADD,
                        FSTORE_3,

                        // lightsun = world.worldProvider.lightBrightnessTable[i / 16] * sun;
                        ALOAD_1,
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.captureReference(GETFIELD),
                        ILOAD_2,
                        BIPUSH, 16,
                        IDIV,
                        FALOAD,
                        FLOAD_3,
                        FMUL,
                        FSTORE, 4,

                        // lighttorch = world.worldProvider.lightBrightnessTable[i % 16] * (torchFlickerX * 0.1F + 1.5F);
                        BinaryRegex.any(0, 20),
                        ILOAD_2,
                        BIPUSH, 16,
                        IREM,
                        FALOAD,
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),

                        // ...
                        BinaryRegex.any(0, 200),

                        // if (world.lightningFlash > 0)
                        ALOAD_1,
                        BytecodeMatcher.captureReference(GETFIELD),
                        IFLE, BinaryRegex.any(2),

                        // ...
                        BinaryRegex.any(0, 200),

                        // if (world.worldProvider.worldType == 1) {
                        ALOAD_1,
                        BinaryRegex.backReference(2),
                        BytecodeMatcher.captureReference(GETFIELD),
                        ICONST_1,
                        IF_ICMPNE, BinaryRegex.any(2),

                        // ...
                        BinaryRegex.any(0, 50),

                        // gamma = mc.gameSettings.gammaSetting;
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.captureReference(GETFIELD),
                        FSTORE, 15,

                        // ...
                        BinaryRegex.any(0, 300),

                        // mc.renderEngine.createTextureFromBytes(lightmapColors, 16, 16, lightmapTexture);
                        ALOAD_0,
                        BinaryRegex.backReference(7),
                        BytecodeMatcher.captureReference(GETFIELD),
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        BIPUSH, 16,
                        BIPUSH, 16,
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        RETURN
                    );
                }
            }
                .setMethodName("updateLightmap")
                .addXref(1, new MethodRef("World", "getSunAngle", "(F)F"))
                .addXref(2, new FieldRef("World", "worldProvider", "LWorldProvider;"))
                .addXref(3, new FieldRef("WorldProvider", "lightBrightnessTable", "[F"))
                .addXref(4, new FieldRef(getDeobfClass(), "torchFlickerX", "F"))
                .addXref(5, new FieldRef("World", "lightningFlash", "I"))
                .addXref(6, new FieldRef("WorldProvider", "worldType", "I"))
                .addXref(7, new FieldRef(getDeobfClass(), "mc", "LMinecraft;"))
                .addXref(8, new FieldRef("Minecraft", "gameSettings", "LGameSettings;"))
                .addXref(9, new FieldRef("GameSettings", "gammaSetting", "F"))
                .addXref(10, new FieldRef("Minecraft", "renderEngine", "LRenderEngine;"))
                .addXref(11, new FieldRef(getDeobfClass(), "lightmapColors", "[I"))
                .addXref(12, new FieldRef(getDeobfClass(), "lightmapTexture", "I"))
                .addXref(13, new MethodRef("RenderEngine", "createTextureFromBytes", "([IIII)V"))
            );

            patches.add(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "torchFlickerX", "F")));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override lightmap";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ASTORE_1
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ASTORE_1,

                        // if (Colorizer.computeLightmap(this, world)) {
                        ALOAD_0,
                        ALOAD_1,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeLightmap", "(LEntityRenderer;LWorld;)Z")),
                        IFEQ, branch("A"),

                        // return;
                        RETURN,

                        // }
                        label("A")
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "updateLightmap", "()V")));
        }
    }

    private abstract class RedstoneWireClassMod extends ClassMod {
        RedstoneWireClassMod(final String description, final MethodRef method) {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // f = (float) l / 15.0f;
                        BytecodeMatcher.anyILOAD,
                        I2F,
                        push(methodInfo, 15.0f),
                        FDIV,
                        BytecodeMatcher.anyFSTORE,

                        // f1 = f * 0.6f + 0.4f;
                        BytecodeMatcher.anyFLOAD,
                        push(methodInfo, 0.6f),
                        FMUL,
                        push(methodInfo, 0.4f),
                        FADD,
                        BytecodeMatcher.anyFSTORE
                    );
                }
            }.setMethod(method));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return description;
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ILOAD, BinaryRegex.capture(BinaryRegex.any()),
                        I2F,
                        push(methodInfo, 15.0f),
                        FDIV,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                        FLOAD, BinaryRegex.backReference(2),
                        push(methodInfo, 0.6f),
                        FMUL,
                        push(methodInfo, 0.4f),
                        FADD,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                        BinaryRegex.any(0, 10),

                        FLOAD, BinaryRegex.backReference(2),
                        FLOAD, BinaryRegex.backReference(2),
                        FMUL,
                        push(methodInfo, 0.7f),
                        FMUL,
                        push(methodInfo, 0.5f),
                        FSUB,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                        FLOAD, BinaryRegex.backReference(2),
                        FLOAD, BinaryRegex.backReference(2),
                        FMUL,
                        push(methodInfo, 0.6f),
                        FMUL,
                        push(methodInfo, 0.7f),
                        FSUB,
                        FSTORE, BinaryRegex.capture(BinaryRegex.any())
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ILOAD, getCaptureGroup(1),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeRedstoneWireColor", "(I)Z")),
                        IFEQ, branch("A"),

                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "redstoneWireRed", "F")),
                        FSTORE, getCaptureGroup(3),
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "redstoneWireGreen", "F")),
                        FSTORE, getCaptureGroup(4),
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "redstoneWireBlue", "F")),
                        FSTORE, getCaptureGroup(5),
                        GOTO, branch("B"),

                        label("A"),
                        getMatch(),
                        label("B")
                    );
                }
            }.targetMethod(method));
        }
    }

    private class BlockRedstoneWireMod extends RedstoneWireClassMod {
        BlockRedstoneWireMod() {
            super("override redstone wire particle color", new MethodRef("BlockRedstoneWire", "randomDisplayTick", "(LWorld;IIILjava/util/Random;)V"));

            classSignatures.add(new ConstSignature("reddust"));
        }
    }

    private class RenderBlocksMod extends RedstoneWireClassMod {
        RenderBlocksMod() {
            super("override redstone wire color", new MethodRef("RenderBlocks", "renderBlockRedstoneWire", "(LBlock;III)Z"));

            classSignatures.add(new ConstSignature(0.1875));
            classSignatures.add(new ConstSignature(0.01));

            MethodRef renderBlockFallingSand = new MethodRef(getDeobfClass(), "renderBlockFallingSand", "(LBlock;LWorld;III)V");
            final MethodRef setColorOpaque_F = new MethodRef("Tessellator", "setColorOpaque_F", "(FFF)V");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD_1,
                        ALOAD_0,
                        BytecodeMatcher.captureReference(GETFIELD),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        ISTORE, 6
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "renderBlockFluids", "(LBlock;III)Z"))
                .addXref(1, new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;"))
                .addXref(2, new MethodRef("Block", "colorMultiplier", "(LIBlockAccess;III)I"))
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD, 5,
                        FLOAD, 6,
                        FLOAD, 8,
                        FMUL,
                        FLOAD, 6,
                        FLOAD, 9,
                        FMUL,
                        FLOAD, 6,
                        FLOAD, 10,
                        FMUL,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL)
                    );
                }
            }.addXref(1, setColorOpaque_F));


            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BinaryRegex.begin(),
                        push(methodInfo, 0.5f),
                        FSTORE, 6,
                        FCONST_1,
                        FSTORE, 7,
                        push(methodInfo, 0.8f),
                        FSTORE, 8,
                        push(methodInfo, 0.6f),
                        FSTORE, 9
                    );
                }
            }.setMethod(renderBlockFallingSand));

            memberMappers.add(new MethodMapper("renderBlockCauldron", "(LBlockCauldron;III)Z"));

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "colorize cauldron water color";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        push(methodInfo, 205),
                        ISTORE, BinaryRegex.any()
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Colorizer.computerWaterColor();
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeWaterColor", "()V")),

                        // tessellator.setColorOpaque(Colorizer.waterColor[0], Colorizer.waterColor[1], Colorizer.waterColor[2]);
                        ALOAD, 5,
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "waterColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "waterColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "waterColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        reference(methodInfo, INVOKEVIRTUAL, setColorOpaque_F)
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "renderBlockCauldron", "(LBlockCauldron;III)Z")));
            
            patches.add(new BytecodePatch() {
                private boolean done;
                
                @Override
                public String getDescription() {
                    return "colorize falling sand and gravel";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // tessellator.setColorOpaque_F($1 * f5, $1 * f5, $1 * f5);
                        ALOAD, 10,
                        BinaryRegex.capture(BytecodeMatcher.anyFLOAD),
                        FLOAD, 12,
                        FMUL,
                        BinaryRegex.backReference(1),
                        FLOAD, 12,
                        FMUL,
                        BinaryRegex.backReference(1),
                        FLOAD, 12,
                        FMUL,
                        reference(methodInfo, INVOKEVIRTUAL, setColorOpaque_F)
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    byte[] extraCode;
                    if (done) {
                        extraCode = new byte[0];
                    } else {
                        done = true;
                        extraCode = buildCode(
                            // setColorF(Colorizer.colorizeBlock(block, world.getWorldChunkManager(), i, j, k));
                            ALOAD_1,
                            ALOAD_2,
                            reference(methodInfo, INVOKEVIRTUAL, new MethodRef("World", "getWorldChunkManager", "()LWorldChunkManager;")),
                            ILOAD_3,
                            ILOAD, 4,
                            ILOAD, 5,
                            reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeBlock", "(LBlock;LWorldChunkManager;III)I")),
                            reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "setColorF", "(I)V"))
                        );
                    }
                    return buildCode(
                        extraCode,

                        // tessellator.setColorOpaque_F(Colorizer.setColor[0] * f5, Colorizer.setColor[1] * f5, Colorizer.setColor[2] * f5);
                        ALOAD, 10,
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_0,
                        FALOAD,
                        FLOAD, 12,
                        FMUL,
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_1,
                        FALOAD,
                        FLOAD, 12,
                        FMUL,
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F")),
                        ICONST_2,
                        FALOAD,
                        FLOAD, 12,
                        FMUL,
                        reference(methodInfo, INVOKEVIRTUAL, setColorOpaque_F)
                    );
                }
            }.targetMethod(renderBlockFallingSand));
            
            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "colorize bottom of water block";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // tessellator.setColorOpaque_F(f3 * f7, f3 * f7, f3 * f7);
                        ALOAD, 5,
                        FLOAD, BinaryRegex.capture(BinaryRegex.any()),
                        FLOAD, BinaryRegex.capture(BinaryRegex.any()),
                        FMUL,
                        FLOAD, BinaryRegex.backReference(1),
                        FLOAD, BinaryRegex.backReference(2),
                        FMUL,
                        FLOAD, BinaryRegex.backReference(1),
                        FLOAD, BinaryRegex.backReference(2),
                        FMUL,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("Tessellator", "setColorOpaque_F", "(FFF)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // tessellator.setColorOpaque_F(f3 * f7 * f, f3 * f7 * f1, f3 * f7 * f2);
                        ALOAD, 5,
                        FLOAD, getCaptureGroup(1),
                        FLOAD, getCaptureGroup(2),
                        FMUL,
                        FLOAD, 7,
                        FMUL,
                        FLOAD, getCaptureGroup(1),
                        FLOAD, getCaptureGroup(2),
                        FMUL,
                        FLOAD, 8,
                        FMUL,
                        FLOAD, getCaptureGroup(1),
                        FLOAD, getCaptureGroup(2),
                        FMUL,
                        FLOAD, 8,
                        FMUL,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("Tessellator", "setColorOpaque_F", "(FFF)V"))
                   );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "renderBlockFluids", "(LBlock;III)Z")));
        }
    }

    private class EntityReddustFXMod extends ClassMod {
        EntityReddustFXMod() {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            ALOAD_0,
                            reference(methodInfo, INVOKESTATIC, new MethodRef("java/lang/Math", "random", "()D")),
                            push(methodInfo, 0.20000000298023224),
                            DMUL,
                            D2F,
                            push(methodInfo, 0.8f),
                            FADD,
                            BytecodeMatcher.anyFLOAD,
                            FMUL,
                            BytecodeMatcher.anyFLOAD,
                            FMUL,
                            BytecodeMatcher.anyReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override redstone particle color";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            FCONST_1,
                            FSTORE, 9,
                            reference(methodInfo, INVOKESTATIC, new MethodRef("java/lang/Math", "random", "()D"))
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        FCONST_1,
                        FSTORE, 9,

                        BIPUSH, 15,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "computeRedstoneWireColor", "(I)Z")),
                        IFEQ, branch("A"),

                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "redstoneWireRed", "F")),
                        FSTORE, 9,
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "redstoneWireGreen", "F")),
                        FSTORE, 10,
                        reference(methodInfo, GETSTATIC, new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "redstoneWireBlue", "F")),
                        FSTORE, 11,

                        label("A"),
                        reference(methodInfo, INVOKESTATIC, new MethodRef("java/lang/Math", "random", "()D"))
                    );
                }
            });
        }
    }

    public class BlockStemMod extends ClassMod {
        BlockStemMod() {
            MethodRef getRenderColor = new MethodRef(getDeobfClass(), "getRenderColor", "(I)I");

            classSignatures.add(new FixedBytecodeSignature(
                // j = i * 32;
                BinaryRegex.begin(),
                ILOAD_1,
                BIPUSH, 32,
                IMUL,
                ISTORE_2,

                // k = 255 - i * 8;
                SIPUSH, 0, 255,
                ILOAD_1,
                BIPUSH, 8,
                IMUL,
                ISUB,
                ISTORE_3,

                // l = i * 4;
                ILOAD_1,
                ICONST_4,
                IMUL,
                ISTORE, 4,

                // return j << 16 | k << 8 | l;
                ILOAD_2,
                BIPUSH, 16,
                ISHL,
                ILOAD_3,
                BIPUSH, 8,
                ISHL,
                IOR,
                ILOAD, 4,
                IOR,
                IRETURN,
                BinaryRegex.end()
            ).setMethod(getRenderColor));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "override pumpkin and melon stem color";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        IRETURN,
                        BinaryRegex.end()
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ILOAD_1,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeStem", "(II)I"))
                    );
                }
            }.targetMethod(getRenderColor));
        }
    }

    private class EntityListMod extends ClassMod {
        EntityListMod() {
            classSignatures.add(new ConstSignature("Skipping Entity with id "));

            memberMappers.add(new MethodMapper("addMapping", "(Ljava/lang/Class;Ljava/lang/String;IZ)V")
                .accessFlag(AccessFlag.STATIC, true)
            );

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "set up mapping for spawnable entities";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        BytecodeMatcher.anyReference(GETSTATIC),
                        ILOAD_2,
                        reference(methodInfo, INVOKESTATIC, new MethodRef("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;")),
                        reference(methodInfo, INVOKEINTERFACE, new InterfaceMethodRef("java/util/List", "add", "(Ljava/lang/Object;)Z")),
                        POP
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ILOAD_2,
                        ALOAD_1,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "setupSpawnerEgg", "(ILjava/lang/String;)V"))
                    );
                }
            }.targetMethod(new MethodRef(getDeobfClass(), "addMapping", "(Ljava/lang/Class;Ljava/lang/String;IZ)V")));
        }
    }

    private class ItemSpawnerEggMod extends ClassMod {
        ItemSpawnerEggMod() {
            parentClass = "Item";

            classSignatures.add(new ConstSignature(".name"));
            classSignatures.add(new ConstSignature("entity."));

            MethodRef getItemNameIS = new MethodRef(getDeobfClass(), "getItemNameIS", "(LItemStack;)Ljava/lang/String;");
            MethodRef colorMultiplier = new MethodRef(getDeobfClass(), "colorMultiplier", "(I)I");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // s1 = EntityList.getEntityString(itemStack.getItemDamage());
                        ALOAD_1,
                        BytecodeMatcher.captureReference(INVOKEVIRTUAL),
                        BytecodeMatcher.captureReference(INVOKESTATIC),
                        ASTORE_3,
                        ALOAD_3
                    );
                }
            }
                .setMethod(getItemNameIS)
                .addXref(1, new MethodRef("ItemStack", "getItemDamage", "()I"))
                .addXref(2, new MethodRef("EntityList", "getEntityString", "(I)Ljava/lang/String;"))
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // 64 + (i * 0x24faef & 0xc0)
                        BIPUSH, 64,
                        ILOAD_1,
                        push(methodInfo, 0x24faef),
                        IMUL,
                        push(methodInfo, 0xc0),
                        IAND,
                        IADD
                    );
                }
            }.setMethod(colorMultiplier));

            patches.add(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "override spawner egg color";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        IRETURN
                    );
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ILOAD_1,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "colorizeSpawnerEgg", "(II)I"))
                    );
                }
            }.targetMethod(colorMultiplier));
        }
    }
}
