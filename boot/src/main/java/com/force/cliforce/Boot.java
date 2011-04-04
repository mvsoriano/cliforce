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
        if (!(new File(getCliforceHome() + "/.force/cliforce_plugins").exists())) {
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
        return System.getProperty("maven.repo", getCliforceHome() + "/.m2/repository/");
    }


    static Properties getCLIForceProperties() throws IOException {
        if (cliforce == null) {
            cliforce = new Properties();
            cliforce.load(Boot.class.getClassLoader().getResourceAsStream("cliforce.properties"));
        }
        return cliforce;
    }

    static Properties getRepositories() throws IOException {
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
