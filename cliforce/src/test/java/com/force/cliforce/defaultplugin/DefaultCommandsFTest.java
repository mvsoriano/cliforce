package com.force.cliforce.defaultplugin;


import com.force.cliforce.Command;
import com.force.cliforce.DefaultPlugin;
import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DefaultCommandsFTest {

    @Test
    public void testClasspathCommandWithNonExistentPlugin() throws Exception {
        DefaultPlugin.ClasspathCommand cmd = getInjectedCommand(DefaultPlugin.ClasspathCommand.class);
        TestCommandContext ctx = new TestCommandContext().withCommandArguments("nonexistent");
        cmd.execute(ctx);
        Assert.assertTrue(ctx.getCommandWriter().getOutput().contains("No such plugin: nonexistent"));
        Assert.assertFalse(ctx.getCommandWriter().getOutput().contains("NullPointerException"));
    }

    @Test
    public void testInstallNonexistentPlugin() throws Exception {
        DefaultPlugin.PluginCommand cmd = getInjectedCommand(DefaultPlugin.PluginCommand.class);
        TestCommandContext ctx = new TestCommandContext().withCommandArguments("nonexistent");
        cmd.execute(ctx);
        Assert.assertTrue(ctx.getCommandWriter().getOutput().contains("The maven artifact associated with the plugin could not be found."));
        Assert.assertFalse(ctx.getCommandWriter().getOutput().contains("DependencyResolutionException"));
    }

    private <T extends Command> T getInjectedCommand(Class<T> cmd) {
        return getTestInjector().getInstance(cmd);
    }

    private Injector getTestInjector() {
        return Guice.createInjector(new TestModule());
    }


}
