package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.BinaryRegex;
import com.pclewis.mcpatcher.BytecodePatch;
import com.pclewis.mcpatcher.FieldRef;
import javassist.bytecode.MethodInfo;

import java.io.IOException;

import static javassist.bytecode.Opcode.*;

class TileSizePatch extends BytecodePatch {
    protected Object value;
    protected String field;
    protected String type;
    protected String extra = "";

    public TileSizePatch(Object value, String field) {
        this.value = value;
        this.field = field;
        if (field.startsWith("float_")) {
            type = "F";
        } else if (field.startsWith("double_")) {
            type = "D";
        } else if (field.startsWith("long_")) {
            type = "L";
        } else {
            type = "I";
        }
    }

    public String prefix(MethodInfo methodInfo) {
        return "";
    }

    public String suffix(MethodInfo methodInfo) {
        return "";
    }

    @Override
    public String getDescription() {
        return String.format("%s%s%s -> %s", extra, value.toString(), (type.equals("I") ? "" : type), field);
    }

    @Override
    public String getMatchExpression(MethodInfo methodInfo) {
        return buildExpression(
            BinaryRegex.capture(prefix(methodInfo)),
            push(methodInfo, value),
            BinaryRegex.capture(suffix(methodInfo))
        );
    }

    @Override
    public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
        return buildCode(
            getCaptureGroup(1),
            reference(methodInfo, GETSTATIC, new FieldRef(HDTexture.class_TileSize, field, type)),
            getCaptureGroup(2)
        );
    }

    protected static class WhilePatch extends TileSizePatch {
        public WhilePatch(Object value, String field) {
            super(value, field);
            this.extra = "while i < ";
        }

        @Override
        public String suffix(MethodInfo methodInfo) {
            return buildExpression(IF_ICMPGE);
        }
    }

    protected static class IfGreaterPatch extends TileSizePatch {
        public IfGreaterPatch(Object value, String field) {
            super(value, field);
            this.extra = "if i > ";
        }

        @Override
        public String suffix(MethodInfo methodInfo) {
            return buildExpression(IF_ICMPGT);
        }
    }

    protected static class IfLessPatch extends TileSizePatch {
        public IfLessPatch(Object value, String field) {
            super(value, field);
            this.extra = "if i < ";
        }

        @Override
        public String suffix(MethodInfo methodInfo) {
            return buildExpression(IF_ICMPLT);
        }
    }

    protected static class BitMaskPatch extends TileSizePatch {
        public BitMaskPatch(Object value, String field) {
            super(value, field);
            this.extra = "& ";
        }

        @Override
        public String suffix(MethodInfo methodInfo) {
            return buildExpression(IAND);
        }
    }

    protected static class MultiplyPatch extends TileSizePatch {
        public MultiplyPatch(Object value, String field) {
            super(value, field);
            this.extra = "* ";
        }

        @Override
        public String suffix(MethodInfo methodInfo) {
            return buildExpression(IMUL);
        }
    }

    protected static class ModPatch extends TileSizePatch {
        public ModPatch(Object value, String field) {
            super(value, field);
            this.extra = "% ";
        }

        @Override
        public String suffix(MethodInfo methodInfo) {
            return buildExpression(IREM);
        }
    }

    protected static class DivPatch extends TileSizePatch {
        public DivPatch(Object value, String field) {
            super(value, field);
            this.extra = "/ ";
        }

        @Override
        public String suffix(MethodInfo methodInfo) {
            return buildExpression(IDIV, I2D);
        }
    }

    protected static class ArraySizePatch extends TileSizePatch {
        public ArraySizePatch(Object value, String field) {
            super(value, field);
            this.extra = "array size ";
        }

        @Override
        public String suffix(MethodInfo methodInfo) {
            return buildExpression(NEWARRAY);
        }
    }

    protected static class ArraySize2DPatch extends TileSizePatch {
        private int dimension;

        public ArraySize2DPatch(Object value, String field, int dimension) {
            super(value, field);
            this.dimension = dimension;
            this.extra = "array size " + dimension + "x";
        }

        @Override
        public String prefix(MethodInfo methodInfo) {
            return buildExpression(push(methodInfo, dimension));
        }

        @Override
        public String suffix(MethodInfo methodInfo) {
            return buildExpression(MULTIANEWARRAY);
        }
    }

    protected static class GetRGBPatch extends BytecodePatch {
        @Override
        public String getDescription() {
            return "getRGB(...16,16,...16) -> getRGB(...int_size,int_size,...int_size)";
        }

        @Override
        public String getMatchExpression(MethodInfo methodInfo) {
            return buildExpression(
                push(methodInfo, 16),
                push(methodInfo, 16),
                BinaryRegex.capture(buildExpression(
                    ALOAD_0,
                    GETFIELD, BinaryRegex.any(), BinaryRegex.any(),
                    ICONST_0
                )),
                push(methodInfo, 16)
            );
        }

        @Override
        public byte[] getReplacementBytes(MethodInfo methodInfo) throws IOException {
            byte[] getField = reference(methodInfo, GETSTATIC, new FieldRef(HDTexture.class_TileSize, "int_size", "I"));
            return buildCode(
                getField,
                getField,
                getCaptureGroup(1),
                getField
            );
        }
    }
}