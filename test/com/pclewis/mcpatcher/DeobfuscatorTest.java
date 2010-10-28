package com.pclewis.mcpatcher;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class DeobfuscatorTest {
    private Deobfuscator de;

    @Before
    public void setUp() throws Exception {
        de = new Deobfuscator(new Version("1.0"));
    }

    @Test
    public void testClasses() {
        de.addClass("oc");
        de.addClass("jo");
        de.addClass("ly");
        de.addClassName("oc", "Minecart");
        de.addClassName("oc", "entities.Minecart");
        de.addClassName("ly", "Block");

        Assert.assertEquals("oc", de.getClassName("oc"));
        Assert.assertEquals("oc", de.getClassName("Minecart"));
        Assert.assertEquals("oc", de.getClassName("entities.Minecart"));
        Assert.assertEquals("ly", de.getClassName("ly"));

        List<String> fn = de.getClassFriendlyNames("oc");
        Assert.assertTrue(fn.contains("Minecart"));
        Assert.assertTrue(fn.contains("entities.Minecart"));
        Assert.assertEquals(2, fn.size());
    }

    @Test
    public void testMethods() {
        de.addClass("cn");
        de.addClassName("cn", "GameState");
        de.addMember("cn", "g", "(IIII)V");
        de.addMemberName("cn", "g", "(IIII)V", "update");
        de.addClass("qq");
        de.addClassName("qq", "Test");
        de.addMember("qq", "g", "(IIII)V");
        de.addMemberName("qq", "g", "(IIII)V", "notupdate");
        de.addMember("qq", "q", "(IIII)V");
        de.addMemberName("qq", "q", "(IIII)V", "update");

        Assert.assertEquals( "g", de.getMemberName("GameState", "update", "(IIII)V") );
    }
}
