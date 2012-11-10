package com.pclewis.mcpatcher;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.pclewis.mcpatcher.BinaryRegex.*;
import static com.pclewis.mcpatcher.BytecodeMatcher.anyReference;
import static com.pclewis.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Internal mod required by the patcher.  Responsible for injecting MCPatcherUtils classes
 * into minecraft.jar.
 * <p/>
 * Also provides a collection of commonly used ClassMods as public static inner classes that
 * can be instantiated or extended as needed.
 */
public final class BaseMod extends Mod {
    public static final String NAME = "__Base";

    BaseMod(MinecraftVersion minecraftVersion) {
        name = NAME;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.0";
        configPanel = new ConfigPanel();
        dependencies.clear();

        classMods.add(new XMinecraftMod());

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.UTILS_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CONFIG_CLASS));
    }

    class ConfigPanel extends ModConfigPanel {
        private JPanel panel;
        private JTextField heapSizeText;
        private JCheckBox debugCheckBox;
        private JCheckBox autoRefreshTexturesCheckBox;

        ConfigPanel() {
            debugCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(com.pclewis.mcpatcher.Config.TAG_DEBUG, debugCheckBox.isSelected());
                }
            });

            autoRefreshTexturesCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set("autoRefreshTextures", autoRefreshTexturesCheckBox.isSelected());
                }
            });
        }

        @Override
        public JPanel getPanel() {
            return panel;
        }

        @Override
        public String getPanelName() {
            return "General options";
        }

        @Override
        public void load() {
            heapSizeText.setText("" + MCPatcherUtils.getInt(com.pclewis.mcpatcher.Config.TAG_JAVA_HEAP_SIZE, 1024));
            debugCheckBox.setSelected(MCPatcherUtils.getBoolean(com.pclewis.mcpatcher.Config.TAG_DEBUG, false));
            autoRefreshTexturesCheckBox.setSelected(MCPatcherUtils.getBoolean("autoRefreshTextures", false));
        }

        @Override
        public void save() {
            try {
                MCPatcherUtils.set(com.pclewis.mcpatcher.Config.TAG_JAVA_HEAP_SIZE, Integer.parseInt(heapSizeText.getText()));
            } catch (Exception e) {
            }
        }
    }

    private class XMinecraftMod extends MinecraftMod {
        XMinecraftMod() {
            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "MCPatcherUtils.setMinecraft(this)";
                }

                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getName().equals("<init>")) {
                        return buildExpression(
                            begin(),
                            ALOAD_0,
                            reference(INVOKESPECIAL, new MethodRef("java.lang.Object", "<init>", "()V"))
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.UTILS_CLASS, "setMinecraft", "(LMinecraft;)V")),
                        push(MCPatcher.minecraft.getVersion().getVersionString()),
                        push(MCPatcher.VERSION_STRING),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.UTILS_CLASS, "setVersions", "(Ljava/lang/String;Ljava/lang/String;)V"))
                    );
                }
            });
        }

        @Override
        public String getDeobfClass() {
            return "Minecraft";
        }
    }

    /**
     * Matches Minecraft class and maps the texturePackList field.
     * <p/>
     * Including
     * <pre>
     *     classMods.add(new BaseMod.MinecraftMod().mapTexturePackList();
     *     classMods.add(new BaseMod.TexturePackListMod());
     *     classMods.add(new BaseMod.TexturePackBaseMod());
     * </pre>
     * will allow you to detect when a different texture pack has been selected:
     * <pre>
     *     private TexturePackBase lastTexturePack;
     *     ...
     *     {
     *         TexturePackBase currentTexturePack = MCPatcherUtils.getMinecraft().texturePackList.selectedTexturePack;
     *         if (currentTexturePack == lastTexturePack) {
     *             // texture pack has not changed
     *         } else {
     *             // texture pack has changed
     *             lastTexturePack = currentTexturePack;
     *         }
     *     }
     * </pre>
     */
    public static class MinecraftMod extends ClassMod {
        public MinecraftMod() {
            classSignatures.add(new FilenameSignature("net/minecraft/client/Minecraft.class"));
        }

        public MinecraftMod mapTexturePackList() {
            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "texturePackList", "LTexturePackList;")));
            return this;
        }

        public MinecraftMod addWorldGetter(MinecraftVersion minecraftVersion) {
            final MethodRef getWorld = new MethodRef(getDeobfClass(), "getWorld", "()LWorld;");

            if (minecraftVersion.compareTo("12w18a") >= 0) {
                final FieldRef worldServer = new FieldRef(getDeobfClass(), "worldServer", "LWorldServer;");
                final FieldRef world = new FieldRef("WorldServer", "world", "LWorld;");

                memberMappers.add(new FieldMapper(worldServer));

                patches.add(new AddMethodPatch(getWorld) {
                    @Override
                    public byte[] generateMethod() throws BadBytecode, IOException {
                        return buildCode(
                            ALOAD_0,
                            reference(GETFIELD, worldServer),
                            reference(GETFIELD, world),
                            ARETURN
                        );
                    }
                });
            } else {
                final FieldRef theWorld = new FieldRef(getDeobfClass(), "theWorld", "LWorld;");

                memberMappers.add(new FieldMapper(theWorld));

                patches.add(new AddMethodPatch(getWorld) {
                    @Override
                    public byte[] generateMethod() throws BadBytecode, IOException {
                        return buildCode(
                            ALOAD_0,
                            reference(GETFIELD, theWorld),
                            ARETURN
                        );
                    }
                });
            }
            return this;
        }
    }

    /**
     * Matches TexturePackList class and maps selected and default texture pack fields.
     */
    public static class TexturePackListMod extends ClassMod {
        protected final boolean useITexturePack;
        protected final String texturePackType;
        protected final FieldRef selectedTexturePack;
        protected final FieldRef defaultTexturePack;
        protected final MethodRef getDefaultTexturePack = new MethodRef(getDeobfClass(), "getDefaultTexturePack", "()LTexturePackBase;");
        protected final MethodRef getSelectedTexturePack = new MethodRef(getDeobfClass(), "getSelectedTexturePack", "()LTexturePackBase;");

        public TexturePackListMod(MinecraftVersion minecraftVersion) {
            classSignatures.add(new ConstSignature(".zip"));
            classSignatures.add(new ConstSignature("texturepacks"));

            if (minecraftVersion.compareTo("12w15a") >= 0) {
                useITexturePack = true;
                texturePackType = "LITexturePack;";

                selectedTexturePack = new FieldRef(getDeobfClass(), "selectedTexturePack", texturePackType);
                defaultTexturePack = new FieldRef(getDeobfClass(), "defaultTexturePack", texturePackType);

                memberMappers.add(new FieldMapper(selectedTexturePack)
                    .accessFlag(AccessFlag.PRIVATE, true)
                    .accessFlag(AccessFlag.STATIC, false)
                    .accessFlag(AccessFlag.FINAL, false)
                );
                memberMappers.add(new FieldMapper(defaultTexturePack)
                    .accessFlag(AccessFlag.PRIVATE, true)
                    .accessFlag(AccessFlag.STATIC, true)
                    .accessFlag(AccessFlag.FINAL, true)
                );

                patches.add(new AddMethodPatch(getDefaultTexturePack) {
                    @Override
                    public byte[] generateMethod() throws BadBytecode, IOException {
                        return buildCode(
                            reference(GETSTATIC, defaultTexturePack),
                            reference(CHECKCAST, new ClassRef("TexturePackBase")),
                            ARETURN
                        );
                    }
                });

                patches.add(new AddMethodPatch(getSelectedTexturePack) {
                    @Override
                    public byte[] generateMethod() throws BadBytecode, IOException {
                        return buildCode(
                            ALOAD_0,
                            reference(GETFIELD, selectedTexturePack),
                            reference(CHECKCAST, new ClassRef("TexturePackBase")),
                            ARETURN
                        );
                    }
                });
            } else {
                useITexturePack = false;
                texturePackType = "LTexturePackBase;";

                selectedTexturePack = new FieldRef(getDeobfClass(), "selectedTexturePack", texturePackType);
                defaultTexturePack = new FieldRef(getDeobfClass(), "defaultTexturePack", texturePackType);

                memberMappers.add(new FieldMapper(selectedTexturePack).accessFlag(AccessFlag.PUBLIC, true));
                memberMappers.add(new FieldMapper(defaultTexturePack).accessFlag(AccessFlag.PRIVATE, true));

                patches.add(new AddMethodPatch(getDefaultTexturePack) {
                    @Override
                    public byte[] generateMethod() throws BadBytecode, IOException {
                        return buildCode(
                            ALOAD_0,
                            reference(GETFIELD, defaultTexturePack),
                            ARETURN
                        );
                    }
                });

                patches.add(new AddMethodPatch(getSelectedTexturePack) {
                    @Override
                    public byte[] generateMethod() throws BadBytecode, IOException {
                        return buildCode(
                            ALOAD_0,
                            reference(GETFIELD, selectedTexturePack),
                            ARETURN
                        );
                    }
                });
            }
        }
    }

    /**
     * Matches TexturePackBase class and maps getInputStream method.
     */
    public static class TexturePackBaseMod extends ClassMod {
        protected final boolean useITexturePack;

        public TexturePackBaseMod(MinecraftVersion minecraftVersion) {
            final MethodRef getResourceAsStream = new MethodRef("java.lang.Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;");

            classSignatures.add(new ConstSignature(getResourceAsStream));
            if (minecraftVersion.compareTo("12w15a") >= 0) {
                useITexturePack = true;
                classSignatures.add(new ConstSignature("/pack.txt"));
                interfaces = new String[]{"ITexturePack"};
            } else {
                useITexturePack = false;
                classSignatures.add(new ConstSignature("pack.txt").negate(true));
            }

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_1,
                        reference(INVOKEVIRTUAL, getResourceAsStream),
                        ARETURN
                    );
                }
            }.setMethodName("getInputStream"));

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "texturePackFileName", "Ljava/lang/String;")));

            patches.add(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "texturePackFileName", "Ljava/lang/String;")));
        }
    }

    /**
     * Matches TexturePackDefault class.
     */
    public static class TexturePackDefaultMod extends ClassMod {
        public TexturePackDefaultMod() {
            parentClass = "TexturePackBase";

            classSignatures.add(new ConstSignature("The default look of Minecraft"));
        }
    }

    /**
     * Matches GLAllocation class and maps createDirectByteBuffer method.
     */
    public static class GLAllocationMod extends ClassMod {
        public GLAllocationMod() {
            classSignatures.add(new ConstSignature(new MethodRef(MCPatcherUtils.GL11_CLASS, "glDeleteLists", "(II)V")));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().equals("(I)Ljava/nio/ByteBuffer;")) {
                        return buildExpression(
                            reference(INVOKESTATIC, new MethodRef("java.nio.ByteBuffer", "allocateDirect", "(I)Ljava/nio/ByteBuffer;"))
                        );
                    } else {
                        return null;
                    }
                }
            }.setMethodName("createDirectByteBuffer"));
        }
    }

    /**
     * Matches Tessellator class and instance and maps several commonly used rendering methods.
     */
    public static class TessellatorMod extends ClassMod {
        protected final MethodRef draw = new MethodRef(getDeobfClass(), "draw", "()I");
        protected final MethodRef startDrawingQuads = new MethodRef(getDeobfClass(), "startDrawingQuads", "()V");
        protected final MethodRef startDrawing = new MethodRef(getDeobfClass(), "startDrawing", "(I)V");
        protected final MethodRef addVertexWithUV = new MethodRef(getDeobfClass(), "addVertexWithUV", "(DDDDD)V");
        protected final MethodRef addVertex = new MethodRef(getDeobfClass(), "addVertex", "(DDD)V");
        protected final MethodRef setTextureUV = new MethodRef(getDeobfClass(), "setTextureUV", "(DD)V");
        protected final FieldRef instance = new FieldRef(getDeobfClass(), "instance", "LTessellator;");

        public TessellatorMod(MinecraftVersion minecraftVersion) {
            final MethodRef draw1;
            if (minecraftVersion.compareTo("Beta 1.9 Prerelease 4") >= 0) {
                draw1 = draw;
            } else {
                draw1 = new MethodRef(getDeobfClass(), "draw1", "()V");

                patches.add(new AddMethodPatch(draw) {
                    @Override
                    public byte[] generateMethod() throws BadBytecode, IOException {
                        return buildCode(
                            ALOAD_0,
                            reference(INVOKEVIRTUAL, draw1),
                            push(0),
                            IRETURN
                        );
                    }
                });
            }

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("Not tesselating!")
                    );
                }
            }.setMethod(draw1));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(7),
                        captureReference(INVOKEVIRTUAL),
                        RETURN
                    );
                }
            }
                .setMethod(startDrawingQuads)
                .addXref(1, startDrawing)
            );

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        DLOAD, 7,
                        DLOAD, 9,
                        captureReference(INVOKEVIRTUAL),

                        ALOAD_0,
                        DLOAD_1,
                        DLOAD_3,
                        DLOAD, 5,
                        captureReference(INVOKEVIRTUAL),

                        RETURN
                    );
                }
            }
                .setMethod(addVertexWithUV)
                .addXref(1, setTextureUV)
                .addXref(2, addVertex)
            );

            memberMappers.add(new FieldMapper(instance).accessFlag(AccessFlag.STATIC, true));
        }
    }

    /**
     * Matches IBlockAccess interface and maps getBlockId, getBlockMetadata methods.
     */
    public static class IBlockAccessMod extends ClassMod {
        public IBlockAccessMod() {
            classSignatures.add(new ClassSignature() {
                @Override
                public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
                    return classFile.isAbstract();
                }
            });

            classSignatures.add(new ClassSignature() {
                @Override
                public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
                    List list = getClassFile().getMethods();
                    return list.size() >= 1 && ((MethodInfo) list.get(0)).getDescriptor().equals("(III)I");
                }
            });

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getBlockId", "(III)I"), new MethodRef(getDeobfClass(), "getBlockMetadata", "(III)I")));
        }

        public IBlockAccessMod mapMaterial() {
            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getBlockMaterial", "(III)LMaterial;")));
            return this;
        }
    }

    /**
     * Matches Block class and maps blockID and blockList fields.
     */
    public static class BlockMod extends ClassMod {
        private static final ArrayList<BlockSubclassEntry> subclasses = new ArrayList<BlockSubclassEntry>() {
            {
                // autogenerated by blockids.pl -- do not edit
                // (block id, field class, field name, field subclass, block name)
                add(new BlockSubclassEntry(1, "Block", "stone", "BlockStone", "stone"));
                add(new BlockSubclassEntry(2, "BlockGrass", "grass", "BlockGrass", "grass"));
                add(new BlockSubclassEntry(3, "Block", "dirt", "BlockDirt", "dirt"));
                add(new BlockSubclassEntry(4, "Block", "cobblestone", "Block", "stonebrick"));
                add(new BlockSubclassEntry(5, "Block", "planks", "BlockWood", "wood"));
                add(new BlockSubclassEntry(6, "Block", "sapling", "BlockSapling", "sapling"));
                add(new BlockSubclassEntry(7, "Block", "bedrock", "Block", "bedrock"));
                add(new BlockSubclassEntry(8, "Block", "waterMoving", "BlockFlowing", "water"));
                add(new BlockSubclassEntry(9, "Block", "waterStill", "BlockStationary", "water"));
                add(new BlockSubclassEntry(10, "Block", "lavaMoving", "BlockFlowing", "lava"));
                add(new BlockSubclassEntry(11, "Block", "lavaStill", "BlockStationary", "lava"));
                add(new BlockSubclassEntry(12, "Block", "sand", "BlockSand", "sand"));
                add(new BlockSubclassEntry(13, "Block", "gravel", "BlockGravel", "gravel"));
                add(new BlockSubclassEntry(14, "Block", "oreGold", "BlockOre", "oreGold"));
                add(new BlockSubclassEntry(15, "Block", "oreIron", "BlockOre", "oreIron"));
                add(new BlockSubclassEntry(16, "Block", "oreCoal", "BlockOre", "oreCoal"));
                add(new BlockSubclassEntry(17, "Block", "wood", "BlockLog", "log"));
                add(new BlockSubclassEntry(18, "BlockLeaves", "leaves", "BlockLeaves", "leaves"));
                add(new BlockSubclassEntry(19, "Block", "sponge", "BlockSponge", "sponge"));
                add(new BlockSubclassEntry(20, "Block", "glass", "BlockGlass", "glass"));
                add(new BlockSubclassEntry(21, "Block", "oreLapis", "BlockOre", "oreLapis"));
                add(new BlockSubclassEntry(22, "Block", "blockLapis", "Block", "blockLapis"));
                add(new BlockSubclassEntry(23, "Block", "dispenser", "BlockDispenser", "dispenser"));
                add(new BlockSubclassEntry(24, "Block", "sandStone", "BlockSandStone", "sandStone"));
                add(new BlockSubclassEntry(25, "Block", "music", "BlockNote", "musicBlock"));
                add(new BlockSubclassEntry(26, "Block", "bed", "BlockBed", "bed"));
                add(new BlockSubclassEntry(27, "Block", "railPowered", "BlockRail", "goldenRail"));
                add(new BlockSubclassEntry(28, "Block", "railDetector", "BlockDetectorRail", "detectorRail"));
                add(new BlockSubclassEntry(29, "Block", "pistonStickyBase", "BlockPistonBase", "pistonStickyBase"));
                add(new BlockSubclassEntry(30, "Block", "web", "BlockWeb", "web"));
                add(new BlockSubclassEntry(31, "BlockTallGrass", "tallGrass", "BlockTallGrass", "tallgrass"));
                add(new BlockSubclassEntry(32, "BlockDeadBush", "deadBush", "BlockDeadBush", "deadbush"));
                add(new BlockSubclassEntry(33, "Block", "pistonBase", "BlockPistonBase", "pistonBase"));
                add(new BlockSubclassEntry(34, "BlockPistonExtension", "pistonExtension", "BlockPistonExtension", "unnamedBlock34"));
                add(new BlockSubclassEntry(35, "Block", "cloth", "BlockCloth", "cloth"));
                add(new BlockSubclassEntry(36, "BlockPistonMoving", "pistonMoving", "BlockPistonMoving", "unnamedBlock36"));
                add(new BlockSubclassEntry(37, "BlockFlower", "plantYellow", "BlockFlower", "flower"));
                add(new BlockSubclassEntry(38, "BlockFlower", "plantRed", "BlockFlower", "rose"));
                add(new BlockSubclassEntry(39, "BlockFlower", "mushroomBrown", "BlockMushroom", "mushroom"));
                add(new BlockSubclassEntry(40, "BlockFlower", "mushroomRed", "BlockMushroom", "mushroom"));
                add(new BlockSubclassEntry(41, "Block", "blockGold", "BlockOreStorage", "blockGold"));
                add(new BlockSubclassEntry(42, "Block", "blockSteel", "BlockOreStorage", "blockIron"));
                add(new BlockSubclassEntry(43, "BlockHalfSlab", "stoneDoubleSlab", "BlockStep", "stoneSlab"));
                add(new BlockSubclassEntry(44, "BlockHalfSlab", "stoneSingleSlab", "BlockStep", "stoneSlab"));
                add(new BlockSubclassEntry(45, "Block", "brick", "Block", "brick"));
                add(new BlockSubclassEntry(46, "Block", "tnt", "BlockTNT", "tnt"));
                add(new BlockSubclassEntry(47, "Block", "bookShelf", "BlockBookshelf", "bookshelf"));
                add(new BlockSubclassEntry(48, "Block", "cobblestoneMossy", "Block", "stoneMoss"));
                add(new BlockSubclassEntry(49, "Block", "obsidian", "BlockObsidian", "obsidian"));
                add(new BlockSubclassEntry(50, "Block", "torchWood", "BlockTorch", "torch"));
                add(new BlockSubclassEntry(51, "BlockFire", "fire", "BlockFire", "fire"));
                add(new BlockSubclassEntry(52, "Block", "mobSpawner", "BlockMobSpawner", "mobSpawner"));
                add(new BlockSubclassEntry(53, "Block", "stairCompactPlanks", "BlockStairs", "stairsWood"));
                add(new BlockSubclassEntry(54, "Block", "chest", "BlockChest", "chest"));
                add(new BlockSubclassEntry(55, "Block", "redstoneWire", "BlockRedstoneWire", "redstoneDust"));
                add(new BlockSubclassEntry(56, "Block", "oreDiamond", "BlockOre", "oreDiamond"));
                add(new BlockSubclassEntry(57, "Block", "blockDiamond", "BlockOreStorage", "blockDiamond"));
                add(new BlockSubclassEntry(58, "Block", "workbench", "BlockWorkbench", "workbench"));
                add(new BlockSubclassEntry(59, "Block", "crops", "BlockCrops", "crops"));
                add(new BlockSubclassEntry(60, "Block", "tilledField", "BlockFarmland", "farmland"));
                add(new BlockSubclassEntry(61, "Block", "stoneOvenIdle", "BlockFurnace", "furnace"));
                add(new BlockSubclassEntry(62, "Block", "stoneOvenActive", "BlockFurnace", "furnace"));
                add(new BlockSubclassEntry(63, "Block", "signPost", "BlockSign", "sign"));
                add(new BlockSubclassEntry(64, "Block", "doorWood", "BlockDoor", "doorWood"));
                add(new BlockSubclassEntry(65, "Block", "ladder", "BlockLadder", "ladder"));
                add(new BlockSubclassEntry(66, "Block", "rail", "BlockRail", "rail"));
                add(new BlockSubclassEntry(67, "Block", "stairCompactCobblestone", "BlockStairs", "stairsStone"));
                add(new BlockSubclassEntry(68, "Block", "signWall", "BlockSign", "sign"));
                add(new BlockSubclassEntry(69, "Block", "lever", "BlockLever", "lever"));
                add(new BlockSubclassEntry(70, "Block", "pressurePlateStone", "BlockPressurePlate", "pressurePlate"));
                add(new BlockSubclassEntry(71, "Block", "doorSteel", "BlockDoor", "doorIron"));
                add(new BlockSubclassEntry(72, "Block", "pressurePlatePlanks", "BlockPressurePlate", "pressurePlate"));
                add(new BlockSubclassEntry(73, "Block", "oreRedstone", "BlockRedstoneOre", "oreRedstone"));
                add(new BlockSubclassEntry(74, "Block", "oreRedstoneGlowing", "BlockRedstoneOre", "oreRedstone"));
                add(new BlockSubclassEntry(75, "Block", "torchRedstoneIdle", "BlockRedstoneTorch", "notGate"));
                add(new BlockSubclassEntry(76, "Block", "torchRedstoneActive", "BlockRedstoneTorch", "notGate"));
                add(new BlockSubclassEntry(77, "Block", "stoneButton", "BlockButton", "button"));
                add(new BlockSubclassEntry(78, "Block", "snow", "BlockSnow", "snow"));
                add(new BlockSubclassEntry(79, "Block", "ice", "BlockIce", "ice"));
                add(new BlockSubclassEntry(80, "Block", "blockSnow", "BlockSnowBlock", "snow"));
                add(new BlockSubclassEntry(81, "Block", "cactus", "BlockCactus", "cactus"));
                add(new BlockSubclassEntry(82, "Block", "blockClay", "BlockClay", "clay"));
                add(new BlockSubclassEntry(83, "Block", "reed", "BlockReed", "reeds"));
                add(new BlockSubclassEntry(84, "Block", "jukebox", "BlockJukeBox", "jukebox"));
                add(new BlockSubclassEntry(85, "Block", "fence", "BlockFence", "fence"));
                add(new BlockSubclassEntry(86, "Block", "pumpkin", "BlockPumpkin", "pumpkin"));
                add(new BlockSubclassEntry(87, "Block", "netherrack", "BlockNetherrack", "hellrock"));
                add(new BlockSubclassEntry(88, "Block", "slowSand", "BlockSoulSand", "hellsand"));
                add(new BlockSubclassEntry(89, "Block", "glowStone", "BlockGlowStone", "lightgem"));
                add(new BlockSubclassEntry(90, "BlockPortal", "portal", "BlockPortal", "portal"));
                add(new BlockSubclassEntry(91, "Block", "pumpkinLantern", "BlockPumpkin", "litpumpkin"));
                add(new BlockSubclassEntry(92, "Block", "cake", "BlockCake", "cake"));
                add(new BlockSubclassEntry(93, "Block", "redstoneRepeaterIdle", "BlockRedstoneRepeater", "diode"));
                add(new BlockSubclassEntry(94, "Block", "redstoneRepeaterActive", "BlockRedstoneRepeater", "diode"));
                add(new BlockSubclassEntry(95, "Block", "lockedChest", "BlockLockedChest", "lockedchest"));
                add(new BlockSubclassEntry(96, "Block", "trapdoor", "BlockTrapDoor", "trapdoor"));
                add(new BlockSubclassEntry(97, "Block", "silverfish", "BlockSilverfish", "monsterStoneEgg"));
                add(new BlockSubclassEntry(98, "Block", "stoneBrick", "BlockStoneBrick", "stonebricksmooth"));
                add(new BlockSubclassEntry(99, "Block", "mushroomCapBrown", "BlockMushroomCap", "mushroom"));
                add(new BlockSubclassEntry(100, "Block", "mushroomCapRed", "BlockMushroomCap", "mushroom"));
                add(new BlockSubclassEntry(101, "Block", "fenceIron", "BlockPane", "fenceIron"));
                add(new BlockSubclassEntry(102, "Block", "thinGlass", "BlockPane", "thinGlass"));
                add(new BlockSubclassEntry(103, "Block", "melon", "BlockMelon", "melon"));
                add(new BlockSubclassEntry(104, "Block", "pumpkinStem", "BlockStem", "pumpkinStem"));
                add(new BlockSubclassEntry(105, "Block", "melonStem", "BlockStem", "pumpkinStem"));
                add(new BlockSubclassEntry(106, "Block", "vine", "BlockVine", "vine"));
                add(new BlockSubclassEntry(107, "Block", "fenceGate", "BlockFenceGate", "fenceGate"));
                add(new BlockSubclassEntry(108, "Block", "stairsBrick", "BlockStairs", "stairsBrick"));
                add(new BlockSubclassEntry(109, "Block", "stairsStoneBrickSmooth", "BlockStairs", "stairsStoneBrickSmooth"));
                add(new BlockSubclassEntry(110, "BlockMycelium", "mycelium", "BlockMycelium", "mycel"));
                add(new BlockSubclassEntry(111, "Block", "waterlily", "BlockLilyPad", "waterlily"));
                add(new BlockSubclassEntry(112, "Block", "netherBrick", "Block", "netherBrick"));
                add(new BlockSubclassEntry(113, "Block", "netherFence", "BlockFence", "netherFence"));
                add(new BlockSubclassEntry(114, "Block", "stairsNetherBrick", "BlockStairs", "stairsNetherBrick"));
                add(new BlockSubclassEntry(115, "Block", "netherStalk", "BlockNetherStalk", "netherStalk"));
                add(new BlockSubclassEntry(116, "Block", "enchantmentTable", "BlockEnchantmentTable", "enchantmentTable"));
                add(new BlockSubclassEntry(117, "Block", "brewingStand", "BlockBrewingStand", "brewingStand"));
                add(new BlockSubclassEntry(118, "Block", "cauldron", "BlockCauldron", "cauldron"));
                add(new BlockSubclassEntry(119, "Block", "endPortal", "BlockEndPortal", "unnamedBlock119"));
                add(new BlockSubclassEntry(120, "Block", "endPortalFrame", "BlockEndPortalFrame", "endPortalFrame"));
                add(new BlockSubclassEntry(121, "Block", "whiteStone", "Block", "whiteStone"));
                add(new BlockSubclassEntry(122, "Block", "dragonEgg", "BlockDragonEgg", "dragonEgg"));
                add(new BlockSubclassEntry(123, "Block", "redstoneLampIdle", "BlockRedstoneLight", "redstoneLight"));
                add(new BlockSubclassEntry(124, "Block", "redstoneLampActive", "BlockRedstoneLight", "redstoneLight"));
                add(new BlockSubclassEntry(125, "BlockHalfSlab", "woodDoubleSlab", "BlockWoodSlab", "woodSlab"));
                add(new BlockSubclassEntry(126, "BlockHalfSlab", "woodSingleSlab", "BlockWoodSlab", "woodSlab"));
                add(new BlockSubclassEntry(127, "Block", "cocoaPlant", "BlockCocoa", "cocoa"));
                add(new BlockSubclassEntry(128, "Block", "stairsSandStone", "BlockStairs", "stairsSandStone"));
                add(new BlockSubclassEntry(129, "Block", "oreEmerald", "BlockOre", "oreEmerald"));
                add(new BlockSubclassEntry(130, "Block", "enderChest", "BlockEnderChest", "enderChest"));
                add(new BlockSubclassEntry(131, "BlockTripWireSource", "tripWireSource", "BlockTripWireSource", "tripWireSource"));
                add(new BlockSubclassEntry(132, "Block", "tripWire", "BlockTripWire", "tripWire"));
                add(new BlockSubclassEntry(133, "Block", "blockEmerald", "BlockOreStorage", "blockEmerald"));
                add(new BlockSubclassEntry(134, "Block", "stairsWoodSpruce", "BlockStairs", "stairsWoodSpruce"));
                add(new BlockSubclassEntry(135, "Block", "stairsWoodBirch", "BlockStairs", "stairsWoodBirch"));
                add(new BlockSubclassEntry(136, "Block", "stairsWoodJungle", "BlockStairs", "stairsWoodJungle"));
                add(new BlockSubclassEntry(137, "Block", "commandBlock", "BlockCommandBlock", "commandBlock"));
                add(new BlockSubclassEntry(138, "Block", "beacon", "BlockBeacon", "beacon"));
                add(new BlockSubclassEntry(139, "Block", "cobblestoneWall", "BlockWall", "cobbleWall"));
                add(new BlockSubclassEntry(140, "Block", "flowerPot", "BlockFlowerPot", "flowerPot"));
                add(new BlockSubclassEntry(141, "Block", "carrot", "BlockCarrot", "carrots"));
                add(new BlockSubclassEntry(142, "Block", "potato", "BlockPotato", "potatoes"));
                add(new BlockSubclassEntry(143, "Block", "woodenButton", "BlockButton", "button"));
                add(new BlockSubclassEntry(144, "Block", "skull", "BlockSkull", "skull"));
                add(new BlockSubclassEntry(145, "Block", "anvil", "BlockAnvil", "anvil"));
            }
        };

        public BlockMod() {
            classSignatures.add(new ConstSignature(" is already occupied by "));

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "blockID", "I"))
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
                .accessFlag(AccessFlag.FINAL, true)
            );

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "blocksList", "[LBlock;"))
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
                .accessFlag(AccessFlag.FINAL, true)
            );
        }

        protected void addBlockSignatures() {
            for (BlockSubclassEntry entry : subclasses) {
                addBlockSignature(entry.blockID, entry.fieldClass, entry.fieldName, entry.className, entry.blockName);
            }
        }

        protected void addBlockSignature(String name) {
            for (BlockSubclassEntry entry : subclasses) {
                if (entry.className.equals(name) || entry.blockName.equals(name) || entry.fieldName.equals(name)) {
                    addBlockSignature(entry.blockID, entry.fieldClass, entry.fieldName, entry.className, entry.blockName);
                    return;
                }
            }
            throw new RuntimeException("unknown Block subclass: " + name);
        }

        protected void addBlockSignature(int blockID) {
            for (BlockSubclassEntry entry : subclasses) {
                if (entry.blockID == blockID) {
                    addBlockSignature(entry.blockID, entry.fieldClass, entry.fieldName, entry.className, entry.blockName);
                    return;
                }
            }
            throw new RuntimeException("unknown Block subclass: block ID" + blockID);
        }

        protected void addBlockSignature(final int blockID, final String fieldClass, final String fieldName, final String className, final String blockName) {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        captureReference(NEW),
                        DUP,
                        blockID == 35 ? "" : push(blockID),
                        nonGreedy(any(0, 60)),
                        blockName.startsWith("unnamedBlock") ? "" : build(
                            push(blockName),
                            anyReference(INVOKEVIRTUAL)
                        ),
                        nonGreedy(any(0, 20)),
                        captureReference(PUTSTATIC)
                    );
                }
            }
                .matchStaticInitializerOnly(true)
                .addXref(1, new ClassRef(className))
                .addXref(2, new FieldRef(getDeobfClass(), fieldName, "L" + fieldClass + ";"))
            );
        }

        private static class BlockSubclassEntry {
            final int blockID;
            final String fieldClass;
            final String fieldName;
            final String className;
            final String blockName;

            BlockSubclassEntry(int blockID, String fieldClass, String fieldName, String className, String blockName) {
                this.blockID = blockID;
                this.fieldClass = fieldClass;
                this.fieldName = fieldName;
                this.className = className;
                this.blockName = blockName;
            }
        }
    }

    /**
     * Matches Item class.
     */
    public static class ItemMod extends ClassMod {
        public ItemMod() {
            classSignatures.add(new ConstSignature("CONFLICT @ "));
            classSignatures.add(new ConstSignature("coal"));
        }
    }

    /**
     * Matches World class.
     */
    public static class WorldMod extends ClassMod {
        public WorldMod() {
            interfaces = new String[]{"IBlockAccess"};

            classSignatures.add(new ConstSignature("ambient.cave.cave"));
            classSignatures.add(new ConstSignature(0x3c6ef35f));
        }
    }

    /**
     * Matches WorldServer class and maps world field.
     */
    public static class WorldServerMod extends ClassMod {
        public WorldServerMod(MinecraftVersion minecraftVersion) {
            final FieldRef world = new FieldRef(getDeobfClass(), "world", "LWorld;");

            classSignatures.add(new ConstSignature("/particles.png"));
            classSignatures.add(new ConstSignature("/terrain.png"));
            classSignatures.add(new ConstSignature("/gui/items.png"));

            memberMappers.add(new FieldMapper(world));

            patches.add(new MakeMemberPublicPatch(world));
        }
    }

    public static class WorldServerMPMod extends ClassMod {
        public WorldServerMPMod(MinecraftVersion minecraftVersion) {
            parentClass = "World";

            classSignatures.add(new ConstSignature("MpServer"));
        }
    }

    /*
     * Matches FontRenderer class and maps charWidth, fontTextureName, and spaceWidth fields.
     */
    public static class FontRendererMod extends ClassMod {
        public FontRendererMod() {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        anyReference(INVOKESPECIAL),
                        ALOAD_0,
                        push(256),
                        NEWARRAY, T_INT,
                        captureReference(PUTFIELD),
                        ALOAD_0,
                        ICONST_0,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, new FieldRef(getDeobfClass(), "charWidth", "[I"))
                .addXref(2, new FieldRef(getDeobfClass(), "fontTextureName", "I"))
            );

            classSignatures.add(new OrSignature(
                new ConstSignature("0123456789abcdef"),
                new ConstSignature("0123456789abcdefk"),
                new ConstSignature("/font/glyph_sizes.bin")
            ));
        }
    }

    /**
     * Matches RenderBlocks class.
     */
    public static class RenderBlocksMod extends ClassMod {
        public RenderBlocksMod() {
            classSignatures.add(new ConstSignature(0.1875));
            classSignatures.add(new ConstSignature(0.01));
        }
    }

    /**
     * Maps RenderEngine class.
     */
    public static class RenderEngineMod extends ClassMod {
        protected final MethodRef glTexSubImage2D = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V");

        public RenderEngineMod() {
            classSignatures.add(new ConstSignature("%clamp%"));
            classSignatures.add(new ConstSignature("%blur%"));
            classSignatures.add(new ConstSignature(glTexSubImage2D));
        }
    }

    /**
     * Maps GameSettings class.
     */
    public static class GameSettingsMod extends ClassMod {
        public GameSettingsMod() {
            classSignatures.add(new ConstSignature("options.txt"));
            classSignatures.add(new OrSignature(
                new ConstSignature("key.forward"),
                new ConstSignature("Forward")
            ));
        }

        /**
         * Map any GameSettings field stored in options.txt.
         *
         * @param option     name in options.txt
         * @param field      name of field in GameSettings class
         * @param descriptor type descriptor
         */
        protected void mapOption(final String option, final String field, final String descriptor) {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (as[0].equals(option)) {
                        ALOAD_3,
                        ICONST_0,
                        AALOAD,
                        push(option),
                        reference(INVOKEVIRTUAL, new MethodRef("java/lang/String", "equals", "(Ljava/lang/Object;)Z")),
                        IFEQ, any(2),

                        // field = ...;
                        nonGreedy(any(0, 20)),
                        captureReference(PUTFIELD)
                    );
                }
            }.addXref(1, new FieldRef(getDeobfClass(), field, descriptor)));
        }
    }

    /**
     * Maps Profiler class and start/endSection methods.
     */
    public static class ProfilerMod extends ClassMod {
        public ProfilerMod() {
            classSignatures.add(new ConstSignature("[UNKNOWN]"));
            classSignatures.add(new ConstSignature(100.0));

            final MethodRef startSection = new MethodRef(getDeobfClass(), "startSection", "(Ljava/lang/String;)V");
            final MethodRef endSection = new MethodRef(getDeobfClass(), "endSection", "()V");
            final MethodRef endStartSection = new MethodRef(getDeobfClass(), "endStartSection", "(Ljava/lang/String;)V");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(INVOKEVIRTUAL),
                        ALOAD_0,
                        ALOAD_1,
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            }
                .setMethod(endStartSection)
                .addXref(1, endSection)
                .addXref(2, startSection)
            );
        }
    }
}
