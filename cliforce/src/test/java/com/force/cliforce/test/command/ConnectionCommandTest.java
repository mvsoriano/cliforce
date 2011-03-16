package com.force.cliforce.test.command;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.Test;

import com.force.cliforce.BaseTest;
import com.force.cliforce.CLIForce;
import com.sforce.ws.ConnectionException;

public class ConnectionCommandTest extends BaseTest {
    
    @Test
    public void testNoConnectionSetBeforeCommand() throws IOException, ConnectionException, ServletException, InterruptedException {
        String out = runCommand("help");
        System.out.println(out);
    }

    @Override
    public void setupCLIForce(CLIForce c) throws IOException {
        // TODO Auto-generated method stub
        
    }
    
}
