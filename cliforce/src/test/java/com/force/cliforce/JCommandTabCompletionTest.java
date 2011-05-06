/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
