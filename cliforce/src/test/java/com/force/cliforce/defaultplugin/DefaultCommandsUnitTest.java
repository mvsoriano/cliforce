package com.force.cliforce.defaultplugin;


import com.force.cliforce.*;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.vmforce.client.VMForceClient;
import mockit.Mock;
import mockit.Mockit;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests commands in the default plugin that dont require any guice injection.
 */
public class DefaultCommandsUnitTest {

    @BeforeClass
    public void mockLogin() {
        // mock login so it doesn't change credentials or connect to sfdc service
        Mockit.setUpMock(MainConnectionManager.class, new Object(){
            @Mock
            void doLogin() {System.out.println("doLogin");}
            @Mock
            void saveLogin() {System.out.println("saveLogin");}
        });
    }

    @AfterClass
    public void clearMocks() {
        Mockit.tearDownMocks();
    }

    @Test
    public void syspropsCommand() throws Exception {
        String stamp = Long.toString(System.currentTimeMillis());
        System.setProperty("test.timestamp", stamp);
        DefaultPlugin.SyspropsCommand cmd = new DefaultPlugin.SyspropsCommand();
        TestCommandContext ctx = new TestCommandContext();
        cmd.execute(ctx);
        Assert.assertTrue(ctx.getCommandWriter().getOutput().contains("test.timestamp"), "Test output did not contain 'test.timestamp'");
        Assert.assertTrue(ctx.getCommandWriter().getOutput().contains(stamp), "Test output did not contain " + stamp);
    }


    @Test
    public void testShellCommandWithNoArgs() throws Exception {
        DefaultPlugin.ShellCommand cmd = new DefaultPlugin.ShellCommand();
        TestCommandContext ctx = new TestCommandContext().withCommandArguments(new String[0]);
        cmd.execute(ctx);
        Assert.assertTrue(ctx.getCommandWriter().getOutput().contains("The sh command expects a command which you would like to execute"), "Incorrect output");
        Assert.assertFalse(ctx.getCommandWriter().getOutput().contains("java.lang.ArrayIndexOutOfBoundsException"), "Incorrect output");
    }

    @DataProvider(name = "expectedData")
    public Object[][] appCommandExpectedInput() {
        return new Object[][]{
                { "sh: Executing: echo something\n  something", new String[] {"echo", "something"}}
                , { "sh: Executing: abc\nThe command failed to execute. Please check the path to the executable you provided",
                        new String[] {"abc"}}
                , {"sh: Executing: ls -e\n  ls: invalid option -- 'e'\n  Try `ls --help' for more information.", new String[] {"ls", "-e"}}
        };
    }

    @Test(dataProvider = "expectedData")
    public void testShellCommandWithArgs(String expectedOutput, String[] args) throws Exception {
       DefaultPlugin.ShellCommand cmd = new DefaultPlugin.ShellCommand();
        TestCommandContext ctx = new TestCommandContext().withCommandArguments(args);
        cmd.execute(ctx);
        Assert.assertTrue(ctx.getCommandWriter().getOutput().contains(expectedOutput), "Incorrect output:" + expectedOutput);
    }

    @Test
    public void testInteractiveLogin() throws Exception {
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

        Mockit.tearDownMocks();
    }

    @Test
    public void testLoginCommandCorrectlyStoresInputs() throws Exception {
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



    @DataProvider(name = "pluginTestData")
    public Object[][] pluginTestData() {
        return new Object[][]{
                {DefaultPlugin.UnplugCommand.class, "Removing plugin: strangeApp\n....not found\n", new String[]{"strangeApp"}, true}
                , {DefaultPlugin.PluginCommand.class, "The maven artifact associated with the plugin could not be found.\n", new String[]{"strangeApp"}, true}
        };
    }

    @Test(dataProvider = "pluginTestData")
    public void testMissingPlugin(Class<? extends Command> commandClass, String expectedOutput, String[] args, boolean exactOutput) throws Exception {
    	TestCommandContext context = new TestCommandContext().withCommandArguments(args).withVmForceClient(new VMForceClient());
        Injector injector = Guice.createInjector(new TestModule());
        injector.getInstance(TestCliforceAccessor.class).setWriter(context.getCommandWriter());
        Command command = injector.getInstance(commandClass);
        command.execute(context);

        String actualOutput = context.out();
        if (exactOutput) {
            Assert.assertEquals(actualOutput, expectedOutput, "Unexpected output for " + command + ": " + actualOutput);
        } else {
            Assert.assertTrue(actualOutput.contains(expectedOutput), "Unexpected output for " + command + ": " + actualOutput);
        }
    }

}
