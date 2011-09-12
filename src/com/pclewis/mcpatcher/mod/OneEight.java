package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class OneEight extends Mod {
    public OneEight() {
        name = MCPatcherUtils.ONE_EIGHT;
        author = "MCPatcher";
        description = "(Experimental) Fixes various bugs in the 1.8 pre-release.";
        version = "1.0";
        defaultEnabled = false;

        classMods.add(new FurnaceMod());
        classMods.add(new EyeTextureMod("Spider", "/mob/spider_eyes.png"));
        classMods.add(new EyeTextureMod("Enderman", "/mob/enderman_eyes.png"));
    }

    private static class FurnaceMod extends ClassMod {
        public FurnaceMod() {
            classSignatures.add(new ConstSignature("Furnace"));
            classSignatures.add(new ConstSignature("BurnTime"));
            classSignatures.add(new ConstSignature("CookTime"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "fix crash when placing items into furnace";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.getDescriptor().equals("()V")) {
                        return buildExpression(
                            BinaryRegex.lookBehind(BinaryRegex.build(
                                ALOAD_0,
                                DUP,
                                GETFIELD, BinaryRegex.any(2),
                                ICONST_1,
                                ISUB,
                                PUTFIELD, BinaryRegex.any(2),
                                ALOAD_0,
                                GETFIELD, BinaryRegex.any(2)
                            ), true),
                            BinaryRegex.capture(BinaryRegex.any(1, 240)),
                            BinaryRegex.capture(BinaryRegex.build(
                                ILOAD_2,
                                IFEQ, BinaryRegex.any(2),
                                ALOAD_0,
                                INVOKEVIRTUAL, BinaryRegex.any(2),
                                RETURN
                            )),
                            BinaryRegex.end()
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        DUP,
                        IFNONNULL, branch("A"),
                        POP,
                        GOTO, branch("B"),

                        label("A"),
                        getCaptureGroup(1),

                        label("B"),
                        getCaptureGroup(2)
                    );
                }
            });
        }
    }

    private static class EyeTextureMod extends ClassMod {
        private String name;

        public EyeTextureMod(final String name, final String resource) {
            this.name = name;

            classSignatures.add(new ConstSignature(resource));

            if (false) {
                patches.add(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return String.format("fix %s eye texture overlay", name);
                    }

                    @Override
                    public String getMatchExpression(MethodInfo methodInfo) {
                        return buildExpression(
                            ICONST_1, /* GL_ONE */
                            ICONST_1, /* GL_ONE */
                            reference(methodInfo, INVOKESTATIC, new MethodRef("org.lwjgl.opengl.GL11", "glBlendFunc", "(II)V"))
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                        return buildCode(
                            push(methodInfo, 770), /* GL_SRC_ALPHA */
                            push(methodInfo, 771), /* GL_ONE_MINUS_SRC_ALPHA */
                            reference(methodInfo, INVOKESTATIC, new MethodRef("org.lwjgl.opengl.GL11", "glBlendFunc", "(II)V"))
                        );
                    }
                });
            }
        }

        @Override
        public String getDeobfClass() {
            return name;
        }
    }
}
