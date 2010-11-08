package com.pclewis.mcpatcher;

import groovy.lang.MissingPropertyException;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BytecodeBuilderTest {
    ConstPool constPool;
    BytecodeBuilder builder;

    @Before
    public void init() {
        constPool = new ConstPool("test");
        builder = new BytecodeBuilder(constPool);
    }

    @Test
    public void testPush() {
        builder.push(123);
        builder.push(256);
        builder.push(90210);
        byte[] code = builder.getCode();
        byte[] expected = [
                (byte)Opcode.BIPUSH, 123,
                (byte)Opcode.SIPUSH, 0x01, 0x00,
                (byte)Opcode.LDC, (byte)ConstPoolUtils.find(constPool, 90210)
        ];
        Assert.assertArrayEquals(expected, code);
    }

    @Test(expected = MissingPropertyException.class)
    public void testInvalidOpcode() {
        builder.methodMissing("asdfas", null);
    }

    @Test
    public void testInvokeStatic() {
        int ci = ConstPoolUtils.addToPool(constPool, new MethodRef("com.example.Test", "function", "(III)V"));
        builder.invokestatic("com.example.Test.function");
        byte[] code = builder.getCode();
        byte[] expected = [
                (byte)Opcode.INVOKESTATIC, Util.b(ci, 1), Util.b(ci, 0)
        ];
        Assert.assertArrayEquals(expected, code);
    }

    @Test
    public void testBuildWithProperty() {
        byte[] code = BytecodeBuilder.build(constPool) {
            push 40
            dcmpg
        };
        byte[] expected = [
                (byte)Opcode.BIPUSH, 40,
                (byte)Opcode.DCMPG
        ];
        Assert.assertArrayEquals(expected, code);
    }
}
