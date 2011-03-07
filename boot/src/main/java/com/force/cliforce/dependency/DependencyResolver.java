package com.force.cliforce.dependency;


import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.file.FileWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * DependencyResolver singleton that uses maven-aether to resolve the dependency graph for a given dependency.
 * NOT THREADSAFE, (could be if we moved the session back to being a local var) but there shouldnt be a reason it needs to be.
 * <p/>
 * This class refuses to put log4j or commons logging onto a classpath, instead substituting log4j-over-slf4j and clogging-over-slf4j
 * so that the debug --on command will cause all log output to be visible.
 */
public class DependencyResolver {


    private static DependencyResolver instance = new DependencyResolver();

    Properties cliforceProperties;
    Properties repositories;
    RepositorySystem repositorySystem;
    MavenRepositorySystemSession session = new MavenRepositorySystemSession();
    String latestMetaVersion = "RELEASE";
    List<RemoteRepository> remoteRepositories;
    URL log4jOverSlf4j;
    URL cloggingOverSlf4j;

    {
        try {
            DefaultServiceLocator locator = new DefaultServiceLocator();
            locator.setServices(WagonProvider.class, new ManualWagonProvider());
            locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
            repositorySystem = locator.getService(RepositorySystem.class);
            String local = System.getProperty("user.home") + "/.m2/repository/";
            LocalRepository localRepo = new LocalRepository(local);
            session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(localRepo));
            repositories = new Properties();
            repositories.load(getClass().getClassLoader().getResourceAsStream("repositories.properties"));
            remoteRepositories = new ArrayList<RemoteRepository>(repositories.size());
            for (String name : repositories.stringPropertyNames()) {
                remoteRepositories.add(new RemoteRepository(name, "default", repositories.getProperty(name)));
            }
            cliforceProperties = new Properties();
            cliforceProperties.load(getClass().getClassLoader().getResourceAsStream("cliforce.properties"));
            //If cliforce itself is a snapshot, use "LATEST" instead of "RELEASE" to find dependencies with no version
            if (cliforceProperties.getProperty("version").contains("SNAPSHOT")) {
                latestMetaVersion = "LATEST";
            }

        } catch (Exception e) {
            System.err.println("Exception configuring maven, cant continue");
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

    public static class ManualWagonProvider implements WagonProvider {

        public Wagon lookup(String roleHint)
                throws Exception {
            if ("file".equals(roleHint)) {
                return new FileWagon();
            } else if ("http".equals(roleHint)) {
                return new LightweightHttpWagon();
            }
            return null;
        }

        public void release(Wagon wagon) {

        }

    }

    public static DependencyResolver getInstance() {
        return instance;
    }


    private URLClassLoader createClassLoaderInternal(String groupId, String artifactId, String version, ClassLoader parent) {
        try {

            DefaultArtifact defaultArtifact = new DefaultArtifact(groupId + ":" + artifactId + ":" + version);
            Dependency dependency =
                    new Dependency(defaultArtifact, "runtime");
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(dependency);
            for (RemoteRepository remoteRepository : remoteRepositories) {
                collectRequest.addRepository(remoteRepository);
            }

            DependencyNode node = repositorySystem.collectDependencies(session, collectRequest).getRoot();

            repositorySystem.resolveDependencies(session, node, null);

            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();

            node.accept(nlg);
            Set<URL> classpath = new HashSet<URL>();


            for (DependencyNode dn : nlg.getNodes()) {
                if (dn.getDependency() != null && ok(dn.getDependency())) {
                    File file = dn.getDependency().getArtifact().getFile();
                    if (file != null) {
                        classpath.add(file.toURI().toURL());
                    }
                }
            }


            for (File file : nlg.getFiles()) {
                classpath.add(file.toURI().toURL());
            }
            setupLoggingDependencies(groupId, artifactId, classpath);
            addLoggingDependencies(classpath);
            return new URLClassLoader(classpath.toArray(new URL[classpath.size()]), parent);

        } catch (DependencyCollectionException e) {
            throw new DependencyResolutionException(e);
        } catch (ArtifactResolutionException e) {
            throw new DependencyResolutionException(e);
        } catch (MalformedURLException e) {
            throw new DependencyResolutionException(e);
        }

    }

    private void setupLoggingDependencies(String groupId, String artifactId, Collection<URL> classpath) {
        if (groupId.equals(cliforceProperties.getProperty("groupId")) && artifactId.equals(cliforceProperties.getProperty("artifactId"))) {
            for (URL url : classpath) {
                if (url.toString().contains("jcl-over-slf4j")) {
                    cloggingOverSlf4j = url;
                }
                if (url.toString().contains("log4j-over-slf4j")) {
                    log4jOverSlf4j = url;
                }
            }
        }
    }

    private void addLoggingDependencies(Collection<URL> classpath) {
        if (log4jOverSlf4j != null) classpath.add(log4jOverSlf4j);
        if (cloggingOverSlf4j != null) classpath.add(cloggingOverSlf4j);
    }

    /**
     * Filter out commons logging and log4j since we are bridging them back to slf4j
     *
     * @param dependency
     * @return
     */
    private boolean ok(Dependency dependency) {
        if (dependency.getArtifact().getGroupId().equals("log4j") && dependency.getArtifact().getArtifactId().equals("log4j")) {
            return false;
        }
        if (dependency.getArtifact().getGroupId().equals("commons-logging") && dependency.getArtifact().getArtifactId().equals("commons-logging")) {
            return false;
        }

        if (dependency.getArtifact().getGroupId().equals("commons-logging") && dependency.getArtifact().getArtifactId().equals("commons-logging-api")) {
            return false;
        }
        return true;
    }

    /**
     * Create a classloader that contains all the runtime dependencies of the given maven dependency. If this
     * is a SNAPSHOT version of cliforce, the maven meta version LATEST is used, if it is a release version of cliforce the maven
     * meta version RELEASE is used.
     *
     * @param groupId    maven groupId
     * @param artifactId maven artifactId
     * @param parent     parent for the created classloader
     * @param out        output adapter
     * @return a classloader with all the runtime dependencies of the given maven artifact.
     * @throws DependencyResolutionException if the artifact cant be resolved
     */
    public ClassLoader createClassLoaderFor(String groupId, String artifactId, ClassLoader parent, OutputAdapter out) throws DependencyResolutionException {
        return createClassLoaderFor(groupId, artifactId, latestMetaVersion, parent, out);
    }

    /**
     * Create a classloader that contains all the runtime dependencies of the given maven dependency.
     * Attempt offline resolution first, and if that fails, attempt online resolution.
     *
     * @param groupId    maven groupId
     * @param artifactId maven artifactid
     * @param version    maven version
     * @param parent     for the created classloader
     * @param out        output adapter
     * @return a classloader with all the runtime dependencies of the given maven artifact.
     * @throws DependencyResolutionException if the artifact cant be resolved
     */
    public ClassLoader createClassLoaderFor(String groupId, String artifactId, String version, ClassLoader parent, OutputAdapter out) throws DependencyResolutionException {
        try {
            return createClassLoaderInternal(groupId, artifactId, version, parent);
        } catch (DependencyResolutionException dre) {
            out.println(dre, "Exception resolving dependencies after trying both offline and online mode");
            throw dre;
        }

    }


}
