package com.force.cliforce;

import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.PartnerConnection;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
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

        public PluginCommand(CLIForce it) {
            force = it;
        }


        @Override
        public String describe() {
            return "adds a plugin to the shell. Syntax: plugin some.plugin.class some:maven:dependency";
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
                            output.printf("Adding Plugin: %s, (%s)\n", p.getName(), p.getClass().getName());
                            for (CommandDescriptor command : commands) {
                                output.printf("  -> adds command %s, (%s)\n", command.name, command.command.getClass().getName());
                                force.commands.put(command.name, command);
                            }
                        }
                    } finally {
                        Thread.currentThread().setContextClassLoader(curr);
                    }
                }
            }
        }

        private String getMavenLocalPath(String cmdArg) {
            String[] deps = cmdArg.split(":");
            if (deps.length == 3) {
                StringBuilder jar = new StringBuilder(System.getProperty("user.home") + "/.m2/repository/");
                jar.append(deps[0].replace(".", "/")).append("/");
                jar.append(deps[1]).append("/").append(deps[2]).append("/");
                jar.append(deps[1]).append("-").append(deps[2]).append(".jar");
                return jar.toString();
            }
            return "";
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
                output.printf("attempting to remove plugin: %s");
                Plugin p = force.plugins.remove(arg);
                if (p == null) {
                    output.println("....not found");
                } else {
                    for (CommandDescriptor commandDescriptor : p.getCommands()) {
                        force.commands.remove(commandDescriptor.name);
                        output.printf("\n removed command: %s", commandDescriptor.name);
                    }
                    output.println("Done");
                }
            }
        }
    }
}
