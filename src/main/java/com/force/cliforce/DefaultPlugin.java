package com.force.cliforce;

import com.force.cliforce.dependency.DependencyResolver;
import com.force.cliforce.dependency.OutputAdapter;
import com.force.cliforce.plugin.dbclean.DBClean;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
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
    public List<Command> getCommands() {
        return Arrays.asList(new ListCustomObjects(),
                new DBClean(),
                new ConnectionInfoCommand(force),
                new HelpCommand(force),
                new PluginCommand(force),
                new UnplugCommand(force),
                new Command() {
                    @Override
                    public String name() {
                        return "exit";
                    }

                    @Override
                    public String describe() {
                        return "Exit this shell";
                    }

                    @Override
                    public void execute(CommandContext ctx) throws Exception {
                        //No-op, will exit
                    }
                });
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
        public String name() {
            return "list";
        }

        @Override
        public void execute(CommandContext ctx) throws Exception {
            ListMetadataQuery q = new ListMetadataQuery();
            q.setType("CustomObject");
            FileProperties[] fpa = ctx.getMetadataConnection().listMetadata(new ListMetadataQuery[]{q}, 20.0);
            List<String> sobjs = new ArrayList<String>();
            for (FileProperties fileProperties : fpa) {
                if (fileProperties.getFullName().endsWith("__c")) {
                    sobjs.add(fileProperties.getFullName());
                }
            }
            DescribeSObjectResult[] describeSObjectResults = ctx.getPartnerConnection().describeSObjects(sobjs.toArray(new String[0]));
            for (DescribeSObjectResult describeSObjectResult : describeSObjectResults) {
                ctx.getCommandWriter().printf("\n{\nCustom Object-> %s \n", describeSObjectResult.getName());
                for (Field field : describeSObjectResult.getFields()) {
                    ctx.getCommandWriter().printf("       field -> %s (type: %s)\n", field.getName(), field.getType().toString());
                }
                ctx.getCommandWriter().print("}\n");
            }
        }
    }

    public static class HelpCommand implements Command {
        private CLIForce force;

        public HelpCommand(CLIForce it) {
            force = it;
        }

        @Override
        public String name() {
            return "help";
        }

        @Override
        public String describe() {
            return "Display this help message";
        }

        @Override
        public void execute(CommandContext ctx) throws Exception {
            for (Map.Entry<String, Command> entry : force.commands.entrySet()) {
                ctx.getCommandWriter().printf("%s: %s\n", entry.getKey(), entry.getValue().describe());
            }
        }
    }

    public static class ConnectionInfoCommand implements Command {

        private CLIForce force;

        @Override
        public String name() {
            return "connection";
        }

        public ConnectionInfoCommand(CLIForce it) {
            force = it;
        }


        @Override
        public String describe() {
            return "Show the current connection info:";
        }

        @Override
        public void execute(CommandContext ctx) throws Exception {
            ctx.getCommandWriter().printf("Current User: %s\n", force.forceEnv.getUser());
            ctx.getCommandWriter().printf("Current Endpoint: %s\n", force.forceEnv.getHost());
        }
    }

    public static class PluginCommand implements Command {

        private CLIForce force;
        public static final String SYNTAX = "plugin some.plugin.ClassName mavengroup:artifact:version";

        public PluginCommand(CLIForce it) {
            force = it;
        }

        @Override
        public String name() {
            return "plugin";
        }

        @Override
        public String describe() {
            return "adds a plugin to the shell. Syntax: " + SYNTAX;
        }


        @Override
        public void execute(final CommandContext ctx) throws Exception {
            String[] args = ctx.getCommandArguments();
            PrintStream output = ctx.getCommandWriter();
            if (args.length == 0) {
                output.println("Listing plugins...");
                for (Map.Entry<String, Plugin> e : force.plugins.entrySet()) {
                    output.printf("Plugin: %s (%s)\n", e.getKey(), e.getValue().getName());
                }
                output.println("Done.");
            } else if (args.length == 2) {
                String pluginClass = args[0];
                String[] pluginDep = args[1].split(":");
                if (pluginDep.length != 3) {
                    output.printf("Unexpected Plugin Dependency Spec %s, expected groupId:artifactId:version", args[1]);
                    return;
                }
                String groupId = pluginDep[0], artifactId = pluginDep[1], version = pluginDep[2];


                ClassLoader curr = Thread.currentThread().getContextClassLoader();
                OutputAdapter oa = new OutputAdapter() {
                    @Override
                    public void println(String msg) {
                        ctx.getCommandWriter().println(msg);
                    }

                    @Override
                    public void println(Exception e, String msg) {
                        ctx.getCommandWriter().println(msg);
                        e.printStackTrace(ctx.getCommandWriter());
                    }
                };
                ClassLoader pcl = DependencyResolver.getInstance().createClassLoaderFor(groupId, artifactId, version, curr, oa);
                try {
                    Thread.currentThread().setContextClassLoader(pcl);
                    Object po = pcl.loadClass(pluginClass).newInstance();
                    if (po instanceof Plugin) {
                        Plugin p = (Plugin) po;
                        List<Command> commands = p.getCommands();
                        force.plugins.put(p.getName(), p);
                        output.printf("Adding Plugin: %s (%s)\n", p.getName(), p.getClass().getName());
                        for (Command command : commands) {
                            output.printf("  -> adds command %s (%s)\n", command.name(), command.getClass().getName());
                            force.commands.put(command.name(), command);
                        }
                    } else {
                        output.printf("ERROR, %s not an instance of Plugin\n", po.getClass().getName());
                    }
                    force.reloadCompletions();
                } finally {
                    Thread.currentThread().setContextClassLoader(curr);
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
        public String name() {
            return "unplug";
        }

        @Override
        public String describe() {
            return "removes a plugin from the shell";
        }

        @Override
        public void execute(CommandContext ctx) throws Exception {
            for (String arg : ctx.getCommandArguments()) {
                ctx.getCommandWriter().printf("attempting to remove plugin: %s\n", arg);
                Plugin p = force.plugins.remove(arg);
                if (p == null) {
                    ctx.getCommandWriter().println("....not found");
                } else {
                    for (Command command : p.getCommands()) {
                        force.commands.remove(command.name());
                        ctx.getCommandWriter().printf("removed command: %s\n", command.name());
                    }
                    force.reloadCompletions();
                    ctx.getCommandWriter().println("\nDone");
                }
            }
        }
    }
}
