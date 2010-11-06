package com.pclewis.mcpatcher;

import org.junit.Assert;
import org.junit.Test;

public class VersionTest {
    @Test public void testCreateMajor() {
        Version v = new Version("1");
        Assert.assertEquals("1", v.toString());
        Assert.assertEquals(null, v.variant );
        Assert.assertEquals( 1, v.major   );
        Assert.assertEquals(-1, v.minor   );
        Assert.assertEquals(-1, v.build   );
        Assert.assertEquals(-1, v.revision);
    }

    @Test public void testCreateMinor() {
        Version v = new Version("1.4");
        Assert.assertEquals("1.4", v.toString());
        Assert.assertEquals(null, v.variant );
        Assert.assertEquals( 1, v.major   );
        Assert.assertEquals( 4, v.minor   );
        Assert.assertEquals(-1, v.build   );
        Assert.assertEquals(-1, v.revision);
    }

    @Test public void testCreateBuild() {
        Version v = new Version("1.4.2");
        Assert.assertEquals("1.4.2", v.toString());
        Assert.assertEquals(null, v.variant );
        Assert.assertEquals( 1, v.major   );
        Assert.assertEquals( 4, v.minor   );
        Assert.assertEquals( 2, v.build   );
        Assert.assertEquals(-1, v.revision);
    }

    @Test public void testCreateRev() {
        Version v = new Version("1.4.2_01");
        Assert.assertEquals("1.4.2_1", v.toString()); // note 0 dropped
        Assert.assertEquals(null, v.variant);
        Assert.assertEquals(1, v.major   );
        Assert.assertEquals(4, v.minor   );
        Assert.assertEquals(2, v.build   );
        Assert.assertEquals(1, v.revision);
    }

    @Test public void testCreateVariant() {
        Version v = new Version("Alpha 1.4");
        Assert.assertEquals("alpha 1.4", v.toString());
        Assert.assertEquals("alpha", v.variant);
        Assert.assertEquals(1, v.major   );
        Assert.assertEquals(4, v.minor   );
        Assert.assertEquals(-1, v.build   );
        Assert.assertEquals(-1, v.revision);
    }

    @Test public void testCompare() {
        Assert.assertTrue(new Version("1.2").compareTo("1.3") < 0);
        Assert.assertTrue(new Version("1.2").compareTo("1.2.1") < 0);
        Assert.assertTrue(new Version("1.2.1").compareTo("1.2.1_01") < 0);
        Assert.assertTrue(new Version("1.2.1").compareTo("Server 1.2.1") < 0);
        Assert.assertTrue(new Version("Alpha 1.2.1").compareTo("Server 1.2.1") < 0);
        Assert.assertTrue(new Version("Server 4.8.12").compareTo("Alpha 9.18.27") > 0);
        Assert.assertTrue(new Version("Alpha 1.2.16").compareTo("Alpha 1.2.3") > 0);
        Assert.assertEquals(0, new Version("1.2.1_01").compareTo("1.2.1_01"));
    }

    @Test public void testV() {
        Assert.assertEquals("alpha 1.2.1_1", new Version("Alpha v1.2.1_01").toString());
        Assert.assertEquals("server 0.2.3", new Version("server version 0.2.3").toString());
    }
}
