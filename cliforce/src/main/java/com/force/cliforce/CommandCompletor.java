/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.force.cliforce;


import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;


public class CommandCompletor implements Completer {

    LazyLogger log = new LazyLogger(this);

    @Inject
    PluginManager pluginManager;

    /*Inject a provider,since it would otherwise be a circular dep*/
    @Inject
    Provider<CLIForce> cliForce;


    @Override
    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        if (buffer.length() != cursor) return 0;//only complete the end of a command
        String[] args = Util.parseCommand(buffer);
        int cmd = new StringsCompleter(pluginManager.getCommandNames().toArray(new String[0])).complete(args[0], cursor, candidates);
        if (candidates.size() == 0 && buffer != null && buffer.length() > 0) {
            log.get().debug("cliforce completor returning 0, from first if branch");
            return 0;
        } else if (candidates.size() == 1 && (buffer.endsWith(" ") || args.length > 1)) {
            candidates.remove(0);
            Command command = pluginManager.getCommand(args[0]);
            if (command != null) {
                ClassLoader curr = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(command.getClass().getClassLoader());
                    if (command instanceof JCommand) {
                        return ((JCommand<?>) command).complete(buffer, args, cursor, candidates, cliForce.get().getContext(args));
                    } else {
                        log.get().debug("cliforce completor executing standard completion");
                        candidates.add(" ");
                        candidates.add(command.describe());
                        return cursor;
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(curr);
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
