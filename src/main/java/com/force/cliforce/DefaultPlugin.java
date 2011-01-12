package com.force.cliforce;

import com.beust.jcommander.Parameter;
import com.force.cliforce.dependency.DependencyResolver;
import com.force.cliforce.dependency.OutputAdapter;
import com.force.cliforce.plugin.*;
import com.force.cliforce.plugin.dbclean.DBClean;

import java.io.*;
import java.util.*;


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
                new ShellCommand(),
                new HistoryCommand(force),
                new DBClean(),
                new BannerCommand(),
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
            return "Display this help message: Usage: help <command>";
        }

        @Override
        public void execute(CommandContext ctx) throws Exception {
            if (ctx.getCommandArguments().length == 0) {
                for (Map.Entry<String, Command> entry : force.commands.entrySet()) {
                    ctx.getCommandWriter().printf("%s:\t\t\t %s\n", entry.getKey(), entry.getValue().describe());
                }
            } else {
                String key = ctx.getCommandArguments()[0];
                Command c = force.commands.get(key);
                if (c == null) {
                    ctx.getCommandWriter().printf("No such command: %s", key);
                } else {
                    ctx.getCommandWriter().printf("%s:\t\t\t %s\n", key, c.describe());
                }
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

    public static class PluginArgs {

        @Parameter(names = {"-a", "--artifact"}, description = "maven artifact id for an artifact in group com.force.clifprce.plugin")
        public String artifact;

        public String group = "com.force.cliforce.plugin";

        @Parameter(names = {"-v", "--version"}, description = "maven artifact version for the specified artifact")
        public String version;

    }

    public static class PluginCommand extends JCommand<PluginArgs> {

        public static final String PLUGIN_GROUP = "com.force.cliforce.plugin";
        private CLIForce force;

        public PluginCommand(CLIForce it) {
            force = it;
        }

        @Override
        public String name() {
            return "plugin";
        }

        @Override
        public String describe() {
            return usage("adds a plugin to the shell");
        }

        @Override
        public PluginArgs getArgs() {
            return new PluginArgs();
        }

        @Override
        public void executeWithArgs(final CommandContext ctx, PluginArgs arg) {
            PrintStream output = ctx.getCommandWriter();
            if (arg.artifact == null) {
                output.println("Listing plugins...");
                for (Map.Entry<String, Plugin> e : force.plugins.entrySet()) {
                    output.printf("Plugin: %s (%s)\n", e.getKey(), e.getValue().getClass().getName());
                }
                output.println("Done.");
            } else {


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
                ClassLoader pcl = null;
                if (arg.version != null) {
                    pcl = DependencyResolver.getInstance().createClassLoaderFor(PLUGIN_GROUP, arg.artifact, arg.version, curr, oa);
                } else {
                    pcl = DependencyResolver.getInstance().createClassLoaderFor(PLUGIN_GROUP, arg.artifact, curr, oa);
                }
                try {
                    Thread.currentThread().setContextClassLoader(pcl);
                    ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, pcl);
                    Iterator<Plugin> iterator = loader.iterator();
                    if (!iterator.hasNext()) {
                        output.printf("Error: %s does not declare a Plugin in META-INF/services/com.force.cliforce.Plugin\n", arg.artifact);
                        return;
                    }
                    Plugin p = iterator.next();
                    List<Command> commands = p.getCommands();
                    force.plugins.put(arg.artifact, p);
                    output.printf("Adding Plugin: %s (%s)\n", arg.artifact, p.getClass().getName());
                    for (Command command : commands) {
                        output.printf("  -> adds command %s (%s)\n", command.name(), command.getClass().getName());
                        force.commands.put(arg.artifact + ":" + command.name(), command);
                    }
                    while (iterator.hasNext()) {
                        Plugin ignore = iterator.next();
                        output.printf("only one plugin per artifact is supported, %s will not be registered\n", ignore.getClass().getName());
                    }
                    force.reloadCompletions();
                } finally {
                    Thread.currentThread().setContextClassLoader(curr);
                }

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
                        force.commands.remove(arg + ":" + command.name());
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

    public static class ShellCommand implements Command {
        @Override
        public String name() {
            return "sh";
        }

        @Override
        public String describe() {
            return "Execute the rest of the command on the OS";
        }

        @Override
        public void execute(CommandContext ctx) throws Exception {
            Process start = new ProcessBuilder(ctx.getCommandArguments()).start();
            Thread t = new Thread(new Reader(start.getInputStream(), ctx.getCommandWriter()));
            t.setDaemon(true);
            t.start();
            start.waitFor();
            t.interrupt();
        }

        private class Reader implements Runnable {
            InputStream in;
            PrintStream out;

            private Reader(InputStream in, PrintStream out) {
                this.in = in;
                this.out = out;
            }

            public void run() {
                InputStreamReader reader = new InputStreamReader(in);
                BufferedReader breader = new BufferedReader(reader);
                String output = null;
                try {
                    while ((output = breader.readLine()) != null) {
                        out.println(output);
                    }
                } catch (IOException e) {
                    out.println("IOException reading process input");
                }
            }
        }

    }


}
