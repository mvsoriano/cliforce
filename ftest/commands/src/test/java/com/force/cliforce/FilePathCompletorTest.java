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

package com.force.cliforce;

import static com.force.cliforce.Util.withSeparator;

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
    
    private final String testParentDirPath = withSeparator(System.getProperty("user.dir")) + withSeparator("target") + "testac";
    private final String testDirContainsZeroFilesPath = withSeparator(testParentDirPath) + "zerofiles";
    private final String testDirContainsOneFilePath = withSeparator(testParentDirPath) + "onefile";
    private final String testDirContainsThreeFilesPath = withSeparator(testParentDirPath) + "threefiles";
    private final String testDirContainsFiftyFilesPath = withSeparator(testParentDirPath) + "fiftyfiles";
    private final String testDirHasSpaceInName = withSeparator(testParentDirPath) + "space directory";
    
    
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
        String buffer = cmdStub + withSeparator(testDirContainsZeroFilesPath);
        int cursor = buffer.length() + 1;
        runCompletorTestCase(buffer, cursor, candidates);
    }
    
    @SuppressWarnings("unchecked")
    @Test(dataProvider = "commandStub")
    public void testPathCompletionWithOneChoice(String cmdStub) {
        runCompletorTestCase(cmdStub + withSeparator(testDirContainsOneFilePath), 0,
                Arrays.asList(new String[] {cmdStub + withSeparator(testDirContainsOneFilePath) + "test0.txt "}));
    }
    
    // TODO re-enable this test once W-933703 is resolved
    @SuppressWarnings("unchecked")
    @Test(enabled=false, dataProvider = "commandStub")
    public void testPathCompletionWithSpaceInDirName(String cmdStub) {
        runCompletorTestCase(cmdStub + withSeparator(testDirHasSpaceInName), 0,
                Arrays.asList(new String[] {cmdStub + withSeparator(testDirHasSpaceInName) + "test0.txt "}));        
    }
    
    @SuppressWarnings("unchecked")
    @Test(dataProvider = "commandStub")
    public void testPathCompletionWithThreeChoices(String cmdStub) {
        String buffer = cmdStub + withSeparator(testDirContainsThreeFilesPath);
        int cursor = buffer.length();
        runCompletorTestCase(buffer, cursor, Arrays.asList(new String[] {"test0.txt ", "test1.txt ", "test2.txt "}));
    }
    
    @SuppressWarnings("unchecked")
    @Test(enabled=false, dataProvider = "commandStub")
    public void testPathCompletionWithFiftyChoices(String cmdStub) {
        // TODO need to figure out what correct expected results are for this test case before enabling
        // there are 50+ possible files to for the tab path completion
        String buffer = cmdStub + withSeparator(testDirContainsFiftyFilesPath);
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
            File testFile = new File(withSeparator(dirPath) + "test" + i + ".txt");
            testFile.createNewFile();
            Assert.assertTrue(testFile.exists(), "failed to create " + testFile.getCanonicalPath()); 
        }
    }

}
