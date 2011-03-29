package com.force.cliforce.defaultplugin;


import com.force.cliforce.*;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;

/*
 * Tests for the default commands in CLIForce.
 * @author sclasen
 * @since
 */
public class DefaultCommandsFTest {


    Injector injector;
    ConnectionManager connection;
    TestCommandContext context;

    // ensure the client can connect to an org
    // TODO: Refactor to use new setup for connectionmanager
    @BeforeClass
    public void setupEnvironment() throws IOException, ServletException {
        injector = Guice.createInjector(new TestModule());

        connection = injector.getInstance(TestConnectionManager.class);
        connection.loadLogin();
        connection.doLogin();

        context = new TestCommandContext().withVmForceClient(connection.getVmForceClient());

        context.getVmForceClient().deleteAllApplications();
    }


    @DataProvider(name = "expectedOutput")
    public Object[][] provideExpectedOutputForCommand() {
        return new Object[][]{
                { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"abcdefg1234567"}}
              , { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"abcdefg1234567", "-s"}}
              , { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"-s", "abcdefg1234567"}}
        };
    }

    @Test(dataProvider = "expectedOutput")
    public void testDefaultCommand(Class<? extends Command> commandClass, String expectedOutput, String[] args) throws Exception {
        context = context.withCommandArguments(args);
        Command command = injector.getInstance(commandClass);
        command.execute(context);
        String actualOutput = context.out();
        Assert.assertEquals(actualOutput, expectedOutput, "Unexpected output for " + command + ": " + actualOutput);
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


}
