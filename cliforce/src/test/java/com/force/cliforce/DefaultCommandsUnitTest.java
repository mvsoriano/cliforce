package com.force.cliforce;


import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests commands in the default plugin that dont require any guice injection.
 */
public class DefaultCommandsUnitTest {

    @Test
    public void syspropsCommand() throws Exception {
        String stamp = Long.toString(System.currentTimeMillis());
        System.setProperty("test.timestamp", stamp);
        DefaultPlugin.SyspropsCommand cmd = new DefaultPlugin.SyspropsCommand();
        TestCommandContext ctx = new TestCommandContext();
        cmd.execute(ctx);
        Assert.assertTrue(ctx.getCommandWriter().getOutput().contains("test.timestamp"), "Test output did not contain 'test.timestamp'");
        Assert.assertTrue(ctx.getCommandWriter().getOutput().contains(stamp), "Test output did not contain "+stamp);
    }

}
