package com.force.cliforce.plugin.db.command;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.force.cliforce.ResourceException;
import com.force.cliforce.TestCommandContext;
import com.force.cliforce.plugin.db.command.Describe.DescribeArgs;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;

/**
 * Negative tests for the describe command
 * 
 * @author John Simone, Tim Kral
 */
public class NegativeDescribeTest {
	
    private final Describe describe = new Describe();
    
	@Test
	public void testDbDescribeConnectionFailure() throws ConnectionException {
        TestCommandContext ctx = new TestCommandContextPartnerException();
        try {
            DescribeArgs args = new DescribeArgs();
            args.all = true;
        	describe.executeWithArgs(ctx, args);
            Assert.fail("Exception should have been thrown when executing describe command");		
        } catch(ResourceException e) {
        	Assert.assertTrue(e.getMessage().startsWith(
        			"Unable to execute the command, since the partner connection is invalid"), 
        			"Unexpected error message. Should have started with: UUnable to execute the command, since the partner connection is invalid but was " +
        			e.getMessage());
        }
	}
	
    private class TestCommandContextPartnerException extends TestCommandContext {

		@Override
		public PartnerConnection getPartnerConnection() {
			throw new RuntimeException();
		}
    	
    }
	
}
