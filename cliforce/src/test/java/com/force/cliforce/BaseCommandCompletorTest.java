package com.force.cliforce;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.testng.Assert;
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
    
    public void verifyCandidateList(List<CharSequence> actualCandidates, List<String> expectedCandidates) {
        Assert.assertEquals(actualCandidates.size(), expectedCandidates.size(), "unexpected number of candidates");
        for (int i = 0; i < actualCandidates.size(); i++) {
            Assert.assertEquals(actualCandidates.get(i).toString(), expectedCandidates.get(i), "unexpected candidate");
        }
    }

}
