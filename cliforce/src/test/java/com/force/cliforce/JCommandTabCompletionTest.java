/*
 * Copyright, 1999-2010, SALESFORCE.com 
 * All Rights Reserved
 * Company Confidential
 */
package com.force.cliforce;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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


}
