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
        byte[] expected = new byte[] {
                (byte)Opcode.BIPUSH, 123,
                (byte)Opcode.SIPUSH, 0x01, 0x00,
                (byte)Opcode.LDC, (byte)ConstPoolUtils.find(constPool, 90210)
        };
        Assert.assertArrayEquals(expected, code);
    }

    @Test(expected = MissingPropertyException.class)
    public void testInvalidOpcode() {
        builder.methodMissing("asdfas", null);
    }
}
