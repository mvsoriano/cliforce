package com.force.cliforce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;

import com.sforce.ws.ConnectionException;

/**
 * Base class for CommandCompletor tests
 *
 * @author jeffrey.lai
 * @since javasdk-21.0.2-BETA
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
    
    /**
     * This method runs a completion test case and verifies results
     * @param buffer is the command text that is typed into the prompt
     * @param expectedCursor this is the expected return value from the complete method
     * @param expectedCandidates these are the expected candidates for completing the command
     */
    public void runCompletorTestCase(String buffer, int expectedCursor, List<String> expectedCandidates) {
        List<CharSequence> candidates = new ArrayList<CharSequence>();
        int cursor = getCompletor().complete(buffer, buffer.length(), candidates);
        Assert.assertEquals(cursor, expectedCursor, "unexpected cursor position");
        verifyCandidateList(candidates, expectedCandidates);
    }
    
    public void verifyCandidateList(List<CharSequence> actualCandidates, List<String> expectedCandidates) {
        Assert.assertEquals(actualCandidates.size(), expectedCandidates.size(), "unexpected number of candidates");
        for (int i = 0; i < actualCandidates.size(); i++) {
            Assert.assertEquals(actualCandidates.get(i).toString(), expectedCandidates.get(i), "unexpected candidate");
        }
    }

}
