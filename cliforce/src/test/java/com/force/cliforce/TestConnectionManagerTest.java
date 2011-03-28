package com.force.cliforce;

import com.google.inject.Inject;
import org.testng.Assert;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.IOException;


@Guice(modules = TestModule.class)
public class TestConnectionManagerTest {




    @Inject
    TestConnectionManager testConnectionManager;

    @Test
    public void loadTestLoginProperties() {
        try {
            testConnectionManager.loadLogin();
        } catch (IOException e) {
            Assert.fail("IOException while loading test login properties");
        }
    }

}
