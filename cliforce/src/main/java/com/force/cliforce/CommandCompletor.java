package com.force.cliforce;


import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;


class CommandCompletor implements Completer {

    LazyLogger log = new LazyLogger(this);

    @Inject
    PluginManager pluginManager;

    /*Inject a provider,since it would otherwise be a circular dep*/
    @Inject
    Provider<CLIForce> cliForce;


    @Override
    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        String[] args = Util.parseCommand(buffer);
        int cmd = new StringsCompleter(pluginManager.getCommandNames().toArray(new String[0])).complete(args[0], cursor, candidates);
        if (candidates.size() == 0 && buffer != null && buffer.length() > 0) {
            log.get().debug("cliforce completor returning 0, from first if branch");
            return 0;
        } else if (candidates.size() == 1 && (buffer.endsWith(" ") || args.length > 1)) {
            //String candidate = (String) candidates.remove(0);
            Command command = pluginManager.getCommand(args[0]);
            if (command != null) {
                if (command instanceof JCommand) {
                    return ((JCommand<?>) command).complete(buffer, args, cursor, candidates, cliForce.get().getContext(args));
                } else {
                    log.get().debug("cliforce completor executing standard completion");
                    candidates.add(" ");
                    candidates.add(command.describe());
                    return cursor;
                }
            } else {
                log.get().debug("cliforce completor returning {} from command null branch", cmd);
                return cmd;
            }

        } else {
            log.get().debug("cliforce completor returning {}  from last else branch", cmd);
            return cmd;
        }
    }
}
