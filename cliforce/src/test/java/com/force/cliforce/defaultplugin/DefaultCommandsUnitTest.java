/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.force.cliforce.defaultplugin;

import static com.force.cliforce.Util.newLine;
import static com.force.cliforce.Util.withNewLine;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.force.cliforce.Command;
import com.force.cliforce.DefaultPlugin;
import com.force.cliforce.TestCliforceAccessor;
import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestModule;
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
                {withNewLine("sh: Executing: echo something") + "  something", new String[]{"echo", "something"}, true}
                , {withNewLine("sh: Executing: abc") + "The command failed to execute. Please check the path to the executable you provided",
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
                {DefaultPlugin.UnplugCommand.class, withNewLine("Removing plugin: strangeApp") + withNewLine("....not found"),
                    new String[]{"strangeApp"}, true}
                , {DefaultPlugin.PluginCommand.class, withNewLine("The maven artifact associated with the plugin could not be found."),
                    new String[]{"strangeApp"}, true}
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

        String[] actualOutput = context.out().split(newLine());


        int j = 0;
        for (int i = 0; i < actualOutput.length; i++) {
            if (actualOutput[i].trim().equals("")) {
                continue;
            }

            Assert.assertTrue(j < expectedOutput.length, "Actual output exceeds expected output. Found:" + actualOutput[i]);
            Assert.assertEquals(actualOutput[i].trim(), expectedOutput[j],
                    "Actual output line:" + withNewLine(actualOutput[i].trim()) + "Expected output line:" + expectedOutput[j]);
            j++;
        }

        Assert.assertEquals(j, expectedOutput.length, "Actual output does not contain:" + expectedOutput[j - 1]);
    }

}
