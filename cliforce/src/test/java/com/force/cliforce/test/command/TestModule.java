package com.force.cliforce.test.command;

import com.force.cliforce.ConnectionManager;
import com.force.cliforce.MainModule;
import com.google.inject.Singleton;

public class TestModule extends MainModule {
    @Override
    public void bindConnectionManager() {
        bind(ConnectionManager.class).to(TestConnectionManager.class).in(Singleton.class);
    }

//    @Override
//    public String[] provideInternalPlugins() {
//        return new String[0];
//    }
}
