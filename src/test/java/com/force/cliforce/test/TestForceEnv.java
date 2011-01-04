package com.force.cliforce.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

import com.force.cliforce.ForceEnv;


// WARNING: Will trash your ~/.force_url file

public class TestForceEnv {

	@After
	public void cleanUp() {
		System.clearProperty("force.url");
	}
	
	@Test
	public void getFromSysProperty() {

		Assume.assumeTrue(System.getenv("FORCE_URL")==null);

		System.setProperty("force.url",
				"force://loginserver.salesforce.com;user=santa@northpole.com;password=claus123");

		ForceEnv env = new ForceEnv();
		
		assertEquals(true,env.isValid());
		
		assertEquals("loginserver.salesforce.com", env.getHost());
		assertEquals("santa@northpole.com", env.getUser());
		assertEquals("claus123", env.getPassword());

	}

	@Test
	public void getNamedFromSysProperty() {

		Assume.assumeTrue(System.getenv("FORCE_ANAME_URL")==null);

		System.setProperty("force.aname.url",
				"force://loginserver.salesforce.com;user=santa@northpole.com;password=claus123");

		ForceEnv env = new ForceEnv("aname");
		
		assertEquals(true,env.isValid());
		
		assertEquals("loginserver.salesforce.com", env.getHost());
		assertEquals("santa@northpole.com", env.getUser());
		assertEquals("claus123", env.getPassword());

	}

	@Test
	public void extraSlash() {

		Assume.assumeTrue(System.getenv("FORCE_URL")==null);

		
		System.setProperty(
				"force.url",
				"force://loginserver.salesforce.com/;user=santa@northpole.com;password=claus123");

		ForceEnv env = new ForceEnv();

		assertEquals(true,env.isValid());

		assertEquals("loginserver.salesforce.com/", env.getHost());
		assertEquals("santa@northpole.com", env.getUser());
		assertEquals("claus123", env.getPassword());
	}

	@Test
	public void emptyPassword() {

		Assume.assumeTrue(System.getenv("FORCE_URL")==null);
		
		System.setProperty("force.url",
				"force://loginserver.salesforce.com;user=santa@northpole.com;password=");

		ForceEnv env = new ForceEnv();

		System.out.println(env.getMessage());
		assertEquals(false,env.isValid());
		assertNotNull(env.getMessage());

	}

	@Test
	public void emptyUser() {

		Assume.assumeTrue(System.getenv("FORCE_URL")==null);

		System.setProperty("force.url",
				"force://loginserver.salesforce.com;user=;password=claus123");

		ForceEnv env = new ForceEnv();
		System.out.println(env.getMessage());

		assertEquals(false,env.isValid());
		assertNotNull(env.getMessage());

	}

	@Test
	public void getFromFile() throws IOException {

		Assume.assumeTrue(System.getenv("FORCE_URL")==null);

		assertEquals(null, System.getProperty("force.url"));

		File f = new File(System.getProperty("user.home") + "/.force_url");

		try {
			FileWriter w = new FileWriter(f);
			w.write("force://loginserver.salesforce.com;user=santa@northpole.com;password=claus123");
			w.close();

			ForceEnv env = new ForceEnv();

			assertEquals(true,env.isValid());

			assertEquals("loginserver.salesforce.com", env.getHost());
			assertEquals("santa@northpole.com", env.getUser());
			assertEquals("claus123", env.getPassword());

		} finally {
			f.delete();
		}
	}

	@Test
	public void getNamedFromFile() throws IOException {

		Assume.assumeTrue(System.getenv("FORCE_NAMED_URL")==null);

		assertEquals(null, System.getProperty("force.named.url"));

		File f = new File(System.getProperty("user.home") + "/.force_named_url");

		try {
			FileWriter w = new FileWriter(f);
			w.write("force://loginserver.salesforce.com;user=santa@northpole.com;password=claus123");
			w.close();

			ForceEnv env = new ForceEnv("named");

			assertEquals(true,env.isValid());

			assertEquals("loginserver.salesforce.com", env.getHost());
			assertEquals("santa@northpole.com", env.getUser());
			assertEquals("claus123", env.getPassword());

		} finally {
			f.delete();
		}
	}

	
	@Test
	public void getFromEnv() {

		Assume.assumeTrue("force://loginserver.salesforce.com;user=santa@northpole.com;password=claus123".equals(System.getenv("FORCE_URL")));
		
		ForceEnv env = new ForceEnv();

		assertEquals(true,env.isValid());

		assertEquals("loginserver.salesforce.com", env.getHost());
		assertEquals("santa@northpole.com", env.getUser());
		assertEquals("claus123", env.getPassword());

	}

}
