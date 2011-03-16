package com.force.cliforce.test.command;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.Test;

import com.sforce.ws.ConnectionException;

public class ConnectionCommandTest extends BaseCommandTest {
    
    @Test
    public void testNoConnectionSetBeforeCommand() throws IOException, ConnectionException, ServletException, InterruptedException {
        String out = runCommand("help");
        System.out.println(out);
    }
    
}
