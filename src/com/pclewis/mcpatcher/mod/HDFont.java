package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class HDFont extends Mod {
    private final boolean haveGetCharWidth;

    public HDFont(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.HD_FONT;
        author = "MCPatcher";
        description = "Provides support for higher resolution fonts.";
        version = "1.3";

        haveGetCharWidth = minecraftVersion.compareTo("1.2.4") >= 0;

        classMods.add(new FontRendererMod());

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.FONT_UTILS_CLASS));
    }

    private class FontRendererMod extends BaseMod.FontRendererMod {
        private final FieldRef charWidth = new FieldRef(getDeobfClass(), "charWidth", "[I");
        private final FieldRef charWidthf = new FieldRef(getDeobfClass(), "charWidthf", "[F");
        private final MethodRef getStringWidth = new MethodRef(getDeobfClass(), "getStringWidth", "(Ljava/lang/String;)I");

        FontRendererMod() {
            memberMappers.add(new MethodMapper("getStringWidth", "(Ljava/lang/String;)I"));

            patches.add(new AddFieldPatch("charWidthf", "[F"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "FontUtils.computeCharWidths on init";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ICONST_0,
                        ISTORE, BinaryRegex.capture(BinaryRegex.any()),
                        ILOAD, BinaryRegex.backReference(1),
                        push(256),
                        IF_ICMPGE, BinaryRegex.any(2),
                        BinaryRegex.any(1, 180),
                        IINC, BinaryRegex.backReference(1), 1,
                        GOTO, BinaryRegex.any(2)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    int registerOffset = getCaptureGroup(1)[0] - 8;
                    return buildCode(
                        ALOAD_0,
                        ALOAD_2,
                        ALOAD, 4 + registerOffset,
                        ALOAD, 7 + registerOffset,
                        ALOAD_0,
                        reference(GETFIELD, charWidth),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "computeCharWidths", "(Ljava/lang/String;Ljava/awt/image/BufferedImage;[I[I)[F")),
                        reference(PUTFIELD, charWidthf)
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "use charWidthf in glTranslatef call";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, charWidth),
                        BinaryRegex.capture(BinaryRegex.any(1, 4)),
                        IALOAD,
                        I2F
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, charWidthf),
                        getCaptureGroup(1),
                        FALOAD
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "use charWidthf in getStringWidth (init)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ICONST_0,
                        ISTORE_2
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        FCONST_0,
                        FSTORE_2
                    );
                }
            }.targetMethod(getStringWidth));

            if (haveGetCharWidth) {
                addStringWidthPatchesV2();
            } else {
                addStringWidthPatchesV1();
            }
        }

        private void addStringWidthPatchesV1() {
            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "use charWidthf in getStringWidth (loop)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ILOAD_2,
                        ALOAD_0,
                        reference(GETFIELD, charWidth),
                        BinaryRegex.capture(BytecodeMatcher.anyILOAD),
                        BIPUSH, 32,
                        IADD,
                        IALOAD,
                        IADD,
                        ISTORE_2
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        FLOAD_2,
                        ALOAD_0,
                        reference(GETFIELD, charWidthf),
                        getCaptureGroup(1),
                        BIPUSH, 32,
                        IADD,
                        FALOAD,
                        FADD,
                        FSTORE_2
                    );
                }
            }.targetMethod(getStringWidth));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "use charWidthf in getStringWidth (return value)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ILOAD_2,
                        IRETURN,
                        BinaryRegex.end()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        FLOAD_2,
                        reference(INVOKESTATIC, new MethodRef("java.lang.Math", "round", "(F)I")),
                        IRETURN
                    );
                }
            }.targetMethod(getStringWidth));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "getStringWidth int -> float";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // k += (k1 - j1) / 2 + 1;
                        ILOAD_2,
                        BinaryRegex.capture(BinaryRegex.any(0, 20)),
                        IADD,
                        ISTORE_2
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        FLOAD_2,
                        getCaptureGroup(1),
                        I2F,
                        FADD,
                        FSTORE_2
                    );
                }
            }.targetMethod(getStringWidth));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "4.0f -> charWidthf[32]";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.capture(BinaryRegex.build(
                            ALOAD_0,
                            DUP,
                            BytecodeMatcher.anyReference(GETFIELD)
                        )),
                        push(4.0f),
                        BinaryRegex.capture(BinaryRegex.build(
                            FADD,
                            BytecodeMatcher.anyReference(PUTFIELD)
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        ALOAD_0,
                        reference(GETFIELD, new FieldRef(getDeobfClass(), "charWidthf", "[F")),
                        push(32),
                        FALOAD,
                        getCaptureGroup(2)
                    );
                }
            });
        }

        private void addStringWidthPatchesV2() {
            final MethodRef getStringWidthf = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getStringWidthf", "(LFontRenderer;Ljava/lang/String;)F");

            memberMappers.add(new MethodMapper("getCharWidth", "(C)I"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "replace getStringWidth";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        BinaryRegex.begin(),
                        BinaryRegex.any(0, 1000),
                        BinaryRegex.end()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        ALOAD_1,
                        reference(INVOKESTATIC, getStringWidthf),
                        F2I,
                        IRETURN
                    );
                }
            }.targetMethod(getStringWidth));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "4.0f -> charWidthf[32]";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(4.0f),
                        FRETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, new FieldRef(getDeobfClass(), "charWidthf", "[F")),
                        push(32),
                        FALOAD,
                        FRETURN
                    );
                }
            });
        }
    }
}
