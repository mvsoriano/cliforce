package com.force.cliforce;


import com.force.cliforce.dependency.DependencyResolver;
import com.google.inject.Exposed;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import jline.Completor;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Private Module that exposes CLIForce and DependencyResolver
 */
public class MainModule extends PrivateModule {
    @Override
    protected void configure() {
        bind(DefaultPlugin.class).in(Singleton.class);
        bind(CLIForce.class).in(Singleton.class);
        expose(CLIForce.class);
        bind(String[].class).annotatedWith(Names.named(CLIForce.INTERNAL_PLUGINS)).toInstance(provideInternalPlugins());
        bindPluginManager();
        bindConnectionManager();
        bindCompletor();
        bind(ExecutorService.class).annotatedWith(Names.named(CLIForce.STARTUP_EXECUTOR)).toInstance(Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        }));
    }

    @Provides
    @Singleton
    @Exposed
    DependencyResolver provideDependencyResolver() {
        try {
            return Boot.getBootResolver();
        } catch (IOException e) {
            throw new RuntimeException("IOException while trying to load the Boot resolver, in CLIForce MainModule");
        }
    }


    /**
     * Hook for subclasses to customize the plugin manager, mostly for testing
     */
    public void bindPluginManager() {
        bind(PluginManager.class).to(MainPluginManager.class).in(Singleton.class);
    }

    /**
     * Hook for subclasses to customize the connection manager, mostly for testing
     */
    public void bindConnectionManager() {
        bind(ConnectionManager.class).to(MainConnectionManager.class).in(Singleton.class);
    }

    /**
     * Hook for subclasses to customize the connection manager, mostly for testing
     */
    public void bindCompletor() {
        bind(Completor.class).to(CommandCompletor.class).in(Singleton.class);
    }


    /**
     * Hook for subclasses to customize the internal plugins, mostly for testing
     */
    public String[] provideInternalPlugins() {
        return new String[]{"connection", "app", "db", "template"};
    }
}
