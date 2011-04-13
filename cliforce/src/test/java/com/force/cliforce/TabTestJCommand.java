/*
 * Copyright, 1999-2010, SALESFORCE.com 
 * All Rights Reserved
 * Company Confidential
 */
package com.force.cliforce;

import com.beust.jcommander.ParameterDescription;
import jline.console.completer.StringsCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * TabTestJCommand
 *
 * @author sclasen
 */
public class TabTestJCommand extends JCommand<TabTestArgs> {


    @Override
    public void executeWithArgs(CommandContext ctx, TabTestArgs args) {
        ctx.getCommandWriter().println("executed");
    }

    @Override
    public String name() {
        return "tabtest";
    }

    @Override
    public String describe() {
        return usage("test command for tab completion");
    }

    @Override
    protected List<CharSequence> getCompletionsForSwitch(String switchForCompletion, String partialValue, ParameterDescription parameterDescription, CommandContext context) {
        if (new HashSet<String>(Arrays.asList(TabTestArgs.sSwitches)).contains(switchForCompletion)) {
            List<CharSequence> candidates = new ArrayList<CharSequence>();
            new StringsCompleter(TabTestArgs.sCompletions).complete(partialValue, partialValue.length(), candidates);
            return candidates;
        } else {
            return super.getCompletionsForSwitch(switchForCompletion, partialValue, parameterDescription, context);
        }
    }
}
