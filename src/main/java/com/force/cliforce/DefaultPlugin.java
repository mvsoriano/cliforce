package com.force.cliforce;

import com.force.cliforce.dependency.DependencyResolver;
import com.force.cliforce.dependency.OutputAdapter;
import com.force.cliforce.plugin.*;
import com.force.cliforce.plugin.dbclean.DBClean;

import java.io.PrintStream;
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
                new AppsCommand(),
                new PushCommand(),
                new StopCommand(), new StartCommand(), new RestartCommand(),
                new HistoryCommand(force),
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

    public static class HistoryCommand implements Command {

        private CLIForce force;

        public HistoryCommand(CLIForce force) {
            this.force = force;
        }

        @Override
        public String name() {
            return "history";
        }

        @Override
        public String describe() {
            return "Show history of previous commands";
        }

        @Override
        public void execute(CommandContext ctx) throws Exception {
            List<String> historyList = (List<String>) force.reader.getHistory().getHistoryList();
            for (String s : historyList) {
                ctx.getCommandWriter().println(s);
            }

        }
    }
}
