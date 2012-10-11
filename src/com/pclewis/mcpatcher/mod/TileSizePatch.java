package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.BytecodePatch;
import com.pclewis.mcpatcher.FieldRef;
import com.pclewis.mcpatcher.MCPatcherUtils;

import java.io.IOException;

import static com.pclewis.mcpatcher.BinaryRegex.*;
import static com.pclewis.mcpatcher.BytecodeMatcher.anyFLOAD;
import static com.pclewis.mcpatcher.BytecodeMatcher.anyFSTORE;
import static javassist.bytecode.Opcode.*;

class TileSizePatch extends BytecodePatch {
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

    @Override
    public String getDescription() {
        return String.format("%s%s%s -> %s", extra, value.toString(), (type.equals("I") ? "" : type), field);
    }

    @Override
    public String getMatchExpression() {
        return buildExpression(
            capture(prefix()),
            push(value),
            capture(suffix())
        );
    }

    @Override
    public byte[] getReplacementBytes() throws IOException {
        return buildCode(
            getCaptureGroup(1),
            reference(GETSTATIC, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, field, type)),
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
                capture(buildExpression(
                    ALOAD_0,
                    GETFIELD, any(), any(),
                    ICONST_0
                )),
                push(16)
            );
        }

        @Override
        public byte[] getReplacementBytes() throws IOException {
            byte[] getField = reference(GETSTATIC, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "int_size", "I"));
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
                capture(subset(new byte[]{IREM, IDIV}, true)),
                push(16),
                IMUL,
                I2F,
                capture(or(
                    build(FCONST_0),
                    build(push(15.99F))
                )),
                FADD,
                push(256.0F)
            );
        }

        @Override
        public byte[] getReplacementBytes() throws IOException {
            byte[] offset = getCaptureGroup(2);
            if (offset[0] != FCONST_0) {
                offset = reference(GETSTATIC, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "float_sizeMinus0_01", "F"));
            }
            return buildCode(
                push(16),
                getCaptureGroup(1),
                reference(GETSTATIC, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "int_size", "I")),
                IMUL,
                I2F,
                offset,
                FADD,
                reference(GETSTATIC, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "float_size16", "F"))
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
                capture(build(
                    anyFLOAD,
                    anyFLOAD,
                    FMUL
                )),
                push(0.0625F),
                capture(build(
                    FADD,
                    anyFSTORE
                ))
            );
        }

        @Override
        public byte[] getReplacementBytes() throws IOException {
            return buildCode(
                getCaptureGroup(1),
                reference(GETSTATIC, new FieldRef(MCPatcherUtils.TILE_SIZE_CLASS, "float_reciprocal", "F")),
                getCaptureGroup(2)
            );
        }
    }
}