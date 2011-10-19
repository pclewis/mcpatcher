package com.pclewis.mcpatcher;

import javassist.bytecode.MethodInfo;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import static javassist.bytecode.Opcode.*;

class BaseMod extends Mod {
    public BaseMod(MinecraftVersion minecraftVersion) {
        name = "__Base";
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.0";
        configPanel = new ConfigPanel();

        classMods.add(new MinecraftMod());

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

    private class MinecraftMod extends ClassMod {
        MinecraftMod() {
            classSignatures.add(new FilenameSignature("net/minecraft/client/Minecraft.class"));

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
    }
}
