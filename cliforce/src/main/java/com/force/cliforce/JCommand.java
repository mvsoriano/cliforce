package com.force.cliforce;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import jline.FileNameCompletor;
import jline.SimpleCompletor;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * base class for Commands that use JCommander to do argument parsing, and JLine completion.
 * <p/>
 * typically, subclasses will implement the describe method by calling usage("Some Description")
 */
public abstract class JCommand<T> implements Command {

    public static final String MAIN_PARAM = "<main param>";
    private LazyLogger log = new LazyLogger(this);

    /**
     * Instance of an object annotated with JCommander annotations. You need to override this method if
     * your args type does not have a default constructor.
     */
    public T getArgs() {
        Class clazz = this.getClass();
        while (!(clazz.getGenericSuperclass() instanceof ParameterizedType)) {
            clazz = (Class) clazz.getGenericSuperclass();
            if (clazz == null) {
                throw new RuntimeException("Unable to determine type of args for class:" + this.getClass().getName());
            }
        }
        ParameterizedType genericSuperclass = (ParameterizedType) clazz.getGenericSuperclass();
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
            ctx.getCommandWriter().printf("Exception while executing command: %s -> %s\n", name(), e.getMessage());
            log.get().debug("Exception while executing command", e);
        }
    }

    public Map<String, String> getCommandOptions() {
        JCommander j = new JCommander(getArgs());
        String mainParam = j.getMainParameter().getDescription();
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
     * Return the candidate set of completions for a given switch and partial value.
     * Subclasses should call super.getCompletionsForSwitch(j,zwitch,partialValue) for switches they
     * do not implement handling for.
     * <p/>
     * This implementation handles completion of values for @Parameters of type java.io.File, and for no-op value completions
     */
    protected List<String> getCompletionsForSwitch(String switchForCompletion, String partialValue, ParameterDescription parameterDescription, CommandContext context) {
        if (parameterDescription.getField().getType().equals(File.class)) {
            List<String> candidates = new ArrayList<String>();
            int ret = new FileNameCompletor().complete(partialValue, partialValue.length(), candidates);
            if (candidates.size() == 1 && partialValue.contains("/")) {
                String dir = partialValue.substring(0, partialValue.lastIndexOf("/")) + "/";
                for (int i = 0; i < candidates.size(); i++) {
                    candidates.set(i, dir + candidates.get(i));
                }
            }
            return candidates;

        } else {
            return Collections.emptyList();
        }
    }


    /**
     * Fill the candidates list with possible completions.
     * return the offset of where the cursor should be placed.
     * When there is only one completion it is easiest to append the completion to the origBuff - partialArg
     * and return 0.
     * <p/>
     * todo build an FSM diagram of the states we can be in.
     */
    public int complete(String origBuff, String[] parsed, int cursor, List<String> candidates, CommandContext ctx) {
        String[] commandArgs = Arrays.copyOfRange(parsed, 1, parsed.length);
        String lastArg = getLastArgumentForCompletion(commandArgs);
        String bufWithoutLast = origBuff.substring(0, origBuff.lastIndexOf(lastArg));
        JCommander j = new JCommander(getArgs());

        //parse the args with jcommander
        if (commandArgs.length > 0 && !commandArgs[0].equals("")) {
            log.get().debug("JCommand parsing for autocomplete");
            try {
                j.parseWithoutValidation(commandArgs);
            } catch (ParameterException ex) {
                log.get().debug("JCommander parameter exception during parse");
                //last param may be partial and cause this
                if (commandArgs.length > 1) {
                    j = new JCommander(getArgs());
                    try {
                        j.parseWithoutValidation(Arrays.copyOfRange(commandArgs, 0, commandArgs.length - 1));
                    } catch (ParameterException e) {
                        log.get().debug("JCommander parameter exception during parse2");
                    }
                }
            }
        }

        //take the result of parsing and create a map of the
        //unassigned parameters that will then be eligibie for completion
        //largestSwitch is used to pad the output of eligible completions
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

        if (j.getMainParameter() != null) {
            descs.put(MAIN_PARAM, j.getMainParameter());
            if (!j.getMainParameter().isAssigned()) {
                switches.add(MAIN_PARAM);
                if (largestSwitch < MAIN_PARAM.length()) {
                    largestSwitch = MAIN_PARAM.length();
                }
            }
        }

        boolean lastArgIsValue = isLastArgAValue(commandArgs, descs);
        //if the last arg is a full switch, just add a space to the buf
        if (descs.containsKey(lastArg) && !origBuff.endsWith(" ")) {
            candidates.add(origBuff.trim() + " ");
            return 0;
        } else if (descs.containsKey(lastArg) && isBooleanParam(descs.get(lastArg))) {
            bufWithoutLast = bufWithoutLast + lastArg;
            lastArg = "";
        }
        List<String> subCandidates = new ArrayList<String>();

        /*START ATTEMPT VALUE COMPLETION*/
        //if the last arg is a possibly uncompleted value or the last arg is a switch with a space after it
        String switchForCompletion = null;
        String partial = null;
        if (lastArgIsValue && !origBuff.endsWith(" ")) {
            switchForCompletion = getSecondLastArgumentForCompletion(commandArgs);
            if (isBooleanParam(descs.get(switchForCompletion))) {
                switchForCompletion = MAIN_PARAM;
            }
            partial = lastArg;
        } else if (descs.containsKey(lastArg) && origBuff.endsWith(" ")) {
            switchForCompletion = lastArg;
            partial = "";
        } else if (descs.containsKey(MAIN_PARAM) && descs.get(MAIN_PARAM).isAssigned()) {
            //we are completing a partial main arg
            switchForCompletion = MAIN_PARAM;
            partial = lastArg;
        }

        if (switchForCompletion != null && descs.containsKey(switchForCompletion)) {
            ParameterDescription desc = descs.get(switchForCompletion);
            List<String> valCandidates = getCompletionsForSwitch(switchForCompletion, partial, desc, ctx);
            if (valCandidates.size() > 1) {
                candidates.addAll(valCandidates);
                String unambig = getUnambiguousCompletions(candidates);
                int overlap = getOverlap(partial, unambig);
                return cursor - overlap;
            } else if (valCandidates.size() == 1) {
                candidates.add(bufWithoutLast + valCandidates.get(0));
                return 0;
            }
        }
        /*END ATTEMPT VALUE COMPLETION*/

        /*No value completion happened attempt switch completion*/
        SimpleCompletor completor = new SimpleCompletor(switches.toArray(new String[0]));
        int res = completor.complete(lastArg, cursor, subCandidates);

        //if the last arg is a value, then try to complete the next switch
        if (subCandidates.size() == 0 && lastArgIsValue) {
            completor.complete("", cursor, subCandidates);
        }

        if (subCandidates.size() == 1 && !isMainParam(subCandidates.get(0))) {
            StringBuilder b = new StringBuilder(bufWithoutLast);
            if (lastArgIsValue) {
                b.append(lastArg).append(" ");
            }
            candidates.add(b.append(subCandidates.get(0)).toString());
            return 0;
        } else if (subCandidates.size() == 0) {
            return -1;
        } else {
            //either there are multiple candidates or the only candidate is the main-param
            //in which case we cause the descriptions of the candidates to be rendered, and nothing to be completed
            if (subCandidates.size() == 1 && isMainParam(subCandidates.get(0))) {
                //attempt value completion for main parameter
                ParameterDescription parameterDescription = descs.get(MAIN_PARAM);
                List<String> valCandidates = getCompletionsForSwitch(MAIN_PARAM, lastArg, parameterDescription, ctx);
                if (valCandidates.size() == 0) {
                    //the leading space is important, as it generates an unambiguous completion of 1 space
                    candidates.add(" main param: " + parameterDescription.getDescription());
                    //add an invisible candidate so jline doesnt complete for us, but just displays choices
                    candidates.add(" ");
                } else {
                    candidates.addAll(valCandidates);
                }
            } else {
                for (String subCandidate : subCandidates) {
                    if (lastArgIsValue) {
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
            }
            String frag = getUnambiguousCompletions(candidates);
            if (frag.length() > 0 && !lastArgIsValue) {
                return cursor - getOverlap(lastArg, frag);
            } else {
                //this is annoying not sure if its a bug in jline or
                //me not understanding it but the cursor stuff seems inconsistent
                if (origBuff.equals(bufWithoutLast)) {
                    return cursor;
                } else if (origBuff.endsWith(" ")) {
                    return cursor - 1;
                } else {
                    return cursor + 1;
                }
            }
        }

    }

    protected int getOverlap(String partial, String unambig) {
        if (unambig.length() == 0) return 0;
        if (partial.length() == 0) return 0;
        if (partial.endsWith(unambig)) return unambig.length();
        for (int i = unambig.length() - 1; i > 0; i--) {
            String sub = unambig.substring(0, unambig.length() - i);
            if (partial.endsWith(sub)) {
                return sub.length();
            }
        }
        return 0;
    }

    protected boolean isBooleanParam(ParameterDescription description) {
        return description.getField().getType().equals(boolean.class) || description.getField().getType().equals(Boolean.class);
    }

    protected boolean isMainParam(String candidate) {
        return candidate.trim().startsWith("<");
    }

    protected String stripLeadingDashes(String swich) {
        while (swich.startsWith("-")) {
            swich = swich.substring(1);
        }
        return swich;
    }

    protected String getDescriptiveCandidate(String key, Map<String, ParameterDescription> descs, int largestKey) {
        StringBuilder b = new StringBuilder(key);
        for (int i = 0; i <= largestKey - key.length(); i++) {
            b.append(" ");
        }
        return b.append(" <").append(descs.get(key.trim()).getDescription()).append(">").toString();
    }


    protected boolean isLastArgAValue(String[] args, Map<String, ParameterDescription> descs) {
        if (descs.containsKey(getLastArgumentForCompletion(args))) {
            return false;
        } else return descs.containsKey(getSecondLastArgumentForCompletion(args));
    }

    protected String getLastArgumentForCompletion(String[] args) {
        if (args.length > 0) {
            return args[args.length - 1];
        } else {
            return "";
        }
    }


    protected String getSecondLastArgumentForCompletion(String[] args) {
        //we dont grab the command here so > 2 not >1
        if (args.length >= 2) {
            return args[args.length - 2];
        } else {
            return "";
        }
    }

    protected String getUnambiguousCompletions(final List candidates) {
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

    protected boolean startsWith(final String starts,
                                 final String[] candidates) {
        for (int i = 0; i < candidates.length; i++) {
            if (!candidates[i].startsWith(starts)) {
                return false;
            }
        }

        return true;
    }


}


