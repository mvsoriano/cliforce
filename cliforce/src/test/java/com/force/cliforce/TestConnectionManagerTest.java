package com.force.cliforce;

import com.google.inject.Inject;
import org.testng.Assert;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;


@Guice(modules = TestModule.class)
public class TestConnectionManagerTest {


    @Inject
    TestConnectionManager testConnectionManager;

    @Test
    public void loadTestLoginProperties() {
        Assert.assertTrue(testConnectionManager.loadLogin());
    }

}
