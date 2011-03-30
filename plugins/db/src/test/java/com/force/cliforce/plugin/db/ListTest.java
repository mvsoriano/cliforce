package com.force.cliforce.plugin.db;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.force.cliforce.Plugin;
import com.force.cliforce.ResourceException;
import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestModule;
import com.force.cliforce.TestPluginInjector;
import com.force.cliforce.plugin.db.command.ListCustomObjects;
import com.google.inject.Guice;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;

public class ListTest {
	
    Plugin dbPlugin = new DBPlugin();
    TestPluginInjector injector;

    @BeforeMethod
    public void methodSetup() {
        injector = Guice.createInjector(new TestModule()).getInstance(TestPluginInjector.class);
    }
    
	@Test
	public void testDbListConnectionFailure() throws ConnectionException {
		ListCustomObjects cmd = injector.getInjectedCommand(dbPlugin, ListCustomObjects.class);
        TestCommandContext ctx = new TestCommandContextMetadataException();
        try {
        	cmd.execute(ctx);
            Assert.fail("Exception should have been thrown when executing list command");		
        } catch(ResourceException e) {
        	Assert.assertTrue(e.getMessage().startsWith(
        			"Unable to execute the command, since the current metadata connection is invalid"), 
        			"Unexpected error message. Should have started with: Unable to execute the command, since the current metadata connection is invalid");
        }
	}
	
    private class TestCommandContextMetadataException extends TestCommandContext {

		@Override
		public MetadataConnection getMetadataConnection() {
			throw new RuntimeException();
		}
    	
    }
	
}
