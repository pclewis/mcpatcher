package com.pclewis.mcpatcher;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static javassist.bytecode.Opcode.*;

/**
 * Internal mod required by the patcher.  Responsible for injecting MCPatcherUtils classes
 * into minecraft.jar.
 * <p/>
 * Also provides a collection of commonly used ClassMods as public static inner classes that
 * can be instantiated or extended as needed.
 */
public final class BaseMod extends Mod {
    BaseMod(MinecraftVersion minecraftVersion) {
        name = "__Base";
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.0";
        configPanel = new ConfigPanel();

        classMods.add(new XMinecraftMod());

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.UTILS_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.CONFIG_CLASS));
    }

    class ConfigPanel extends ModConfigPanel {
        private JPanel panel;
        private JTextField heapSizeText;
        private JCheckBox debugCheckBox;

        ConfigPanel() {
            debugCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MCPatcherUtils.set(com.pclewis.mcpatcher.Config.TAG_DEBUG, debugCheckBox.isSelected());
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
                            BinaryRegex.begin(),
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
            memberMappers.add(new FieldMapper("texturePackList", "LTexturePackList;"));
            return this;
        }
    }

    /**
     * Matches TexturePackList class and maps selected and default texture pack fields.
     */
    public static class TexturePackListMod extends ClassMod {
        public TexturePackListMod() {
            classSignatures.add(new ConstSignature(".zip"));
            classSignatures.add(new ConstSignature("texturepacks"));

            memberMappers.add(new FieldMapper("selectedTexturePack", "LTexturePackBase;").accessFlag(AccessFlag.PUBLIC, true));
            memberMappers.add(new FieldMapper("defaultTexturePack", "LTexturePackBase;").accessFlag(AccessFlag.PRIVATE, true));
            memberMappers.add(new FieldMapper("minecraft", "LMinecraft;"));
        }
    }

    /**
     * Matches TexturePackBase class and maps getInputStream method.
     */
    public static class TexturePackBaseMod extends ClassMod {
        public TexturePackBaseMod() {
            final MethodRef getResourceAsStream = new MethodRef("java.lang.Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;");

            classSignatures.add(new ConstSignature(getResourceAsStream));
            classSignatures.add(new ConstSignature("pack.txt").negate(true));

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

            memberMappers.add(new FieldMapper("texturePackFileName", "Ljava/lang/String;"));
        }
    }

    /**
     * Matches TexturePackDefault class.
     */
    public static class TexturePackDefaultMod extends ClassMod {
        public TexturePackDefaultMod() {
            classSignatures.add(new ConstSignature("The default look of Minecraft"));
        }
    }

    /**
     * Matches GLAllocation class and maps createDirectByteBuffer method.
     */
    public static class GLAllocationMod extends ClassMod {
        public GLAllocationMod() {
            classSignatures.add(new ConstSignature(new MethodRef("org.lwjgl.opengl.GL11", "glDeleteLists", "(II)V")));

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

            memberMappers.add(new MethodMapper(new String[]{"getBlockId", "getBlockMetadata"}, "(III)I"));
        }

        public IBlockAccessMod mapMaterial() {
            memberMappers.add(new MethodMapper("getBlockMaterial", "(III)LMaterial;"));
            return this;
        }
    }

    /**
     * Matches Block class and maps blockID and blockList fields.
     */
    public static class BlockMod extends ClassMod {
        private static final ArrayList<BlockSubclassEntry> subclasses = new ArrayList<BlockSubclassEntry>() {
            {
                add(new BlockSubclassEntry(1, "BlockStone", "stone", "Block", "stone"));
                add(new BlockSubclassEntry(2, "BlockGrass", "grass", "BlockGrass", "grass"));
                add(new BlockSubclassEntry(3, "BlockDirt", "dirt", "Block", "dirt"));
                add(new BlockSubclassEntry(4, "Block", "stonebrick", "Block", "cobblestone"));
                add(new BlockSubclassEntry(5, "Block", "wood", "Block", "planks"));
                add(new BlockSubclassEntry(6, "BlockSapling", "sapling", "Block", "sapling"));
                add(new BlockSubclassEntry(7, "Block", "bedrock", "Block", "bedrock"));
                add(new BlockSubclassEntry(8, "BlockFlowing", "water", "Block", "waterMoving"));
                add(new BlockSubclassEntry(9, "BlockStationary", "water", "Block", "waterStill"));
                add(new BlockSubclassEntry(10, "BlockFlowing", "lava", "Block", "lavaMoving"));
                add(new BlockSubclassEntry(11, "BlockStationary", "lava", "Block", "lavaStill"));
                add(new BlockSubclassEntry(12, "BlockSand", "sand", "Block", "sand"));
                add(new BlockSubclassEntry(13, "BlockGravel", "gravel", "Block", "gravel"));
                add(new BlockSubclassEntry(14, "BlockOre", "oreGold", "Block", "oreGold"));
                add(new BlockSubclassEntry(15, "BlockOre", "oreIron", "Block", "oreIron"));
                add(new BlockSubclassEntry(16, "BlockOre", "oreCoal", "Block", "oreCoal"));
                add(new BlockSubclassEntry(17, "BlockLog", "log", "Block", "wood"));
                add(new BlockSubclassEntry(18, "BlockLeaves", "leaves", "BlockLeaves", "leaves"));
                add(new BlockSubclassEntry(19, "BlockSponge", "sponge", "Block", "sponge"));
                add(new BlockSubclassEntry(20, "BlockGlass", "glass", "Block", "glass"));
                add(new BlockSubclassEntry(21, "BlockOre", "oreLapis", "Block", "oreLapis"));
                add(new BlockSubclassEntry(22, "Block", "blockLapis", "Block", "blockLapis"));
                add(new BlockSubclassEntry(23, "BlockDispenser", "dispenser", "Block", "dispenser"));
                add(new BlockSubclassEntry(24, "BlockSandStone", "sandStone", "Block", "sandStone"));
                add(new BlockSubclassEntry(25, "BlockNote", "musicBlock", "Block", "music"));
                add(new BlockSubclassEntry(26, "BlockBed", "bed", "Block", "bed"));
                add(new BlockSubclassEntry(27, "BlockRail", "goldenRail", "Block", "railPowered"));
                add(new BlockSubclassEntry(28, "BlockDetectorRail", "detectorRail", "Block", "railDetector"));
                add(new BlockSubclassEntry(29, "BlockPistonBase", "pistonStickyBase", "Block", "pistonStickyBase"));
                add(new BlockSubclassEntry(30, "BlockWeb", "web", "Block", "web"));
                add(new BlockSubclassEntry(31, "BlockTallGrass", "tallgrass", "BlockTallGrass", "tallGrass"));
                add(new BlockSubclassEntry(32, "BlockDeadBush", "deadbush", "BlockDeadBush", "deadBush"));
                add(new BlockSubclassEntry(33, "BlockPistonBase", "pistonBase", "Block", "pistonBase"));
                // block id 34
                // block id 35
                // block id 36
                add(new BlockSubclassEntry(37, "BlockFlower", "flower", "BlockFlower", "plantYellow"));
                add(new BlockSubclassEntry(38, "BlockFlower", "rose", "BlockFlower", "plantRed"));
                add(new BlockSubclassEntry(39, "BlockMushroom", "mushroom", "BlockFlower", "mushroomBrown"));
                add(new BlockSubclassEntry(40, "BlockMushroom", "mushroom", "BlockFlower", "mushroomRed"));
                add(new BlockSubclassEntry(41, "BlockOreStorage", "blockGold", "Block", "blockGold"));
                add(new BlockSubclassEntry(42, "BlockOreStorage", "blockIron", "Block", "blockSteel"));
                add(new BlockSubclassEntry(43, "BlockStep", "stoneSlab", "Block", "stairDouble"));
                add(new BlockSubclassEntry(44, "BlockStep", "stoneSlab", "Block", "stairSingle"));
                add(new BlockSubclassEntry(45, "Block", "brick", "Block", "brick"));
                add(new BlockSubclassEntry(46, "BlockTNT", "tnt", "Block", "tnt"));
                add(new BlockSubclassEntry(47, "BlockBookshelf", "bookshelf", "Block", "bookShelf"));
                add(new BlockSubclassEntry(48, "Block", "stoneMoss", "Block", "cobblestoneMossy"));
                add(new BlockSubclassEntry(49, "BlockObsidian", "obsidian", "Block", "obsidian"));
                add(new BlockSubclassEntry(50, "BlockTorch", "torch", "Block", "torchWood"));
                add(new BlockSubclassEntry(51, "BlockFire", "fire", "BlockFire", "fire"));
                add(new BlockSubclassEntry(52, "BlockMobSpawner", "mobSpawner", "Block", "mobSpawner"));
                add(new BlockSubclassEntry(53, "BlockStairs", "stairsWood", "Block", "stairCompactPlanks"));
                add(new BlockSubclassEntry(54, "BlockChest", "chest", "Block", "chest"));
                add(new BlockSubclassEntry(55, "BlockRedstoneWire", "redstoneDust", "Block", "redstoneWire"));
                add(new BlockSubclassEntry(56, "BlockOre", "oreDiamond", "Block", "oreDiamond"));
                add(new BlockSubclassEntry(57, "BlockOreStorage", "blockDiamond", "Block", "blockDiamond"));
                add(new BlockSubclassEntry(58, "BlockWorkbench", "workbench", "Block", "workbench"));
                add(new BlockSubclassEntry(59, "BlockCrops", "crops", "Block", "crops"));
                add(new BlockSubclassEntry(60, "BlockFarmland", "farmland", "Block", "tilledField"));
                add(new BlockSubclassEntry(61, "BlockFurnace", "furnace", "Block", "stoneOvenIdle"));
                add(new BlockSubclassEntry(62, "BlockFurnace", "furnace", "Block", "stoneOvenActive"));
                add(new BlockSubclassEntry(63, "BlockSign", "sign", "Block", "signPost"));
                add(new BlockSubclassEntry(64, "BlockDoor", "doorWood", "Block", "doorWood"));
                add(new BlockSubclassEntry(65, "BlockLadder", "ladder", "Block", "ladder"));
                add(new BlockSubclassEntry(66, "BlockRail", "rail", "Block", "rail"));
                add(new BlockSubclassEntry(67, "BlockStairs", "stairsStone", "Block", "stairCompactCobblestone"));
                add(new BlockSubclassEntry(68, "BlockSign", "sign", "Block", "signWall"));
                add(new BlockSubclassEntry(69, "BlockLever", "lever", "Block", "lever"));
                add(new BlockSubclassEntry(70, "BlockPressurePlate", "pressurePlate", "Block", "pressurePlateStone"));
                add(new BlockSubclassEntry(71, "BlockDoor", "doorIron", "Block", "doorSteel"));
                add(new BlockSubclassEntry(72, "BlockPressurePlate", "pressurePlate", "Block", "pressurePlatePlanks"));
                add(new BlockSubclassEntry(73, "BlockRedstoneOre", "oreRedstone", "Block", "oreRedstone"));
                add(new BlockSubclassEntry(74, "BlockRedstoneOre", "oreRedstone", "Block", "oreRedstoneGlowing"));
                add(new BlockSubclassEntry(75, "BlockRedstoneTorch", "notGate", "Block", "torchRedstoneIdle"));
                add(new BlockSubclassEntry(76, "BlockRedstoneTorch", "notGate", "Block", "torchRedstoneActive"));
                add(new BlockSubclassEntry(77, "BlockButton", "button", "Block", "button"));
                add(new BlockSubclassEntry(78, "BlockSnow", "snow", "Block", "snow"));
                add(new BlockSubclassEntry(79, "BlockIce", "ice", "Block", "ice"));
                add(new BlockSubclassEntry(80, "BlockSnowBlock", "snow", "Block", "blockSnow"));
                add(new BlockSubclassEntry(81, "BlockCactus", "cactus", "Block", "cactus"));
                add(new BlockSubclassEntry(82, "BlockClay", "clay", "Block", "blockClay"));
                add(new BlockSubclassEntry(83, "BlockReed", "reeds", "Block", "reed"));
                add(new BlockSubclassEntry(84, "BlockJukeBox", "jukebox", "Block", "jukebox"));
                add(new BlockSubclassEntry(85, "BlockFence", "fence", "Block", "fence"));
                add(new BlockSubclassEntry(86, "BlockPumpkin", "pumpkin", "Block", "pumpkin"));
                add(new BlockSubclassEntry(87, "BlockNetherrack", "hellrock", "Block", "netherrack"));
                add(new BlockSubclassEntry(88, "BlockSoulSand", "hellsand", "Block", "slowSand"));
                add(new BlockSubclassEntry(89, "BlockGlowStone", "lightgem", "Block", "glowStone"));
                add(new BlockSubclassEntry(90, "BlockPortal", "portal", "BlockPortal", "portal"));
                add(new BlockSubclassEntry(91, "BlockPumpkin", "litpumpkin", "Block", "pumpkinLantern"));
                add(new BlockSubclassEntry(92, "BlockCake", "cake", "Block", "cake"));
                add(new BlockSubclassEntry(93, "BlockRedstoneRepeater", "diode", "Block", "redstoneRepeaterIdle"));
                add(new BlockSubclassEntry(94, "BlockRedstoneRepeater", "diode", "Block", "redstoneRepeaterActive"));
                add(new BlockSubclassEntry(95, "BlockLockedChest", "lockedchest", "Block", "lockedChest"));
                add(new BlockSubclassEntry(96, "BlockTrapDoor", "trapdoor", "Block", "trapdoor"));
                // block id 97
                add(new BlockSubclassEntry(98, "BlockStoneBrick", "stonebricksmooth", "Block", "stoneBrick"));
                add(new BlockSubclassEntry(99, "BlockMushroomCap", "mushroom", "Block", "mushroomCapBrown"));
                add(new BlockSubclassEntry(100, "BlockMushroomCap", "mushroom", "Block", "mushroomCapRed"));
                add(new BlockSubclassEntry(101, "BlockPane", "fenceIron", "Block", "fenceIron"));
                add(new BlockSubclassEntry(102, "BlockPane", "thinGlass", "Block", "thinGlass"));
                add(new BlockSubclassEntry(103, "BlockMelon", "melon", "Block", "melon"));
                add(new BlockSubclassEntry(104, "BlockStem", "pumpkinStem", "Block", "pumpkinStem"));
                add(new BlockSubclassEntry(105, "BlockStem", "pumpkinStem", "Block", "melonStem"));
                add(new BlockSubclassEntry(106, "BlockVine", "vine", "Block", "vine"));
                add(new BlockSubclassEntry(107, "BlockFenceGate", "fenceGate", "Block", "fenceGate"));
                add(new BlockSubclassEntry(108, "BlockStairs", "stairsBrick", "Block", "stairsBrick"));
                add(new BlockSubclassEntry(109, "BlockStairs", "stairsStoneBrickSmooth", "Block", "stairsStoneBrickSmooth"));
                add(new BlockSubclassEntry(110, "BlockMycelium", "mycel", "BlockMycelium", "mycelium"));
                add(new BlockSubclassEntry(111, "BlockLilyPad", "waterlily", "Block", "waterlily"));
                add(new BlockSubclassEntry(112, "Block", "netherBrick", "Block", "netherBrick"));
                add(new BlockSubclassEntry(113, "BlockFence", "netherFence", "Block", "replaceID"));
                add(new BlockSubclassEntry(114, "BlockStairs", "stairsNetherBrick", "Block", "stairsNetherBrick"));
                add(new BlockSubclassEntry(115, "BlockNetherStalk", "netherStalk", "Block", "netherStalk"));
                add(new BlockSubclassEntry(116, "BlockEnchantmentTable", "enchantmentTable", "Block", "enchantmentTable"));
                add(new BlockSubclassEntry(117, "BlockBrewingStand", "brewingStand", "Block", "brewingStand"));
                add(new BlockSubclassEntry(118, "BlockCauldron", "cauldron", "Block", "cauldron"));
                // block id 119
                add(new BlockSubclassEntry(120, "BlockEndPortalFrame", "endPortalFrame", "Block", "endPortalFrame"));
                add(new BlockSubclassEntry(121, "Block", "whiteStone", "Block", "whiteStone"));
                add(new BlockSubclassEntry(122, "BlockDragonEgg", "dragonEgg", "Block", "dragonEgg"));
            }
        };

        public BlockMod() {
            classSignatures.add(new ConstSignature(" is already occupied by "));

            memberMappers.add(new FieldMapper("blockID", "I")
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
                .accessFlag(AccessFlag.FINAL, true)
            );

            memberMappers.add(new FieldMapper("blocksList", "[LBlock;")
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
                .accessFlag(AccessFlag.FINAL, true)
            );
        }

        protected void addBlockSignatures() {
            for (BlockSubclassEntry entry : subclasses) {
                addBlockSignature(entry.blockID, entry.blockName, entry.className, entry.fieldName, entry.fieldClass);
            }
        }

        protected void addBlockSignature(String name) {
            for (BlockSubclassEntry entry : subclasses) {
                if (entry.className.equals(name) || entry.blockName.equals(name) || entry.fieldName.equals(name)) {
                    addBlockSignature(entry.blockID, entry.blockName, entry.className, entry.fieldName, entry.fieldClass);
                    return;
                }
            }
            throw new RuntimeException("unknown Block subclass: " + name);
        }

        protected void addBlockSignature(int blockID) {
            for (BlockSubclassEntry entry : subclasses) {
                if (entry.blockID == blockID) {
                    addBlockSignature(entry.blockID, entry.blockName, entry.className, entry.fieldName, entry.fieldClass);
                    return;
                }
            }
            throw new RuntimeException("unknown Block subclass: block ID" + blockID);
        }

        protected void addBlockSignature(final int blockID, final String blockName, final String className, final String fieldName, final String fieldClass) {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isStaticInitializer()) {
                        return buildExpression(
                            BytecodeMatcher.captureReference(NEW),
                            DUP,
                            push(blockID),
                            BinaryRegex.nonGreedy(BinaryRegex.any(0, 60)),
                            push(blockName),
                            BytecodeMatcher.anyReference(INVOKEVIRTUAL),
                            BinaryRegex.nonGreedy(BinaryRegex.any(0, 20)),
                            BytecodeMatcher.captureReference(PUTSTATIC)
                        );
                    } else {
                        return null;
                    }
                }
            }
                .addXref(1, new ClassRef(className))
                .addXref(2, new FieldRef(getDeobfClass(), fieldName, "L" + fieldClass + ";"))
            );
        }

        private static class BlockSubclassEntry {
            int blockID;
            String className;
            String blockName;
            String fieldClass;
            String fieldName;

            BlockSubclassEntry(int blockID, String className, String blockName, String fieldClass, String fieldName) {
                this.blockID = blockID;
                this.className = className;
                this.blockName = blockName;
                this.fieldClass = fieldClass;
                this.fieldName = fieldName;
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
            classSignatures.add(new ConstSignature("ambient.cave.cave"));
            classSignatures.add(new ConstSignature("Saving level"));
            classSignatures.add(new ConstSignature("Saving chunks"));
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
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            BinaryRegex.begin(),
                            ALOAD_0,
                            BytecodeMatcher.anyReference(INVOKESPECIAL),
                            ALOAD_0,
                            push(256),
                            NEWARRAY, T_INT,
                            BytecodeMatcher.captureReference(PUTFIELD),
                            ALOAD_0,
                            ICONST_0,
                            BytecodeMatcher.captureReference(PUTFIELD)
                        );
                    } else {
                        return null;
                    }
                }
            }
                .addXref(1, new FieldRef(getDeobfClass(), "charWidth", "[I"))
                .addXref(2, new FieldRef(getDeobfClass(), "fontTextureName", "I"))
            );

            classSignatures.add(new OrSignature(
                new ConstSignature("0123456789abcdef"),
                new ConstSignature("0123456789abcdefk")
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
     * Maps GameSettings class.
     */
    public static class GameSettingsMod extends ClassMod {
        public GameSettingsMod() {
            classSignatures.add(new ConstSignature("key.forward"));
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
                        IFEQ, BinaryRegex.any(2),

                        // field = ...;
                        BinaryRegex.nonGreedy(BinaryRegex.any(0, 20)),
                        BytecodeMatcher.captureReference(PUTFIELD)
                    );
                }
            }.addXref(1, new FieldRef(getDeobfClass(), field, descriptor)));
        }
    }
}
