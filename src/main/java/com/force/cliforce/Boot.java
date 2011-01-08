package com.force.cliforce;


import com.force.cliforce.dependency.DependencyResolver;
import com.force.cliforce.dependency.OutputAdapter;

import java.lang.reflect.InvocationTargetException;

public class Boot {

    //todo dont hardcode version get from pom.properties?
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        ClassLoader cl = DependencyResolver.getInstance().createClassLoaderFor("com.force.cliforce", "cliforce", "0.0.1-SNAPSHOT", parent.getParent(), new SystemOutputAdapter());
        Thread.currentThread().setContextClassLoader(cl);
        cl.loadClass("com.force.cliforce.CLIForce").getMethod("main", new Class<?>[]{String[].class}).invoke(null, new Object[]{args});
    }

    private static class SystemOutputAdapter implements OutputAdapter {
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
