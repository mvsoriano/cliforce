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

package com.force.cliforce;


import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sforce.ws.ConnectionException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Tests for the default commands in CLIForce.
 * @author sclasen
 * @since
 */
public class DefaultCommandsFTest extends BaseCliforceCommandTest {

    Injector injector;
    TestCommandContext context;

     @BeforeClass
    public void setupTestContext() throws IOException, ServletException {
        injector = Guice.createInjector(new TestModule());
        context = new TestCommandContext();
    }


    @DataProvider(name = "commandExecutionWithArgs")
    public Object[][] providecommandExecutionWithArgs() {
        return new Object[][]{
                { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"abcdefg1234567"}}
              , { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"abcdefg1234567", "-s"}}
              , { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"-s", "abcdefg1234567"}}
              , { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"abcdefg1234567", "--sort"}}
              , { DefaultPlugin.ClasspathCommand.class, "No such plugin: abcdefg1234567\n", new String[]{"--sort", "abcdefg1234567"}}
        };
    }

    @Test(dataProvider = "commandExecutionWithArgs")
    public void testDefaultCommand(Class<? extends Command> commandClass, String expectedOutput, String[] args) throws Exception {
        context = context.withCommandArguments(args);
        Command command = injector.getInstance(commandClass);
        command.execute(context);
        String actualOutput = context.out();
        Assert.assertEquals(actualOutput, expectedOutput, "Unexpected output for " + command + ": " + actualOutput);
    }

    @DataProvider(name = "stringCommands")
    public Object[][] provideStringCommands() {
        return new Object[][] {
                { "!debug --on", "Unknown Command !debug --on\n" }
              , { "!debug", "Unknown Command !debug\n" }
        };
    }

    @Test(dataProvider = "stringCommands")
    public void testStringCommand(String commandLineInput, String expectedOutput) throws IOException, ServletException, InterruptedException, ConnectionException {
        String actualOutput = executeCommand(commandLineInput);
        Assert.assertEquals(actualOutput, expectedOutput, "Unexpected command output: " + actualOutput);
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

    @Override
    public void setupCLIForce(CLIForce c) throws IOException {
        //overriding with an empty method because we don't want to install additional plugins
    }

    @Override
    public String getPluginArtifact() {
        // not needed to test Default commands
        return null;
    }

    @Override
    public Plugin getPlugin() {
        // not needed to test Default commands
        return null;
    }
}
