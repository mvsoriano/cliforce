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
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class DependencyResolver {


    private static DependencyResolver instance = new DependencyResolver();

    Set<String> cliforceDependencies;
    Properties cliforceProperties;
    RepositorySystem repositorySystem;

    {
        try {
            DefaultServiceLocator locator = new DefaultServiceLocator();
            locator.setServices(WagonProvider.class, new ManualWagonProvider());
            locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
            repositorySystem = locator.getService(RepositorySystem.class);
            cliforceProperties = new Properties();
            cliforceProperties.load(getClass().getClassLoader().getResourceAsStream("cliforce.properties"));
            createClassLoaderInternal(cliforceProperties.getProperty("groupId"), cliforceProperties.getProperty("artifactId"), cliforceProperties.getProperty("version"), Thread.currentThread().getContextClassLoader(), new Boot.SystemOutputAdapter());
        } catch (Exception e) {
            System.err.println("Exception configuring ivy, cant continue");
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


    private URLClassLoader createClassLoaderInternal(String groupId, String artifactId, String version, ClassLoader parent, OutputAdapter out) {
        try {
            MavenRepositorySystemSession session = new MavenRepositorySystemSession();
            LocalRepository localRepo = new LocalRepository("/home/sclasen/.m2/repository");
            session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(localRepo));
            Dependency dependency =
                    new Dependency(new DefaultArtifact(groupId + ":" + artifactId + ":" + version), "runtime");
            RemoteRepository central = new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(dependency);
            collectRequest.addRepository(central);
            DependencyNode node = repositorySystem.collectDependencies(session, collectRequest).getRoot();

            repositorySystem.resolveDependencies(session, node, null);

            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            node.accept(nlg);
            List<URL> classpath = new ArrayList<URL>();
            for (File file : nlg.getFiles()) {
                classpath.add(file.toURI().toURL());
            }
            return new URLClassLoader(classpath.toArray(new URL[classpath.size()]), parent);

        } catch (DependencyCollectionException e) {
            throw new RuntimeException(e);
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

    public ClassLoader createClassLoaderFor(String groupId, String artifactId, ClassLoader parent, OutputAdapter out) {
        return createClassLoaderFor(groupId, artifactId, "latest.release", parent, out);
    }

    public ClassLoader createClassLoaderFor(String groupId, String artifactId, String version, ClassLoader parent, OutputAdapter out) {

        return createClassLoaderInternal(groupId, artifactId, version, parent, out);
    }


}
