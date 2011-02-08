package com.force.cliforce.dependency;


import com.force.cliforce.Boot;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/*
NOT THREADSAFE, but there shouldnt be a reason it needs to be.
 */
public class DependencyResolver {


    private static DependencyResolver instance = new DependencyResolver();

    Properties cliforceProperties;
    RepositorySystem repositorySystem;
    MavenRepositorySystemSession session = new MavenRepositorySystemSession();
    String latestMetaVersion = "RELEASE";
    List<RemoteRepository> remoteRepositories;

    {
        try {
            DefaultServiceLocator locator = new DefaultServiceLocator();
            locator.setServices(WagonProvider.class, new ManualWagonProvider());
            locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
            repositorySystem = locator.getService(RepositorySystem.class);
            String local = System.getProperty("user.home") + "/.m2/repository/";
            LocalRepository localRepo = new LocalRepository(local);
            session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(localRepo));
            RemoteRepository central = new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
            RemoteRepository force = new RemoteRepository("force", "default", "http://repo.t.salesforce.com/archiva/repository/releases");
            RemoteRepository forcesnap = new RemoteRepository("forcesnap", "default", "http://repo.t.salesforce.com/archiva/repository/snapshots");
            remoteRepositories = Arrays.asList(central, force, forcesnap);
            cliforceProperties = new Properties();
            cliforceProperties.load(getClass().getClassLoader().getResourceAsStream("cliforce.properties"));
            //If cliforce itself is a snapshot, use "LATEST" instead of "RELEASE" to find dependencies with no version
            if (cliforceProperties.getProperty("version").contains("SNAPSHOT")) {
                latestMetaVersion = "LATEST";
            }
            createClassLoaderFor(cliforceProperties.getProperty("groupId"), cliforceProperties.getProperty("artifactId"), cliforceProperties.getProperty("version"), Thread.currentThread().getContextClassLoader(), new Boot.SystemOutputAdapter());
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


    private URLClassLoader createClassLoaderInternal(String groupId, String artifactId, String version, ClassLoader parent, boolean offline) {
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
            List<URL> classpath = new ArrayList<URL>();


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
            return new URLClassLoader(classpath.toArray(new URL[classpath.size()]), parent);

        } catch (DependencyCollectionException e) {
            throw new DependencyResolutionException(e);
        } catch (ArtifactResolutionException e) {
            throw new DependencyResolutionException(e);
        } catch (MalformedURLException e) {
            throw new DependencyResolutionException(e);
        }

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
        return true;
    }

    public ClassLoader createClassLoaderFor(String groupId, String artifactId, ClassLoader parent, OutputAdapter out) throws DependencyResolutionException {
        return createClassLoaderFor(groupId, artifactId, latestMetaVersion, parent, out);
    }

    public ClassLoader createClassLoaderFor(String groupId, String artifactId, String version, ClassLoader parent, OutputAdapter out) throws DependencyResolutionException {
        try {
            //TRY OFFLINE RESOLUTION FIRST FOR SPEED
            return createClassLoaderInternal(groupId, artifactId, version, parent, true);
        } catch (DependencyResolutionException e) {
            try {
                return createClassLoaderInternal(groupId, artifactId, version, parent, false);
            } catch (DependencyResolutionException dre) {
                out.println(dre, "Exception resolving dependencies after trying both offline and online mode");
                throw dre;
            }

        }
    }


}
