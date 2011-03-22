package com.force.cliforce.plugin.connection;

import javax.inject.Inject;

import org.testng.Assert;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import com.force.cliforce.Plugin;
import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestModule;
import com.force.cliforce.TestPlugin;
import com.force.cliforce.TestPluginInjector;
import com.force.cliforce.plugin.connection.command.ListConnectionsCommand;

@Guice(modules = TestModule.class)
public class ConnectionTest {
	
    @Inject
    TestPluginInjector injector;
    
    @Test
    public void testListConnectionEmpty() {
        Plugin connPlugin = new ConnectionPlugin();
        ListConnectionsCommand cmd = injector.getInjectedCommand(connPlugin, ListConnectionsCommand.class);
        TestCommandContext ctx = new TestCommandContext();
        cmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(),"There are no connections configured. Please use connection:add to add one.", 
        		"unexpected output from command");
    }

}
