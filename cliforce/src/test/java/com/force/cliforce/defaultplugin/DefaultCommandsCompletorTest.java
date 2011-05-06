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

import com.force.cliforce.BaseCliforceCommandTest;
import com.force.cliforce.CLIForce;
import com.force.cliforce.Plugin;
import org.testng.annotations.Test;
import scala.actors.threadpool.Arrays;

import java.io.IOException;
import java.util.Collections;

/**
 * Tests command completion for the default command, i.e. all commands that are loaded
 * by the default plugin.
 *
 * @author jeffrey.lai, dhain
 */
@SuppressWarnings("unchecked")
public class DefaultCommandsCompletorTest extends BaseCliforceCommandTest {

    @Override
    public void setupCLIForce(CLIForce c) throws IOException {
        //overriding this with an empty method because we don't want to install additional plugins
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

    @Test
    public void noCompletionInTheMiddle() {
        runCompletorTestCase("e", 0, 0, Collections.<String>emptyList());
        runCompletorTestCase("class", 2, 0, Collections.<String>emptyList());
    }

    @Test
    public void testBasicCommandCompletion() {
        runCompletorTestCase("e", 0, Arrays.asList(new String[]{"env", "exit"}));
    }

    @Test
    public void testEmptyStringBuffer() {
        runCompletorTestCase("", 0, Arrays.asList(new String[]{"banner", "classpath", "debug", "env", "exit", "help", "history", "plugin", "require", "sh", "sysprops", "unplug", "version"}));
        runCompletorTestCase(" ", 0, Arrays.asList(new String[]{"banner", "classpath", "debug", "env", "exit", "help", "history", "plugin", "require", "sh", "sysprops", "unplug", "version"}));
    }

    @Test
    public void testFullCommandInBufferNoArgs() {
        runCompletorTestCase("banner", 0, Arrays.asList(new String[]{"banner "}));
    }

    @Test
    public void testBannerCommandHelpText() {
        runCompletorTestCase("banner ", 7, Arrays.asList(new String[]{" ", "print the banner"}));
    }

    @Test
    public void testClasspathPartialCommand() {
        runCompletorTestCase("cl", 0, Arrays.asList(new String[]{"classpath "}));
    }

    @Test
    public void testClasspathCommandOneHyphen() {
        runCompletorTestCase("classpath -", 10, Arrays.asList(new String[]{"-s"}));
    }

    @Test
    public void testClasspathCommandTwoHyphens() {
        runCompletorTestCase("classpath --", 0, Arrays.asList(new String[]{"classpath --sort "}));
    }

    @Test
    public void testClasspathCommandNoLoadedPlugins() {
        runCompletorTestCase("classpath --sort ", 17, Arrays.asList(new String[]{" main param: name of the plugin to get the classpath for, or none for the cliforce classpath", " "}));
    }

    @Test(enabled = false)
    public void testDebug() {
        //TODO: write automation when fixing W-932298 and W-932306
    }

    @Test
    public void testEnvHelpText() {
        runCompletorTestCase("env ", 4, Arrays.asList(new String[]{" ", "Display the current environment variables"}));
    }

    @Test
    public void testExitHelpText() {
        runCompletorTestCase("exit ", 5, Arrays.asList(new String[]{" ", "Exit this shell"}));
    }

    //W-936274
    @Test
    public void testHelpHelpText() {
        runCompletorTestCase("help ", 5, Arrays.asList(new String[]{" ", "Display this help message, or help for a specific command\n\tUsage: help <command>"}));
    }

    @Test
    public void testHistoryHelpText() {
        runCompletorTestCase("history ", 8, Arrays.asList(new String[]{" ", "Show history of previous commands"}));
    }

    @Test
    public void testPlugin() {
        runCompletorTestCase("pl", 0, Arrays.asList(new String[]{"plugin "}));
        runCompletorTestCase("plugin ", 7, Arrays.asList(new String[]{"<main param>   <maven artifact id for an artifact in group com.force.cliforce.plugin>",
                "--version, -v  <maven artifact version for the specified artifact, if unspecified RELEASE meta-version is used>"}));
        runCompletorTestCase("plugin --version ", 0, Arrays.asList(new String[]{"plugin --version "}));
    }

    @Test
    public void testRequire() {
        runCompletorTestCase("r", 0, Arrays.asList(new String[]{"require "}));
        runCompletorTestCase("require ", 8, Arrays.asList(new String[]{"<main param>   <maven artifact id for an artifact in group com.force.cliforce.plugin>",
                "--version, -v  <maven artifact version for the specified artifact, if unspecified RELEASE meta-version is used>"}));
        runCompletorTestCase("require --version ", 0, Arrays.asList(new String[]{"require --version "}));
    }

    @Test
    public void testSh() {
        runCompletorTestCase("sh ", 3, Arrays.asList(new String[]{" ", "Execute the rest of the command on the OS"}));
    }

    @Test
    public void testSysprops() {
        runCompletorTestCase("sy", 0, Arrays.asList(new String[]{"sysprops "}));
        runCompletorTestCase("sysprops ", 9, Arrays.asList(new String[]{" ", "Display the current Java system properties"}));
    }

    @Test
    public void testUnplug() {
        runCompletorTestCase("u", 0, Arrays.asList(new String[]{"unplug "}));
        runCompletorTestCase("unplug ", 7, Arrays.asList(new String[]{" ", "removes a plugin and its commands from the shell"}));
    }

    @Test
    public void testVersion() {
        runCompletorTestCase("v", 0, Arrays.asList(new String[]{"version "}));
        runCompletorTestCase("version ", 8, Arrays.asList(new String[]{" ", "Show the current running version of cliforce"}));
    }


    
}
