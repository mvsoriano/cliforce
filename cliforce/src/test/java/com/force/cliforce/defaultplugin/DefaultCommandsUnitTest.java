package com.force.cliforce.defaultplugin;


import java.util.Arrays;
import java.util.List;

import mockit.Mock;
import mockit.Mockit;

import org.testng.Assert;
import org.testng.annotations.*;

import com.force.cliforce.*;
import com.force.cliforce.command.DebugCommand;
import com.google.inject.Guice;
import com.google.inject.Injector;


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
                {"sh: Executing: echo something\n  something", new String[]{"echo", "something"}, true}
                , {"sh: Executing: abc\nThe command failed to execute. Please check the path to the executable you provided",
                        new String[]{"abc"}, true}
                , {"stderr", new String[]{"dir ", "-3", "/3"} /*-3 for unix, /3 for win*/, false /* check that stderr is not in the output */}
        };
    }

    @Test(dataProvider = "expectedData")
    public void testShellCommandWithArgs(String expectedOutput, String[] args, boolean testForMatch) throws Exception {
        DefaultPlugin.ShellCommand cmd = new DefaultPlugin.ShellCommand();
        TestCommandContext ctx = new TestCommandContext().withCommandArguments(args);
        cmd.execute(ctx);
        if (testForMatch) {
            Assert.assertTrue(ctx.getCommandWriter().getOutput().contains(expectedOutput), "Incorrect output:" + expectedOutput);
        } else {
            Assert.assertFalse(ctx.getCommandWriter().getOutput().contains(expectedOutput), "Incorrect output:" + expectedOutput);
        }
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
        TestCommandContext context = new TestCommandContext().withCommandArguments(args);
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

    @Test
    public void testDebugCommand() throws Exception {
        String[] expectedOutput = new String[]{
                "Setting logger level to DEBUG"
                , "Setting logger level to OFF"
        };

        TestCommandContext context = new TestCommandContext();
        Injector injector = Guice.createInjector(new TestModule());
        Command command = injector.getInstance(DebugCommand.class);
        injector.getInstance(TestCliforceAccessor.class).setWriter(context.getCommandWriter());
        command.execute(context);
        command.execute(context.withCommandArguments("--off"));

        String[] actualOutput = context.out().split("\n");


        int j = 0;
        for (int i = 0; i < actualOutput.length; i++) {
            if (actualOutput[i].trim().equals("")) {
                continue;
            }

            Assert.assertTrue(j < expectedOutput.length, "Actual output exceeds expected output. Found:" + actualOutput[i]);
            Assert.assertEquals(actualOutput[i].trim(), expectedOutput[j], "Actual output line:" + actualOutput[i].trim() + "\nExpected output line:" + expectedOutput[j]);
            j++;
        }

        Assert.assertEquals(j, expectedOutput.length, "Actual output does not contain:" + expectedOutput[j - 1]);
    }

}
