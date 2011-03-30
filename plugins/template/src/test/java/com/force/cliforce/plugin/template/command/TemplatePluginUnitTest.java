package com.force.cliforce.plugin.template.command;


import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestModule;
import com.google.inject.Guice;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

public class TemplatePluginUnitTest {

    @Test
    public void templateOutputDosentHaveUglyPrefix() throws Exception {
        NewProjectCommand cmd = Guice.createInjector(new TestModule()).getInstance(NewProjectCommand.class);
        File workingDir = new File(new File(System.getProperty("user.home")), "template-test");
        if (!workingDir.exists()) {
            workingDir.mkdir();
        }
        String project = "template" + Long.toString(System.currentTimeMillis());
        TestCommandContext ctx = new TestCommandContext().withCommandArguments("--dir", workingDir.getAbsolutePath(), project);
        cmd.execute(ctx);
        Assert.assertFalse(ctx.out().contains("sh->mvn"));
        File proj = new File(workingDir, project);
        Assert.assertTrue(proj.exists());
        Assert.assertTrue(new File(proj, "pom.xml").exists());
    }
}
