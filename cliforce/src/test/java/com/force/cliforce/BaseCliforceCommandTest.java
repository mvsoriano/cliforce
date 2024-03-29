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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.sforce.ws.ConnectionException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

import javax.servlet.ServletException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Use this class if you need the CLIForce object in your command test.
 *
 * @author jeffrey.lai
 * @since javasdk-21.0.2-BETA
 */
public abstract class BaseCliforceCommandTest {

    private CLIForce cliForce;
    private ByteArrayOutputStream baos;
    private Injector injector;
    protected CommandCompletor completor;
    protected TestPluginInstaller testPluginInstaller;
    private ConnectionManager connectionManager;
    
    @BeforeClass
    public void classSetup() throws InterruptedException, IOException, ConnectionException, ServletException {
        Module guiceModule = setupTestModule();
        injector = Guice.createInjector(guiceModule);
        cliForce = injector.getInstance(CLIForce.class);
        baos = new ByteArrayOutputStream();
        InputStream in = new ByteArrayInputStream(new byte[]{});
        testPluginInstaller = getInjector().getInstance(TestPluginInstaller.class);
        cliForce.init(in, new PrintWriter(baos, true));
        setupCLIForce(cliForce);
        completor = getInjector().getInstance(CommandCompletor.class);
        connectionManager = getInjector().getInstance(ConnectionManager.class);
    }  
    
    public Injector getInjector() {
        return injector;
    }

    public TestModule setupTestModule() {
        return new TestModule();
    }

    public CommandCompletor getCompletor() {
        return completor;
    }

    public CLIForce getCLIForce() {
        return cliForce;
    }
    
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    /**
     * You will need to implement this method to return the String name of the additional plugin you want to install
     */
    public abstract String getPluginArtifact();

    /**
     * You will need to implement this method to return the additional plugin you want to install
     */
    public abstract Plugin getPlugin();

    public boolean isInternal() {
        return true;
    }

    /**
     * To execute this command each command and argument must be passed in as a separate String
     */
    protected String executeCommand(String... cmd) throws IOException, ConnectionException, ServletException, InterruptedException {
        baos.reset();
        cliForce.executeWithArgs(cmd);
        String outputStr = new String(baos.toByteArray());
        return outputStr;
    }

    /**
     * This is a convenience method that allows you to execute a full String containing spaces between the command and arguments
     */
    public String runCommand(String cmd) throws IOException, ConnectionException, ServletException, InterruptedException {
        return executeCommand(cmd.split(" "));
    }

    /**
     * If you want to run a test for cliforce that only requires the default plugin, you should override this method to do nothing.
     */
    public void setupCLIForce(CLIForce c) throws IOException {
        c.installPlugin(getPluginArtifact(), "LATEST", getPlugin(), isInternal());
    }

    /**
     * This method runs a completion test case and verifies results
     *
     * @param buffer             is the command text that is typed into the prompt
     * @param expectedCursor     this is the expected return value from the complete method
     * @param expectedCandidates these are the expected candidates for completing the command
     */
    public void runCompletorTestCase(String buffer, int expectedCursor, List<String> expectedCandidates) {
        runCompletorTestCase(buffer, buffer.length(), expectedCursor, expectedCandidates);
    }

    /**
     * This method runs a completion test case and verifies results
     *
     * @param buffer             is the command text that is typed into the prompt
     * @param expectedCursor     this is the expected return value from the complete method
     * @param expectedCandidates these are the expected candidates for completing the command
     */
    public void runCompletorTestCase(String buffer, int bufCursor, int expectedCursor, List<String> expectedCandidates) {
        List<CharSequence> candidates = new ArrayList<CharSequence>();
        int cursor = getCompletor().complete(buffer, bufCursor, candidates);
        Assert.assertEquals(cursor, expectedCursor, "unexpected cursor position");
        verifyCandidateList(candidates, expectedCandidates);
    }


    /**
     * This method verifies tab completion candidates
     *
     * @param actualCandidates
     * @param expectedCandidates
     */
    public void verifyCandidateList(List<CharSequence> actualCandidates, List<String> expectedCandidates) {
        Assert.assertEquals(actualCandidates.size(), expectedCandidates.size(), "unexpected number of candidates");
        for (int i = 0; i < actualCandidates.size(); i++) {
            Assert.assertEquals(actualCandidates.get(i).toString().replaceAll("[\\s]+", " "), expectedCandidates.get(i).replaceAll("[\\s]+", " "), "unexpected candidate");
        }
    }

}
