package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

import static com.pclewis.mcpatcher.BinaryRegex.*;
import static com.pclewis.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BetterGrass extends Mod {
    private static final String field_MATRIX = "grassMatrix";
    private static final String fieldtype_MATRIX = "[[I";

    private boolean haveAO;

    public BetterGrass(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.BETTER_GRASS;
        author = "MCPatcher";
        description = "Improves the look of the sides of grass blocks. Inspired by MrMessiah's mod.";
        version = "1.0";
        defaultEnabled = false;

        haveAO = minecraftVersion.compareTo("Beta 1.6") >= 0;

        classMods.add(new MaterialMod());
        classMods.add(new BlockMod());
        classMods.add(new BlockGrassMod("Grass", 2, 3, 0));
        if (minecraftVersion.compareTo("Beta 1.9 Prerelease 1") >= 0) {
            classMods.add(new BlockGrassMod("Mycelium", 110, 77, 78));
        }
        classMods.add(new BaseMod.IBlockAccessMod().mapMaterial());
        classMods.add(new RenderBlocksMod());
    }

    private class MaterialMod extends ClassMod {
        MaterialMod() {
            classSignatures.add(new FixedBytecodeSignature(
                begin(),
                ALOAD_0,
                ICONST_1,
                PUTFIELD, any(2),
                ALOAD_0,
                ARETURN,
                end()
            ));

            classSignatures.add(new FixedBytecodeSignature(
                begin(),
                ICONST_0,
                IRETURN,
                end()
            ));

            classSignatures.add(new FixedBytecodeSignature(
                begin(),
                ICONST_1,
                IRETURN,
                end()
            ));

            classSignatures.add(new ConstSignature("CONFLICT @ ").negate(true));

            classSignatures.add(new ClassSignature() {
                @Override
                public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
                    int count = 0;
                    int flags = AccessFlag.PUBLIC | AccessFlag.STATIC | AccessFlag.FINAL;
                    String descriptor = "L" + getClassFile().getName() + ";";
                    for (Object o : getClassFile().getFields()) {
                        FieldInfo fieldInfo = (FieldInfo) o;
                        if ((fieldInfo.getAccessFlags() & flags) == flags &&
                            fieldInfo.getDescriptor().equals(descriptor)) {
                            count++;
                        }
                    }
                    return count > 10;
                }
            });

            memberMappers.add(new FieldMapper(null, new FieldRef(getDeobfClass(), "ground", "LMaterial;")).accessFlag(AccessFlag.STATIC, true));
        }
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
            memberMappers.add(new MethodMapper(new MethodRef(getDeobfClass(), "getBlockTexture", "(LIBlockAccess;IIII)I")));
        }
    }

    private class BlockGrassMod extends ClassMod {
        private byte[] material;
        private String blockName;

        BlockGrassMod(final String blockName, final int blockID, final int halfTextureID, final int fullTextureID) {
            this.blockName = blockName;

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD, capture(any()),
                        captureReference(GETSTATIC),
                        IF_ACMPEQ, any(2),
                        ALOAD, backReference(1),
                        captureReference(GETSTATIC),
                        IF_ACMPNE, any(2),
                        BIPUSH, 68,
                        IRETURN,
                        push(halfTextureID),
                        IRETURN
                    );
                }

                @Override
                public void afterMatch(ClassFile classFile, MethodInfo methodInfo) {
                    material = matcher.getCaptureGroup(1);
                }
            }
                .addXref(2, new FieldRef("Material", "snow", "LMaterial;"))
                .addXref(3, new FieldRef("Material", "builtSnow", "LMaterial;"))
                .setMethodName("getBlockTexture")
            );

            classSignatures.add(new FixedBytecodeSignature(
                BIPUSH, 9,
                IF_ICMPLT, any(2)
            ));

            classSignatures.add(new FixedBytecodeSignature(
                captureReference(INVOKEINTERFACE)
            ).addXref(1, new InterfaceMethodRef("IBlockAccess", "getBlockMaterial", "(III)LMaterial;")));

            final FieldRef array = new FieldRef(getDeobfClass(), field_MATRIX, fieldtype_MATRIX);

            patches.add(new AddFieldPatch(array, AccessFlag.PUBLIC | AccessFlag.STATIC));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "initialize " + field_MATRIX;
                }

                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().isConstructor()) {
                        return buildExpression(
                            RETURN
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    byte[] getArray = reference(GETSTATIC, array);
                    byte[] putArray = reference(PUTSTATIC, array);
                    return buildCode(
                        // if (grassMatrix != null)
                        getArray,
                        ACONST_NULL,
                        IF_ACMPNE, branch("A"),

                        // grassMatrix = new int[4][2];
                        ICONST_4,
                        ICONST_2,
                        reference(MULTIANEWARRAY, new ClassRef(fieldtype_MATRIX)), 2,
                        putArray,

                        // a[0][1] = -1;
                        getArray,
                        ICONST_0,
                        AALOAD,
                        ICONST_1,
                        ICONST_M1,
                        IASTORE,

                        // a[1][1] = 1;
                        getArray,
                        ICONST_1,
                        AALOAD,
                        ICONST_1,
                        ICONST_1,
                        IASTORE,

                        // a[2][0] = -1;
                        getArray,
                        ICONST_2,
                        AALOAD,
                        ICONST_0,
                        ICONST_M1,
                        IASTORE,

                        // a[3][0] = 1;
                        getArray,
                        ICONST_3,
                        AALOAD,
                        ICONST_0,
                        ICONST_1,
                        IASTORE,

                        label("A"),
                        RETURN
                    );
                }
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "check surrounding blocks in getBlockTexture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // return material != Material.snow && material != Material.builtSnow ? 3 : 68;
                        ALOAD, capture(any()),
                        capture(build(GETSTATIC, any(2))),
                        IF_ACMPEQ, any(2),
                        ALOAD, backReference(1),
                        capture(build(GETSTATIC, any(2))),
                        IF_ACMPNE, any(2),
                        BIPUSH, 68,
                        IRETURN,
                        push(halfTextureID),
                        IRETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    byte[] snow = reference(GETSTATIC, new FieldRef("Material", "snow", "LMaterial;"));
                    byte[] builtSnow = reference(GETSTATIC, new FieldRef("Material", "builtSnow", "LMaterial;"));
                    byte[] getBlockID = reference(INVOKEINTERFACE, new InterfaceMethodRef("IBlockAccess", "getBlockId", "(III)I"));
                    byte[] matrix = reference(GETSTATIC, new FieldRef("BlockGrass", field_MATRIX, fieldtype_MATRIX));

                    return buildCode(
                        // l -= 2;
                        IINC, 5, -2,

                        // if (material == Material.snow)
                        ALOAD, material,
                        snow,
                        IF_ACMPEQ, branch("A"),

                        // if (material == Material.builtSnow)
                        ALOAD, material,
                        builtSnow,
                        IF_ACMPEQ, branch("A"),

                        // if (iblockaccess.getBlockId(i+a[l][0], j-1, k+a[l][1]) == 2)
                        ALOAD, 1,
                        ILOAD, 2,
                        matrix,
                        ILOAD, 5,
                        AALOAD,
                        ICONST_0,
                        IALOAD,
                        IADD,
                        ILOAD, 3,
                        ICONST_1,
                        ISUB,
                        ILOAD, 4,
                        matrix,
                        ILOAD, 5,
                        AALOAD,
                        ICONST_1,
                        IALOAD,
                        IADD,
                        getBlockID,
                        push(blockID),
                        IF_ICMPEQ, branch("B"),

                        // return 3;
                        push(halfTextureID),
                        IRETURN,

                        // return 0;
                        label("B"),
                        push(fullTextureID),
                        IRETURN,

                        // material = iblockaccess.getBlockMaterial(i+a[l][0], j, k+a[l][1]);
                        label("A"),
                        ALOAD, 1,
                        ILOAD, 2,
                        matrix,
                        ILOAD, 5,
                        AALOAD,
                        ICONST_0,
                        IALOAD,
                        IADD,
                        ILOAD, 3,
                        ILOAD, 4,
                        matrix,
                        ILOAD, 5,
                        AALOAD,
                        ICONST_1,
                        IALOAD,
                        IADD,
                        reference(INVOKEINTERFACE, new InterfaceMethodRef("IBlockAccess", "getBlockMaterial", "(III)LMaterial;")),
                        ASTORE, material,

                        // if (material == Material.snow)
                        ALOAD, material,
                        snow,
                        IF_ACMPEQ, branch("C"),

                        // if (material == Material.builtSnow)
                        ALOAD, material,
                        builtSnow,
                        IF_ACMPEQ, branch("C"),

                        // return 68;
                        BIPUSH, 68,
                        IRETURN,

                        // return 66;
                        label("C"),
                        BIPUSH, 66,
                        IRETURN
                    );
                }
            });
        }

        @Override
        public String getDeobfClass() {
            return "Block" + blockName;
        }
    }

    private class RenderBlocksMod extends ClassMod {
        private int eastFace;
        private int westFace;
        private int northFace;
        private int southFace;

        private int redMultiplier;
        private int greenMultiplier;
        private int blueMultiplier;

        RenderBlocksMod() {
            classSignatures.add(new ConstSignature(0.02734375));
            classSignatures.add(new ConstSignature(0.0234375));
            classSignatures.add(new ConstSignature(0.03515625));
            classSignatures.add(new ConstSignature(0.03125));

            MethodRef renderStandardBlockWithColorMultiplier = new MethodRef(getDeobfClass(), "renderStandardBlockWithColorMultiplier", "(LBlock;IIIFFF)Z");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().matches("^\\(L[^;]+;IIIFFF\\)Z$")) {
                        return buildExpression(
                            push(0.5f),
                            FSTORE, any(),
                            push(1.0f),
                            FSTORE, capture(any()),
                            push(0.8f),
                            FSTORE, any(),
                            push(0.6f),
                            FSTORE, any(),

                            any(0, 20),

                            FLOAD, backReference(1),
                            FLOAD, 5,
                            FMUL,
                            FSTORE, capture(any()),

                            FLOAD, backReference(1),
                            FLOAD, 6,
                            FMUL,
                            FSTORE, capture(any()),

                            FLOAD, backReference(1),
                            FLOAD, 7,
                            FMUL,
                            FSTORE, capture(any())
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public void afterMatch(ClassFile classFile) {
                    redMultiplier = matcher.getCaptureGroup(2)[0] & 0xff;
                    greenMultiplier = matcher.getCaptureGroup(3)[0] & 0xff;
                    blueMultiplier = matcher.getCaptureGroup(4)[0] & 0xff;
                    Logger.log(Logger.LOG_CONST, "non-AO multipliers (R G B) = (%d %d %d)",
                        redMultiplier, greenMultiplier, blueMultiplier
                    );
                }
            }.setMethod(renderStandardBlockWithColorMultiplier));

            memberMappers.add(new FieldMapper(new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;")));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "if (getBlockTexture == 0) useBiomeColor = true (non-AO pre-1.8)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // Tessellator.setColorOpaque_F(f11 * f22, f14 * f22, f17 * f22);
                        capture(build(
                            ALOAD, capture(any()),
                            anyFLOAD,
                            FLOAD, capture(any()),
                            FMUL,
                            anyFLOAD,
                            FLOAD, backReference(3),
                            FMUL,
                            anyFLOAD,
                            FLOAD, backReference(3),
                            FMUL,
                            captureReference(INVOKEVIRTUAL)
                        )),

                        // int l = block.getBlockTexture(blockAccess, i, j, k, ?);
                        capture(build(
                            ALOAD_1,
                            ALOAD_0,
                            reference(GETFIELD, new FieldRef("RenderBlocks", "blockAccess", "LIBlockAccess;")),
                            ILOAD_2,
                            ILOAD_3,
                            ILOAD, 4,
                            any(1, 2),
                            reference(INVOKEVIRTUAL, new MethodRef("Block", "getBlockTexture", "(LIBlockAccess;IIII)I"))
                        )),
                        ISTORE, capture(any())
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        getCaptureGroup(5),
                        DUP,
                        ISTORE, getCaptureGroup(6),

                        IFNE, branch("A"),
                        ALOAD, getCaptureGroup(2),
                        FLOAD, redMultiplier,
                        FLOAD, getCaptureGroup(3),
                        FMUL,
                        FLOAD, greenMultiplier,
                        FLOAD, getCaptureGroup(3),
                        FMUL,
                        FLOAD, blueMultiplier,
                        FLOAD, getCaptureGroup(3),
                        FMUL,
                        getCaptureGroup(4),
                        GOTO, branch("B"),

                        label("A"),
                        getCaptureGroup(1),

                        label("B")
                    );
                }
            }.targetMethod(renderStandardBlockWithColorMultiplier));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "if (getBlockTexture == 0) useBiomeColor = true (non-AO post-1.8)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // int l = block.getBlockTexture(blockAccess, i, j, k, ?);
                        capture(build(
                            ALOAD_1,
                            ALOAD_0,
                            reference(GETFIELD, new FieldRef("RenderBlocks", "blockAccess", "LIBlockAccess;")),
                            ILOAD_2,
                            ILOAD_3,
                            ILOAD, 4,
                            any(1, 2),
                            reference(INVOKEVIRTUAL, new MethodRef("Block", "getBlockTexture", "(LIBlockAccess;IIII)I"))
                        )),
                        ISTORE, capture(any()),

                        capture(any(20, 40)),

                        // Tessellator.setColorOpaque_F(f11 * f22, f14 * f22, f17 * f22);
                        capture(build(
                            ALOAD, capture(any()),
                            anyFLOAD,
                            FLOAD, capture(any()),
                            FMUL,
                            anyFLOAD,
                            FLOAD, capture(any()),
                            FMUL,
                            anyFLOAD,
                            FLOAD, capture(any()),
                            FMUL,
                            captureReference(INVOKEVIRTUAL)
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        getCaptureGroup(1),
                        ISTORE, getCaptureGroup(2),

                        // Tessellator.setColorOpaque_F(f * f18, f1 * f21, f2 * f24);
                        ILOAD, getCaptureGroup(2),
                        IFNE, branch("A"),
                        ALOAD, getCaptureGroup(5),
                        FLOAD, 5,
                        FLOAD, getCaptureGroup(6),
                        FMUL,
                        FLOAD, 6,
                        FLOAD, getCaptureGroup(7),
                        FMUL,
                        FLOAD, 7,
                        FLOAD, getCaptureGroup(8),
                        FMUL,
                        getCaptureGroup(9),

                        label("A"),
                        getCaptureGroup(3),
                        getCaptureGroup(4)
                    );
                }
            }.targetMethod(renderStandardBlockWithColorMultiplier));

            if (haveAO) {
                setupAO();
            }
        }

        private void setupAO() {
            MethodRef renderStandardBlockWithAmbientOcclusion = new MethodRef(getDeobfClass(), "renderStandardBlockWithAmbientOcclusion", "(LBlock;IIIFFF)Z");

            classSignatures.add(new FixedBytecodeSignature(
                ICONST_0,
                or(
                    build(
                        // vanilla minecraft
                        DUP,
                        ISTORE, capture(any()), // southFace
                        DUP,
                        ISTORE, capture(any()), // northFace
                        DUP,
                        ISTORE, capture(any()), // westFace
                        DUP,
                        ISTORE, capture(any())  // eastFace
                    ),
                    build(
                        // ModLoader
                        ISTORE, capture(any()), // southFace
                        ICONST_0,
                        ISTORE, capture(any()), // northFace
                        ICONST_0,
                        ISTORE, capture(any()), // westFace
                        ICONST_0,
                        ISTORE, capture(any()), // eastFace
                        ICONST_0
                    )
                ),
                anyISTORE
            ) {
                public void afterMatch(ClassFile classFile) {
                    byte[][] m = new byte[][]{
                        matcher.getCaptureGroup(1),
                        matcher.getCaptureGroup(2),
                        matcher.getCaptureGroup(3),
                        matcher.getCaptureGroup(4),
                        matcher.getCaptureGroup(5),
                        matcher.getCaptureGroup(6),
                        matcher.getCaptureGroup(7),
                        matcher.getCaptureGroup(8),
                    };
                    southFace = m[(m[0] == null ? 4 : 0)][0] & 0xff;
                    northFace = m[(m[1] == null ? 5 : 1)][0] & 0xff;
                    westFace = m[(m[2] == null ? 6 : 2)][0] & 0xff;
                    eastFace = m[(m[3] == null ? 7 : 3)][0] & 0xff;
                    Logger.log(Logger.LOG_CONST, "AO faces (N S E W) = (%d %d %d %d)",
                        northFace, southFace, eastFace, westFace
                    );
                }
            }.setMethod(renderStandardBlockWithAmbientOcclusion));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "if (getBlockTexture == 0) useBiomeColor = true (AO)";
                }

                @Override
                public String getMatchExpression() {
                    return capture(build(
                        ICONST_0,
                        or(
                            build(
                                // vanilla minecraft
                                DUP,
                                ISTORE, southFace,
                                DUP,
                                ISTORE, northFace,
                                DUP,
                                ISTORE, westFace,
                                DUP,
                                ISTORE, eastFace
                            ),
                            build(
                                // ModLoader
                                ISTORE, southFace,
                                ICONST_0,
                                ISTORE, northFace,
                                ICONST_0,
                                ISTORE, westFace,
                                ICONST_0,
                                ISTORE, eastFace,
                                ICONST_0
                            )
                        ),
                        anyISTORE
                    ));
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    byte[] blockAccess = reference(GETFIELD, new FieldRef("RenderBlocks", "blockAccess", "LIBlockAccess;"));
                    byte[] getBlockTexture = reference(INVOKEVIRTUAL, new MethodRef("Block", "getBlockTexture", "(LIBlockAccess;IIII)I"));

                    return buildCode(
                        getCaptureGroup(1),

                        // if (block.getBlockTexture(blockAccess, i, j, k, 2) == 0) eastFace = true;
                        ALOAD_1,
                        ALOAD_0,
                        blockAccess,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        ICONST_2,
                        getBlockTexture,
                        IFNE, branch("east"),
                        ICONST_1,
                        ISTORE, eastFace,
                        label("east"),

                        // if (block.getBlockTexture(blockAccess, i, j, k, 3) == 0) westFace = true;
                        ALOAD_1,
                        ALOAD_0,
                        blockAccess,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        ICONST_3,
                        getBlockTexture,
                        IFNE, branch("west"),
                        ICONST_1,
                        ISTORE, westFace,
                        label("west"),

                        // if (block.getBlockTexture(blockAccess, i, j, k, 4) == 0) northFace = true;
                        ALOAD_1,
                        ALOAD_0,
                        blockAccess,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        ICONST_4,
                        getBlockTexture,
                        IFNE, branch("north"),
                        ICONST_1,
                        ISTORE, northFace,
                        label("north"),

                        // if (block.getBlockTexture(blockAccess, i, j, k, 5) == 0) southFace = true;
                        ALOAD_1,
                        ALOAD_0,
                        blockAccess,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        ICONST_5,
                        getBlockTexture,
                        IFNE, branch("south"),
                        ICONST_1,
                        ISTORE, southFace,
                        label("south")
                    );
                }
            }.targetMethod(renderStandardBlockWithAmbientOcclusion));
        }
    }
}
