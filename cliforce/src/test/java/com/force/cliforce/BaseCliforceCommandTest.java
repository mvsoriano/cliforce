package com.force.cliforce;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeClass;

import com.google.inject.Guice;
import com.google.inject.Module;
import com.sforce.ws.ConnectionException;

/**
 * Use this class if you require the a CLIForce object in your command test.
 */
public abstract class BaseCliforceCommandTest {
    
    private CLIForce cliForce;
    private ByteArrayOutputStream baos;
    
    @BeforeClass
    public void classSetup() throws InterruptedException, IOException, ConnectionException, ServletException {
        Module guiceModule = setupTestModule();
        cliForce = Guice.createInjector(guiceModule).getInstance(CLIForce.class);
        baos = new ByteArrayOutputStream();
        InputStream in = new ByteArrayInputStream(new byte[] {});
        cliForce.init(in, new PrintWriter(baos, true));
        setupCLIForce(cliForce);
    }  
    
    public TestModule setupTestModule() {
        return new TestModule();
    }

    public abstract String getPluginArtifact();
    
    public abstract Plugin getPlugin();
 
    public boolean isInternal(){
        return true;
    }
    
    protected String executeCommand(String... cmd) throws IOException, ConnectionException, ServletException, InterruptedException {
        baos.reset();
        cliForce.executeWithArgs(cmd);
        String outputStr = new String(baos.toByteArray());
        return outputStr;
    }
    
    public String runCommand(String cmd) throws IOException, ConnectionException, ServletException, InterruptedException {
        return executeCommand(cmd.split(" "));
    }
    
    public CLIForce getCLIForce() {
        return cliForce;
    }
    
    public void setupCLIForce(CLIForce c) throws IOException {
      c.installPlugin(getPluginArtifact(), "LATEST", getPlugin(), isInternal());
    }

}
