package com.force.cliforce.defaultplugin;

import java.io.IOException;
import org.testng.annotations.Test;

import scala.actors.threadpool.Arrays;

import com.force.cliforce.BaseCommandCompletorTest;
import com.force.cliforce.CLIForce;
import com.force.cliforce.Plugin;

public class DefaultCommandsCompletorTest extends BaseCommandCompletorTest {
    
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
    
    @SuppressWarnings("unchecked")
    @Test
    public void testBasicCommandCompletion() {
        runCompletorTestCase("e", 0, Arrays.asList(new String[] {"env", "exit"}));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testEmptyStringBuffer() {
        runCompletorTestCase("", 0, Arrays.asList(new String[] {"banner", "classpath", "debug", "env", "exit", "help", "history", "login", "plugin", "require", "sh", "sysprops", "unplug", "version"}));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testFullCommandInBufferNoArgs() {
        runCompletorTestCase("banner", 0, Arrays.asList(new String[] {"banner "}));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testBannerCommandHelpText() {
        runCompletorTestCase("banner ", 7, Arrays.asList(new String[] {" ", "print the banner"}));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testClasspathCommandOneHyphen() {
        runCompletorTestCase("classpath -", 12, Arrays.asList(new String[] {"--sort, -s  <sort the returned list of files on the classpath alphabetically>", " "}));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testClasspathCommandTwoHyphens() {
        runCompletorTestCase("classpath --", 0, Arrays.asList(new String[] {"classpath --sort "}));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testRequireCommandHelpText() {
        runCompletorTestCase("require ", 8, Arrays.asList(new String[] {"<main param>   <maven artifact id for an artifact in group com.force.cliforce.plugin>", 
                "--version, -v  <maven artifact version for the specified artifact, if unspecified RELEASE meta-version is used>"}));
    }

}
