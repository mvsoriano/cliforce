package com.force.cliforce;

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
        runCompletorTestCase("app", 0, Arrays.asList(new String[] {"app:apps", "app:delete", "app:push", "app:restart", "app:start", "app:stop", "app:tail"}));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAppsCommandNoSpace() {
        runCompletorTestCase("app:apps", 0, Arrays.asList(new String[] {"app:apps "}));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAppsCommandHelpText() {
        runCompletorTestCase("app:apps ", 9, Arrays.asList(new String[] {" ", "lists deployed apps"}));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteCommandNoSpace() {
        runCompletorTestCase("app:delete", 0, Arrays.asList(new String[] {"app:delete "}));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteCommandHelpText() {
        runCompletorTestCase("app:delete ", 11, Arrays.asList(new String[] {" main param: the name of the application", " "}));
    }

}
