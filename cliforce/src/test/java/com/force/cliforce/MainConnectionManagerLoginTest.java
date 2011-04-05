package com.force.cliforce;

import com.google.inject.Guice;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Test login methods in MainConnectionManager class
 *
 * @author jeffrey.lai
 * @since javasdk-21.0.2-BETA
 */
public class MainConnectionManagerLoginTest {

    @Test
    public void testLoginSuccess() throws IOException {
        MainConnectionManager mgr = setupLoginTest(System.getProperty("positive.test.user.home"));
        mgr.doLogin();
        Assert.assertNotNull(mgr.getVmForceClient().getFullToken(), "we expected login to succeed, but it failed");
    }

    @Test
    public void testLoginFail() throws IOException {
        MainConnectionManager mgr = setupLoginTest(System.getProperty("negative.test.user.home"));
        try {
            mgr.doLogin();
            Assert.fail("we expected login to fail");
        } catch (HttpClientErrorException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid username, password, security token; or user locked out."), "unexpected error message");
            Assert.assertEquals(e.getStatusCode(), HttpStatus.FORBIDDEN, "unexpected status code");
        }
    }

    private MainConnectionManager setupLoginTest(String home) throws IOException {
        MainConnectionManager mgr = Guice.createInjector(new TestModule(home)).getInstance(MainConnectionManager.class);
        mgr.loadLogin();
        return mgr;
    }

}
