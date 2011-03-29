package com.force.cliforce;


import javax.inject.Singleton;
import java.util.Collections;
import java.util.Set;

public class TestModule extends MainModule {


    public TestModule() {
        this(System.getProperty("positive.test.user.home"));
    }

    public TestModule(String userHomeDirectory) {
        System.setProperty("user.home", userHomeDirectory);
    }


    @Override
    protected void configure() {
        super.configure();
        bind(TestPluginInjector.class).in(Singleton.class);
        bind(TestPluginInstaller.class).in(Singleton.class);
        expose(PluginManager.class);
        expose(ConnectionManager.class);
        expose(TestPluginInjector.class);
        expose(TestPluginInstaller.class);
    }


    @Override
    public Set<String> provideInternalPlugins() {
        return Collections.emptySet();
    }
}
