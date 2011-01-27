package com.force.cliforce.dependency;

import org.junit.Test;

public class DependencyResolverTest {

    @Test
    public void testSingleDependencyResolution() throws ClassNotFoundException {
        ClassLoader loader = DependencyResolver.getInstance().createClassLoaderFor("log4j", "log4j", "1.2.16", null, new OutputAdapter() {
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
    public void testReleaseDependencyResolution() throws ClassNotFoundException {
        ClassLoader loader = DependencyResolver.getInstance().createClassLoaderFor("log4j", "log4j", "RELEASE",null, new OutputAdapter() {
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
    public void testPluginDependencyResolution() throws ClassNotFoundException {
        ClassLoader loader = DependencyResolver.getInstance().createClassLoaderFor("com.force.cliforce.plugin", "cliplugin", null, new OutputAdapter() {
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
