package com.force.cliforce.plugin.template.command;


import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestModule;
import com.force.cliforce.Util;
import com.google.inject.Guice;
import com.sforce.async.RestConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

public class TemplatePluginUnitTest {

    @Test
    public void templateOutputDosentHaveUglyPrefix() throws Exception {
        NewProjectCommand cmd = Guice.createInjector(new TestModule()).getInstance(NewProjectCommand.class);
        File workingDir = new File(Util.getCliforceHome(), "template-test");
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


    @Test
    public void templateConnectionExceptions() throws Exception {
        NewProjectCommand cmd = Guice.createInjector(new TestModule()).getInstance(NewProjectCommand.class);
        File workingDir = new File(Util.getCliforceHome(), "template-test");
        if (!workingDir.exists()) {
            workingDir.mkdir();
        }
        String project = "template" + Long.toString(System.currentTimeMillis());
        TestCommandContext ctx = new TestCommandContextConnectionExceptions().withCommandArguments("--dir", workingDir.getAbsolutePath(), project);
        //As long as this doesn't throw an exception and the command still works the test passes
        cmd.execute(ctx);
        File proj = new File(workingDir, project);
        Assert.assertTrue(proj.exists());
        Assert.assertTrue(new File(proj, "pom.xml").exists());
    }

    private class TestCommandContextConnectionExceptions extends TestCommandContext {

        @Override
        public MetadataConnection getMetadataConnection() {
            throw new RuntimeException();
        }

        @Override
        public PartnerConnection getPartnerConnection() {
            throw new RuntimeException();
        }

        @Override
        public RestConnection getRestConnection() {
            throw new RuntimeException();
        }

    }
}
