/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.force.cliforce;


import com.force.cliforce.dependency.DependencyResolver;
import com.google.inject.Exposed;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import jline.console.completer.Completer;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Private Module that exposes cliforce and DependencyResolver
 */
public class MainModule extends PrivateModule {
    @Override
    protected void configure() {
        bind(DefaultPlugin.class).in(Singleton.class);
        bindCLIForce();
        expose(CLIForce.class);
        bind(new TypeLiteral<Set<String>>(){}).annotatedWith(Names.named(CLIForce.INTERNAL_PLUGINS)).toInstance(provideInternalPlugins());
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
            throw new RuntimeException("IOException while trying to load the Boot resolver, in cliforce MainModule");
        }
    }

    public void bindCLIForce(){
        bind(CLIForce.class).in(Singleton.class);
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
        bind(Completer.class).to(CommandCompletor.class).in(Singleton.class);
    }


    /**
     * Hook for subclasses to customize the internal plugins, mostly for testing
     */
    public Set<String> provideInternalPlugins() {
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("codegen", "connection", "db", "template", "jpa")));
    }
}
