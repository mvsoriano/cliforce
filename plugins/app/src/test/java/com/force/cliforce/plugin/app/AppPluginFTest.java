package com.force.cliforce.plugin.app;


import com.force.cliforce.*;
import com.force.cliforce.plugin.app.command.DeleteAppCommand;
import com.force.cliforce.plugin.app.command.RestartCommand;
import com.force.cliforce.plugin.app.command.StartCommand;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;

public class AppPluginFTest {

    Injector testInjector;
    PluginManager testPluginManager;
    ConnectionManager testConnectionManager;


    @BeforeTest
    public void createInjectorAndInstallAppPlugin() throws IOException {
        testInjector = Guice.createInjector(new TestModule());
        TestPluginInstaller testPluginInstaller = testInjector.getInstance(TestPluginInstaller.class);
        testPluginInstaller.installDefaultPlugin();
        testPluginInstaller.installPlugin("app", "LATEST", new AppPlugin(), true);
        testConnectionManager = testInjector.getInstance(ConnectionManager.class);
        testConnectionManager.loadLogin();
        testConnectionManager.doLogin();
        testPluginManager = testInjector.getInstance(PluginManager.class);
    }

    @Test
    public void deletionOfNonExistentApp() throws Exception {
        TestCommandContext ctx = new TestCommandContext().withCommandArguments("nonexistent").withVmForceClient(testConnectionManager.getVmForceClient());
        DeleteAppCommand cmd = getInjectedCommand("app:delete");
        cmd.execute(ctx);
        Assert.assertTrue(ctx.out().contains("the application was not found"), ctx.out());
        Assert.assertFalse(ctx.out().contains("done"), ctx.out());
    }

    @Test
    public void cantManuallyPluginOrUnplugAppPlugin() throws Exception {
        TestCommandContext ctx = new TestCommandContext().withCommandArguments("app");
        DefaultPlugin.PluginCommand plug = getInjectedCommand("plugin");
        DefaultPlugin.UnplugCommand unplug = getInjectedCommand("unplug");
        plug.execute(ctx);
        Assert.assertTrue(ctx.out().equals("Manually installing internal plugins [app] is not supported\n"), ctx.out());
        ctx.getCommandWriter().reset();
        unplug.execute(ctx);
        Assert.assertTrue(ctx.out().equals("Removing internal plugins [app] is not supported\n"), ctx.out());
    }
    
    @Test
    public void startOfNonExistentApp() throws Exception {
        TestCommandContext ctx = new TestCommandContext().withCommandArguments("nonexistent").withVmForceClient(testConnectionManager.getVmForceClient());
        StartCommand cmd = getInjectedCommand("app:start");
        cmd.execute(ctx);
        Assert.assertTrue(ctx.out().contains("No such app nonexistent"), ctx.out());
    }
    
    @Test
    public void restartOfNonExistentApp() throws Exception {
    	TestCommandContext ctx = new TestCommandContext().withCommandArguments("nonexistent").withVmForceClient(testConnectionManager.getVmForceClient());
    	RestartCommand cmd = getInjectedCommand("app:restart");
    	cmd.execute(ctx);
    	Assert.assertTrue(ctx.out().contains("No such app nonexistent"), ctx.out());
    }

    <T extends Command> T getInjectedCommand(String name) throws Exception {
        return (T) testPluginManager.getCommand(name);
    }


}
