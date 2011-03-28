package com.force.cliforce.defaultplugin;


import com.force.cliforce.Command;
import com.force.cliforce.DefaultPlugin;
import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestModule;
import com.google.inject.Guice;
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


    private <T extends Command> T getInjectedCommand(Class<T> cmd) {
        return Guice.createInjector(new TestModule()).getInstance(cmd);
    }

}
