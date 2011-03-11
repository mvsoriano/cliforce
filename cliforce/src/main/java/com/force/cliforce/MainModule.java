package com.force.cliforce;


import com.force.cliforce.dependency.DependencyResolver;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;

import javax.inject.Singleton;
import java.io.IOException;

public class MainModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DefaultPlugin.class).in(Singleton.class);
        bind(CLIForce.class).in(Singleton.class);
        bind(String[].class).annotatedWith(Names.named("internalPlugins")).toInstance(new String[]{"connection", "app", "db", "template"});
    }

    @Provides
    DependencyResolver provideDependencyResolver() {
        try {
            return Boot.getBootResolver();
        } catch (IOException e) {
            throw new RuntimeException("IOException while trying to load the Boot resolver, in CLIForce MainModule");
        }
    }
}
