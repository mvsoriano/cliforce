package com.force.cliforce;


import com.google.inject.AbstractModule;

import javax.inject.Singleton;

public class PluginModule extends AbstractModule {

    private Plugin plugin;

    public PluginModule(Plugin p) {
        this.plugin = p;
    }

    @Override
    protected void configure() {
        for (Class<? extends Command> command : plugin.getCommands()) {
            bind(command).in(Singleton.class);
        }
    }
}