package com.force.cliforce.plugin.connection;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.force.cliforce.TestCommandContext;
import com.force.cliforce.plugin.connection.command.AddConnectionCommand;
import com.force.cliforce.plugin.connection.command.ListConnectionsCommand;

public class ConnectionTest {

    // TODO enable once W-915603 is resolved
    @Test(enabled=false)
    public void testListConnectionEmpty() throws Exception {
        ListConnectionsCommand cmd = new ListConnectionsCommand();
        TestCommandContext ctx = new TestCommandContext();
        cmd.execute(ctx);
        // TODO need to change assert below to correct expected value
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "", "unexpected command result");
    }
    
    @Test
    public void testAddConnectionValidArguments() throws Exception {
        AddConnectionCommand cmd = new AddConnectionCommand();
        TestCommandContext ctx = new TestCommandContext().withCommandArguments(new String[] {"jeff", "force://vmf01.t.salesforce.com;user=user@user.com;password=mountains4"});
        
        
        cmd.execute(ctx);
        System.out.println(ctx.getCommandWriter().getOutput());
    }
    
}