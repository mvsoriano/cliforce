package com.force.cliforce;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

/**
 * base class for Commands that use JCommander to do argument parsing, and JLine completion
 */
public abstract class JCommand<T> implements Command {

    /**
     * Instance of an object annotated with JCommander annotations. You need to override this method if
     * your args type does not have a default constructor.
     */
    public T getArgs() {
        ParameterizedType genericSuperclass = (ParameterizedType) this.getClass().getGenericSuperclass();
        try {
            return (T) ((Class) genericSuperclass.getActualTypeArguments()[0]).newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("Exception instantiating arg object", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Exception instantiating arg object", e);
        }
    }

    public abstract void executeWithArgs(CommandContext ctx, T args);

    @Override
    public void execute(CommandContext ctx) throws Exception {
        T args = getArgs();

        try {
            JCommander j = new JCommander(args, ctx.getCommandArguments());
            executeWithArgs(ctx, args);
        } catch (ParameterException e) {
            ctx.getCommandWriter().printf("Exception while executing command %s:\n", name());
            ctx.getCommandWriter().printStackTrace(e);
        }
    }

    public Map<String, String> getCommandOptions() {
        JCommander j = new JCommander(getArgs());
        String mainParam = null;
        try {
            Method m = JCommander.class.getDeclaredMethod("createDescriptions");
            m.setAccessible(true);
            m.invoke(j, null);
            Method mm = JCommander.class.getDeclaredMethod("getMainParameterDescription");
            mm.setAccessible(true);
            mainParam = (String) mm.invoke(j, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Map<String, String> opts = new HashMap<String, String>();
        for (ParameterDescription parameterDescription : j.getParameters()) {
            opts.put(parameterDescription.getNames(), parameterDescription.getDescription() + (parameterDescription.getParameter().required() ? "(required)" : ""));
        }
        if (mainParam != null) {
            opts.put("<main>", mainParam);
        }


        return opts;
    }

    public String usage(String description) {
        Map<String, String> commandOptions = getCommandOptions();
        StringBuilder usage = new StringBuilder(description).append("\n\tUsage: ").append(name()).append("\n");
        for (Map.Entry<String, String> e : commandOptions.entrySet()) {
            usage.append("\t").append(e.getKey()).append("\t").append(e.getValue()).append("\n");
        }
        return usage.toString();
    }
}
