package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;
import javassist.bytecode.*;

import java.io.IOException;
import java.util.List;

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
        version = "0.1";

        allowedDirs.clear();
        allowedDirs.add("");

        classMods.add(new ColorizerGrassMod());
        classMods.add(new MaterialMod());
        classMods.add(new BlockMod());
        classMods.add(new BlockGrassMod());
        classMods.add(new IBlockAccessMod());
        classMods.add(new RenderBlocksMod());
    }

    private static class ColorizerGrassMod extends ClassMod {
        public ColorizerGrassMod() {
            classSignatures.add(new ConstSignature("/misc/grasscolor.png"));
        }
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
                    return ++count == 2;
                }
            });
        }
    }

    private class BlockMod extends ClassMod {
        public BlockMod() {
            classSignatures.add(new ConstSignature(" is already occupied by "));

            methodMappers.add(new MethodMapper("getBlockTexture", "(LIBlockAccess;IIII)I"));
        }
    }

    private class BlockGrassMod extends ClassMod {
        private byte[] getBlockMaterial;
        private byte[] material;

        public BlockGrassMod() {
            classSignatures.add(new FixedBytecodeSignature(
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
            ) {
                @Override
                public void afterMatch(ClassFile classFile) {
                    material = matcher.getCaptureGroup(1).clone();
                    int snow = ((matcher.getCaptureGroup(2)[1] << 8) | matcher.getCaptureGroup(2)[2]) & 0xffff;
                    int builtSnow = ((matcher.getCaptureGroup(3)[1] << 8) | matcher.getCaptureGroup(3)[2]) & 0xffff;
                    ConstPool cp = classFile.getConstPool();
                    classMap.addClassMap("Material", cp.getFieldrefClassName(snow));
                    classMap.addFieldMap("Material", "snow", cp.getFieldrefName(snow));
                    classMap.addFieldMap("Material", "builtSnow", cp.getFieldrefName(builtSnow));
                }
            }.setMethodName("getBlockTexture"));

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
                    ConstPool cp = classFile.getConstPool();
                    classMap.addClassMap("IBlockAccess", cp.getInterfaceMethodrefClassName(index));
                    classMap.addMethodMap("IBlockAccess", "getBlockMaterial", cp.getInterfaceMethodrefName(index));
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
                        ICONST_3,
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

                        // if (iblockaccess.getBlockId(i+a[l][0], j-1, k+a[l][1]) == 3)
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

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "disable biome color for snow";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return BinaryRegex.capture(BinaryRegex.build(
                        BinaryRegex.begin(),
                        BinaryRegex.any(0, 30),
                        DALOAD,
                        DSTORE, BinaryRegex.any(1, 12),
                        DALOAD,
                        DSTORE, BinaryRegex.any(1, 30),
                        BinaryRegex.end()
                    ));
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        // Material material = iblockaccess.getBlockMaterial(i, j + 1, k);
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ICONST_1,
                        IADD,
                        ILOAD, 4,
                        reference(methodInfo, INVOKEINTERFACE, new InterfaceMethodRef("IBlockAccess", "getBlockMaterial", "(III)LMaterial;")),
                        DUP,

                        // if (material == Material.snow)
                        reference(methodInfo, GETSTATIC, new FieldRef("Material", "snow", "LMaterial;")),
                        IF_ACMPEQ, branch("A"),

                        // if (material == Material.builtSnow)
                        reference(methodInfo, GETSTATIC, new FieldRef("Material", "builtSnow", "LMaterial;")),
                        IF_ACMPEQ, branch("B"),

                        getCaptureGroup(1),

                        // return 0xffffff;
                        label("A"),
                        POP,
                        label("B"),
                        push(methodInfo, 0xffffff),
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

    private class IBlockAccessMod extends ClassMod {
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

            methodMappers.add(new MethodMapper("getBlockMaterial", "(III)LMaterial;"));

            methodMappers.add(new MethodMapper(null, "(III)I") {
                private int count = 0;

                @Override
                public boolean match(MethodInfo methodInfo) {
                    if (!super.match(methodInfo)) {
                        return false;
                    }
                    switch (++count) {
                        case 1:
                            name = "getBlockId";
                            return true;

                        case 2:
                            name = "getBlockMetadata";
                            return true;

                        default:
                            return false;
                    }
                }
            });
        }
    }

    private class RenderBlocksMod extends ClassMod {
        private int eastFace;
        private int westFace;
        private int northFace;
        private int southFace;

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
                ISTORE, BinaryRegex.capture(BinaryRegex.any())
            ) {
                public void afterMatch(ClassFile classFile) {
                    southFace = matcher.getCaptureGroup(1)[0] & 0xff;
                    northFace = matcher.getCaptureGroup(2)[0] & 0xff;
                    westFace = matcher.getCaptureGroup(3)[0] & 0xff;
                    eastFace = matcher.getCaptureGroup(4)[0] & 0xff;
                    Logger.log(Logger.LOG_CONST, "faces (N S E W) = (%d %d %d %d)",
                        northFace, southFace, eastFace, westFace
                    );
                }
            });

            fieldMappers.add(new FieldMapper("blockAccess", "LIBlockAccess;"));

            patches.add(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "if (getBlockTexture == 3) ...";
                }

                @Override
                public String getMatchExpression(MethodInfo methodInfo) {
                    return BinaryRegex.capture(buildExpression(
                        ICONST_0,
                        DUP,
                        ISTORE, southFace,
                        DUP,
                        ISTORE, northFace,
                        DUP,
                        ISTORE, westFace,
                        DUP,
                        ISTORE, eastFace
                    ));
                }

                @Override
                public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
                    return buildCode(
                        getCaptureGroup(1),

                        ALOAD_1,
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef("RenderBlocks", "blockAccess", "LIBlockAccess;")),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        ICONST_2,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("Block", "getBlockTexture", "(LIBlockAccess;IIII)I")),
                        IFNE, branch("east"),
                        ICONST_1,
                        ISTORE, eastFace,
                        label("east"),

                        ALOAD_1,
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef("RenderBlocks", "blockAccess", "LIBlockAccess;")),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        ICONST_3,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("Block", "getBlockTexture", "(LIBlockAccess;IIII)I")),
                        IFNE, branch("west"),
                        ICONST_1,
                        ISTORE, westFace,
                        label("west"),

                        ALOAD_1,
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef("RenderBlocks", "blockAccess", "LIBlockAccess;")),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        ICONST_4,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("Block", "getBlockTexture", "(LIBlockAccess;IIII)I")),
                        IFNE, branch("north"),
                        ICONST_1,
                        ISTORE, northFace,
                        label("north"),

                        ALOAD_1,
                        ALOAD_0,
                        reference(methodInfo, GETFIELD, new FieldRef("RenderBlocks", "blockAccess", "LIBlockAccess;")),
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        ICONST_5,
                        reference(methodInfo, INVOKEVIRTUAL, new MethodRef("Block", "getBlockTexture", "(LIBlockAccess;IIII)I")),
                        IFNE, branch("south"),
                        ICONST_1,
                        ISTORE, southFace,
                        label("south")
                    );
                }
            });
        }
    }
}
