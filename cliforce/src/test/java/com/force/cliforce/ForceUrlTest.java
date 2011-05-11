/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.force.cliforce;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Tests for validation of force urls
 * @author jeffrey.lai
 * @since javasdk-21.0.2-BETA
 */
public class ForceUrlTest {
	
	// order of values in each String[] is: force url, sfdc endpoint, user, password
	@DataProvider(name="validForceUrls")
	public String[][] getValidForceUrls() {
		return new String[][] {
				new String[] {"force://loginserver.salesforce.com;user=santa@northpole.com;password=claus123", "loginserver.salesforce.com", "santa@northpole.com", "claus123", null, null},
				new String[] {"force://loginserver.salesforce.com;user=santa@northpole.com;password=claus123;oauth_key=foo;oauth_secret=bar", "loginserver.salesforce.com", "santa@northpole.com", "claus123", "foo", "bar"},
				new String[] {"force://loginserver.salesforce.com/;user=santa@northpole.com;password=claus123", "loginserver.salesforce.com/", "santa@northpole.com", "claus123", null, null},
				new String[] {"force://login.salesforce.com;user=email@address.com;password=asdf", "login.salesforce.com", "email@address.com", "asdf", null, null},
				new String[] {"force://login.salesforce.com;user=user@user.com;password=mountains4", "login.salesforce.com", "user@user.com", "mountains4", null, null},
				new String[] {"force://login.salesforce.com;user=cal@bears.com;password=password", "login.salesforce.com", "cal@bears.com", "password", null, null},
				new String[] {"force://login.salesforce.com;user=force@magic.com;password=force://", "login.salesforce.com", "force@magic.com", "force://", null, null},
//				uncomment line below once W-919227 is resolved
//				new String[] {"force://login.salesforce.com;user=email@address.com;password=password=", "login.salesforce.com", "password="},
//				uncomment line below once W-919232 is resolved
//				new String[] {"force://login.salesforce.com;user=email@address.com;password=123;456", "login.salesforce.com", "123;456"}
				};
		}

	@Test(dataProvider="validForceUrls")
	public void testValidForceUrls(String forceUrl, String sfdcEndpoint, String user, String password, String oauthKey, String oauthSecret) {
		ForceEnv env = new ForceEnv(forceUrl, "unit test");
		assertTrue(env.isValid(), "we expected " + forceUrl + " to be valid, but it is invalid with error " + env.getMessage());
		assertEquals(env.getHost(), sfdcEndpoint, "unexpected host");
		assertEquals(env.getUser(), user, "unexpected user");
		assertEquals(env.getPassword(), password, "unexpected password");
		assertEquals(env.getUrl(), forceUrl, "unexpected url");
		assertEquals(env.getProtocol(), "force", "unexpected protocol");
		assertEquals(env.getConfigSource(), "unit test", "unexpected configSource");
		assertEquals(env.getOauthKey(), oauthKey, "unexpected oauthKey");
		assertEquals(env.getOauthSecret(), oauthSecret, "unexpected oauthSecret");
	}
	
	// order of values in each String[] is: force url, error message
	@DataProvider(name="invalidForceUrls")
	public String[][] getInvalidForceUrls() {
		return new String[][] {
				new String[] {"force://loginserver.salesforce.com;user=santa@northpole.com;password=", "Password could not be found in URL"},
				new String[] {"force://loginserver.salesforce.com;user=santa@northpole.com;password=pass;oauth_key=foo", "Both oauth_key and oauth_secret are required"},
				new String[] {"force://loginserver.salesforce.com;user=;password=claus123", "User could not be found in URL"},
				new String[] {"force://vmf02.t.salesforce.com;jeff@test.com;123456", "User could not be found in URL"},
				new String[] {"http://login.salesforce.com;user=email@address.com;password=hamlet", "Unsupported protocol: http. Only 'force' is supported as protocol."},
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
