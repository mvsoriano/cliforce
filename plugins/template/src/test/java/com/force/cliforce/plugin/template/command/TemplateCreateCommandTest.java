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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.force.cliforce.BaseCliforceCommandTest;
import com.force.cliforce.Plugin;
import com.force.cliforce.plugin.template.TemplatePlugin;
import com.sforce.ws.ConnectionException;

/**
 * Tests for the template:create command.
 *
 * @author Jeff Lai
 * 
 */
public class TemplateCreateCommandTest extends BaseCliforceCommandTest {
    
    private final String templateParentDir = System.getProperty("basedir") + "/target/test-template";
    
    @Override
    @BeforeClass
    public void classSetup() throws InterruptedException, IOException, ConnectionException, ServletException {
        super.classSetup();
        File parentDir = new File(templateParentDir);
        // in case a previous test run failed to cleanup, delete any existing directories we use
        if (parentDir.exists()) FileUtils.deleteDirectory(parentDir); 
        // create directory used for testing
        parentDir.mkdirs();
    }
    
    @AfterClass(alwaysRun=true)
    public void classTeardown() throws IOException  {
        File parentDir = new File(templateParentDir);
        if (parentDir.exists()) FileUtils.deleteDirectory(parentDir);
    }

    @Override
    public String getPluginArtifact() {
        return "template";
    }

    @Override
    public Plugin getPlugin() {
        return new TemplatePlugin();
    }
    
    @Test
    public void testCreateTemplateAndInstall() throws IOException, ConnectionException, ServletException, InterruptedException {
        System.out.println("creating template.  please wait...");
        String output = runCommand("template:create springmvc -d " + templateParentDir + " -p com.pack");
        assertMvnBuildSuccessful(output, "creation of template not successful");
        // execute mvn install on the created template to make sure it compiles properly
        System.out.println("running maven build on template.  please wait...");
        output = runProcess("mvn install -DskipTests -e", templateParentDir + "/springmvc");
        assertMvnBuildSuccessful(output, "mvn install without tests was not successful");
        // execute mvn install with tests enabled
        uncommentEntityAnnotation(templateParentDir + "/springmvc/src/main/java/com/pack/model/MyEntity.java");
        output = runProcess("mvn install -e", templateParentDir + "/springmvc");
        assertMvnBuildSuccessful(output, "mvn install with tests was not successful");
    }
    
    /**
     * This method asserts that a mvn build was successful
     * @param output is the console output from executed command
     * @param assertMsg additional message to be printed in test failure report
     */
    private void assertMvnBuildSuccessful(String output, String assertMsg) {
        Assert.assertTrue(output.contains("BUILD SUCCESSFUL"), assertMsg + "\nConsole output contained:\n" +
                "===============================================================================\n" +
                output +
                "===============================================================================\n");
    }
    
    /**
     * This method uses ProcessBuilder to execute a command in the shell.
     * @param cmd
     * @param workingDir
     * @return console output of command as a string
     * @throws IOException
     * @throws InterruptedException
     */
    private String runProcess(String cmd, String workingDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
        Map<String, String> env = pb.environment();
        // set force url, which is needed to execute tests in generated template
        env.put("FORCE_FORCEDATABASE_URL", getConnectionManager().getCurrentEnv().getUrl());
        pb.directory(new File(workingDir));
        Process p = pb.start();
        int retVal = p.waitFor();
        // convert console output into a String
        BufferedReader br = null;
        StringBuilder sb = null;
        try {
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            sb = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            } 
        } finally {
            if (br != null) br.close();
        }
        Assert.assertEquals(retVal, 0, "Process was not terminated normally for the command " + cmd + 
                "\nThe console output is printed below:\n" +
                "===============================================================================\n" +
                sb.toString() +
                "===============================================================================\n");
        // kill process
        p.destroy();
        return sb.toString();
    }
    
    private void uncommentEntityAnnotation(String pathToEntityFile) throws IOException {
        // read Entity java file and write new modified version of entity file
        BufferedReader br = null;
        File entityFile = new File(pathToEntityFile);
        File tmpEntityFile = new File(pathToEntityFile + ".tmp");
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(entityFile)));
            String line = null;
            bw = new BufferedWriter(new FileWriter(tmpEntityFile));
            while ((line = br.readLine()) != null) {
                // if the line contains the commented Entity annotation, replace it
                if (line.contains("//@Entity")) {
                    line = line.replace("//@Entity", "@Entity");
                }
                bw.write(line + "\n");
            } 
        } finally {
            if (br != null) br.close();
            if (bw != null) bw.close();
        }
        // delete and replace entity java file with new one
        Assert.assertTrue(entityFile.delete(), "Did not successfully delete the file " + entityFile.getCanonicalPath());
        Assert.assertTrue(tmpEntityFile.renameTo(entityFile), 
                "Did not successfully rename " + tmpEntityFile.getCanonicalPath() + " to " + entityFile.getCanonicalPath());
    }

}
