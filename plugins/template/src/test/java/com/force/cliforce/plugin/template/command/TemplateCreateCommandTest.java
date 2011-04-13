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
 * @author jeffrey.lai
 * @since 
 */
public class TemplateCreateCommandTest extends BaseCliforceCommandTest {
    
    private final String templateParentDir = System.getProperty("basedir") + "/target/test-template";
    
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
//        File parentDir = new File(templateParentDir);
//        if (parentDir.exists()) FileUtils.deleteDirectory(parentDir);
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
    public void testCreateSpringMVC() throws IOException, ConnectionException, ServletException, InterruptedException {
        String output = runCommand("template:create springmvc -d " + templateParentDir);
        Assert.assertTrue(output.contains("BUILD SUCCESSFUL"), "creation of template not successful");
        // execute mvn install on the created template to make sure it compiles properly
        output = runProcess("mvn install -DskipTests -e", templateParentDir + "/springmvc");
        Assert.assertTrue(output.contains("BUILD SUCCESSFUL"), "mvn install without tests was not successful");
        // execute mvn install with tests enabled
        uncommentEntityAnnotation(templateParentDir + "/springmvc/src/main/java/com/vmf01/springmvc/model/MyEntity.java");
        runProcess("mvn install -e", templateParentDir + "/springmvc");
        Assert.assertTrue(output.contains("BUILD SUCCESSFUL"), "mvn install with tests was not successful");
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
