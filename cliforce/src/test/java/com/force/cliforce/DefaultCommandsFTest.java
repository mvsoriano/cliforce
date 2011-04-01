package com.force.cliforce;


import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sforce.ws.ConnectionException;
import mockit.Mock;
import mockit.Mocked;
import mockit.Mockit;
import mockit.NonStrictExpectations;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/*
 * Tests for the default commands in CLIForce.
 * @author sclasen
 * @since
 */
public class DefaultCommandsFTest extends BaseCliforceCommandTest {

    Injector injector;
    ConnectionManager connection;
    TestCommandContext context;

    // ensure the client can connect to an org
    // TODO: Refactor to use new setup for connectionmanager
    @BeforeClass
    public void setupTestContext() throws IOException, ServletException {
        injector = Guice.createInjector(new TestModule());

        connection = injector.getInstance(ConnectionManager.class);
        connection.loadLogin();
        connection.doLogin();

        context = new TestCommandContext().withVmForceClient(connection.getVmForceClient());

        context.getVmForceClient().deleteAllApplications();
    }

    @DataProvider(name = "commandExecutionWithArgs")
    public Object[][] providecommandExecutionWithArgs() {
        return new Object[][]{
                { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"abcdefg1234567"}}
              , { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"abcdefg1234567", "-s"}}
              , { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"-s", "abcdefg1234567"}}
              , { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"abcdefg1234567", "--sort"}}
              , { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"--sort", "abcdefg1234567"}}
        };
    }

    @Test(dataProvider = "commandExecutionWithArgs")
    public void testDefaultCommand(Class<? extends Command> commandClass, String expectedOutput, String[] args) throws Exception {
        context = context.withCommandArguments(args);
        Command command = injector.getInstance(commandClass);
        command.execute(context);
        String actualOutput = context.out();
        Assert.assertEquals(actualOutput, expectedOutput, "Unexpected output for " + command + ": " + actualOutput);
    }

    @DataProvider(name = "stringCommands")
    public Object[][] provideStringCommands() {
        return new Object[][] {
                { "!debug --on", "!debug --on: event not found\n" }
              , { "!debug", "!debug: event not found" }
        };
    }

    @Test(dataProvider = "stringCommands", enabled = false)
    public void testStringCommand(String commandLineInput, String expectedOutput) throws IOException, ServletException, InterruptedException, ConnectionException {
        String actualOutput = executeCommand(commandLineInput);
        Assert.assertEquals(actualOutput, expectedOutput, "Unexpected command output: " + actualOutput);
    }

    @Test
    public void testClasspathCommandWithNonExistentPlugin() throws Exception {
        DefaultPlugin.ClasspathCommand cmd = getInjectedCommand(DefaultPlugin.ClasspathCommand.class);
        TestCommandContext ctx = new TestCommandContext().withCommandArguments("nonexistent");
        cmd.execute(ctx);
        Assert.assertTrue(ctx.getCommandWriter().getOutput().contains("No such plugin: nonexistent"), "unexpected output");
        Assert.assertFalse(ctx.getCommandWriter().getOutput().contains("NullPointerException"), "unexpected output");
    }

    @Test
    public void testInstallNonexistentPlugin() throws Exception {
        DefaultPlugin.PluginCommand cmd = getInjectedCommand(DefaultPlugin.PluginCommand.class);
        TestCommandContext ctx = new TestCommandContext().withCommandArguments("nonexistent");
        cmd.execute(ctx);
        Assert.assertTrue(ctx.getCommandWriter().getOutput().contains("The maven artifact associated with the plugin could not be found."), "unexpected output");
        Assert.assertFalse(ctx.getCommandWriter().getOutput().contains("DependencyResolutionException"), "unexpected output");
    }

    private <T extends Command> T getInjectedCommand(Class<T> cmd) {
        return getTestInjector().getInstance(cmd);
    }

    private Injector getTestInjector() {
        return Guice.createInjector(new TestModule());
    }

    @Override
    public void setupCLIForce(CLIForce c) throws IOException {
        //overriding with an empty method because we don't want to install additional plugins
    }

    @Override
    public String getPluginArtifact() {
        // not needed to test Default commands
        return null;
    }

    @Override
    public Plugin getPlugin() {
        // not needed to test Default commands
        return null;
    }
}
