package com.force.cliforce.test.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeClass;

import com.force.cliforce.CLIForce;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.sforce.ws.ConnectionException;

public abstract class BaseCommandTest {
    
    private CLIForce cliForce;
    
    @BeforeClass
    public void classSetup() throws InterruptedException {
        Module guiceModule = new TestModule();
        cliForce = Guice.createInjector(guiceModule).getInstance(CLIForce.class);
        cliForce.setCurrentEnvironment("test");
    }
    
    public String runCommand(String cmd) throws IOException, ConnectionException, ServletException, InterruptedException {
//        InputStream in = new ByteArrayInputStream(cmd.getBytes("UTF-8"));
        InputStream in = new ByteArrayInputStream(new byte[] {});
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        cliForce.init(in, writer);
        cliForce.executeWithArgs(new String[] {cmd});
        String outputStr = new String(out.toByteArray());
        in.close();
        out.close();
        writer.close();
        return outputStr;
    }
    
    public CLIForce getCLIForce() {
        return cliForce;
    }
    
}
