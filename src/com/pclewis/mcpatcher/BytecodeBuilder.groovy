package com.pclewis.mcpatcher

import javassist.bytecode.ConstPool
import javassist.bytecode.Opcode

class BytecodeBuilder implements javassist.bytecode.Opcode {
    private ConstPool constPool;
    private final ByteArrayOutputStream codeStream = new ByteArrayOutputStream();

    public BytecodeBuilder(ConstPool constPool) {
        this.constPool = constPool;
    }

    public byte[] getCode() {
        return codeStream.toByteArray();
    }

    public void push(int value) {
        if(value == 0) {
			iconst_0;
		} else if (value == 1) {
			iconst_1;
		} else if(value <= Byte.MAX_VALUE) {
			bipush( (byte)value );
		} else if (value <= Short.MAX_VALUE) {
			sipush( (int)value );
		} else {
			int index = ConstPoolUtils.findOrAdd(constPool, value);
			cpush index;
		}
    }

    public void cpush(int i) {
        if(i>=Byte.MAX_VALUE)
			ldc_w i;
        else
            ldc( (byte)i );
    }

    def methodMissing(String name, args) {
        String op = name.toUpperCase();

        codeStream.write((byte)Opcode."$op");
        args.each { arg ->
            // crazy errors when codeStream is named code instead ...
            if(arg instanceof Byte) {
                codeStream.write(arg);
            } else if(arg instanceof Integer) {
                codeStream.write(Util.b(arg, 1))
                codeStream.write(Util.b(arg,0));
            }
        }
    }

    public static byte[] build(ConstPool cp, Closure c) {
        BytecodeBuilder builder = new BytecodeBuilder(cp);
        c.setDelegate(builder);
        c();
        return builder.getCode();
    }
}
