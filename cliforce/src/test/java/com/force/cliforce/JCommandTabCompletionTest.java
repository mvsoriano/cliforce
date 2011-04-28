/*
 * Copyright, 1999-2010, SALESFORCE.com 
 * All Rights Reserved
 * Company Confidential
 */
package com.force.cliforce;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * JCommandTabCompletionTest
 *
 * @author sclasen
 */
public class JCommandTabCompletionTest {

    public static final String TEST_PLUGIN_NAME = "test";

    TabTestJCommand command = new TabTestJCommand();

    String commandName = TEST_PLUGIN_NAME + ":" + command.name();


    public String getFullBuffer(String argsWithoutCommand) {
        return commandName + " " + argsWithoutCommand;
    }

    public List<CharSequence> testTabCompletion(String argsWoCommand) {
        List<CharSequence> candidates = new ArrayList<CharSequence>();
        String fullBuffer = getFullBuffer(argsWoCommand);
        String[] args = Util.parseCommand(fullBuffer);
        command.complete(fullBuffer, args, fullBuffer.length(), candidates, new TestCommandContext());
        return candidates;
    }


    @Test
    public void testCompletion() {
        Assert.assertTrue(testTabCompletion("-i 4 -s ").containsAll(Arrays.asList(TabTestArgs.sCompletions)));
    }

    @Test
    public void testFileCompletions() throws URISyntaxException {
    	Assert.assertTrue(compareTrimmedStrings(testTabCompletion("-p "), Arrays.asList(new TabTestArgs().pCompletions)));
    }
    
    @Test
    public void testBooleanValueCompletion() {
    	List<CharSequence> completions = testTabCompletion("--b");
    	Assert.assertTrue(completions.get(0).toString().contains(TabTestArgs.bLong));
    }
    
    @Test
    public void testBooleanValueLastParameterCompletion() throws URISyntaxException {
    	List<CharSequence> completions = testTabCompletion("-i 4 -s " + TabTestArgs.sCompletions[0] + " -p " + new TabTestArgs().pCompletions[0] + " ");
    	Assert.assertTrue(completions.contains(TabTestArgs.bCompletionsAfterValue[0]), "Boolean switch value " +TabTestArgs.bCompletionsAfterValue[0]+ " was not completed.");
    }
    
    /**
     * JCommander puts a command separator (in our case a space) at the end of each argument it finds
     * so we need to trim completion candidates before comparing.
     */
    private boolean compareTrimmedStrings(List<CharSequence> actual, List<String> expected) {
    	if(actual.size() != expected.size()) return false;
    	Iterator<CharSequence> actualIter = actual.iterator();
    	while(actualIter.hasNext()){
    		if(! expected.contains(actualIter.next().toString().trim())) return false;
    	}
    	return true;
    }
}
