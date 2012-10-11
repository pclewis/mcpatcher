package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import java.io.IOException;

import static com.pclewis.mcpatcher.BinaryRegex.*;
import static com.pclewis.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BetterGrass extends Mod {
    private static final String field_MATRIX = "grassMatrix";
    private static final String fieldtype_MATRIX = "[[I";

    final MethodRef getBlockTexture = new MethodRef("Block", "getBlockTexture", "(LIBlockAccess;IIII)I");

    private final boolean haveAO;

    public BetterGrass(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.BETTER_GRASS;
        author = "MCPatcher";
        description = "Improves the look of the sides of grass blocks. Inspired by MrMessiah's mod.";
        version = "1.1";
        defaultEnabled = false;

        haveAO = minecraftVersion.compareTo("Beta 1.6") >= 0;

        classMods.add(new BlockMod());
        classMods.add(new BlockGrassMod("Grass", 2, 3, 0));
        if (minecraftVersion.compareTo("Beta 1.9 Prerelease 1") >= 0) {
            classMods.add(new BlockGrassMod("Mycelium", 110, 77, 78));
        }
        classMods.add(new BaseMod.IBlockAccessMod().mapMaterial());
        classMods.add(new RenderBlocksMod());
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
            memberMappers.add(new MethodMapper(getBlockTexture));
        }
    }

    private class BlockGrassMod extends ClassMod {
        private final String blockName;

        BlockGrassMod(final String blockName, final int blockID, final int halfTextureID, final int fullTextureID) {
            this.blockName = blockName;

            final FieldRef snow = new FieldRef("Material", "snow", "LMaterial;");
            final FieldRef builtSnow = new FieldRef("Material", "builtSnow", "LMaterial;");
            final FieldRef grassMatrix = new FieldRef(getDeobfClass(), field_MATRIX, fieldtype_MATRIX);
            final InterfaceMethodRef getBlockMaterial = new InterfaceMethodRef("IBlockAccess", "getBlockMaterial", "(III)LMaterial;");
            final InterfaceMethodRef getBlockId = new InterfaceMethodRef("IBlockAccess", "getBlockId", "(III)I");

            final BytecodeSignature matchMaterial = new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // var6 = par1IBlockAccess.getBlockMaterial(...);
                        lookBehind(build(
                            captureReference(INVOKEINTERFACE),
                            ASTORE, capture(any()),
                            any(0, 20)
                        ), true),

                        // return var6 != Material.snow && var6 != Material.builtSnow ? halfTextureID : 68
                        ALOAD, backReference(2),
                        captureReference(GETSTATIC),
                        IF_ACMPEQ, any(2),
                        ALOAD, backReference(2),
                        captureReference(GETSTATIC),
                        IF_ACMPNE_or_IF_ACMPEQ, any(2),
                        or(build(push(68)), build(push(halfTextureID))),
                        or(build(IRETURN), build(GOTO, any(2))),
                        or(build(push(68)), build(push(halfTextureID))),
                        IRETURN
                    );
                }
            }
                .addXref(1, getBlockMaterial)
                .addXref(3, snow)
                .addXref(4, builtSnow)
                .setMethod(getBlockTexture)
            ;

            classSignatures.add(matchMaterial);

            patches.add(new AddFieldPatch(grassMatrix, AccessFlag.PUBLIC | AccessFlag.STATIC));

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
                    return buildCode(
                        // if (grassMatrix != null)
                        reference(GETSTATIC, grassMatrix),
                        IFNONNULL, branch("A"),

                        // grassMatrix = new int[4][2];
                        push(4),
                        push(2),
                        reference(MULTIANEWARRAY, new ClassRef(fieldtype_MATRIX)), 2,
                        reference(PUTSTATIC, grassMatrix),

                        // a[0][1] = -1;
                        reference(GETSTATIC, grassMatrix),
                        push(0),
                        AALOAD,
                        push(1),
                        push(-1),
                        IASTORE,

                        // a[1][1] = 1;
                        reference(GETSTATIC, grassMatrix),
                        push(1),
                        AALOAD,
                        push(1),
                        push(1),
                        IASTORE,

                        // a[2][0] = -1;
                        reference(GETSTATIC, grassMatrix),
                        push(2),
                        AALOAD,
                        push(0),
                        push(-1),
                        IASTORE,

                        // a[3][0] = 1;
                        reference(GETSTATIC, grassMatrix),
                        push(3),
                        AALOAD,
                        push(0),
                        push(1),
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
                    return matchMaterial.getMatchExpression();
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    int material = getCaptureGroup(2)[0] & 0xff;
                    return buildCode(
                        // l -= 2;
                        IINC, 5, -2,

                        // if (material == Material.snow)
                        ALOAD, material,
                        reference(GETSTATIC, snow),
                        IF_ACMPEQ, branch("A"),

                        // if (material == Material.builtSnow)
                        ALOAD, material,
                        reference(GETSTATIC, builtSnow),
                        IF_ACMPEQ, branch("A"),

                        // if (iblockaccess.getBlockId(i+a[l][0], j-1, k+a[l][1]) == 2)
                        ALOAD, 1,
                        ILOAD, 2,
                        reference(GETSTATIC, grassMatrix),
                        ILOAD, 5,
                        AALOAD,
                        push(0),
                        IALOAD,
                        IADD,
                        ILOAD, 3,
                        push(1),
                        ISUB,
                        ILOAD, 4,
                        reference(GETSTATIC, grassMatrix),
                        ILOAD, 5,
                        AALOAD,
                        push(1),
                        IALOAD,
                        IADD,
                        reference(INVOKEINTERFACE, getBlockId),
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
                        reference(GETSTATIC, grassMatrix),
                        ILOAD, 5,
                        AALOAD,
                        push(0),
                        IALOAD,
                        IADD,
                        ILOAD, 3,
                        ILOAD, 4,
                        reference(GETSTATIC, grassMatrix),
                        ILOAD, 5,
                        AALOAD,
                        push(1),
                        IALOAD,
                        IADD,
                        reference(INVOKEINTERFACE, getBlockMaterial),
                        ASTORE, material,

                        // if (material == Material.snow)
                        ALOAD, material,
                        reference(GETSTATIC, snow),
                        IF_ACMPEQ, branch("C"),

                        // if (material == Material.builtSnow)
                        ALOAD, material,
                        reference(GETSTATIC, builtSnow),
                        IF_ACMPEQ, branch("C"),

                        // return 68;
                        push(68),
                        IRETURN,

                        // return 66;
                        label("C"),
                        push(66),
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

    private class RenderBlocksMod extends BaseMod.RenderBlocksMod {
        private final FieldRef blockAccess = new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;");
        private final MethodRef renderStandardBlockWithColorMultiplier = new MethodRef(getDeobfClass(), "renderStandardBlockWithColorMultiplier", "(LBlock;IIIFFF)Z");
        private final MethodRef getBlockTexture = new MethodRef("Block", "getBlockTexture", "(LIBlockAccess;IIII)I");

        RenderBlocksMod() {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0.5f),
                        anyFSTORE,
                        push(1.0f),
                        anyFSTORE,
                        push(0.8f),
                        anyFSTORE,
                        push(0.6f),
                        anyFSTORE
                    );
                }
            }.setMethod(renderStandardBlockWithColorMultiplier));

            memberMappers.add(new FieldMapper(blockAccess));

            patches.add(new BytecodePatch() {
                private int redMultiplier;
                private int greenMultiplier;
                private int blueMultiplier;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
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
                        }

                        @Override
                        public boolean afterMatch() {
                            redMultiplier = matcher.getCaptureGroup(2)[0] & 0xff;
                            greenMultiplier = matcher.getCaptureGroup(3)[0] & 0xff;
                            blueMultiplier = matcher.getCaptureGroup(4)[0] & 0xff;
                            Logger.log(Logger.LOG_CONST, "non-AO multipliers (R G B) = (%d %d %d)",
                                redMultiplier, greenMultiplier, blueMultiplier
                            );
                            return true;
                        }
                    });
                }

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
                            reference(INVOKEVIRTUAL, new MethodRef("Block", "getBlockTexture", "(LIBlockAccess;IIII)I")),
                            ISTORE, capture(any())
                        )),

                        // Tessellator.setColorOpaque_F(f11 * f22, f14 * f22, f17 * f22);
                        capture(build(
                            nonGreedy(any(20, 60)),
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

                        // Tessellator.setColorOpaque_F(f * f18, f1 * f21, f2 * f24);
                        ILOAD, getCaptureGroup(2),
                        IFNE, branch("A"),
                        ALOAD, getCaptureGroup(4),
                        FLOAD, 5,
                        FLOAD, getCaptureGroup(5),
                        FMUL,
                        FLOAD, 6,
                        FLOAD, getCaptureGroup(6),
                        FMUL,
                        FLOAD, 7,
                        FLOAD, getCaptureGroup(7),
                        FMUL,
                        getCaptureGroup(8),

                        label("A"),
                        getCaptureGroup(3)
                    );
                }
            }.targetMethod(renderStandardBlockWithColorMultiplier));

            if (haveAO) {
                setupAO();
            }
        }

        private void setupAO() {
            final MethodRef renderStandardBlockWithAmbientOcclusion = new MethodRef(getDeobfClass(), "renderStandardBlockWithAmbientOcclusion", "(LBlock;IIIFFF)Z");

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0x0f000f)
                    );
                }
            }.setMethod(renderStandardBlockWithAmbientOcclusion));

            patches.add(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "if (getBlockTexture == 0) useBiomeColor = true (AO)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0),
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
                                push(0),
                                ISTORE, capture(any()), // northFace
                                push(0),
                                ISTORE, capture(any()), // westFace
                                push(0),
                                ISTORE, capture(any()), // eastFace
                                push(0)
                            )
                        ),
                        anyISTORE
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    byte[][] m = new byte[][]{
                        getCaptureGroup(1),
                        getCaptureGroup(2),
                        getCaptureGroup(3),
                        getCaptureGroup(4),
                        getCaptureGroup(5),
                        getCaptureGroup(6),
                        getCaptureGroup(7),
                        getCaptureGroup(8),
                    };
                    int southFace = m[(m[0] == null ? 4 : 0)][0] & 0xff;
                    int northFace = m[(m[1] == null ? 5 : 1)][0] & 0xff;
                    int westFace = m[(m[2] == null ? 6 : 2)][0] & 0xff;
                    int eastFace = m[(m[3] == null ? 7 : 3)][0] & 0xff;
                    Logger.log(Logger.LOG_BYTECODE, "AO faces (N S E W) = (%d %d %d %d)",
                        northFace, southFace, eastFace, westFace
                    );
                    return buildCode(
                        getCodeForFace(eastFace, 2, "east"),
                        getCodeForFace(westFace, 3, "west"),
                        getCodeForFace(northFace, 4, "north"),
                        getCodeForFace(southFace, 5, "south")
                    );
                }

                private Object[] getCodeForFace(int register, int face, String label) {
                    return new Object[]{
                        // if (block.getBlockTexture(blockAccess, i, j, k, face) == 0) xxxxFace = true;
                        ALOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, blockAccess),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        push(face),
                        reference(INVOKEVIRTUAL, getBlockTexture),
                        IFNE, branch(label),
                        ICONST_1,
                        registerLoadStore(ISTORE, register),
                        label(label)
                    };
                }
            }.targetMethod(renderStandardBlockWithAmbientOcclusion));
        }
    }
}
