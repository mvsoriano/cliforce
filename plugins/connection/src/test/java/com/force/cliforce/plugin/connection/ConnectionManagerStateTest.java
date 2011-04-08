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
 * @author jeffrey.lai
 * @since javasdk-21.0.2-BETA
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
        String output = runCommand("connection:add --notoken -n jeff -h vmf01.t.salesforce.com -u user@domain.com -p mountains4");
        Assert.assertEquals(output, "Connection: jeff added\n", "unexpected output from command");
        Assert.assertEquals(getCLIForce().getCurrentEnvironment(),"jeff", "unexpected current environment");
        output = runCommand("connection:remove jeff");
        Assert.assertEquals(output, "Connection: jeff removed\n", "unexpected output from command");
        Assert.assertNull(getCLIForce().getCurrentEnvironment(), "current environment should be null. current environment was " + getCLIForce().getCurrentEnvironment());
    }
    
}
