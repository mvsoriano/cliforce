package com.force.cliforce;


import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class UtilTest {

    @Test
    public void getVersionAsDouble() {
        Assert.assertEquals(22.0, Util.getApiVersionAsDouble());
        Assert.assertEquals("22.0", Util.getApiVersion());
    }

    @Test
    public void testPropertiesReadWrite() throws IOException {
        Properties p = new Properties();
        p.setProperty("test", "123");
        String id = Long.toString(System.currentTimeMillis());
        File f = Util.getForcePropertiesFile(id);
        if(!f.getParentFile().exists()){
            f.getParentFile().mkdir();
        }
        f.deleteOnExit();
        p.store(new FileOutputStream(f), "utilTest");
        Properties read = new Properties();
        Util.readProperties(id, read);
        Assert.assertEquals(read.getProperty("test"), "123");
        Properties write = new Properties();
        write.setProperty("test", "234");
        Util.writeProperties(id, write);
        p = new Properties();
        p.load(new FileInputStream(f));
        Assert.assertEquals(p.getProperty("test"), "234");
    }


}
