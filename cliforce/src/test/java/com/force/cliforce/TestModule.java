package com.force.cliforce;


import javax.inject.Singleton;

public class TestModule extends MainModule {

    @Override
    protected void configure() {
        super.configure();
        bind(TestPluginInjector.class).in(Singleton.class);
        expose(PluginManager.class);
        expose(ConnectionManager.class);
        expose(TestConnectionManager.class);
        expose(TestPluginInjector.class);
    }

    @Override
    public void bindConnectionManager() {
        TestConnectionManager test = new TestConnectionManager();
        bind(ConnectionManager.class).toInstance(test);
        bind(TestConnectionManager.class).toInstance(test);
    }
}
