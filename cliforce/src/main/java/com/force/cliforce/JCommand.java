package com.force.cliforce;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import jline.Completor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * base class for Commands that use JCommander to do argument parsing, and JLine completion.
 * <p/>
 * typically, subclasses will implement the describe method by calling usage("Some Description")
 */
public abstract class JCommand<T> implements Command {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

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

    /**
     * get command completors for this command.
     *
     * @param writer can be used to write to cliforce, such as for displaying usage before completions
     * @return
     */
    public Completor getCommandCompletor(CommandWriter writer) {
        return new JCommandCompletor();
    }

    private class JCommandCompletor implements Completor {


        @Override
        public int complete(String buffer, int cursor, List candidates) {

            if (buffer == null) buffer = "";
            logger.debug(buffer);
            logger.debug("Cursor:" + cursor);
            JCommander j = new JCommander(getArgs());
            String[] argv = Util.parseCommand(buffer);
            try {
                if (argv.length > 0 && !argv[0].equals("")) {
                    logger.debug("JCommand parsing for autocomplete");
                    j.parse(argv);
                }
            } catch (ParameterException ex) {
                logger.debug("JCommander parameter exception during parse");
            }
            Set<Field> fieldsAssigned = new HashSet<Field>();
            Field f = null;
            Map<String, ParameterDescription> descs = new HashMap<String, ParameterDescription>();
            try {
                f = JCommander.class.getDeclaredField("m_descriptions");
                f.setAccessible(true);
                descs = (Map<String, ParameterDescription>) f.get(j);
            } catch (NoSuchFieldException e) {
                logger.error("error while reflecting on jcommander", e);
            } catch (IllegalAccessException e) {
                logger.error("error while reflecting on jcommander", e);
            }


            for (ParameterDescription parameterDescription : descs.values()) {
                if (parameterDescription.isAssigned()) {
                    fieldsAssigned.add(parameterDescription.getField());
                }
            }
            for (ParameterDescription parameterDescription : j.getParameters()) {

                if (!fieldsAssigned.contains(parameterDescription.getField())) {
                    String names = parameterDescription.getNames();
                    String complete = names;
                    Field field = parameterDescription.getField();
                    if (!(field.getType().equals(boolean.class) || field.getType().equals(Boolean.class))) {
                        complete += " <" + parameterDescription.getDescription() + ">";
                    }
                    candidates.add(complete);


                }
            }
            if (j.getMainParameterDescription() != null) {
                if(candidates.size() == 0){
                   candidates.add(" ");
                }
                candidates.add("<" + j.getMainParameterDescription() + ">");
            }
            return cursor;
        }
    }

}
