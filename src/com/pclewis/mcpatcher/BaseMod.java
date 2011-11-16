package com.pclewis.mcpatcher;

import javassist.bytecode.AccessFlag;
import javassist.bytecode.MethodInfo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import static javassist.bytecode.Opcode.*;

/**
 * Internal mod required by the patcher.  Responsible for injecting MCPatcherUtils classes
 * into minecraft.jar.
 *
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
                        push(methodInfo, MCPatcher.minecraft.getVersion().toString()),
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
     *
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
     * Matches World class.
     */
    public static class WorldMod extends ClassMod {
        public WorldMod() {
            classSignatures.add(new ConstSignature("ambient.cave.cave"));
            classSignatures.add(new ConstSignature("Saving level"));
            classSignatures.add(new ConstSignature("Saving chunks"));
        }
    }
}
