package com.force.cliforce.plugin.connection;

import com.force.cliforce.ConnectionManager;
import com.force.cliforce.Plugin;
import com.force.cliforce.ResourceException;
import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestModule;
import com.force.cliforce.TestPluginInjector;
import com.force.cliforce.plugin.connection.command.AddConnectionCommand;
import com.force.cliforce.plugin.connection.command.CurrentConnectionCommand;
import com.force.cliforce.plugin.connection.command.ListConnectionsCommand;
import com.force.cliforce.plugin.connection.command.RemoveConnectionCommand;
import com.force.cliforce.plugin.connection.command.RenameConnectionCommand;
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
                "===========================\n", "unexpected output from command");
    }

    @Test
    public void testRemoveConnection() throws Exception {
        TestCommandContext ctx = addConnSetup();
        RemoveConnectionCommand rmCmd = injector.getInjectedCommand(connPlugin, RemoveConnectionCommand.class);
        ctx.setCommandArguments(new String[]{"jeff"});
        rmCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "Connection: jeff removed\n", "unexpected ouput from command");
    }
    
    @Test
    public void testConnectionCurrentNoConnection() {
        CurrentConnectionCommand cmd = injector.getInjectedCommand(connPlugin, CurrentConnectionCommand.class);
        TestCommandContext ctx = new TestCommandContext();
        try {
            cmd.execute(ctx);
            Assert.fail("executing command should have thrown an exception");
        } catch (ResourceException e) {
            Assert.assertEquals(e.getMessage(), "Unable to execute the command, since the current force connection is null.\n" +
                    "Please add a valid connection using connection:add", "unexpected error message");
        }
    }
    
    @Test
    public void testConnectionCurrentOneConnection() throws Exception {
        TestCommandContext ctx = addConnSetup();
        CurrentConnectionCommand cmd = injector.getInjectedCommand(connPlugin, CurrentConnectionCommand.class);
        cmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "Current Connection Name: jeff\n" +
                "Current User: user@user.com\n" +
                "Current Endpoint: vmf01.t.salesforce.com\n", "unexpected result returned");
    }
    
    @Test
    public void testRenameConnection() throws Exception {
        TestCommandContext ctx = addConnSetup();
        RenameConnectionCommand reCmd = injector.getInjectedCommand(connPlugin, RenameConnectionCommand.class);
        ListConnectionsCommand listCmd = injector.getInjectedCommand(connPlugin, ListConnectionsCommand.class);
        ctx.setCommandArguments(new String[] { "jeff", "asdf" });
        reCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "Renamed connection jeff to asdf\n", "unexpected output from command");
        ctx.setCommandArguments(new String[0]);
        ctx.getCommandWriter().reset();
        listCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "\n===========================\n" +
                "Name:     asdf\n" +
                "Host:     vmf01.t.salesforce.com\n" +
                "User:     user@user.com\n" +
                "Password: **********\n" +
                "Valid:    true\n" +
                "Message:  None\n" +
                "===========================\n");
    }
    
    @Test
    public void testRenameConnectionSameName() throws Exception {
        TestCommandContext ctx = add2ConnSetup();
        RenameConnectionCommand reCmd = injector.getInjectedCommand(connPlugin, RenameConnectionCommand.class);
        ListConnectionsCommand listCmd = injector.getInjectedCommand(connPlugin, ListConnectionsCommand.class);
        ctx.setCommandArguments(new String[] { "jeff", "asdf" });
        reCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "There is already a connection named asdf, please rename or delete it first\n", "unexpected output from command");
        ctx.setCommandArguments(new String[0]);
        ctx.getCommandWriter().reset();
        listCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "\n===========================\n" +
                "Name:     asdf\n" +
                "Host:     vmf01.t.salesforce.com\n" +
                "User:     user@domain.com\n" +
                "Password: *******\n" +
                "Valid:    true\n" +
                "Message:  None\n" +
                "===========================\n\n" +
                "===========================\n" +
                "Name:     jeff\n" +
                "Host:     vmf01.t.salesforce.com\n" +
                "User:     user@user.com\n" +
                "Password: **********\n" +
                "Valid:    true\n" +
                "Message:  None\n" +
                "===========================\n", "unexpected output from command");
    }

    private TestCommandContext addConnSetup() throws Exception {
        ConnectionManager connMan = injector.getInjector().getInstance(ConnectionManager.class);
        AddConnectionCommand addCmd = injector.getInjectedCommand(connPlugin, AddConnectionCommand.class);
        TestCommandContext ctx = new TestCommandContext().withCommandArguments(new String[]{"--notoken", "-n", "jeff", "-h", "vmf01.t.salesforce.com", "-u", "user@user.com", "-p", "mountains4"});
        addCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "Connection: jeff added\n", "unexpected output from connection:add command");
        ctx.getCommandWriter().reset();
        Assert.assertNotNull(connMan.getCurrentEnv(), "connection manager current env was null");
        ctx = ctx.withForceEnv(connMan.getCurrentEnv());
        return ctx;
    }
    
    private TestCommandContext add2ConnSetup() throws Exception {
        AddConnectionCommand addCmd = injector.getInjectedCommand(connPlugin, AddConnectionCommand.class);
        TestCommandContext ctx = addConnSetup();
        ctx = ctx.withCommandArguments(new String[]{"--notoken", "-n", "asdf", "-h", "vmf01.t.salesforce.com", "-u", "user@domain.com", "-p", "u8s9032"});
        addCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "Connection: asdf added\n", "unexpected output from connection:add command");
        ctx.getCommandWriter().reset();
        return ctx;
    }

}
