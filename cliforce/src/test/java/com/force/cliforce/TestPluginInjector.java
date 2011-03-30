package com.force.cliforce;


import com.google.inject.Injector;

import javax.inject.Inject;

public class TestPluginInjector {

    @Inject
    Injector injector;

    public <T extends Command> T getInjectedCommand(Plugin p, Class<T> command) {
        if (p.getCommands().contains(command)) {
            PluginModule m = new PluginModule(p);
            return injector.createChildInjector(m).getInstance(command);
        } else {
            throw new IllegalArgumentException("The provided plugin does not supply command type you specified");
        }
    }
    
    public Injector getInjector() {
        return injector;
    }


}
