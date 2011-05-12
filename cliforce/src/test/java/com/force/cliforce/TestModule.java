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

import static org.testng.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;

import jline.console.completer.Completer;

public class TestModule extends MainModule {

    public TestModule() {
        // Any test calling this constructor requires the positive.test.user.home System property be set.
        this(System.getProperty("positive.test.user.home"), false /*allowNullDir*/);
    }

    public TestModule(String userHomeDirectory) {
        this(userHomeDirectory, true /*allowNullDir*/);
    }

    private TestModule(String userHomeDirectory, boolean allowNullDir) {
        if (userHomeDirectory == null && !allowNullDir) {
            fail("Cannot run with null userHomeDirectory (check System property positive.test.user.home)");
        }
        
        if (userHomeDirectory != null) {
            System.setProperty("cliforce.home", userHomeDirectory);
        }
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
