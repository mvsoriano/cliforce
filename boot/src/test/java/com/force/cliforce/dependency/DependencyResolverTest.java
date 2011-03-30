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
