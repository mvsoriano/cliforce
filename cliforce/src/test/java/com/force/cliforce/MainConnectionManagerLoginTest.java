package com.force.cliforce;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.springframework.web.client.HttpClientErrorException;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * 
 * Test login methods in MainConnectionManager class
 *
 * @author jeffrey.lai
 * @since javasdk-21.0.2-BETA
 */
public class MainConnectionManagerLoginTest {
	
	@Test
	public void testLoginSuccess() throws IOException {
		MainConnectionManager mgr = setupLoginTest("test.login.success");
		mgr.doLogin();
		Assert.assertNotNull(mgr.getVmForceClient().getFullToken(), "we expected login to succeed, but it failed");
	}
	
	@Test
	public void testLoginFail() throws IOException {
		MainConnectionManager mgr = setupLoginTest("test.login.fail");
		try {
			mgr.doLogin();
			Assert.fail("we expected login to fail");
		} catch (HttpClientErrorException e) {
			Assert.assertEquals(e.getMessage(), "403 Forbidden", "unexpected error message");
		}
	}
	
	private MainConnectionManager setupLoginTest(String properties) throws IOException {
		MainConnectionManager mgr = new MainConnectionManager();
		URL url = ClassLoader.getSystemResource(properties);
		Properties prop = new Properties();
		prop.load(url.openStream());
		mgr.setLogin(prop.getProperty("user"), prop.getProperty("password"), prop.getProperty("target"));
		return mgr;
	}
	
}
