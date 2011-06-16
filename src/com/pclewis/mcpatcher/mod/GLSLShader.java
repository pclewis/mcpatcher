package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
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

        filesToAdd.add("com/pclewis/mcpatcher/mod/Shaders.class");
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
        }
    }
}
