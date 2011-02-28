package com.force.cliforce;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * base class for Commands that use JCommander to do argument parsing, and JLine completion.
 * <p/>
 * typically, subclasses will implement the describe method by calling usage("Some Description")
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
        String mainParam = j.getMainParameterDescription();
        Map<String, String> opts = new TreeMap<String, String>();
        for (ParameterDescription parameterDescription : j.getParameters()) {
            opts.put(parameterDescription.getNames(), parameterDescription.getDescription() + (parameterDescription.getParameter().required() ? "(required)" : ""));
        }
        if (mainParam != null) {
            opts.put("<main>", "<" + mainParam + ">");
        }


        return opts;
    }

    public String usage(String description) {
        Map<String, String> commandOptions = getCommandOptions();
        String main = commandOptions.remove("<main>");
        if (main == null) main = "";
        StringBuilder usage = new StringBuilder(description).append("\n\tUsage: ").append(name());
        boolean args = false;
        if (commandOptions.size() > 0) {
            args = true;
        }
        if (args) usage.append(" [args]");
        usage.append(" ").append(main).append("\n");
        if (args) usage.append("\targs:\n");
        for (Map.Entry<String, String> e : commandOptions.entrySet()) {
            usage.append("\t").append(e.getKey()).append("\t").append(e.getValue()).append("\n");
        }
        return usage.toString();
    }

    static class Arg {
        @Parameter(names = {"-t"}, description = "test")
        String test = null;

        @Parameter(description = "main")
        List<String> mainParams;
    }

    public static void main(String[] args) {
        JCommander commander = new JCommander(new Arg());
        System.out.println(commander.getParameters().size());
        JCommander commander2 = new JCommander(new Arg(), "-t", "testing");
        System.out.println(commander2.getParameters().size());

    }

}
