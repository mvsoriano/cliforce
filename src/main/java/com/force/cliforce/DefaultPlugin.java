package com.force.cliforce;

import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.PartnerConnection;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class DefaultPlugin implements Plugin {


    private CLIForce force;

    public DefaultPlugin(CLIForce it) {
        force = it;
    }

    @Override
    public List<CommandDescriptor> getCommands() {
        return Arrays.asList(new CommandDescriptor("list", new ListCustomObjects()),
                new CommandDescriptor("dbclean", new DBClean()),
                new CommandDescriptor("info", new InfoCommand(force)),
                new CommandDescriptor("help", new HelpCommand(force)),
                new CommandDescriptor("plugin", new PluginCommand(force)),
                new CommandDescriptor("unplug", new UnplugCommand(force)),
                new CommandDescriptor("exit", new Command() {
                    @Override
                    public String describe() {
                        return "Exit this shell";
                    }

                    @Override
                    public void execute(String[] args, PartnerConnection partner, MetadataConnection metadata, PrintWriter output) throws Exception {
                        //No-op, will exit
                    }
                }));
    }

    @Override
    public String getName() {
        return "DefaultPlugin";
    }

    public static class ListCustomObjects implements Command {

        @Override
        public String describe() {
            return "list the existing custom objects and their fields";
        }

        @Override
        public void execute(String[] args, PartnerConnection partner, MetadataConnection metadata, PrintWriter out) throws Exception {
            ListMetadataQuery q = new ListMetadataQuery();
            q.setType("CustomObject");
            FileProperties[] fpa = metadata.listMetadata(new ListMetadataQuery[]{q}, 20.0);
            List<String> sobjs = new ArrayList<String>();
            for (FileProperties fileProperties : fpa) {
                if (fileProperties.getFullName().endsWith("__c")) {
                    sobjs.add(fileProperties.getFullName());
                }
            }
            DescribeSObjectResult[] describeSObjectResults = partner.describeSObjects(sobjs.toArray(new String[0]));
            for (DescribeSObjectResult describeSObjectResult : describeSObjectResults) {
                out.printf("\n{\nCustom Object-> %s \n", describeSObjectResult.getName());
                for (Field field : describeSObjectResult.getFields()) {
                    out.printf("       field -> %s (type: %s)\n", field.getName(), field.getType().toString());
                }
                out.print("}\n");
            }
        }
    }

    public static class HelpCommand implements Command {
        private CLIForce force;

        public HelpCommand(CLIForce it) {
            force = it;
        }

        @Override
        public String describe() {
            return "Display this help message";
        }

        @Override
        public void execute(String[] args, PartnerConnection partner, MetadataConnection metadata, PrintWriter log) throws Exception {
            for (Map.Entry<String, CommandDescriptor> entry : force.commands.entrySet()) {
                log.printf("%s: %s\n", entry.getKey(), entry.getValue().command.describe());
            }
        }
    }

    public static class InfoCommand implements Command {

        private CLIForce force;

        public InfoCommand(CLIForce it) {
            force = it;
        }

        @Override
        public String describe() {
            return "Show the current connection info:";
        }

        @Override
        public void execute(String[] args, PartnerConnection partner, MetadataConnection metadata, PrintWriter log) throws Exception {
            log.printf("Current User: %s\n", force.forceEnv.getUser());
            log.printf("Current Endpoint: %s\n", force.forceEnv.getHost());
        }
    }

    public static class PluginCommand implements Command {

        private CLIForce force;
        public static final String SYNTAX = "plugin some.plugin.ClassName mavengroup:artifact:version";

        public PluginCommand(CLIForce it) {
            force = it;
        }


        @Override
        public String describe() {
            return "adds a plugin to the shell. Syntax: " + SYNTAX;
        }


        @Override
        public void execute(String[] args, PartnerConnection partner, MetadataConnection metadata, PrintWriter output) throws Exception {
            if (args.length == 0) {
                output.println("Listing plugins...");
                for (Map.Entry<String, Plugin> e : force.plugins.entrySet()) {
                    output.printf("Plugin: %s (%s)\n", e.getKey(), e.getValue().getName());
                }
                output.println("Done.");
            } else if (args.length == 2) {
                String pluginClass = args[0];
                String pluginDep = args[1];
                File jar = new File(getMavenLocalPath(pluginDep));

                if (jar.exists()) {
                    ClassLoader curr = Thread.currentThread().getContextClassLoader();
                    URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{jar.toURI().toURL()}, curr);
                    try {
                        Thread.currentThread().setContextClassLoader(urlClassLoader);
                        Object po = urlClassLoader.loadClass(pluginClass).newInstance();
                        if (po instanceof Plugin) {
                            Plugin p = (Plugin) po;
                            List<CommandDescriptor> commands = p.getCommands();
                            force.plugins.put(p.getName(), p);
                            output.printf("Adding Plugin: %s (%s)\n", p.getName(), p.getClass().getName());
                            for (CommandDescriptor command : commands) {
                                output.printf("  -> adds command %s (%s)\n", command.name, command.command.getClass().getName());
                                force.commands.put(command.name, command);
                            }
                        }
                        force.reloadCompletions();
                    } finally {
                        Thread.currentThread().setContextClassLoader(curr);
                    }
                }
            } else {
                StringBuilder b = new StringBuilder();
                for (String arg : args) {
                    b.append(arg).append(" ");
                }
                output.println("Unexpected Command Format:" + b.toString());
                output.println("expected syntax:" + SYNTAX);
            }
        }

        String getMavenLocalPath(String cmdArg) {
            String[] deps = cmdArg.split(":");
            if (deps.length == 3) {
                String groupId = deps[0];
                String artifactId = deps[1];
                String version = deps[2];
                StringBuilder jar = new StringBuilder(System.getProperty("user.home") + "/.m2/repository/");
                jar.append(groupId.replace(".", "/")).append("/");
                jar.append(artifactId).append("/").append(version).append("/");
                jar.append(artifactId).append("-").append(version).append(".jar");
                return jar.toString();
            }
            return "";
        }

        List<URL> resolveWithDependencies(String group, String artifact, String version) throws RuntimeException {
            try {
                Ivy ivy = Ivy.newInstance();

                ivy.configureDefault();

                File ivyfile = File.createTempFile("ivy", ".xml");
                ivyfile.deleteOnExit();
                DefaultModuleDescriptor md = DefaultModuleDescriptor
                        .newDefaultInstance(ModuleRevisionId.newInstance(group,
                                artifact + "-caller", "working"));
                DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md,
                        ModuleRevisionId.newInstance(group, artifact, version),
                        false, false, true);
                md.addDependency(dd);
                XmlModuleDescriptorWriter.write(md, ivyfile);

                String[] confs = new String[]{"default"};
                ResolveOptions resolveOptions = new ResolveOptions().setConfs(confs);
                ResolveReport report = ivy.resolve(ivyfile.toURI().toURL(), resolveOptions);
                if (!report.hasError()) {
                    List<URL> urls = new ArrayList<URL>();
                    for (Artifact a : (List<Artifact>) report.getArtifacts()) {
                        urls.add(a.getUrl());
                    }
                    return urls;
                } else {
                    throw new RuntimeException("Error Resolving dependencies");
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }


    public static class UnplugCommand implements Command {
        private CLIForce force;

        public UnplugCommand(CLIForce it) {
            force = it;
        }

        @Override
        public String describe() {
            return "removes a plugin from the shell";
        }

        @Override
        public void execute(String[] args, PartnerConnection partner, MetadataConnection metadata, PrintWriter output) throws Exception {
            for (String arg : args) {
                output.printf("attempting to remove plugin: %s\n", arg);
                Plugin p = force.plugins.remove(arg);
                if (p == null) {
                    output.println("....not found");
                } else {
                    for (CommandDescriptor commandDescriptor : p.getCommands()) {
                        force.commands.remove(commandDescriptor.name);
                        output.printf("removed command: %s\n", commandDescriptor.name);
                    }
                    force.reloadCompletions();
                    output.println("\nDone");
                }
            }
        }
    }
}
