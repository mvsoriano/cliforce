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

package com.force.cliforce.dependency;


import java.io.IOException;

import org.testng.annotations.Test;

import com.force.cliforce.Boot;

public class DependencyResolverTest {




    @Test
    public void testSingleDependencyResolution() throws ClassNotFoundException, IOException {
        ClassLoader loader = Boot.getBootResolver().createClassLoaderFor("log4j", "log4j", "1.2.16", null, new OutputAdapter() {
            @Override
            public void println(String msg) {
                System.out.println(msg);
            }

            @Override
            public void println(Exception e, String msg) {
                System.out.println(msg);
                e.printStackTrace(System.out);
            }
        });
        loader.loadClass("org.apache.log4j.Logger");

    }

    @Test
    public void testReleaseDependencyResolution() throws ClassNotFoundException, IOException {
        ClassLoader loader = Boot.getBootResolver().createClassLoaderFor("log4j", "log4j", "RELEASE",null, new OutputAdapter() {
            @Override
            public void println(String msg) {
                System.out.println(msg);
            }

            @Override
            public void println(Exception e, String msg) {
                System.out.println(msg);
                e.printStackTrace(System.out);
            }
        });
        loader.loadClass("org.apache.log4j.Logger");

    }




    @Test
    public void testPluginDependencyResolution() throws ClassNotFoundException, IOException {
        Boot.getBootResolver().createClassLoaderFor("com.force.cliforce.plugin", "cliplugin", null, new OutputAdapter() {
            @Override
            public void println(String msg) {
                System.out.println(msg);
            }

            @Override
            public void println(Exception e, String msg) {
                System.out.println(msg);
                e.printStackTrace(System.out);
            }
        });

    }

}
