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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * DependencyResolver  that uses maven-aether to resolve the dependency graph for a given dependency.
 * <p/>
 * <p/>
 * This class refuses to put log4j or commons logging onto a classpath, instead substituting log4j-over-slf4j and clogging-over-slf4j
 * so that the debug --on command will cause all log output to be visible.
 */
public class DependencyResolver {

    public enum Scope {
        RUNTIME,
        COMPILE,
        TEST
    }

    public static final String JCL_OVER_SLF4J = "jcl-over-slf4j";
    public static final String LOG4J_OVER_SLF4J = "log4j-over-slf4j";
    public static final String LOG_4_J = "log4j";
    public static final String COMMONS_LOGGING = "commons-logging";
    public static final String COMMONS_LOGGING_API = "commons-logging-api";
    public static final String COMPILE = "compile";
    public static final String TEST = "test";
    public static final String SNAPSHOT = "SNAPSHOT";
    public static final String LATEST = "LATEST";
    public static final String RELEASE = "RELEASE";
    public static final String DEFAULT_LAYOUT = "default";
    public static final String FILE_HINT = "file";
    public static final String HTTP_HINT = "http";

    private volatile boolean initialized = false;
    private final Object lock = new Object();
    private String cliforceGroup;
    private String cliforceArtifact;
    private String cliforceVersion;
    private RepositorySystem repositorySystem;
    private MavenRepositorySystemSession session = new MavenRepositorySystemSession();
    private String latestMetaVersion = RELEASE;
    private List<RemoteRepository> remoteRepositories;
    private URL log4jOverSlf4j;
    private URL cloggingOverSlf4j;
    private LocalRepository localRepository = new LocalRepository(Boot.getLocalMavenRepository());


    public void setRepositories(Properties repositories) {
        remoteRepositories = new ArrayList<RemoteRepository>(repositories.size());
        for (String name : repositories.stringPropertyNames()) {
            remoteRepositories.add(new RemoteRepository(name, DEFAULT_LAYOUT, repositories.getProperty(name)));
        }
    }

    public void setRepositories(Map<String, String> repositories) {
        remoteRepositories = new ArrayList<RemoteRepository>(repositories.size());
        for (String name : repositories.keySet()) {
            remoteRepositories.add(new RemoteRepository(name, DEFAULT_LAYOUT, repositories.get(name)));
        }
    }

    public void setCLIForceMavenCoordinates(String coords) {
        String[] gav = coords.split(":");
        if (gav.length != 3) {
            throw new IllegalArgumentException("Expected coordinates in groupId:artifactId:version format");
        } else {
            cliforceGroup = gav[0];
            cliforceArtifact = gav[1];
            cliforceVersion = gav[2];
            if (cliforceVersion.contains(SNAPSHOT)) {
                latestMetaVersion = LATEST;
            }
        }
    }

    public void setLocalRepository(String path) {
        localRepository = new LocalRepository(path);
    }

    private void initialize() {
        if (!initialized) {
            synchronized (lock) {
                if (!initialized) {
                    try {
                        DefaultServiceLocator locator = new DefaultServiceLocator();
                        locator.setServices(WagonProvider.class, new ManualWagonProvider());
                        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
                        repositorySystem = locator.getService(RepositorySystem.class);
                        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(localRepository));
                        initialized = true;
                    } catch (Exception e) {
                        System.err.println("Exception configuring maven, cant continue");
                        e.printStackTrace(System.err);
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public static class ManualWagonProvider implements WagonProvider {

        @Override
        public Wagon lookup(String roleHint)
                throws Exception {
            if (FILE_HINT.equals(roleHint)) {
                return new FileWagon();
            } else if (HTTP_HINT.equals(roleHint)) {
                return new LightweightHttpWagon();
            }
            return null;
        }

        @Override
        public void release(Wagon wagon) {

        }
    }
    
    private URLClassLoader createClassLoaderInternal(String groupId, String artifactId, String type, String version, Scope scope, ClassLoader parent) {
        try {
            initialize();
            DefaultArtifact defaultArtifact = new DefaultArtifact(groupId + ":" + artifactId + ":" + type + ":" + version);
            Dependency dependency =
                    new Dependency(defaultArtifact, scope.toString().toLowerCase());
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
                    if (file != null && !isWar(file)) {
                        classpath.add(file.toURI().toURL());
                        if (scope == Scope.TEST && version.equals(dn.getVersion().toString())) {
                            file = new File(file.getPath().replace(".jar", "-tests.jar"));
                            if (file.exists()) {
                                classpath.add(file.toURI().toURL());
                            }
                        }
                    } else {
                		try {
                			String warDirName = Boot.getCliforceHome() + "/" + ZipUtil.TEMP_SUB_DIR_NAME;
                			ZipUtil.unzipWarFile(file, new File(warDirName));
                			//classpath.add(new URL("file:" + warDirName + "*"));
                			//classpath.add(new URL("file:" + warDirName));
                			classpath.add(new URL("file:" + warDirName + "WEB-INF/classes/"));
//                			File warDirFile = new File(warDirName);
//                			URL warDirUrl = new URL("file:" + warDirName + "*");
//                			URL pesUrl = new URL("file:" + warDirName + "persistence.xml");
                			//throw new DependencyResolutionException("adding to cs: " + warDirFile.toURI().toURL() + " -- " + warDirUrl.toString() + "--" + pesUrl.toString());
                		} catch(IOException e) {
                			throw new DependencyResolutionException(e);
                		}                    	
                    }
                }
            }


//            for (File file : nlg.getFiles()) {
//            	if(isWar(file)) {
//            		try {
//            			File warDir = new File(Boot.getCliforceHome() + "/" + ZipUtil.TEMP_SUB_DIR_NAME);
//            			ZipUtil.unzipWarFile(file, warDir);
//            			classpath.add(warDir.toURI().toURL());
//            		} catch(IOException e) {
//            			throw new DependencyResolutionException(e);
//            		}
//            	} else {
//            		//classpath.add(file.toURI().toURL());
//            	}
//            }
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
    
    private boolean isWar(File file) {
    	return "war".equals(file.getName().substring(file.getName().length() - 3, file.getName().length()));
    }

    private void setupLoggingDependencies(String groupId, String artifactId, Collection<URL> classpath) {
        if (groupId.equals(cliforceGroup) && artifactId.equals(cliforceArtifact)) {
            for (URL url : classpath) {


                if (url.toString().contains(JCL_OVER_SLF4J)) {
                    cloggingOverSlf4j = url;
                }
                if (url.toString().contains(LOG4J_OVER_SLF4J)) {
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
        if (dependency.getArtifact().getGroupId().equals(LOG_4_J) && dependency.getArtifact().getArtifactId().equals(LOG_4_J)) {
            return false;
        }
        if (dependency.getArtifact().getGroupId().equals(COMMONS_LOGGING) && dependency.getArtifact().getArtifactId().equals(COMMONS_LOGGING)) {
            return false;
        }

        if (dependency.getArtifact().getGroupId().equals(COMMONS_LOGGING) && dependency.getArtifact().getArtifactId().equals(COMMONS_LOGGING_API)) {
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
        return createClassLoaderFor(groupId, artifactId, version, Scope.RUNTIME, parent, out);
    }
    
    public ClassLoader createClassLoaderFor(String groupId, String artifactId, String version, Scope scope, ClassLoader parent, OutputAdapter out) throws DependencyResolutionException {
    	return createClassLoaderFor(groupId, artifactId, "jar", version, scope, parent, out);
    }
    
    /**
     * Create a classloader that contains all the runtime dependencies of the given maven dependency.
     * Attempt offline resolution first, and if that fails, attempt online resolution.
     *
     * @param groupId    maven groupId
     * @param artifactId maven artifactid
     * @param version    maven version
     * @param scope      maven dependency scope ("runtime", "test" etc.)
     * @param parent     for the created classloader
     * @param out        output adapter
     * @return a classloader with all the runtime dependencies of the given maven artifact.
     * @throws DependencyResolutionException if the artifact cant be resolved
     */
    public ClassLoader createClassLoaderFor(String groupId, String artifactId, String type, String version, Scope scope, ClassLoader parent, OutputAdapter out) throws DependencyResolutionException {
        try {
            return createClassLoaderInternal(groupId, artifactId, type, version, scope, parent);
        } catch (DependencyResolutionException dre) {
            out.println(dre, "Exception resolving dependencies");
            throw dre;
        }
    }
}
