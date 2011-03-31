package com.force.cliforce.plugin.connection;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.force.cliforce.BaseCliforceCommandTest;
import com.force.cliforce.Plugin;
import com.sforce.ws.ConnectionException;

/**
 * Tests how connection plugin commands affect the state of the connection manager
 *
 * @author jeffrey.lai
 * @since javasdk-21.0.2-BETA
 */
public class ConnectionManagerStateTest extends BaseCliforceCommandTest {
    
    @Override
    public String getPluginArtifact() {
        return "connection";
    }

    @Override
    public Plugin getPlugin() {
        return new ConnectionPlugin();
    }

    @Test
    public void testAddRemoveConnection() throws IOException, ConnectionException, ServletException, InterruptedException {
        Assert.assertNull(getCLIForce().getCurrentEnvironment(),"current environment should be null");
        String output = runCommand("connection:add --notoken -n jeff -h vmf01.t.salesforce.com -u user@domain.com -p mountains4");
        Assert.assertEquals(output, "Connection: jeff added\n", "unexpected output from command");
        Assert.assertEquals(getCLIForce().getCurrentEnvironment(),"jeff", "unexpected current environment");
        output = runCommand("connection:remove jeff");
        Assert.assertEquals(output, "Connection: jeff removed\n", "unexpected output from command");
        Assert.assertNull(getCLIForce().getCurrentEnvironment(),"current environment should be null");
    }
    
}
