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

package com.force.cliforce.plugin.connection;

import static com.force.cliforce.Util.newLine;
import static com.force.cliforce.Util.withNewLine;
import static com.force.cliforce.Util.withSeparator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.*;

import com.force.cliforce.*;
import com.force.cliforce.plugin.connection.command.*;
import com.force.sdk.connector.ForceServiceConnector;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;

/**
 * Tests for commands in the connection plugin
 *
 * @author Jeff Lai
 * 
 */
public class ConnectionTest {
    Plugin connPlugin = new ConnectionPlugin();
    TestPluginInjector injector;
    final String connectionUrl = "force://login.salesforce.com;user=user@user.com;password=mountains4";
    String tmpUserHome = withSeparator(System.getProperty("user.dir")) + withSeparator("target") + "tmp-user-home";
    File parentDir = new File(tmpUserHome);

    @BeforeMethod
    public void methodSetup() {
        injector = Guice.createInjector(new TestModule(tmpUserHome)).getInstance(TestPluginInjector.class);
    }
    
    @BeforeClass
    public void classSetup() throws IOException {
        // in case a previous test run failed to cleanup, delete any existing directories we use
        if (parentDir.exists()) FileUtils.deleteDirectory(parentDir); 
        // create directory used for testing
        FileUtils.copyDirectory(new File(System.getProperty("positive.test.user.home")), parentDir);
    }
    
    @AfterClass(alwaysRun=true)
    public void classTeardown() throws IOException {
        if (parentDir.exists()) FileUtils.deleteDirectory(parentDir);
    }

    @Test
    public void testListConnectionEmpty() {
        ListConnectionsCommand cmd = injector.getInjectedCommand(connPlugin, ListConnectionsCommand.class);
        TestCommandContext ctx = new TestCommandContext();
        cmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(),
                withNewLine("There are no connections configured. Please use connection:add to add one."),
                "unexpected output from command");
    }

    @Test
    public void testAddConnection() throws Exception {
        TestCommandContext ctx = addConnSetup(new String[][]{{"jeff", connectionUrl}});
        ListConnectionsCommand listCmd = injector.getInjectedCommand(connPlugin, ListConnectionsCommand.class);
        ctx.setCommandArguments(new String[0]);
        listCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(),
                  newLine()
                + withNewLine("===========================")
                + withNewLine("Name:         jeff")
                + withNewLine("Host:         login.salesforce.com")
                + withNewLine("User:         user@user.com")
                + withNewLine("Password:     **********")
                + withNewLine("OAuth Key:    None")
                + withNewLine("OAuth Secret: None")
                + withNewLine("Valid:        true")
                + withNewLine("Message:      None")
                + withNewLine("==========================="), "unexpected output from command");
    }

    @Test
    public void testInteractiveAddConnectionWithoutOAuth() throws Exception {
        Injector testModuleInjector = Guice.createInjector(new TestModule(tmpUserHome));
        testModuleInjector.getInstance(ConnectionManager.class);

        AddConnectionCommand cmd = testModuleInjector.getInstance(AddConnectionCommand.class);

        List<String> orderedInputs = Arrays.asList("testInteractiveAddConnection", "some.random@user.name.com", "Imagin@ryPa$$w3rd", "apiKey", "some.random.target.com", "N");
        TestCommandContext ctx = new TestCommandContext().withForceEnv(new ForceEnv("some.random.target.com", "some.random@user.name.com", "Imagin@ryPa$$w3rd")).withTestCommandReader(new TestCommandReader(orderedInputs));

        cmd.execute(ctx);

        Assert.assertEquals(
                ctx.getCommandWriter().getOutput(),
                    withNewLine("connection name: testInteractiveAddConnection")
                  + withNewLine("user: some.random@user.name.com")
                  + withNewLine("password: *****************")
                  + withNewLine("security token: apiKey")
                  + withNewLine("host (defaults to login.salesforce.com): some.random.target.com")
                  + withNewLine("Enter oauth key and secret? (Y to enter, anything else to skip): N")
                  + withNewLine("Connection: testInteractiveAddConnection added"),
                "unexpected output: " + ctx.getCommandWriter().getOutput());
    }

    @Test
    public void testInteractiveAddConnectionWithOAuth() throws Exception {
        Injector testModuleInjector = Guice.createInjector(new TestModule(tmpUserHome));
        testModuleInjector.getInstance(ConnectionManager.class);

        AddConnectionCommand cmd = testModuleInjector.getInstance(AddConnectionCommand.class);

        List<String> orderedInputs = Arrays.asList("testInteractiveAddConnection", "some.random@user.name.com", "Imagin@ryPa$$w3rd", "apiKey", "some.random.target.com", "Y", "secret", "key");
        TestCommandContext ctx = new TestCommandContext().withForceEnv(new ForceEnv("some.random.target.com", "some.random@user.name.com", "Imagin@ryPa$$w3rd")).withTestCommandReader(new TestCommandReader(orderedInputs));

        cmd.execute(ctx);

        Assert.assertEquals(
                ctx.getCommandWriter().getOutput(),
                    withNewLine("connection name: testInteractiveAddConnection")
                  + withNewLine("user: some.random@user.name.com")
                  + withNewLine("password: *****************")
                  + withNewLine("security token: apiKey")
                  + withNewLine("host (defaults to login.salesforce.com): some.random.target.com")
                  + withNewLine("Enter oauth key and secret? (Y to enter, anything else to skip): Y")
                  + withNewLine("oauth key:secret")
                  + withNewLine("oauth secret:key")
                  + withNewLine("Connection: testInteractiveAddConnection added"),
                "unexpected output: " + ctx.getCommandWriter().getOutput());
    }

    @Test
    public void testRemoveConnection() throws Exception {
        TestCommandContext ctx = addConnSetup(new String[][]{{"jeff", connectionUrl}});
        RemoveConnectionCommand rmCmd = injector.getInjectedCommand(connPlugin, RemoveConnectionCommand.class);
        ctx.setCommandArguments(new String[]{"jeff"});
        rmCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), withNewLine("Connection: jeff removed"), "unexpected ouput from command");
    }

    @Test
    public void testDefaultConnectionWithOneConnection() throws Exception {
        TestCommandContext ctx = addConnSetup(new String[][]{{"jeff", connectionUrl}});
        DefaultConnectionCommand connectionCommand = injector.getInjectedCommand(connPlugin, DefaultConnectionCommand.class);
        ctx.setCommandArguments(new String[]{});
        connectionCommand.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(),
                withNewLine("The currently selected default connection name is: jeff"),
                "unexpected ouput from command: " + ctx.getCommandWriter().getOutput());
    }

    @Test
    public void testDefaultConnectionWithTwoConnections() throws Exception {
        TestCommandContext ctx = addConnSetup(new String[][]{
                {"jeff", connectionUrl}
                , {"jeff2", connectionUrl}
        });
        DefaultConnectionCommand connectionCommand = injector.getInjectedCommand(connPlugin, DefaultConnectionCommand.class);
        ctx.setCommandArguments(new String[]{});
        connectionCommand.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(),
                withNewLine("The currently selected default connection name is: jeff"),
                "unexpected ouput from command: " + ctx.getCommandWriter().getOutput());
    }

    @Test
    public void testDefaultConnectionNonExistentConn() throws Exception {
        TestCommandContext ctx = addConnSetup(new String[][]{{"jeff", connectionUrl}});
        DefaultConnectionCommand connectionCommand = injector.getInjectedCommand(connPlugin, DefaultConnectionCommand.class);
        ctx.setCommandArguments(new String[]{"fake"});
        connectionCommand.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(),
                withNewLine("There is no such connection: fake available"),
                "unexpected ouput from command");
    }

    @Test
    public void testConnectionCurrentNoConnection() {
        CurrentConnectionCommand cmd = injector.getInjectedCommand(connPlugin, CurrentConnectionCommand.class);
        TestCommandContext ctx = new TestCommandContext();
        try {
            cmd.execute(ctx);
            Assert.fail("executing command should have thrown an exception");
        } catch (ResourceException e) {
            Assert.assertEquals(e.getMessage(),
                    withNewLine("Unable to execute the command, since the current force connection is null.") +
                        "Please add a valid connection using connection:add",
                    "unexpected error message");
        }
    }

    @Test
    public void testConnectionCurrentOneConnection() throws Exception {
        TestCommandContext ctx = addConnSetup(new String[][]{{"jeff", connectionUrl}});
        CurrentConnectionCommand cmd = injector.getInjectedCommand(connPlugin, CurrentConnectionCommand.class);
        cmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(),
                  withNewLine("Current Connection Name: jeff")
                + withNewLine("Current User: user@user.com")
                + withNewLine("Current Endpoint: login.salesforce.com"),
              "unexpected result returned");
    }

    @Test
    public void testTestConnectionCommand() throws ConnectionException, IOException {
        Injector guiceInjector = Guice.createInjector(new TestModule());
        injector = guiceInjector.getInstance(TestPluginInjector.class);
        ConnectionManager cmgr = guiceInjector.getInstance(ConnectionManager.class);
        cmgr.loadUserConnections();
        TestCommandContext ctx = new TestContextWithConnector(cmgr.getCurrentConnector());
        TestConnectionCommand cmd = injector.getInjectedCommand(connPlugin, TestConnectionCommand.class);
        cmd.execute(ctx);
        Assert.assertEquals(ctx.out(), withNewLine("connection valid"));
        guiceInjector = Guice.createInjector(new TestModule(System.getProperty("negative.test.user.home")));
        cmgr = guiceInjector.getInstance(ConnectionManager.class);
        cmgr.loadUserConnections();
        ctx = new TestContextWithConnector(cmgr.getCurrentConnector());
        injector = guiceInjector.getInstance(TestPluginInjector.class);
        cmd = injector.getInjectedCommand(connPlugin, TestConnectionCommand.class);
        cmd.execute(ctx);
        Assert.assertEquals(ctx.out(), withNewLine("connection invalid") + withNewLine("execute debug and retry to see failure information"));
    }

    private class TestContextWithConnector extends TestCommandContext {
        private ForceServiceConnector connector;

        public TestContextWithConnector(ForceServiceConnector c) {
            super();
            connector = c;
        }

        @Override
        public PartnerConnection getPartnerConnection() {
            try {
                return connector.getConnection();
            } catch (ConnectionException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Test
    public void testRenameConnection() throws Exception {
        TestCommandContext ctx = addConnSetup(new String[][]{{"jeff", connectionUrl}});
        RenameConnectionCommand reCmd = injector.getInjectedCommand(connPlugin, RenameConnectionCommand.class);
        ListConnectionsCommand listCmd = injector.getInjectedCommand(connPlugin, ListConnectionsCommand.class);
        ctx.setCommandArguments(new String[]{"jeff", "asdf"});
        reCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(), withNewLine("Renamed connection jeff to asdf"),
                "unexpected output from command");
        ctx.setCommandArguments(new String[0]);
        ctx.getCommandWriter().reset();
        listCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(),
                  newLine() 
                + withNewLine("===========================")
                + withNewLine("Name:         asdf")
                + withNewLine("Host:         login.salesforce.com")
                + withNewLine("User:         user@user.com")
                + withNewLine("Password:     **********")
                + withNewLine("OAuth Key:    None")
                + withNewLine("OAuth Secret: None")
                + withNewLine("Valid:        true")
                + withNewLine("Message:      None")
                + withNewLine("==========================="));
    }

    @Test
    public void testRenameConnectionSameName() throws Exception {
        TestCommandContext ctx = addConnSetup(new String[][]{{"jeff", connectionUrl},
                {"asdf", "force://login.salesforce.com;user=user@domain.com;password=s3d4gd3"}});
        RenameConnectionCommand reCmd = injector.getInjectedCommand(connPlugin, RenameConnectionCommand.class);
        ListConnectionsCommand listCmd = injector.getInjectedCommand(connPlugin, ListConnectionsCommand.class);
        ctx.setCommandArguments(new String[]{"jeff", "asdf"});
        reCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(),
                withNewLine("There is already a connection named asdf, please rename or delete it first"),
                "unexpected output from command");
        ctx.setCommandArguments(new String[0]);
        ctx.getCommandWriter().reset();
        listCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(),
                  newLine() 
                + withNewLine("===========================")
                + withNewLine("Name:         asdf")
                + withNewLine("Host:         login.salesforce.com")
                + withNewLine("User:         user@domain.com")
                + withNewLine("Password:     *******")
                + withNewLine("OAuth Key:    None")
                + withNewLine("OAuth Secret: None")
                + withNewLine("Valid:        true")
                + withNewLine("Message:      None")
                + withNewLine("===========================")
                + newLine()
                + withNewLine("===========================")
                + withNewLine("Name:         jeff")
                + withNewLine("Host:         login.salesforce.com")
                + withNewLine("User:         user@user.com")
                + withNewLine("Password:     **********")
                + withNewLine("OAuth Key:    None")
                + withNewLine("OAuth Secret: None")
                + withNewLine("Valid:        true")
                + withNewLine("Message:      None")
                + withNewLine("==========================="), "unexpected output from command");
    }

    @Test
    public void testAddDuplicateConnection() throws Exception {
        TestCommandContext ctx = addConnSetup(new String[][]{{"jeff", connectionUrl}});
        ctx = ctx.withCommandArguments("--notoken", "-n", "jeff", "-h", "login.salesforce.com", "-u", "user@user.com", "-p", "mountains4");
        ctx = ctx.withCommandReader(new TestCommandReader(Lists.newArrayList("9")));
        AddConnectionCommand addCmd = injector.getInjectedCommand(connPlugin, AddConnectionCommand.class);
        addCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(),
                withNewLine("There is already a connection named jeff, please rename or remove it first"));
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
            Assert.assertEquals(ctx.getCommandWriter().getOutput(),
                    withNewLine("Connection: " + arr[0] + " added"), "unexpected output from connection:add command");
            ctx.getCommandWriter().reset();
        }
        Assert.assertNotNull(connMan.getCurrentEnv(), "connection manager current env was null");
        ctx = ctx.withForceEnv(connMan.getCurrentEnv());
        return ctx;
    }

    @Test
    public void testSpaceInConnectionName() throws Exception {
        TestCommandContext ctx = addConnSetup(new String[][]{
                {"jeff", connectionUrl}
        });
        ctx = ctx.withCommandArguments("--notoken",
                "-n","\'hello world\'",
                "-h", "login.salesforce.com",
                "-u", "user@domain.com",
                "-p", "mountains4");
        ctx = ctx.withCommandReader(new TestCommandReader(Lists.newArrayList("9")));
        AddConnectionCommand addCmd = injector.getInjectedCommand(connPlugin, AddConnectionCommand.class);
        addCmd.execute(ctx);
        Assert.assertEquals(ctx.getCommandWriter().getOutput(),
                withNewLine("Space and tab are not allowed in connection name."),
                "unexpected output from command");
    }
}
