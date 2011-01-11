package com.force.cliforce.dependency;


import com.force.cliforce.Boot;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.util.filter.Filter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.ParseException;
import java.util.*;

public class DependencyResolver {


    private static DependencyResolver instance = new DependencyResolver();
    private Ivy ivy;
    Set<String> cliforceDependencies;
    Properties cliforceProperties;

    {
        try {
            ivy = Ivy.newInstance();
            ivy.configure(getClass().getClassLoader().getResource("cliforce-ivy-settings.xml"));
            cliforceProperties = new Properties();
            cliforceProperties.load(getClass().getClassLoader().getResourceAsStream("cliforce.properties"));
            ArtifactFilter f = new ArtifactFilter();
            createClassLoaderInternal(cliforceProperties.getProperty("groupId"), cliforceProperties.getProperty("artifactId"), cliforceProperties.getProperty("version"), Thread.currentThread().getContextClassLoader(), new Boot.SystemOutputAdapter(), f);
            cliforceDependencies = f.getArtifactIds();
        } catch (Exception e) {
            System.err.println("Exception configuring ivy, cant continue");
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

    public static DependencyResolver getInstance() {
        return instance;
    }


    private URLClassLoader createClassLoaderInternal(String groupId, String artifactId, String version, ClassLoader parent, OutputAdapter out, Filter filter) {
        File ivyfile = null;
        try {
            ivyfile = File.createTempFile("ivy", ".xml");
            ivyfile.deleteOnExit();
            ModuleRevisionId working = ModuleRevisionId.newInstance(groupId, artifactId + "-caller", "working");
            DefaultModuleDescriptor md = DefaultModuleDescriptor.newDefaultInstance(working);
            ModuleRevisionId moduleRevisionId = ModuleRevisionId.newInstance(groupId, artifactId, version);
            DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, moduleRevisionId, false, false, true);
            md.addDependency(dd);
            XmlModuleDescriptorWriter.write(md, ivyfile);

            String[] confs = new String[]{"*"};
            ResolveOptions resolveOptions = new ResolveOptions().setConfs(confs);
            resolveOptions.setArtifactFilter(filter);
            ResolveReport report = ivy.resolve(ivyfile.toURI().toURL(), resolveOptions);
            if (!report.hasError()) {
                List<URL> urls = new ArrayList<URL>();
                for (ArtifactDownloadReport a : report.getAllArtifactsReports()) {
                    urls.add(a.getLocalFile().toURI().toURL());
                }
                for (URL url : urls) {
                    out.println("Loader: " + url.toString());
                }
                URLClassLoader ucl = new URLClassLoader(urls.toArray(new URL[0]), parent);
                return ucl;
            } else {
                for (ArtifactDownloadReport rpt : report.getFailedArtifactsReports()) {
                    out.println(rpt.getDownloadDetails());
                }
                throw new RuntimeException("Unable to resolve dependencies");
            }
        } catch (IOException e) {
            out.println(e, "Error Resolving dependencies");
            throw new RuntimeException("Error Resolving dependencies", e);
        } catch (ParseException e) {
            out.println(e, "Error Resolving dependencies");
            throw new RuntimeException("Error Resolving dependencies", e);
        }

    }

    public ClassLoader createClassLoaderFor(String groupId, String artifactId, ClassLoader parent, OutputAdapter out) {
        return createClassLoaderFor(groupId, artifactId, "latest.release", parent, out);
    }

    public ClassLoader createClassLoaderFor(String groupId, String artifactId, String version, ClassLoader parent, OutputAdapter out) {
        Filter f = null;
        if (groupId.equals(cliforceProperties.getProperty("groupId")) && artifactId.equals(cliforceProperties.getProperty("artifactId"))) {
            f = new ArtifactFilter();
        } else {
            f = new CliforceArtifactFilter();
        }
        return createClassLoaderInternal(groupId, artifactId, version, parent, out, f);
    }

    public static class ArtifactFilter implements Filter {

        static Set<String> excludedTypes = new HashSet<String>() {{
            add("source");
            add("javadoc");
        }};

        Set<String> artifactIds = new HashSet<String>();

        public boolean accept(Object o) {
            if (o instanceof MDArtifact) {
                MDArtifact mda = (MDArtifact) o;
                artifactIds.add(mda.toString());
                return !excludedTypes.contains(mda.getType());
            } else {
                return true;
            }

        }

        public Set<String> getArtifactIds() {
            return artifactIds;
        }

        public String toString() {
            return "ArtifactFilter";
        }
    }

    public class CliforceArtifactFilter extends ArtifactFilter {
        @Override
        public boolean accept(Object o) {
            if (super.accept(o)) {
                if (o instanceof MDArtifact) {
                    MDArtifact mda = (MDArtifact) o;
                    return !cliforceDependencies.contains(mda.toString());
                }
            }
            return false;
        }

    }


}
