package com.force.cliforce.plugin.app;


import com.force.cliforce.Command;
import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestConnectionManager;
import com.force.cliforce.TestModule;
import com.force.cliforce.plugin.app.command.DeleteAppCommand;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AppPluginFTest {


    @Test
    public void deletionOfNonExistentApp() throws Exception {
        Injector testInjector = getTestInjector();
        TestConnectionManager testConnectionManager = testInjector.getInstance(TestConnectionManager.class);
        testConnectionManager.loadLogin();
        testConnectionManager.doLogin();
        DeleteAppCommand cmd = testInjector.getInstance(DeleteAppCommand.class);
        TestCommandContext ctx = new TestCommandContext().withCommandArguments("nonexistent").withVmForceClient(testConnectionManager.getVmForceClient());
        cmd.execute(ctx);
        Assert.assertTrue(ctx.out().contains("the application was not found"), ctx.out());
        Assert.assertFalse(ctx.out().contains("done"), ctx.out());
    }


    private <T extends Command> T getInjectedCommand(Class<T> cmd) {
        return getTestInjector().getInstance(cmd);
    }

    private Injector getTestInjector() {
        return Guice.createInjector(new TestModule());
    }


}
