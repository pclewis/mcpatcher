package com.pclewis.mcpatcher;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;
import javassist.bytecode.MethodInfo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.getName().equals("<init>")) {
                        return buildExpression(
                            BinaryRegex.begin(),
                            ALOAD_0,
                            reference(methodInfo, INVOKESPECIAL, new MethodRef("java.lang.Object", "<init>", "()V"))
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getInsertBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.UTILS_CLASS, "setMinecraft", "(LMinecraft;)V")),
                        push(methodInfo, MCPatcher.minecraft.getVersion().getVersionString()),
                        push(methodInfo, MCPatcher.VERSION_STRING),
                        reference(methodInfo, INVOKESTATIC, new MethodRef(MCPatcherUtils.UTILS_CLASS, "setVersions", "(Ljava/lang/String;Ljava/lang/String;)V"))
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
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD_1,
                        reference(methodInfo, INVOKEVIRTUAL, getResourceAsStream),
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
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.getDescriptor().equals("(I)Ljava/nio/ByteBuffer;")) {
                        return buildExpression(
                            reference(methodInfo, INVOKESTATIC, new MethodRef("java.nio.ByteBuffer", "allocateDirect", "(I)Ljava/nio/ByteBuffer;"))
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
                    List list = classFile.getMethods();
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
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isStaticInitializer()) {
                        return buildExpression(
                            BytecodeMatcher.captureReference(NEW),
                            DUP,
                            push(methodInfo, blockID),
                            BinaryRegex.nonGreedy(BinaryRegex.any(0, 60)),
                            push(methodInfo, blockName),
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
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            BinaryRegex.begin(),
                            ALOAD_0,
                            BytecodeMatcher.anyReference(INVOKESPECIAL),
                            ALOAD_0,
                            push(methodInfo, 256),
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
}
