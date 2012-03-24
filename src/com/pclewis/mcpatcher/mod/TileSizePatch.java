package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

class TileSizePatch extends BytecodePatch {
    static final MethodRef getTileSize1 = new MethodRef(MCPatcherUtils.TILE_SIZE_CLASS, "getTileSize", "(LTextureFX;)L" + MCPatcherUtils.TILE_SIZE_CLASS.replace('.', '/') + ";");
    static final MethodRef getTileSize2 = new MethodRef(MCPatcherUtils.TILE_SIZE_CLASS, "getTileSize", "(LItemRenderer;)L" + MCPatcherUtils.TILE_SIZE_CLASS.replace('.', '/') + ";");
    
    private MethodRef tileSizeMethod = getTileSize1;
    
    protected Object value;
    protected String field;
    protected String type;
    protected String extra = "";

    TileSizePatch(Object value, String field) {
        this.value = value;
        this.field = field;
        if (field.startsWith("float_")) {
            type = "F";
        } else if (field.startsWith("double_")) {
            type = "D";
        } else if (field.startsWith("long_")) {
            type = "J";
        } else {
            type = "I";
        }
    }

    String prefix() {
        return "";
    }

    String suffix() {
        return "";
    }
    
    TileSizePatch setTileSizeMethod(MethodRef methodRef) {
        tileSizeMethod = methodRef;
        return this;
    }

    @Override
    public String getDescription() {
        return String.format("%s%s%s -> %s", extra, value.toString(), (type.equals("I") ? "" : type), field);
    }

    @Override
    public String getMatchExpression() {
        return buildExpression(
            BinaryRegex.capture(prefix()),
            push(value),
            BinaryRegex.capture(suffix())
        );
    }

    @Override
    public byte[] getReplacementBytes() throws IOException {
        return buildCode(
            getCaptureGroup(1),
            ALOAD_0,
            reference(INVOKESTATIC, tileSizeMethod),
            reference(GETFIELD, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, field, type)),
            getCaptureGroup(2)
        );
    }

    static class WhilePatch extends TileSizePatch {
        public WhilePatch(Object value, String field) {
            super(value, field);
            this.extra = "while i < ";
        }

        @Override
        public String suffix() {
            return buildExpression(IF_ICMPGE);
        }
    }

    static class IfGreaterPatch extends TileSizePatch {
        public IfGreaterPatch(Object value, String field) {
            super(value, field);
            this.extra = "if i > ";
        }

        @Override
        public String suffix() {
            return buildExpression(IF_ICMPGT);
        }
    }

    static class IfLessPatch extends TileSizePatch {
        public IfLessPatch(Object value, String field) {
            super(value, field);
            this.extra = "if i < ";
        }

        @Override
        public String suffix() {
            return buildExpression(IF_ICMPLT);
        }
    }

    static class BitMaskPatch extends TileSizePatch {
        public BitMaskPatch(Object value, String field) {
            super(value, field);
            this.extra = "& ";
        }

        @Override
        public String suffix() {
            return buildExpression(IAND);
        }
    }

    static class MultiplyPatch extends TileSizePatch {
        public MultiplyPatch(Object value, String field) {
            super(value, field);
            this.extra = "* ";
        }

        @Override
        public String suffix() {
            return buildExpression(IMUL);
        }
    }

    static class ModPatch extends TileSizePatch {
        public ModPatch(Object value, String field) {
            super(value, field);
            this.extra = "% ";
        }

        @Override
        public String suffix() {
            return buildExpression(IREM);
        }
    }

    static class DivPatch extends TileSizePatch {
        public DivPatch(Object value, String field) {
            super(value, field);
            this.extra = "/ ";
        }

        @Override
        public String suffix() {
            return buildExpression(IDIV, I2D);
        }
    }

    static class ArraySizePatch extends TileSizePatch {
        public ArraySizePatch(Object value, String field) {
            super(value, field);
            this.extra = "array size ";
        }

        @Override
        public String suffix() {
            return buildExpression(NEWARRAY);
        }
    }

    static class ArraySize2DPatch extends TileSizePatch {
        private int dimension;

        public ArraySize2DPatch(Object value, String field, int dimension) {
            super(value, field);
            this.dimension = dimension;
            this.extra = "array size " + dimension + "x";
        }

        @Override
        public String prefix() {
            return buildExpression(push(dimension));
        }

        @Override
        public String suffix() {
            return buildExpression(MULTIANEWARRAY);
        }
    }

    static class GetRGBPatch extends BytecodePatch {
        @Override
        public String getDescription() {
            return "getRGB(...16,16,...16) -> getRGB(...int_size,int_size,...int_size)";
        }

        @Override
        public String getMatchExpression() {
            return buildExpression(
                push(16),
                push(16),
                BinaryRegex.capture(buildExpression(
                    ALOAD_0,
                    GETFIELD, BinaryRegex.any(), BinaryRegex.any(),
                    ICONST_0
                )),
                push(16)
            );
        }

        @Override
        public byte[] getReplacementBytes() throws IOException {
            byte[] getField = buildCode(
                ALOAD_0,
                reference(INVOKESTATIC, getTileSize1),
                reference(GETFIELD, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "int_size", "I"))
            );
            return buildCode(
                getField,
                getField,
                getCaptureGroup(1),
                getField
            );
        }
    }

    static class ToolTexPatch extends BytecodePatch {
        @Override
        public String getDescription() {
            return "tool tex calculation";
        }

        @Override
        public String getMatchExpression() {
            return buildExpression(
                push(16),
                BinaryRegex.capture(BinaryRegex.subset(new byte[]{IREM, IDIV}, true)),
                push(16),
                IMUL,
                I2F,
                BinaryRegex.capture(BinaryRegex.or(
                    BinaryRegex.build(FCONST_0),
                    BinaryRegex.build(push(15.99F))
                )),
                FADD,
                push(256.0F)
            );
        }

        @Override
        public byte[] getReplacementBytes() throws IOException {
            byte[] offset = getCaptureGroup(2);
            if (offset[0] != FCONST_0) {
                offset = buildCode(
                    ALOAD_0,
                    reference(INVOKESTATIC, getTileSize2),
                    reference(GETFIELD, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "float_sizeMinus0_01", "F"))
                );
            }
            return buildCode(
                push(16),
                getCaptureGroup(1),
                ALOAD_0,
                reference(INVOKESTATIC, getTileSize2),
                reference(GETFIELD, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "int_size", "I")),
                IMUL,
                I2F,
                offset,
                FADD,
                ALOAD_0,
                reference(INVOKESTATIC, getTileSize2),
                reference(GETFIELD, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "float_size16", "F"))
            );
        }
    }

    static class ToolPixelTopPatch extends BytecodePatch {
        @Override
        public String getDescription() {
            return "tool pixel top";
        }

        @Override
        public String getMatchExpression() {
            return buildExpression(
                BinaryRegex.capture(BinaryRegex.build(
                    BytecodeMatcher.anyFLOAD,
                    BytecodeMatcher.anyFLOAD,
                    FMUL
                )),
                push(0.0625F),
                BinaryRegex.capture(BinaryRegex.build(
                    FADD,
                    BytecodeMatcher.anyFSTORE
                ))
            );
        }

        @Override
        public byte[] getReplacementBytes() throws IOException {
            return buildCode(
                getCaptureGroup(1),
                ALOAD_0,
                reference(INVOKESTATIC, getTileSize2),
                reference(GETFIELD, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "float_reciprocal", "F")),
                getCaptureGroup(2)
            );
        }
    }
}