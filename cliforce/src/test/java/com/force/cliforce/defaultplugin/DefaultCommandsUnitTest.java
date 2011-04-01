package com.force.cliforce.defaultplugin;


import com.force.cliforce.*;
import com.google.inject.Guice;
import com.google.inject.Injector;
import mockit.Mock;
import mockit.Mockit;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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



}
