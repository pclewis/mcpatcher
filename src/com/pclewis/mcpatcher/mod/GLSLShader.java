package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import com.sun.xml.internal.stream.Entity;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class GLSLShader extends Mod {
    public static final String class_Shaders = "com.pclewis.mcpatcher.mod.Shaders";

    public GLSLShader() {
        name = "GLSL Shader";
        description = "Adds graphical shaders to the game.  Based on daxnitro's mod.";
        version = "1.0";

        classMods.add(new MinecraftMod());
        classMods.add(new EntityRendererMod());

        filesToAdd.add("com/pclewis/mcpatcher/mod/Shaders.class");
        filesToAdd.add("com/pclewis/mcpatcher/mod/Shaders$1.class");
        filesToAdd.add("com/pclewis/mcpatcher/mod/Shaders$2.class");
        filesToAdd.add("com/pclewis/mcpatcher/mod/Shaders$LightSource.class");
        filesToAdd.add("com/pclewis/mcpatcher/mod/Shaders$Option.class");
    }

    private class MinecraftMod extends ClassMod {
        public MinecraftMod() {
            classSignatures.add(new FilenameSignature("net/minecraft/client/Minecraft.class"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "call setUpBuffers";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return BinaryRegex.capture(buildExpression(
                        reference(methodInfo, INVOKESTATIC, new MethodRef("org.lwjgl.opengl.Display", "create", "()V"))
                    ));
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        ALOAD_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "setUpBuffers", "(LMinecraft;)V"))
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "GL11.glViewport -> Shaders.viewport";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        reference(methodInfo, INVOKESTATIC, new MethodRef("org.lwjgl.opengl.GL11", "glViewport", "(IIII)V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "viewport", "(IIII)V"))
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef("org.lwjgl.opengl.Display", "update", "()V"))
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

            patches.add(new BytecodePatch() {
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
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(methodInfo, INVOKESTATIC, new MethodRef(class_Shaders, "setUpBuffers", "(LMinecraft;)V")),
                        RETURN
                    );
                }
            });
        }
    }

    private class EntityRendererMod extends ClassMod {
        public EntityRendererMod() {
            classSignatures.add(new ConstSignature("ambient.weather.rain"));
            classSignatures.add(new ConstSignature(0x2b24abb));
            classSignatures.add(new ConstSignature(0x66397));
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
                        reference(methodInfo, INVOKESTATIC, new MethodRef("org.lwjgl.opengl.GL11", "glColorMask", "(ZZZZ)V"))
                    );
                }
            }.setMethodName("renderWorld2"));

            memberMappers.add(new FieldMapper(
                new String[] {
                    "farPlaneDistance",
                    null, null, null, null, null, null, null, null, null, null,
                    "fogColorRed", "fogColorGreen", "fogColorBlue",
                    "fogColor2", "fogColor1"
                },
                "F"
            ));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "call Shaders.processScene";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return BinaryRegex.capture(BinaryRegex.or(
                        buildExpression(reference(methodInfo, INVOKEVIRTUAL, new MethodRef("EntityRenderer", "renderWorld1", "(F)V"))),
                        buildExpression(reference(methodInfo, INVOKEVIRTUAL, new MethodRef("EntityRenderer", "renderWorld2", "(FJ)V")))
                    ));
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
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
        }
    }
}
