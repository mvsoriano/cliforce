package com.force.cliforce;


import org.testng.Assert;
import org.testng.annotations.Test;

public class LazyLoggerTest {


    @Test
    public void loadLogger() {
        LazyLogger log = new LazyLogger(this);
        log.get().error("foo");
        Assert.assertEquals(log.clazz, this.getClass());
        LazyLogger stat = new LazyLogger(LazyLoggerTest.class);
        Assert.assertEquals(stat.clazz, this.getClass());
    }
}
