package com.force.cliforce;


import com.force.cliforce.dependency.DependencyResolver;
import com.force.cliforce.dependency.OutputAdapter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

public class Boot {

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        Properties p = new Properties();
        p.load(parent.getResourceAsStream("cliforce.properties"));
        ClassLoader cl = DependencyResolver.getInstance().createClassLoaderFor(p.getProperty("groupId"), p.getProperty("artifactId"), p.getProperty("version"), null, new SystemOutputAdapter());
        Thread.currentThread().setContextClassLoader(cl);
        cl.loadClass(p.getProperty("main")).getMethod("main", new Class<?>[]{String[].class}).invoke(null, new Object[]{args});
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
