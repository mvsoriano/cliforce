package com.force.cliforce.defaultplugin;


import com.force.cliforce.*;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sforce.soap.partner.GetUserInfoResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.vmforce.client.VMForceClient;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        connection = injector.getInstance(ConnectionManager.class);
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
              , { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"abcdefg1234567", "--sort"}}
              , { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"--sort", "abcdefg1234567"}}
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
    public void testInteractiveLogin() throws Exception {
        DefaultPlugin.LoginCommand cmd = getInjectedCommand(DefaultPlugin.LoginCommand.class);
        List<String> orderedInputs = Arrays.asList(new String[]{
            "some.random.target.com", "some.random@user.name.com", "Imagin@ryPa$$w3rd", "n"
        });
        TestCommandWriter out = new TestCommandWriter();
        TestCommandContext ctx = new TestCommandContext().withCommandWriter(out).withCommandReader(new TestCommandReader(orderedInputs, out));
        cmd.execute(ctx);
        Assert.assertEquals(
                out.getOutput()
              , "Please log in\n" +
                        "Target login server [api.alpha.vmforce.com]:some.random.target.com\n" +
                        "Login server: some.random.target.com\n" +
                        "Username:some.random@user.name.com\n" +
                        "Password:*****************\n" +
                        "Unable to log in with provided credentials\n" +
                        "Enter Y to try again, anything else to cancel.n\n"
              , "unexpected output: " + ctx.getCommandWriter().getOutput());
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
