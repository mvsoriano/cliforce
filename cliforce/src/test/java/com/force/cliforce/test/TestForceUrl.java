package com.force.cliforce.test;

import com.force.cliforce.ForceEnv;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class TestForceUrl {
	
	// order of values in each String[] is: force url, sfdc endpoint, user, password
	@DataProvider(name="validForceUrls")
	public String[][] getValidForceUrls() {
		return new String[][] {
				new String[] {"force://loginserver.salesforce.com;user=santa@northpole.com;password=claus123", "loginserver.salesforce.com", "santa@northpole.com", "claus123"},
				new String[] {"force://loginserver.salesforce.com/;user=santa@northpole.com;password=claus123", "loginserver.salesforce.com/", "santa@northpole.com", "claus123"},
				new String[] {"force://vmf01.t.salesforce.com;user=email@address.com;password=asdf", "vmf01.t.salesforce.com", "email@address.com", "asdf"},
				new String[] {"force://vmf01.t.salesforce.com;user=user@user.com;password=mountains4", "vmf01.t.salesforce.com", "user@user.com", "mountains4"},
				new String[] {"force://vmf01.t.salesforce.com;user=cal@bears.com;password=password", "vmf01.t.salesforce.com", "cal@bears.com", "password"},
				new String[] {"force://vmf01.t.salesforce.com;user=force@magic.com;password=force://", "vmf01.t.salesforce.com", "force@magic.com", "force://"},
//				uncomment line below once W-919227 is resolved
//				new String[] {"force://login.salesforce.com;user=email@address.com;password=password=", "login.salesforce.com", "password="},
//				uncomment line below once W-919232 is resolved
//				new String[] {"force://login.salesforce.com;user=email@address.com;password=123;456", "login.salesforce.com", "123;456"}
				};
		}

	@Test(dataProvider="validForceUrls")
	public void testValidForceUrls(String forceUrl, String sfdcEndpoint, String user, String password) {
		ForceEnv env = new ForceEnv(forceUrl, "unit test");
		assertTrue(env.isValid(), "we expected " + forceUrl + " to be valid, but it is invalid with error " + env.getMessage());
		assertEquals(env.getHost(), sfdcEndpoint, "unexpected host");
		assertEquals(env.getUser(), user, "unexpected user");
		assertEquals(env.getPassword(), password, "unexpected password");
		assertEquals(env.getUrl(), forceUrl, "unexpected url");
		assertEquals(env.getProtocol(), "force", "unexpected protocol");
		assertEquals(env.getConfigSource(), "unit test", "unexpected configSource");
	}
	
	// order of values in each String[] is: force url, error message
	@DataProvider(name="invalidForceUrls")
	public String[][] getInvalidForceUrls() {
		return new String[][] {
				new String[] {"force://loginserver.salesforce.com;user=santa@northpole.com;password=", "Password could not be found in URL"},
				new String[] {"force://loginserver.salesforce.com;user=;password=claus123", "User could not be found in URL"},
				new String[] {"force://vmf02.t.salesforce.com;jeff@test.com;123456", "Unable to successfully parse the URL"},
				new String[] {"http://vmf01.t.salesforce.com;user=email@address.com;password=hamlet", "Unsupported protocol: http. Only 'force' is supported as protocol."},
				new String[] {"", "Unable to successfully parse the URL"},
		};
	}
	
	@Test(dataProvider="invalidForceUrls")
	public void testInValidForceUrls(String forceUrl, String errMsg) {
		ForceEnv env = new ForceEnv(forceUrl, "unit test");
		assertFalse(env.isValid(), "we expected " + forceUrl + " to be invalid, but it was valid");
		assertEquals(env.getMessage(), errMsg, "unexpected error message");
	}

}
