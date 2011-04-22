package com.force.cliforce;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import scala.actors.threadpool.Arrays;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.force.cliforce.plugin.codegen.command.JPAClass;
import com.force.cliforce.plugin.template.command.NewProjectCommand;
import com.sforce.ws.ConnectionException;

/**
 * 
 * Tests for file path completion in commands
 *
 * @author jeffrey.lai
 * @since javasdk-21.0.2-BETA
 */
public class FilePathCompletorTest extends BasePluginsTest {
    
    private final String testParentDirPath = System.getProperty("basedir") + "/target/testac";
    private final String testDirContainsZeroFilesPath = testParentDirPath + "/zerofiles";
    private final String testDirContainsOneFilePath = testParentDirPath + "/onefile";
    private final String testDirContainsThreeFilesPath = testParentDirPath + "/threefiles";
    private final String testDirContainsFiftyFilesPath = testParentDirPath + "/fiftyfiles";
    private final String testDirHasSpaceInName = testParentDirPath + "/space directory";
    
    
    @Override
    @BeforeClass
    public void classSetup() throws InterruptedException, IOException, ConnectionException, ServletException {
        super.classSetup();
        File parentDir = new File(testParentDirPath);
        // in case a previous test run failed to cleanup, delete any existing directories we use
        if (parentDir.exists()) FileUtils.deleteDirectory(parentDir); 
        
        // setup test directories and files
        createDir(testDirContainsZeroFilesPath);

        createDir(testDirContainsOneFilePath);
        createFiles(testDirContainsOneFilePath, 1);
        
        createDir(testDirContainsThreeFilesPath);
        createFiles(testDirContainsThreeFilesPath, 3);
        
        createDir(testDirContainsFiftyFilesPath);
        createFiles(testDirContainsFiftyFilesPath, 50);
        
        createDir(testDirHasSpaceInName);
        createFiles(testDirHasSpaceInName, 1);
    }
    
    @AfterClass(alwaysRun=true)
    public void classTeardown() throws IOException  {
        File parentDir = new File(testParentDirPath);
        if (parentDir.exists()) FileUtils.deleteDirectory(parentDir);
    }
    
    @DataProvider(name = "commandStubWithClass")
    public Object[][] commandStubWithClass() {
        return new Object[][] {
                new Object[] {"template:create -d ", NewProjectCommand.class},
                new Object[] {"codegen:jpaClass --destDir ", JPAClass.class},
                new Object[] {"codegen:jpaClass --projectDir ", JPAClass.class}
        };
    }
    
    @DataProvider(name = "commandStub")
    public Object[][] commandStub() {
        return new Object[][] {
                new Object[] {"template:create -d "},
                new Object[] {"codegen:jpaClass --destDir "},
                new Object[] {"codegen:jpaClass --projectDir "}
        };
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test(dataProvider = "commandStubWithClass")
    public void testPathCompletionWithZeroChoices(String cmdStub, Class cmdClass) throws InstantiationException, IllegalAccessException {
        // create expected candidates
        JCommand command = (JCommand)cmdClass.newInstance();
        JCommander j = new JCommander(command.getArgs());
        List<ParameterDescription> params = j.getParameters();
        List candidates = new ArrayList<String>();       
        for (ParameterDescription param : params) {
            if (Arrays.asList(param.getParameter().names()).contains(cmdStub.split(" ")[1])) { 
                // skip the parameter that's already included in the stub
                continue;
            } else {
                String[] sortedKey = param.getParameter().names();
                Arrays.sort(sortedKey);
                String stringSK = "";
                for (int i = 0; i < sortedKey.length; i++) {
                    stringSK = stringSK + " " + sortedKey[i];
                    if (i < sortedKey.length - 1) stringSK = stringSK + ",";
                }
                candidates.add(stringSK + " <" + param.getParameter().description() + ">");
            }
        }
        // add main parameter if it's not null
        if (j.getMainParameter() != null) {
            candidates.add(" <main param> <" + j.getMainParameterDescription() + ">");
        }     
        Collections.sort(candidates);
        String buffer = cmdStub + testDirContainsZeroFilesPath + "/";
        int cursor = buffer.length() + 1;
        runCompletorTestCase(buffer, cursor, candidates);
    }
    
    @SuppressWarnings("unchecked")
    @Test(dataProvider = "commandStub")
    public void testPathCompletionWithOneChoice(String cmdStub) {
        runCompletorTestCase(cmdStub + testDirContainsOneFilePath + "/", 0, Arrays.asList(new String[] {cmdStub + testDirContainsOneFilePath + "/test0.txt "}));
    }
    
    // TODO re-enable this test once W-933703 is resolved
    @SuppressWarnings("unchecked")
    @Test(enabled=false, dataProvider = "commandStub")
    public void testPathCompletionWithSpaceInDirName(String cmdStub) {
        runCompletorTestCase(cmdStub + testDirHasSpaceInName + "/", 0, Arrays.asList(new String[] {cmdStub + testDirHasSpaceInName + "/test0.txt "}));        
    }
    
    @SuppressWarnings("unchecked")
    @Test(dataProvider = "commandStub")
    public void testPathCompletionWithThreeChoices(String cmdStub) {
        String buffer = cmdStub + testDirContainsThreeFilesPath + "/";
        int cursor = buffer.length();
        runCompletorTestCase(buffer, cursor, Arrays.asList(new String[] {"test0.txt ", "test1.txt ", "test2.txt "}));
    }
    
    @SuppressWarnings("unchecked")
    @Test(enabled=false, dataProvider = "commandStub")
    public void testPathCompletionWithFiftyChoices(String cmdStub) {
        // TODO need to figure out what correct expected results are for this test case before enabling
        // there are 50+ possible files to for the tab path completion
        String buffer = cmdStub + testDirContainsFiftyFilesPath + "/";
        int cursor = buffer.length();
        runCompletorTestCase(buffer, cursor, Arrays.asList(new String[] {""}));
    }
    
    private void createDir(String dirPath) {
        File testDir = new File(dirPath);
        testDir.mkdirs();
        Assert.assertTrue(testDir.exists(), "failed to create directory " + dirPath);        
    }
    
    private void createFiles(String dirPath, int numFiles) throws IOException {
        for (int i = 0; i < numFiles; i++) {
            File testFile = new File(dirPath + "/test" + i + ".txt");
            testFile.createNewFile();
            Assert.assertTrue(testFile.exists(), "failed to create " + testFile.getCanonicalPath()); 
        }
    }

}
