package com.force.cliforce;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import scala.actors.threadpool.Arrays;

import com.force.cliforce.plugin.app.AppPlugin;
import com.sforce.ws.ConnectionException;

/**
 * 
 * Tests for file path completion in app commands
 *
 * @author jeffrey.lai
 * @since javasdk-21.0.2-BETA
 */
public class AppCommandFilePathCompletorTest extends BaseCommandCompletorTest {
    
    private final String testParentDirPath = System.getProperty("basedir") + "/target/testac";
    private final String testDirContainsZeroFilesPath = testParentDirPath + "/zerofiles";
    private final String testDirContainsOneFilePath = testParentDirPath + "/onefile";
    private final String testDirContainsThreeFilesPath = testParentDirPath + "/threefiles";
    private final String testDirContainsFiftyFilesPath = testParentDirPath + "/fiftyfiles";
    private final String testDirHasSpaceInName = testParentDirPath + "/space directory";
    
    @Override
    public String getPluginArtifact() {
        return "app";
    }

    @Override
    public Plugin getPlugin() {
        return new AppPlugin();
    }
    
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
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAppPushPathCompletionWithZeroChoices() {
        String buffer = "app:push -p " + testDirContainsZeroFilesPath + "/";
        int cursor = buffer.length() + 1;
        runCompletorTestCase(buffer, cursor, Arrays.asList(new String[] {" --instances, -i  <Number of instances to deploy (default 1)>",  
                " --mem, -m        <Memory to allocate to the application, in MB (default 512)(valid values 64, 128, 256, 512 or 1024)>", " <main param>     <Name of the application to push>"}));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAppPushPathCompletionWithOneChoice() {
        runCompletorTestCase("app:push -p " + testDirContainsOneFilePath + "/", 0, Arrays.asList(new String[] {"app:push -p " + testDirContainsOneFilePath + "/test0.txt "}));
    }
    
    // TODO re-enable this test once W-933703 is resolved
    @SuppressWarnings("unchecked")
    @Test(enabled=false)
    public void testAppPushPathCompletionWithSpaceInDirName() {
        runCompletorTestCase("app:push -p " + testDirHasSpaceInName + "/", 0, Arrays.asList(new String[] {"app:push -p " + testDirHasSpaceInName + "/test0.txt "}));        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAppPushPathCompletionWithThreeChoices() {
        String buffer = "app:push -p " + testDirContainsThreeFilesPath + "/";
        int cursor = buffer.length();
        runCompletorTestCase(buffer, cursor, Arrays.asList(new String[] {"test0.txt ", "test1.txt ", "test2.txt "}));
    }
    
    @SuppressWarnings("unchecked")
    @Test(enabled=false)
    public void testAppPushPathCompletionWithFiftyChoices() {
        // TODO need to figure out what correct expected results are for this test case before enabling
        // there are 50+ possible files to for the tab path completion
        String buffer = "app:push -p " + testDirContainsFiftyFilesPath + "/";
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
