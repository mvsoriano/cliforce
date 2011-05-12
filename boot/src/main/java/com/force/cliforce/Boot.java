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
import com.force.cliforce.dependency.OutputAdapter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

public class Boot {

    static Properties cliforce;
    static Properties repositories;

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        if (!(new File(withSeparator(getCliforceHome()) + withSeparator(".force") + "cliforce_plugins").exists())) {
            System.out.println("Downloading dependencies, this can take some time the first time you run cliforce");
        }
        DependencyResolver resolver = getBootResolver();
        ClassLoader cl = resolver.createClassLoaderFor(cliforce.getProperty("groupId"), cliforce.getProperty("artifactId"), null, new SystemOutputAdapter());
        Thread.currentThread().setContextClassLoader(cl);
        cl.loadClass(cliforce.getProperty("main")).getMethod("main", new Class<?>[]{String[].class}).invoke(null, new Object[]{args});
    }

    public static DependencyResolver getBootResolver() throws IOException {
        return getBootResolver(getCLIForceProperties(), getRepositories());
    }

    private static DependencyResolver getBootResolver(Properties cli, Properties repo) {
        DependencyResolver resolver = new DependencyResolver();
        resolver.setRepositories(repo);
        resolver.setCLIForceMavenCoordinates(cli.getProperty("groupId") + ":" + cli.getProperty("artifactId") + ":" + cli.getProperty("version"));
        return resolver;
    }

    public static String getCliforceHome() {
        return System.getProperty("cliforce.home", System.getProperty("user.home"));
    }

    public static String getLocalMavenRepository() {
        return System.getProperty("maven.repo", withSeparator(getCliforceHome()) + withSeparator(".m2") + withSeparator("repository"));
    }


    public static Properties getCLIForceProperties() throws IOException {
        if (cliforce == null) {
            cliforce = new Properties();
            cliforce.load(Boot.class.getClassLoader().getResourceAsStream("cliforce.properties"));
        }
        return cliforce;
    }

    public static Properties getRepositories() throws IOException {
        if (repositories == null) {
            repositories = new Properties();
            if (getCLIForceProperties().getProperty("version").contains("SNAPSHOT")) {
                repositories.load(Boot.class.getClassLoader().getResourceAsStream("snapshot-repositories.properties"));
            } else {
                repositories.load(Boot.class.getClassLoader().getResourceAsStream("release-repositories.properties"));
            }

        }
        return repositories;
    }

    private static String withSeparator(String str) {
        return str + File.separator;
    }
    
    public static class SystemOutputAdapter implements OutputAdapter {
        @Override
        public void println(String msg) {
            System.out.println(msg);
        }

        @Override
        public void println(Exception e, String msg) {
            System.out.print(msg);
            e.printStackTrace(System.out);
        }
    }

}
