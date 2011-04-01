package com.force.cliforce;


import com.google.inject.Guice;
import com.google.inject.Injector;
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
        // mock login so it doesn't change credentials or connect to sfdc service
        Mockit.setUpMock(MainConnectionManager.class, new Object(){
            @Mock
            void doLogin() {System.out.println("doLogin");}
            @Mock
            void saveLogin() {System.out.println("saveLogin");}
        });

        Injector testModuleInjector = Guice.createInjector(new TestModule());
        ConnectionManager connectionManager = testModuleInjector.getInstance(ConnectionManager.class);

        DefaultPlugin.LoginCommand cmd = testModuleInjector.getInstance(DefaultPlugin.LoginCommand.class);

        List<String> orderedInputs = Arrays.asList("some.random.target.com", "some.random@user.name.com", "Imagin@ryPa$$w3rd", "n");
        TestCommandContext ctx = new TestCommandContext().withTestCommandReader(new TestCommandReader(orderedInputs));

        cmd.execute(ctx);

        // because we're mocking the actual login, we expect a login success
        Assert.assertEquals(
                ctx.getCommandWriter().getOutput()
              , "Please log in\n" +
                        "Target login server [api.alpha.vmforce.com]:some.random.target.com\n" +
                        "Login server: some.random.target.com\n" +
                        "Username:some.random@user.name.com\n" +
                        "Password:*****************\n" +
                        "Login successful.\n"
              , "unexpected output: " + ctx.getCommandWriter().getOutput());
    }

    @Test
    public void testLoginCommandCorrectlyStoresInputs() throws Exception {
        // mock login so it doesn't change credentials or connect to sfdc service
        Mockit.setUpMock(MainConnectionManager.class, new Object(){
            @Mock
            void doLogin() {System.out.println("doLogin");}
            @Mock
            void saveLogin() {System.out.println("saveLogin");}
        });

        Injector testModuleInjector = Guice.createInjector(new TestModule());
        ConnectionManager connectionManager = testModuleInjector.getInstance(ConnectionManager.class);

        Assert.assertEquals(connectionManager.getUser(), null, "unexpected username: " + connectionManager.getUser());
        Assert.assertEquals(connectionManager.getPassword(), null, "unexpected username: " + connectionManager.getPassword());
        Assert.assertEquals(connectionManager.getTarget(), null, "unexpected target: " + connectionManager.getTarget());

        DefaultPlugin.LoginCommand cmd = testModuleInjector.getInstance(DefaultPlugin.LoginCommand.class);

        List<String> orderedInputs = Arrays.asList("some.random.target.com", "some.random@user.name.com", "Imagin@ryPa$$w3rd", "n");
        TestCommandContext ctx = new TestCommandContext().withTestCommandReader(new TestCommandReader(orderedInputs));

        cmd.execute(ctx);

        Assert.assertEquals(connectionManager.getUser(), "some.random@user.name.com", "unexpected username: " + connectionManager.getUser());
        Assert.assertEquals(connectionManager.getPassword(), "Imagin@ryPa$$w3rd", "unexpected username: " + connectionManager.getPassword());
        Assert.assertEquals(connectionManager.getTarget(), "some.random.target.com", "unexpected target: " + connectionManager.getTarget());
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
