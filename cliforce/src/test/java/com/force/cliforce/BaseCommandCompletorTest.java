package com.force.cliforce;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeClass;

import com.sforce.ws.ConnectionException;

/**
 * 
 * Base class for CommandCompletor tests
 */
public abstract class BaseCommandCompletorTest extends BaseCliforceCommandTest {
    
    protected CommandCompletor completor;
    
    @Override
    @BeforeClass
    public void classSetup() throws InterruptedException, IOException, ConnectionException, ServletException {
        super.classSetup();
        completor = getInjector().getInstance(CommandCompletor.class);
    }
    
    public CommandCompletor getCompletor() {
        return completor;
    }

}
