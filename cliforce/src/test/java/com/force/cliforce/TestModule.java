package com.force.cliforce;


import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

import jline.console.completer.Completer;

public class TestModule extends MainModule {

    public TestModule() {
        this(System.getProperty("positive.test.user.home"));
    }

    public TestModule(String userHomeDirectory) {
        System.setProperty("cliforce.home", userHomeDirectory);
    }

    @Override
    protected void configure() {
        super.configure();
        bind(TestPluginInjector.class).in(Singleton.class);
        bind(TestPluginInstaller.class).in(Singleton.class);
        bind(TestCliforceAccessor.class).in(Singleton.class);
        expose(PluginManager.class);
        expose(ConnectionManager.class);
        expose(TestPluginInjector.class);
        expose(TestPluginInstaller.class);
        expose(TestCliforceAccessor.class);
        
    }

    @Override
    public Set<String> provideInternalPlugins() {
        //mutable for testing
        return new HashSet<String>();
    }
    
    @Override
    public void bindCompletor() {
        CommandCompletor cmdComp = new CommandCompletor();
        bind(Completer.class).toInstance(cmdComp);
        bind(CommandCompletor.class).toInstance(cmdComp);
        expose(CommandCompletor.class);
    }
    
}
