package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;

import java.io.IOException;
import java.util.List;

import static javassist.bytecode.Opcode.*;

public class BetterGrass extends Mod {
    private static final String field_MATRIX = "grassMatrix";
    private static final String fieldtype_MATRIX = "[[I";

    public BetterGrass(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.BETTER_GRASS;
        author = "MCPatcher";
        description = "Improves the look of the sides of grass blocks. Inspired by MrMessiah's mod.";
        version = "1.0";
        defaultEnabled = false;

        classMods.add(new MaterialMod());
        classMods.add(new BlockMod());
        classMods.add(new BlockGrassMod("Grass", 2, 3, 0));
        if (minecraftVersion.compareTo(MinecraftVersion.parseVersion("Minecraft Beta 1.9 Prerelease 1")) >= 0) {
            classMods.add(new BlockGrassMod("Mycelium", 110, 77, 78));
        }
        classMods.add(new IBlockAccessMod());
        classMods.add(new RenderBlocksMod());
    }

    private static class MaterialMod extends ClassMod {
        public MaterialMod() {
            classSignatures.add(new FixedBytecodeSignature(
                BinaryRegex.begin(),
                ALOAD_0,
                ICONST_1,
                PUTFIELD, BinaryRegex.any(2),
                ALOAD_0,
                ARETURN,
                BinaryRegex.end()
            ));

            classSignatures.add(new FixedBytecodeSignature(
                BinaryRegex.begin(),
                ICONST_0,
                IRETURN,
                BinaryRegex.end()
            ));

            classSignatures.add(new FixedBytecodeSignature(
                BinaryRegex.begin(),
                ICONST_1,
                IRETURN,
                BinaryRegex.end()
            ));

            classSignatures.add(new ConstSignature("CONFLICT @ ").negate(true));

            classSignatures.add(new ClassSignature() {
                @Override
                public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
                    int count = 0;
                    int flags = AccessFlag.PUBLIC | AccessFlag.STATIC | AccessFlag.FINAL;
                    String descriptor = "L" + classFile.getName() + ";";
                    for (Object o : classFile.getFields()) {
                        FieldInfo fieldInfo = (FieldInfo) o;
                        if ((fieldInfo.getAccessFlags() & flags) == flags &&
                            fieldInfo.getDescriptor().equals(descriptor)) {
                            count++;
                        }
                    }
                    return count > 10;
                }
            });

            memberMappers.add(new FieldMapper(new String[]{null, "ground"}, "LMaterial;").accessFlag(AccessFlag.STATIC, true));
        }
    }

    private static class BlockMod extends ClassMod {
        public BlockMod() {
            classSignatures.add(new ConstSignature(" is already occupied by "));

            memberMappers.add(new MethodMapper("getBlockTexture", "(LIBlockAccess;IIII)I"));
        }
    }

    private class BlockGrassMod extends ClassMod {
        private byte[] material;
        private String blockName;

        public BlockGrassMod(final String blockName, final int blockID, final int halfTextureID, final int fullTextureID) {
            this.blockName = blockName;

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        ALOAD, BinaryRegex.capture(BinaryRegex.any()),
                        BytecodeMatcher.captureReference(GETSTATIC),
                        IF_ACMPEQ, BinaryRegex.any(2),
                        ALOAD, BinaryRegex.backReference(1),
                        BytecodeMatcher.captureReference(GETSTATIC),
                        IF_ACMPNE, BinaryRegex.any(2),
                        BIPUSH, 68,
                        IRETURN,
                        push(methodInfo, halfTextureID),
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
                IF_ICMPLT, BinaryRegex.any(2)
            ));

            classSignatures.add(new FixedBytecodeSignature(
                BytecodeMatcher.captureReference(INVOKEINTERFACE)
            ).addXref(1, new InterfaceMethodRef("IBlockAccess", "getBlockMaterial", "(III)LMaterial;")));

            patches.add(new AddFieldPatch(field_MATRIX, fieldtype_MATRIX, AccessFlag.PUBLIC | AccessFlag.STATIC));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "initialize " + field_MATRIX;
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            RETURN
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    FieldRef array = new FieldRef(getDeobfClass(), field_MATRIX, fieldtype_MATRIX);
                    byte[] getArray = reference(methodInfo, GETSTATIC, array);
                    byte[] putArray = reference(methodInfo, PUTSTATIC, array);
                    return buildCode(
                        // if (grassMatrix != null)
                        getArray,
                        ACONST_NULL,
                        IF_ACMPNE, branch("A"),

                        // grassMatrix = new int[4][2];
                        ICONST_4,
                        ICONST_2,
                        reference(methodInfo, MULTIANEWARRAY, new ClassRef(fieldtype_MATRIX)), 2,
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
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // return material != Material.snow && material != Material.builtSnow ? 3 : 68;
                        ALOAD, BinaryRegex.capture(BinaryRegex.any()),
                        BinaryRegex.capture(BinaryRegex.build(GETSTATIC, BinaryRegex.any(2))),
                        IF_ACMPEQ, BinaryRegex.any(2),
                        ALOAD, BinaryRegex.backReference(1),
                        BinaryRegex.capture(BinaryRegex.build(GETSTATIC, BinaryRegex.any(2))),
                        IF_ACMPNE, BinaryRegex.any(2),
                        BIPUSH, 68,
                        IRETURN,
                        push(methodInfo, halfTextureID),
                        IRETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    byte[] snow = reference(methodInfo, GETSTATIC, new FieldRef("Material", "snow", "LMaterial;"));
                    byte[] builtSnow = reference(methodInfo, GETSTATIC, new FieldRef("Material", "builtSnow", "LMaterial;"));
                    byte[] getBlockID = reference(methodInfo, INVOKEINTERFACE, new InterfaceMethodRef("IBlockAccess", "a", "(III)I"));
                    byte[] matrix = reference(methodInfo, GETSTATIC, new FieldRef("BlockGrass", field_MATRIX, fieldtype_MATRIX));

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
                        push(methodInfo, blockID),
                        IF_ICMPEQ, branch("B"),

                        // return 3;
                        push(methodInfo, halfTextureID),
                        IRETURN,

                        // return 0;
                        label("B"),
                        push(methodInfo, fullTextureID),
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
                        reference(methodInfo, INVOKEINTERFACE, new InterfaceMethodRef("IBlockAccess", "getBlockMaterial", "(III)LMaterial;")),
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

    private static class IBlockAccessMod extends ClassMod {
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

            memberMappers.add(new MethodMapper("getBlockMaterial", "(III)LMaterial;"));
            memberMappers.add(new MethodMapper(new String[]{"getBlockId", "getBlockMetadata"}, "(III)I"));
        }
    }

    private static class RenderBlocksMod extends ClassMod {
        private int eastFace;
        private int westFace;
        private int northFace;
        private int southFace;

        private int redMultiplier;
        private int greenMultiplier;
        private int blueMultiplier;

        public RenderBlocksMod() {
            classSignatures.add(new FixedBytecodeSignature(
                ICONST_0,
                DUP,
                ISTORE, BinaryRegex.capture(BinaryRegex.any()),
                DUP,
                ISTORE, BinaryRegex.capture(BinaryRegex.any()),
                DUP,
                ISTORE, BinaryRegex.capture(BinaryRegex.any()),
                DUP,
                ISTORE, BinaryRegex.capture(BinaryRegex.any()),
                BytecodeMatcher.anyISTORE
            ) {
                public void afterMatch(ClassFile classFile) {
                    southFace = matcher.getCaptureGroup(1)[0] & 0xff;
                    northFace = matcher.getCaptureGroup(2)[0] & 0xff;
                    westFace = matcher.getCaptureGroup(3)[0] & 0xff;
                    eastFace = matcher.getCaptureGroup(4)[0] & 0xff;
                    Logger.log(Logger.LOG_CONST, "AO faces (N S E W) = (%d %d %d %d)",
                        northFace, southFace, eastFace, westFace
                    );
                }
            }.setMethodName("renderStandardBlockWithAmbientOcclusion"));

            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.getDescriptor().matches("^\\(L[^;]+;IIIFFF\\)Z$")) {
                        return buildExpression(
                            push(methodInfo, 0.5f),
                            FSTORE, BinaryRegex.any(),
                            push(methodInfo, 1.0f),
                            FSTORE, BinaryRegex.capture(BinaryRegex.any()),
                            push(methodInfo, 0.8f),
                            FSTORE, BinaryRegex.any(),
                            push(methodInfo, 0.6f),
                            FSTORE, BinaryRegex.any(),

                            BinaryRegex.any(0, 20),

                            FLOAD, BinaryRegex.backReference(1),
                            FLOAD, 5,
                            FMUL,
                            FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                            FLOAD, BinaryRegex.backReference(1),
                            FLOAD, 6,
                            FMUL,
                            FSTORE, BinaryRegex.capture(BinaryRegex.any()),

                            FLOAD, BinaryRegex.backReference(1),
                            FLOAD, 7,
                            FMUL,
                            FSTORE, BinaryRegex.capture(BinaryRegex.any())
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
            }.setMethodName("renderStandardBlockWithColorMultiplier"));

            memberMappers.add(new FieldMapper("blockAccess", "LIBlockAccess;"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "if (getBlockTexture == 0) useBiomeColor = true (AO)";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return BinaryRegex.capture(BinaryRegex.build(
                        ICONST_0,
                        BinaryRegex.or(
                            BinaryRegex.build(
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
                            BinaryRegex.build(
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
                        BytecodeMatcher.anyISTORE
                    ));
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    byte[] blockAccess = reference(methodInfo, GETFIELD, new FieldRef("RenderBlocks", "blockAccess", "LIBlockAccess;"));
                    byte[] getBlockTexture = reference(methodInfo, INVOKEVIRTUAL, new MethodRef("Block", "getBlockTexture", "(LIBlockAccess;IIII)I"));

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
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "if (getBlockTexture == 0) useBiomeColor = true (non-AO pre-1.8)";
                }

                @Override
                public boolean filterMethod(MethodInfo methodInfo) {
                    MethodRef m = (MethodRef) map(new MethodRef("RenderBlocks", "renderStandardBlockWithColorMultiplier", "(LBlock;IIIFFF)Z"));
                    return m.getName().equals(methodInfo.getName()) && m.getType().equals(methodInfo.getDescriptor());
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // Tessellator.setColorOpaque_F(f11 * f22, f14 * f22, f17 * f22);
                        BinaryRegex.capture(BinaryRegex.build(
                            ALOAD, BinaryRegex.capture(BinaryRegex.any()),
                            BytecodeMatcher.anyFLOAD,
                            FLOAD, BinaryRegex.capture(BinaryRegex.any()),
                            FMUL,
                            BytecodeMatcher.anyFLOAD,
                            FLOAD, BinaryRegex.backReference(3),
                            FMUL,
                            BytecodeMatcher.anyFLOAD,
                            FLOAD, BinaryRegex.backReference(3),
                            FMUL,
                            BytecodeMatcher.captureReference(INVOKEVIRTUAL)
                        )),

                        // int l = block.getBlockTexture(blockAccess, i, j, k, ?);
                        BinaryRegex.capture(BinaryRegex.build(
                            ALOAD_1,
                            ALOAD_0,
                            reference(methodInfo, GETFIELD, new FieldRef("RenderBlocks", "blockAccess", "LIBlockAccess;")),
                            ILOAD_2,
                            ILOAD_3,
                            ILOAD, 4,
                            BinaryRegex.any(1, 2),
                            reference(methodInfo, INVOKEVIRTUAL, new MethodRef("Block", "getBlockTexture", "(LIBlockAccess;IIII)I"))
                        )),
                        ISTORE, BinaryRegex.capture(BinaryRegex.any())
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
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
            });

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "if (getBlockTexture == 0) useBiomeColor = true (non-AO post-1.8)";
                }

                @Override
                public boolean filterMethod(MethodInfo methodInfo) {
                    MethodRef m = (MethodRef) map(new MethodRef("RenderBlocks", "renderStandardBlockWithColorMultiplier", "(LBlock;IIIFFF)Z"));
                    return m.getName().equals(methodInfo.getName()) && m.getType().equals(methodInfo.getDescriptor());
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        // int l = block.getBlockTexture(blockAccess, i, j, k, ?);
                        BinaryRegex.capture(BinaryRegex.build(
                            ALOAD_1,
                            ALOAD_0,
                            reference(methodInfo, GETFIELD, new FieldRef("RenderBlocks", "blockAccess", "LIBlockAccess;")),
                            ILOAD_2,
                            ILOAD_3,
                            ILOAD, 4,
                            BinaryRegex.any(1, 2),
                            reference(methodInfo, INVOKEVIRTUAL, new MethodRef("Block", "getBlockTexture", "(LIBlockAccess;IIII)I"))
                        )),
                        ISTORE, BinaryRegex.capture(BinaryRegex.any()),

                        BinaryRegex.capture(BinaryRegex.any(20, 40)),

                        // Tessellator.setColorOpaque_F(f11 * f22, f14 * f22, f17 * f22);
                        BinaryRegex.capture(BinaryRegex.build(
                            ALOAD, BinaryRegex.capture(BinaryRegex.any()),
                            BytecodeMatcher.anyFLOAD,
                            FLOAD, BinaryRegex.capture(BinaryRegex.any()),
                            FMUL,
                            BytecodeMatcher.anyFLOAD,
                            FLOAD, BinaryRegex.capture(BinaryRegex.any()),
                            FMUL,
                            BytecodeMatcher.anyFLOAD,
                            FLOAD, BinaryRegex.capture(BinaryRegex.any()),
                            FMUL,
                            BytecodeMatcher.captureReference(INVOKEVIRTUAL)
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
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
            });
        }
    }
}
