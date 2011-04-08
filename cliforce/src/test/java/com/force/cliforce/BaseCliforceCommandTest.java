package com.force.cliforce;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeClass;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.sforce.ws.ConnectionException;

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
    
    @BeforeClass
    public void classSetup() throws InterruptedException, IOException, ConnectionException, ServletException {
        Module guiceModule = setupTestModule();
        injector = Guice.createInjector(guiceModule);
        cliForce = injector.getInstance(CLIForce.class);
        baos = new ByteArrayOutputStream();
        InputStream in = new ByteArrayInputStream(new byte[] {});
        cliForce.init(in, new PrintWriter(baos, true));
        setupCLIForce(cliForce);
    }  
    
    public Injector getInjector() {
        return injector;
    }
    
    public TestModule setupTestModule() {
        return new TestModule();
    }

    /**
     * You will need to implement this method to return the String name of the additional plugin you want to install
     */
    public abstract String getPluginArtifact();
    
    /**
     * You will need to implement this method to return the additional plugin you want to install
     */
    public abstract Plugin getPlugin();
 
    public boolean isInternal(){
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
    
    public CLIForce getCLIForce() {
        return cliForce;
    }
    
    /**
     * If you want to run a test for cliforce that only requires the default plugin, you should override this method to do nothing.
     */
    public void setupCLIForce(CLIForce c) throws IOException {
      c.installPlugin(getPluginArtifact(), "LATEST", getPlugin(), isInternal());
    }

}
