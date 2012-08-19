package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class HDFont extends Mod {
    private final boolean haveAlternateFont;
    private final boolean haveFontHeight;
    private final boolean haveUnicode;
    private final boolean haveGetCharWidth;

    public HDFont(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.HD_FONT;
        author = "MCPatcher";
        description = "Provides support for higher resolution fonts.";
        version = "1.4";

        addDependency(BaseTexturePackMod.NAME);

        haveAlternateFont = minecraftVersion.compareTo("Beta 1.9 Prerelease 3") >= 0;
        haveFontHeight =  minecraftVersion.compareTo("Beta 1.9 Prerelease 6") >= 0;
        haveUnicode = minecraftVersion.compareTo("11w49a") >= 0 || minecraftVersion.compareTo("1.0.1") >= 0;
        haveGetCharWidth = minecraftVersion.compareTo("1.2.4") >= 0;

        classMods.add(new MinecraftMod());
        classMods.add(new FontRendererMod());
        classMods.add(new BaseMod.RenderEngineMod());
        classMods.add(new BaseMod.GameSettingsMod());

        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.FONT_UTILS_CLASS));
        filesToAdd.add(ClassMap.classNameToFilename(MCPatcherUtils.FONT_UTILS_CLASS + "$1"));
    }

    private class MinecraftMod extends BaseMod.MinecraftMod {
        MinecraftMod() {
            final FieldRef fontRenderer = new FieldRef(getDeobfClass(), "fontRenderer", "LFontRenderer;");
            final FieldRef alternateFontRenderer = new FieldRef(getDeobfClass(), "alternateFontRenderer", "LFontRenderer;");
            final FieldRef renderEngine = new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;");
            final FieldRef gameSettings = new FieldRef(getDeobfClass(), "gameSettings", "LGameSettings;");

            if (haveAlternateFont) {
                memberMappers.add(new FieldMapper(fontRenderer, alternateFontRenderer));
            } else {
                memberMappers.add(new FieldMapper(fontRenderer));
                memberMappers.add(new FieldMapper(alternateFontRenderer));
            }
            memberMappers.add(new FieldMapper(renderEngine));
            memberMappers.add(new FieldMapper(gameSettings));
        }
    }

    private class FontRendererMod extends BaseMod.FontRendererMod {
        private final FieldRef charWidth = new FieldRef(getDeobfClass(), "charWidth", "[I");
        private final FieldRef charWidthf = new FieldRef(getDeobfClass(), "charWidthf", "[F");
        private final FieldRef fontHeight = new FieldRef(getDeobfClass(), "FONT_HEIGHT", "I");
        private final FieldRef isUnicode = new FieldRef(getDeobfClass(), "isUnicode", "Z");
        private final MethodRef getStringWidth = new MethodRef(getDeobfClass(), "getStringWidth", "(Ljava/lang/String;)I");
        private final MethodRef constructor = new MethodRef(getDeobfClass(), "<init>", "(LGameSettings;Ljava/lang/String;LRenderEngine;" + (haveUnicode ? "Z" : "") + ")V");

        FontRendererMod() {
            if (haveFontHeight) {
                classSignatures.add(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        if (getMethodInfo().isConstructor()) {
                            return buildExpression(
                                ALOAD_0,
                                BinaryRegex.or(
                                    BinaryRegex.build(push(8)),
                                    BinaryRegex.build(push(9))
                                ),
                                BytecodeMatcher.captureReference(PUTFIELD)
                            );
                        } else {
                            return null;
                        }
                    }
                }
                    .setMethod(constructor)
                    .addXref(1, fontHeight)
                );
            } else {
                patches.add(new AddFieldPatch(fontHeight));
            }

            if (haveUnicode) {
                classSignatures.add(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        if (getMethodInfo().isConstructor()) {
                            return buildExpression(
                                ALOAD_0,
                                ILOAD, 4,
                                BytecodeMatcher.captureReference(PUTFIELD)
                            );
                        } else {
                            return null;
                        }
                    }
                }.addXref(1, isUnicode));

                patches.add(new MakeMemberPublicPatch(isUnicode));
            } else {
                patches.add(new AddFieldPatch(isUnicode));
            }

            memberMappers.add(new MethodMapper(getStringWidth));

            patches.add(new AddFieldPatch(charWidthf));

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

            patches.add(new AddMethodPatch(new MethodRef(getDeobfClass(), "initialize", "()V")) {
                MethodInfo constructor;

                @Override
                public void prePatch(ClassFile classFile) {
                    constructor = null;
                }

                @Override
                public byte[] generateMethod() {
                    getDescriptor();
                    CodeAttribute ca = constructor.getCodeAttribute();
                    getMethodInfo().setDescriptor(constructor.getDescriptor().replace("Z)", ")"));
                    maxStackSize = ca.getMaxStack();
                    numLocals = ca.getMaxLocals();
                    exceptionTable = ca.getExceptionTable();
                    byte[] code = ca.getCode().clone();
                    if (haveUnicode) {  // remove java.lang.Object<init> call
                        code[0] = ICONST_0;
                        code[1] = ISTORE;
                        code[2] = 4;
                    } else {
                        code[0] = NOP;
                        code[1] = NOP;
                        code[2] = NOP;
                    }
                    code[3] = NOP;
                    return code;
                }

                @Override
                public String getDescriptor() {
                    if (constructor == null) {
                        for (Object o : getClassFile().getMethods()) {
                            MethodInfo method = (MethodInfo) o;
                            if (method.isConstructor() &&
                                ((haveUnicode && method.getDescriptor().contains("Z)")) ||
                                    (!haveUnicode && !method.getDescriptor().equals("()V")))) {
                                constructor = method;
                                break;
                            }
                        }
                        if (constructor == null) {
                            throw new RuntimeException("could not find FontRenderer constructor");
                        }
                    }
                    return constructor.getDescriptor().replace("Z)", ")");
                }
            });

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
                        reference(GETFIELD, charWidthf),
                        push(32),
                        FALOAD,
                        getCaptureGroup(2)
                    );
                }
            });
        }

        private void addStringWidthPatchesV2() {
            final MethodRef getStringWidthf = new MethodRef(MCPatcherUtils.FONT_UTILS_CLASS, "getStringWidthf", "(LFontRenderer;Ljava/lang/String;)F");

            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getCharWidth", "(C)I")));

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
                        reference(GETFIELD, charWidthf),
                        push(32),
                        FALOAD,
                        FRETURN
                    );
                }
            });
        }
    }
}
