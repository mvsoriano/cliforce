package com.force.cliforce;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import scala.actors.threadpool.Arrays;

import com.force.cliforce.plugin.app.AppPlugin;

public class AppCommandCompletorTest extends BaseCommandCompletorTest {

    @Override
    public String getPluginArtifact() {
        return "app";
    }

    @Override
    public Plugin getPlugin() {
        return new AppPlugin();
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testBasicAppCommandCompletion() {
        List<CharSequence> candidates = new ArrayList<CharSequence>();
        String buffer = "app";
        int cursor = getCompletor().complete(buffer, buffer.length(), candidates);
        Assert.assertEquals(cursor, 0, "unexpected cursor position");
        verifyCandidateList(candidates, Arrays.asList(new String[] {"app:apps", "app:delete", "app:push", "app:restart", "app:start", "app:stop", "app:tail"}));
        
    }

}
