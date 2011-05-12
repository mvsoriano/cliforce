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

package com.force.cliforce.plugin.template.command;

import static com.force.cliforce.Util.withNewLine;
import static com.force.cliforce.Util.withSeparator;

import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestModule;
import com.force.cliforce.Util;
import com.google.inject.Guice;
import com.sforce.async.BulkConnection;
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
        Assert.assertTrue(proj.exists(), "template project directory does not exist");
        Assert.assertTrue(new File(proj, "pom.xml").exists(), "template project pom.xml does not exist");
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
        Assert.assertTrue(proj.exists(),  "template project directory does not exist");
        Assert.assertTrue(new File(proj, "pom.xml").exists(), "template project pom.xml does not exist");
    }

    @Test
    public void testInvalidDirectoryForTemplateCreate() throws Exception {
        NewProjectCommand command = Guice.createInjector(new TestModule()).getInstance(NewProjectCommand.class);
        TestCommandContext commandContext = new TestCommandContext().withCommandArguments("project", "-d",
                withSeparator("totally") + withSeparator("invalid") + withSeparator("path") + "abc123doeraeme");
        command.execute(commandContext);
        Assert.assertTrue(commandContext.out().contains(withSeparator("totally") + withSeparator("invalid")
                + withSeparator("path") + withNewLine("abc123doeraeme does not exist or is not a directory")),
                "Unexpected output for " + command + " " + commandContext.out());
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
        public BulkConnection getBulkConnection() {
            throw new RuntimeException();
        }

    }
}
