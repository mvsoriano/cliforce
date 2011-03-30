package com.force.cliforce.plugin.connection;

import com.force.cliforce.Plugin;
import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestModule;
import com.force.cliforce.TestPluginInjector;
import com.force.cliforce.plugin.connection.command.AddConnectionCommand;
import com.force.cliforce.plugin.connection.command.ListConnectionsCommand;
import com.force.cliforce.plugin.connection.command.RemoveConnectionCommand;
import com.google.inject.Guice;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for commands in the connection plugin
 *
 * @author jeffrey.lai
 * @since javasdk-21.0.2-BETA
 */
public class ConnectionTest {
    Plugin connPlugin = new ConnectionPlugin();
    TestPluginInjector injector;

    @BeforeMethod
    public void methodSetup() {
        injector = Guice.createInjector(new TestModule()).getInstance(TestPluginInjector.class);
    }

    @Test
    public void testListConnectionEmpty() {
        ListConnectionsCommand cmd = injector.getInjectedCommand(connPlugin, ListConnectionsCommand.class);
        TestCommandContext ctx = new TestCommandContext();
        cmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "There are no connections configured. Please use connection:add to add one.\n",
                "unexpected output from command");
    }

    @Test
    public void testAddConnection() throws Exception {
        TestCommandContext ctx = addConnSetup();
        ListConnectionsCommand listCmd = injector.getInjectedCommand(connPlugin, ListConnectionsCommand.class);
        ctx.setCommandArguments(new String[0]);
        listCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "\n===========================\n" +
                "Name:     jeff\n" +
                "Host:     vmf01.t.salesforce.com\n" +
                "User:     user@user.com\n" +
                "Password: **********\n" +
                "Valid:    true\n" +
                "Message:  None\n" +
                "===========================\n");
    }

    @Test
    public void testRemoveConnection() throws Exception {
        TestCommandContext ctx = addConnSetup();
        RemoveConnectionCommand rmCmd = injector.getInjectedCommand(connPlugin, RemoveConnectionCommand.class);
        ctx.setCommandArguments(new String[]{"jeff"});
        rmCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "Connection: jeff removed\n", "unexpected ouput from command");
    }

    private TestCommandContext addConnSetup() throws Exception {
        AddConnectionCommand addCmd = injector.getInjectedCommand(connPlugin, AddConnectionCommand.class);
        TestCommandContext ctx = new TestCommandContext().withCommandArguments(new String[]{"--notoken", "-n", "jeff", "-h", "vmf01.t.salesforce.com", "-u", "user@user.com", "-p", "mountains4"});
        addCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "Connection: jeff added\n", "unexpected output from connection:add command");
        ctx.getCommandWriter().reset();
        return ctx;
    }

}
