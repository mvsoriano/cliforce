package com.force.cliforce;

import com.google.inject.Module;


public interface CLIForceModule extends Module {
    void bindPluginManager();

    void bindConnectionManager();

    String[] provideInternalPlugins();
}
