package com.force.cliforce;


import org.testng.Assert;
import org.testng.annotations.Test;
public class UtilTest {

    @Test
    public void getVersionAsDouble(){
        Assert.assertEquals(21.0, Util.getApiVersionAsDouble());
        Assert.assertEquals("21.0", Util.getApiVersion());
    }


}
