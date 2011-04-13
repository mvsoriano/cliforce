package com.force.cliforce.plugin.connection;

import com.force.cliforce.ConnectionManager;
import com.force.cliforce.ForceEnv;
import com.force.cliforce.Plugin;
import com.force.cliforce.ResourceException;
import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestModule;
import com.force.cliforce.TestPluginInjector;
import com.force.cliforce.plugin.connection.command.AddConnectionCommand;
import com.force.cliforce.plugin.connection.command.CurrentConnectionCommand;
import com.force.cliforce.plugin.connection.command.DefaultConnectionCommand;
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
        TestCommandContext ctx = addConnSetup(new String[][] { {"jeff", "force://vmf01.t.salesforce.com;user=user@user.com;password=mountains4"} });
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
        TestCommandContext ctx = addConnSetup(new String[][] { {"jeff", "force://vmf01.t.salesforce.com;user=user@user.com;password=mountains4"} });
        RemoveConnectionCommand rmCmd = injector.getInjectedCommand(connPlugin, RemoveConnectionCommand.class);
        ctx.setCommandArguments(new String[]{"jeff"});
        rmCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "Connection: jeff removed\n", "unexpected ouput from command");
    }
    
    @Test
    public void testDefaultConnectionWithOneConnection() throws Exception {
    	TestCommandContext ctx = addConnSetup(new String[][] { {"jeff", "force://vmf01.t.salesforce.com;user=user@user.com;password=mountains4"} });
    	DefaultConnectionCommand connectionCommand = injector.getInjectedCommand(connPlugin, DefaultConnectionCommand.class);
        ctx.setCommandArguments(new String[]{});
    	connectionCommand.execute(ctx);
    	Assert.assertEquals(ctx.getCommandWriter().getOutput(), "The currently selected default connection name is: jeff\n", "unexpected ouput from command: " + ctx.getCommandWriter().getOutput());
    }

    @Test
    public void testDefaultConnectionWithTwoConnections() throws Exception {
    	TestCommandContext ctx = addConnSetup(new String[][] {
                {"jeff", "force://vmf01.t.salesforce.com;user=user@user.com;password=mountains4"}
              , {"jeff2", "force://vmf01.t.salesforce.com;user=user@user.com;password=mountains4"}
        });
    	DefaultConnectionCommand connectionCommand = injector.getInjectedCommand(connPlugin, DefaultConnectionCommand.class);
        ctx.setCommandArguments(new String[]{});
    	connectionCommand.execute(ctx);
    	Assert.assertEquals(ctx.getCommandWriter().getOutput(), "The currently selected default connection name is: jeff\n", "unexpected ouput from command: " + ctx.getCommandWriter().getOutput());
    }

    @Test
    public void testDefaultConnectionNonExistentConn() throws Exception {
    	TestCommandContext ctx = addConnSetup(new String[][] { {"jeff", "force://vmf01.t.salesforce.com;user=user@user.com;password=mountains4"} });
    	DefaultConnectionCommand connectionCommand = injector.getInjectedCommand(connPlugin, DefaultConnectionCommand.class);
    	ctx.setCommandArguments(new String[]{"fake"});
    	connectionCommand.execute(ctx);
    	Assert.assertEquals(ctx.getCommandWriter().getOutput(), "There is no such connection: fake available\n", "unexpected ouput from command");
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
        TestCommandContext ctx = addConnSetup(new String[][] { {"jeff", "force://vmf01.t.salesforce.com;user=user@user.com;password=mountains4"} });
        CurrentConnectionCommand cmd = injector.getInjectedCommand(connPlugin, CurrentConnectionCommand.class);
        cmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), "Current Connection Name: jeff\n" +
                "Current User: user@user.com\n" +
                "Current Endpoint: vmf01.t.salesforce.com\n", "unexpected result returned");
    }
    
    @Test
    public void testRenameConnection() throws Exception {
        TestCommandContext ctx = addConnSetup(new String[][] { {"jeff", "force://vmf01.t.salesforce.com;user=user@user.com;password=mountains4"} });
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
        TestCommandContext ctx = addConnSetup(new String[][] { {"jeff", "force://vmf01.t.salesforce.com;user=user@user.com;password=mountains4"},
                {"asdf", "force://vmf01.t.salesforce.com;user=user@domain.com;password=s3d4gd3"} });
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

    
    /**
     * Takes in 2 dim Array, which contain connection names and force urls.
     * column 0 should be connection name and column 1 should be force url
     */
    private TestCommandContext addConnSetup(String[][] connArray) throws Exception {
        ConnectionManager connMan = injector.getInjector().getInstance(ConnectionManager.class);
        AddConnectionCommand addCmd = injector.getInjectedCommand(connPlugin, AddConnectionCommand.class);
        ForceEnv env;
        TestCommandContext ctx = new TestCommandContext();
        for (String[] arr : connArray) {
            env = new ForceEnv(arr[1], "test");
            ctx = ctx.withCommandArguments(new String[]{"--notoken", "-n", arr[0], "-h", env.getHost(), "-u", env.getUser(), "-p", env.getPassword()});
            addCmd.execute(ctx);
            Assert.assertEquals(ctx.getCommandWriter().getOutput(), "Connection: " + arr[0] + " added\n", "unexpected output from connection:add command");
            ctx.getCommandWriter().reset();
        }
        Assert.assertNotNull(connMan.getCurrentEnv(), "connection manager current env was null");
        ctx = ctx.withForceEnv(connMan.getCurrentEnv());
        return ctx;
    }

}
