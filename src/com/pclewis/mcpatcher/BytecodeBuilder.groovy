package com.pclewis.mcpatcher

import java.util.regex.Matcher
import javassist.bytecode.ConstPool
import javassist.bytecode.Opcode

class BytecodeBuilder implements javassist.bytecode.Opcode {
    private final ConstPool constPool;
    private final ByteArrayOutputStream codeStream = new ByteArrayOutputStream();

    public BytecodeBuilder(ConstPool constPool) {
        this.constPool = constPool;
    }

    public byte[] getCode() {
        return codeStream.toByteArray();
    }

    public void push(Class c, Object v) {
        push(v.asType(c));
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

    public void push(float f) {
        cpush ConstPoolUtils.findOrAdd(constPool, f);
    }

    public void push(double d) {
        ldc2_w ConstPoolUtils.findOrAdd(constPool, d);
    }

    public void invokestatic(String method) {
        Matcher matcher = (method =~ /^(.+)\.([^.]+)$/);

        if(matcher.matches()) {
            invokestatic ConstPoolUtils.find(constPool, new MethodRef(matcher[0][1], matcher[0][2]));
        } else {
            throw new IllegalArgumentException();
        }
    }

    def methodMissing(String name, args) {
        String op = name.toUpperCase();

        codeStream.write((byte)Opcode."$op");
        Class nextType;
        args.each { arg ->
            // crazy errors when codeStream is named code instead ...
            if(nextType != null) {
                arg = arg.asType(nextType);
                nextType = null;
            }
            
            if(arg instanceof Class) {
                nextType = arg;
            } else if (arg instanceof Byte) {
                codeStream.write(arg);
            } else if(arg instanceof Integer) {
                codeStream.write(Util.b(arg, 1))
                codeStream.write(Util.b(arg, 0));
            }
        }
    }

    def propertyMissing(String name) {
        methodMissing(name, []);
    }

    public static byte[] build(ConstPool cp, Closure c) {
        BytecodeBuilder builder = new BytecodeBuilder(cp);
        c.delegate = builder;
        c.resolveStrategy = Closure.DELEGATE_ONLY;
        c();
        return builder.getCode();
    }
}
