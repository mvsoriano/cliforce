package com.force.cliforce;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import jline.SimpleCompletor;
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

    public static final String MAIN_PARAM = "<main param>";
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
     * return the offset of where the cursor should be placed.
     */
    public int complete(String origBuff, String[] parsed, int cursor, List candidates) {
        int parsedLength = 0;
        for (String s : parsed) {
            parsedLength += s.length();
        }


        String[] argv = Arrays.copyOfRange(parsed, 1, parsed.length);
        String last = getLastArgumentForCompletion(argv);
        String bufWithoutLast = origBuff.substring(0, origBuff.lastIndexOf(last));
        JCommander j = j = new JCommander(getArgs());


        if (argv.length > 0 && !argv[0].equals("")) {
            logger.debug("JCommand parsing for autocomplete");
            try {
                j.parseWithoutValidation(argv);
            } catch (ParameterException ex) {
                logger.debug("JCommander parameter exception during parse");
                //last param may be partial and cause this
                if (argv.length > 1) {
                    j = new JCommander(getArgs());
                    try {
                        j.parseWithoutValidation(Arrays.copyOfRange(argv, 0, argv.length - 1));
                    } catch (ParameterException e) {
                        logger.debug("JCommander parameter exception during parse2");
                    }
                }
            }
        }

        int largestSwitch = 0;
        Map<String, ParameterDescription> descs = new HashMap<String, ParameterDescription>();
        List<String> switches = new ArrayList<String>();
        for (ParameterDescription parameterDescription : j.getParameters()) {
            for (String key : parameterDescription.getParameter().names()) {
                descs.put(key, parameterDescription);
                if (!parameterDescription.isAssigned()) {
                    switches.add(key);
                    if (key.length() > largestSwitch) {
                        largestSwitch = key.length();
                    }
                }
            }
        }

        if (j.getMainParameter() != null && !j.getMainParameter().isAssigned()) {
            descs.put(MAIN_PARAM, j.getMainParameter());
            switches.add(MAIN_PARAM);
            if (largestSwitch < MAIN_PARAM.length()) {
                largestSwitch = MAIN_PARAM.length();
            }
        }

        if (descs.containsKey(last)) {
            candidates.add(origBuff.trim() + " ");
            return 0;
        }
        List<String> subCandidates = new ArrayList<String>();
        SimpleCompletor completor = new SimpleCompletor(switches.toArray(new String[0]));
        int res = completor.complete(last, cursor, subCandidates);
        boolean isLastArgAVal = isLastArgAValue(argv, descs);
        if (subCandidates.size() == 0 && isLastArgAVal) {
            completor.complete("", cursor, subCandidates);
        }


        /* if (l.size() <= 1) {
                    //check if the currently being completed arg has its own completion
                    //should add method getCompletorForArg("--conn") which could return a completor for a set of connections
                    //right now just special sauce for fileName completion for parameters that are of type File.
                    String arg = getLastArgumentForCompletion(argv);
                    ParameterDescription desc = descs.get(arg);
                    String compBuf = "";
                    if (desc == null && argv.length > 1) {
                        compBuf = arg;
                        arg = argv[argv.length - 2];
                        desc = descs.get(arg);
                    }
                    if (desc != null) {
                        if (desc.getField().getType().equals(File.class)) {
                            int ret = new FileNameCompletor().complete(compBuf, cursor, candidates);
                            if (candidates.size() > 1) {
                                return  cursor - getUnambiguousCompletions(candidates).length() + 1;
                            } else if (candidates.size() == 1) {
                                String last = getLastArgumentForCompletion(argv);
                                int minus = last.length();
                                if (last.contains("/")) {
                                    minus = last.lastIndexOf("/");
                                }
                                return cursor - minus;
                            } else if (candidates.size() == 0) {
                                return cursor;
                            }
                        }
                    }
                }
        */
        logger.debug("sub candidates:" + subCandidates.size());

        if (subCandidates.size() == 1) {
            StringBuilder b = new StringBuilder(bufWithoutLast);
            if(isLastArgAVal){
                b.append(" ").append(last).append(" ");
            }
            candidates.add(b.append(subCandidates.get(0)));
            return 0;
        } else if (subCandidates.size() == 0) {
            return -1;
        } else {
            //getCompletorFor(j,descs).complete(last,cursor,candidates);
            for (String subCandidate : subCandidates) {
                if (isLastArgAVal) {
                    candidates.add(" " + getDescriptiveCandidate(subCandidate, descs, largestSwitch));
                } else {
                    candidates.add(getDescriptiveCandidate(subCandidate, descs, largestSwitch));
                }
                Collections.sort(candidates, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return stripLeadingDashes(o1).compareTo(stripLeadingDashes(o2));
                    }
                });
            }
            if (candidates.size() == 1 && ((String) candidates.get(0)).startsWith(MAIN_PARAM)) {
                candidates.add(" ");
            }
            logger.debug("candidates:" + candidates.size());
            logger.debug("ret:" + cursor);
            String frag = getUnambiguousCompletions(candidates);
            if (frag.length() > 0 && !isLastArgAVal) {
                return cursor - frag.length();
            } else {
                if (origBuff.endsWith(" ")) {
                    return cursor - 1;
                } else {
                    return cursor + 1;
                }
            }
        }

    }

    String stripLeadingDashes(String switchh) {
        while (switchh.startsWith("-")) {
            switchh = switchh.substring(1);
        }
        return switchh;
    }

    String getDescriptiveCandidate(String key, Map<String, ParameterDescription> descs, int largestKey) {
        StringBuilder b = new StringBuilder(key);
        for (int i = 0; i <= largestKey - key.length(); i++) {
            b.append(" ");
        }
        return b.append(" <").append(descs.get(key.trim()).getDescription()).append(">").toString();
    }

    boolean isLastArgAValue(String[] args, Map<String, ParameterDescription> descs) {
        if (descs.containsKey(getLastArgumentForCompletion(args))) {
            return false;
        } else return descs.containsKey(getSecondLastArgumentForCompletion(args));
    }

    String getLastArgumentForCompletion(String[] args) {
        if (args.length > 0) {
            return args[args.length - 1];
        } else {
            return "";
        }
    }


    String getSecondLastArgumentForCompletion(String[] args) {
        if (args.length > 1) {
            return args[args.length - 2];
        } else {
            return "";
        }
    }


    SimpleCompletor getCompletorFor(JCommander j, Map<String, ParameterDescription> descs) {
        List<String> candidates = new ArrayList<String>();
        for (ParameterDescription parameterDescription : j.getParameters()) {
            if (!parameterDescription.isAssigned()) {
                String names = parameterDescription.getNames();
                String complete = names;
                Field field = parameterDescription.getField();
                if (!(field.getType().equals(boolean.class) || field.getType().equals(Boolean.class))) {
                    complete += " <" + parameterDescription.getDescription() + ">";
                }
                candidates.add(complete);
            }
        }
        if (j.getMainParameter() != null && !j.getMainParameter().isAssigned()) {
            if (candidates.size() == 0) {
                candidates.add(" ");
            }
            candidates.add("<main arg:" + j.getMainParameterDescription() + ">");
        }


        return new SimpleCompletor(candidates.toArray(new String[0]));
    }

    private final String getUnambiguousCompletions(final List candidates) {
        if ((candidates == null) || (candidates.size() == 0)) {
            return "";
        }

        // convert to an array for speed
        String[] strings =
                (String[]) candidates.toArray(new String[candidates.size()]);

        String first = strings[0];
        StringBuffer candidate = new StringBuffer();

        for (int i = 0; i < first.length(); i++) {
            if (startsWith(first.substring(0, i + 1), strings)) {
                candidate.append(first.charAt(i));
            } else {
                break;
            }
        }

        return candidate.toString();
    }

    private final boolean startsWith(final String starts,
                                     final String[] candidates) {
        for (int i = 0; i < candidates.length; i++) {
            if (!candidates[i].startsWith(starts)) {
                return false;
            }
        }

        return true;
    }

}



