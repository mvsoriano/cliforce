package com.force.cliforce.plugin.codegen;

import java.util.Arrays;
import java.util.List;

import com.force.cliforce.Command;
import com.force.cliforce.Plugin;
import com.force.cliforce.plugin.codegen.command.JPAClass;
import com.force.cliforce.plugin.codegen.command.Describe;

/**
 * CLIForce plugin for automatic code generation.
 * 
 * @author Tim Kral
 */
public class CodeGenPlugin implements Plugin {

    @SuppressWarnings("unchecked")
    @Override
    public List<Class<? extends Command>> getCommands() {
        return Arrays.<Class<? extends Command>>asList(Describe.class, JPAClass.class);
    }
}
