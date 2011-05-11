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

package com.force.cliforce.plugin.connection;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.force.cliforce.BaseCliforceCommandTest;
import com.force.cliforce.Plugin;
import com.force.cliforce.TestModule;
import com.sforce.ws.ConnectionException;

/**
 * Tests how connection plugin commands affect the state of the connection manager
 *
 * @author Jeff Lai
 * 
 */
public class ConnectionManagerStateTest extends BaseCliforceCommandTest {
    
	private final String emptyDirPath = "/tmp/emptytestdir";
	
    @Override
    public TestModule setupTestModule() {
        // we need an empty directory to make sure no properties are loaded so we have an empty state
        File emptyDir = new File(emptyDirPath);
        if (!emptyDir.exists()) {
            emptyDir.mkdirs();
        }
        return new TestModule(emptyDirPath);
    }
    
    @Override
    public String getPluginArtifact() {
        return "connection";
    }

    @Override
    public Plugin getPlugin() {
        return new ConnectionPlugin();
    }

    @AfterClass(alwaysRun=true)
    public void cleanupForceDir() throws IOException {
    	File emptyDir = new File(emptyDirPath);
    	if(emptyDir.exists()) FileUtils.deleteDirectory(emptyDir);
    }
    
    
    @Test
    public void testAddRemoveConnection() throws IOException, ConnectionException, ServletException, InterruptedException {
        Assert.assertNull(getCLIForce().getCurrentEnvironment(), "current environment should be null. current environment was " + getCLIForce().getCurrentEnvironment());
        String output = runCommand("connection:add --notoken -n jeff -h login.salesforce.com -u user@domain.com -p mountains4");
        Assert.assertEquals(output, "Connection: jeff added\n", "unexpected output from command");
        Assert.assertEquals(getCLIForce().getCurrentEnvironment(),"jeff", "unexpected current environment");
        output = runCommand("connection:remove jeff");
        Assert.assertEquals(output, "Connection: jeff removed\n", "unexpected output from command");
        Assert.assertNull(getCLIForce().getCurrentEnvironment(), "current environment should be null. current environment was " + getCLIForce().getCurrentEnvironment());
    }
}
