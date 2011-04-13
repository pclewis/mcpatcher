package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.*;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

public class BetterGrass extends Mod {
    private static final String field_MATRIX = "grassMatrix";
    private static final String fieldtype_MATRIX = "[[I";

    private String colorMultiplierType;
    private String colorMultiplierTypeStatic;
    private byte[] colorMultiplierCode;

    public BetterGrass() {
        name = "Better Grass";
        author = "MCPatcher";
        description = "Improves the look of the sides of grass blocks. Based on MrMessiah's mod.";
        version = "1.0";

        allowedDirs.clear();
        allowedDirs.add("");

        classMods.add(new ColorizerGrassMod());
        classMods.add(new MaterialMod());
        classMods.add(new BlockGrassMod());
        classMods.add(new BlockDirtMod());
    }

    private static class ColorizerGrassMod extends ClassMod {
        public ColorizerGrassMod() {
            classSignatures.add(new ConstSignature("/misc/grasscolor.png"));
        }
    }

    private static class MaterialMod extends ClassMod {
        public MaterialMod() {
            classSignatures.add(new FixedBytecodeSignature(
                "^",
                ALOAD_0,
                ICONST_1,
                PUTFIELD, BinaryRegex.any(2),
                ALOAD_0,
                ARETURN,
                "$"
            ));
            classSignatures.add(new ConstSignature("CONFLICT @ ").negate(true));

            fieldMappers.add(new FieldMapper("ground", "LMaterial;") {
                int count = 0;

                @Override
                public boolean match(FieldInfo fieldInfo) {
                    if (!super.match(fieldInfo)) {
                        return false;
                    }
                    if ((fieldInfo.getAccessFlags() & AccessFlag.STATIC) == 0) {
                        return false;
                    }
                    count++;
                    return count == 2;
                }
            });
        }
    }

    private class BlockGrassMod extends ClassMod {
        private byte[] getBlockMaterial;

        public BlockGrassMod() {
            classSignatures.add(new FixedBytecodeSignature(
                BIPUSH, 68,
                IRETURN,
                ICONST_3,
                IRETURN
            ).setMethodName("getBlockTexture"));

            classSignatures.add(new FixedBytecodeSignature(
                BIPUSH, 9,
                IF_ICMPLT, BinaryRegex.any(2)
            ));

            classSignatures.add(new FixedBytecodeSignature(
                BinaryRegex.capture(BinaryRegex.build(INVOKEINTERFACE, BinaryRegex.any(4)))
            ) {
                @Override
                public void afterMatch(ClassFile classFile) {
                    getBlockMaterial = matcher.getCaptureGroup(1).clone();
                    int index = (getBlockMaterial[1] << 8) | getBlockMaterial[2];
                    Logger.log(Logger.LOG_CONST, "getBlockMaterial ref %d", index);
                }
            });

            classSignatures.add(new FixedBytecodeSignature(
                DALOAD,
                DSTORE, BinaryRegex.any(1, 12),
                DALOAD,
                DSTORE, BinaryRegex.any()
            ) {
                @Override
                public void afterMatch(ClassFile classFile, MethodInfo methodInfo) {
                    colorMultiplierType = methodInfo.getDescriptor();
                    colorMultiplierTypeStatic = colorMultiplierType.replaceFirst("\\(", "(I");
                    colorMultiplierCode = methodInfo.getCodeAttribute().getCode().clone();
                    Logger.log(Logger.LOG_CONST, "colorMultiplier %s %d bytes",
                        colorMultiplierType, colorMultiplierCode.length
                    );
                }
            }.setMethodName("colorMultiplier"));

            patches.add(new AddFieldPatch(field_MATRIX, fieldtype_MATRIX, AccessFlag.PUBLIC | AccessFlag.STATIC));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "blockIndexInTexture 3 -> 0";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    if (methodInfo.isConstructor()) {
                        return buildExpression(
                            ICONST_3
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        ICONST_0
                    );
                }
            });

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
                    FieldRef array = new FieldRef("BlockGrass", field_MATRIX, fieldtype_MATRIX);
                    byte[] getArray = reference(methodInfo, GETSTATIC, array);
                    byte[] putArray = reference(methodInfo, PUTSTATIC, array);
                    return buildCode(
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
                        ICONST_3,
                        IRETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    String type = methodInfo.getDescriptor().replaceFirst("^\\(L", "").replaceFirst(";.*", "");
                    byte[] material = getCaptureGroup(1);
                    byte[] snow = getCaptureGroup(2);
                    byte[] builtSnow = getCaptureGroup(3);
                    byte[] getBlockID = reference(methodInfo, INVOKEINTERFACE, new InterfaceMethodRef(type, "a", "(III)I"));
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

                        // if (nm1.getBlockId(i+a[l][0], j-1, k+a[l][1]) == 3)
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
                        ICONST_2,
                        IF_ICMPEQ, branch("B"),

                        // return 3;
                        ICONST_3,
                        IRETURN,

                        // return 0;
                        label("B"),
                        ICONST_0,
                        IRETURN,

                        // material = nm1.getBlockMaterial(i+a[l][0], j, k+a[l][1]);
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
                        getBlockMaterial,
                        ASTORE, material,

                        // if (material == Material.snow)
                        ALOAD, material,
                        snow,
                        IF_ACMPEQ, branch("C"),

                        // if (material == Material.builtSnow)
                        ALOAD, material,
                        snow,
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

            patches.add(new AddMethodPatch("colorMultiplierStatic", null) {
                @Override
                protected void prePatch(ClassFile classFile) {
                    type = colorMultiplierTypeStatic;
                }

                @Override
                public byte[] generateMethod(ClassFile classFile, MethodInfo methodInfo) throws BadBytecode, IOException {
                    methodInfo.setAccessFlags(methodInfo.getAccessFlags() | AccessFlag.STATIC);
                    return colorMultiplierCode;
                }
            });
        }
    }

    private class BlockDirtMod extends ClassMod {
        public BlockDirtMod() {
            classSignatures.add(new BytecodeSignature() {
                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return buildExpression(
                        "^",
                        ALOAD_0,
                        ILOAD_1,
                        ILOAD_2,
                        reference(methodInfo, GETSTATIC, new FieldRef("jh", "b", "Ljh;")),
                        INVOKESPECIAL, BinaryRegex.any(2),
                        RETURN,
                        "$"
                    );
                }
            });

            patches.add(new AddMethodPatch(null, null) {
                @Override
                protected void prePatch(ClassFile classFile) {
                    type = colorMultiplierType;
                    name = map(new MethodRef("BlockGrass", "colorMultiplier", type)).getName();
                }

                @Override
                public byte[] generateMethod(ClassFile classFile, MethodInfo methodInfo) throws BadBytecode, IOException {
                    return buildCode(
                        ICONST_0,
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        reference(methodInfo, INVOKESTATIC, new MethodRef("BlockGrass", "colorMultiplierStatic", colorMultiplierTypeStatic)),
                        IRETURN
                    );
                }
            });
        }
    }
}
