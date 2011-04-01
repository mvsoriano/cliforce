package com.force.cliforce.defaultplugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
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
        List<CharSequence> candidates = new ArrayList<CharSequence>();
        String buffer = "e";
        int cursor = getCompletor().complete(buffer, buffer.length(), candidates);
        Assert.assertEquals(cursor, 0, "unexpected cursor position");
        verifyCandidateList(candidates, Arrays.asList(new String[] {"env", "exit"}));
    }

}
