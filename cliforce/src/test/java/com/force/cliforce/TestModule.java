package com.force.cliforce;


import com.google.inject.name.Names;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.Set;

public class TestModule extends MainModule {

    @Override
    protected void configure() {
        super.configure();
        bind(TestPluginInjector.class).in(Singleton.class);
        bind(TestPluginInstaller.class).in(Singleton.class);
        expose(PluginManager.class);
        expose(ConnectionManager.class);
        expose(TestConnectionManager.class);
        expose(TestPluginInjector.class);
        expose(TestPluginInstaller.class);
    }

    @Override
    public void bindConnectionManager() {
        TestConnectionManager test = new TestConnectionManager();
        bind(ConnectionManager.class).toInstance(test);
        bind(TestConnectionManager.class).toInstance(test);
        bind(String.class).annotatedWith(Names.named("test-credentials")).toInstance("test.login");
    }

    public void bindCLIForce() {
        bind(CLIForce.class);
    }

    @Override
    public Set<String> provideInternalPlugins() {
        return Collections.emptySet();
    }
}
