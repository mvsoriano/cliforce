package com.force.cliforce;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * base class for Commands that use JCommander to do argument parsing
 */
public abstract class JCommand<T> implements Command {

    /**
     * Instance of an object annotated with JCommander annotations
     */
    public abstract T getArgObject();

    public abstract void executeWithArgs(CommandContext ctx, T args);

    @Override
    public void execute(CommandContext ctx) throws Exception {
        T args = getArgObject();

        try {
            JCommander j = new JCommander(args, ctx.getCommandArguments());
            executeWithArgs(ctx, args);
        } catch (ParameterException e) {
            ctx.getCommandWriter().printf("Exception while executing command %s: %s\n", name(), e.getMessage());
        }
    }

    public Map<String, String> getCommandOptions() {
        JCommander j = new JCommander(getArgObject());
        try {
            Method m = JCommander.class.getDeclaredMethod("createDescriptions");
            m.setAccessible(true);
            m.invoke(j, null);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Map<String, String> opts = new HashMap<String, String>();
        for (ParameterDescription parameterDescription : j.getParameters()) {
            opts.put(parameterDescription.getNames(), parameterDescription.getDescription());
        }
        return opts;
    }
}
