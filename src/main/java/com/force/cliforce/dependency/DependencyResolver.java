package com.force.cliforce.dependency;


import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.util.filter.AndFilter;
import org.apache.ivy.util.filter.ArtifactTypeFilter;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.NotFilter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DependencyResolver {

    private static DependencyResolver instance = new DependencyResolver();
    private Ivy ivy = Ivy.newInstance();

    {
        try {
            ivy.configure(getClass().getClassLoader().getResource("cliforce-ivy-settings.xml"));

        } catch (Exception e) {
            System.err.println("Exception configuring ivy, cant continue");
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

    public static DependencyResolver getInstance() {
        return instance;
    }

    public ClassLoader createClassLoaderFor(URL mavenPom, ClassLoader parent, OutputAdapter out) {
        return null;
    }

    public ClassLoader createClassLoaderFor(String groupId, String artifactId, String version, ClassLoader parent, OutputAdapter out) {
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
            ResolveReport report = ivy.resolve(ivyfile.toURI().toURL(), resolveOptions);
            if (!report.hasError()) {
                List<URL> urls = new ArrayList<URL>();
                for (ArtifactDownloadReport a : report.getAllArtifactsReports()) {
                    urls.add(a.getLocalFile().toURI().toURL());
                }
                for (URL url : urls) {
                    out.println("Loader: " + url.toString());
                }
                return new URLClassLoader(urls.toArray(new URL[0]), parent);
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


}
