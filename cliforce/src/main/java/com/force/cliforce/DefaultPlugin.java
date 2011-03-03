package com.force.cliforce;

import com.beust.jcommander.Parameter;
import com.force.cliforce.command.BannerCommand;
import com.force.cliforce.command.DebugCommand;
import com.force.cliforce.dependency.DependencyResolver;
import com.force.cliforce.dependency.OutputAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import static java.lang.String.format;

/**
 * The default cliforce plugin, provides the sh, banner, history, debug,
 * info, help, plugin, unplug, version and require commands
 */
public class DefaultPlugin implements Plugin {


    @Override
    public List<Command> getCommands() {
        return Arrays.asList(
                new ShellCommand(),
                new BannerCommand(),
                new HistoryCommand(),
                new DebugCommand(),
                new HelpCommand(),
                new PluginCommand(),
                new RequirePluginCommand(),
                new UnplugCommand(),
                new VersionCommand(),
                new EnvCommand(),
                new SyspropsCommand(),
                new ClasspathCommand(),
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


        @Override
        public String name() {
            return "help";
        }

        @Override
        public String describe() {
            return "Display this help message, or help for a specific command\n\tUsage: help <command>";
        }

        @Override
        public void execute(CommandContext ctx) throws Exception {
            Map<String, String> descs = CLIForce.getInstance().getCommandDescriptions();
            if (ctx.getCommandArguments().length == 0) {
                int longCmd = 0;
                for (String key : descs.keySet()) {
                    longCmd = Math.max(longCmd, key.length());
                }
                int offset = longCmd + 4;

                for (Map.Entry<String, String> entry : descs.entrySet()) {
                    String pad = pad(offset - entry.getKey().length());
                    ctx.getCommandWriter().printf("%s:" + pad + "%s\n", entry.getKey(), entry.getValue());
                }
            } else {
                String key = ctx.getCommandArguments()[0];
                String desc = descs.get(key);
                if (desc == null) {
                    ctx.getCommandWriter().printf("No such command: %s\n", key);
                } else {
                    ctx.getCommandWriter().printf("%s:\t\t\t %s\n", key, desc);
                }
            }
        }

        private String pad(int pad) {
            StringBuilder padb = new StringBuilder();
            for (int i = 0; i < pad; i++) padb.append(" ");
            return padb.toString();
        }
    }


    public static class PluginArgs {

        @Parameter(description = "maven artifact id for an artifact in group com.force.cliforce.plugin")
        public List<String> artifacts = new ArrayList<String>();

        public void setArtifact(String artifact) {
            artifacts.add(0, artifact);
        }

        public String artifact() {
            return artifacts.size() > 0 ? artifacts.get(0) : null;
        }

        public String group = "com.force.cliforce.plugin";

        @Parameter(names = {"-v", "--version"}, description = "maven artifact version for the specified artifact, if unspecified RELEASE meta-version is used")
        public String version = "RELEASE";

        //internal plugins are reinstalled every restart, so we dont save their installation or output that they are being installed
        public boolean internal = false;

    }

    public static class PluginCommand extends JCommand<PluginArgs> {

        public static final String PLUGIN_GROUP = "com.force.cliforce.plugin";


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
            CommandWriter output = ctx.getCommandWriter();
            if (arg.artifact() == null) {
                output.println("Listing plugins...");
                for (Map.Entry<String, String> e : CLIForce.getInstance().getInstalledPlugins().entrySet()) {
                    output.printf("Plugin: %s (%s)\n", e.getKey(), e.getValue());
                }
                output.println("Done.");
            } else {

                if (CLIForce.getInstance().getInstalledPluginVersion(arg.artifact()) != null) {
                    output.printf("Plugin %s is already installed, please execute 'unplug %s' before running this command", arg.artifact(), arg.artifact());
                    return;
                }

                ClassLoader curr = Thread.currentThread().getContextClassLoader();
                OutputAdapter oa = new OutputAdapter() {
                    @Override
                    public void println(String msg) {
                        ctx.getCommandWriter().println(msg);
                    }

                    @Override
                    public void println(Exception e, String msg) {
                        ctx.getCommandWriter().printf("%s: %s", msg, e.toString());
                    }
                };
                ClassLoader pcl = null;
                if (arg.version != null) {
                    pcl = DependencyResolver.getInstance().createClassLoaderFor(PLUGIN_GROUP, arg.artifact(), arg.version, curr, oa);
                } else {
                    pcl = DependencyResolver.getInstance().createClassLoaderFor(PLUGIN_GROUP, arg.artifact(), curr, oa);
                }
                try {
                    Thread.currentThread().setContextClassLoader(pcl);
                    ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, pcl);
                    Iterator<Plugin> iterator = loader.iterator();
                    if (!iterator.hasNext()) {
                        output.printf("Error: %s does not declare a Plugin in META-INF/services/com.force.cliforce.Plugin\n", arg.artifact());
                        return;
                    }
                    Plugin p = iterator.next();

                    CLIForce.getInstance().installPlugin(arg.artifact(), arg.version, p, arg.internal);

                    while (iterator.hasNext()) {
                        Plugin ignore = iterator.next();
                        output.printf("only one plugin per artifact is supported, %s will not be registered\n", ignore.getClass().getName());
                    }
                    loader.reload();
                } finally {
                    Thread.currentThread().setContextClassLoader(curr);
                }

            }
        }
    }


    public static class RequirePluginCommand extends JCommand<PluginArgs> {


        @Override
        public String name() {
            return "require";
        }

        @Override
        public String describe() {
            return usage("exit the shell if a specified version of a plugin is not installed");
        }

        @Override
        public PluginArgs getArgs() {
            return new PluginArgs();
        }

        @Override
        public void executeWithArgs(final CommandContext ctx, PluginArgs arg) {
            CommandWriter output = ctx.getCommandWriter();
            String version = CLIForce.getInstance().getInstalledPluginVersion(arg.artifact());
            if (version == null) {
                ctx.getCommandWriter().printf("Required Plugin %s version %s is not installed, exiting\n", arg.artifact(), arg.version);
                throw new ExitException("Required Plugin Not Installed");
            }
            if (!version.equals(arg.version)) {
                //this will be funny about RELEASE for now...best practice use and require explicit versions
                ctx.getCommandWriter().printf("incorrect version %s of Required Plugin %s version %s is not installed, exiting\n", version, arg.artifact(), arg.version);
                throw new ExitException("Required Plugin Not Installed");
            }

        }
    }


    public static class UnplugCommand implements Command {


        @Override
        public String name() {
            return "unplug";
        }

        @Override
        public String describe() {
            return "removes a plugin and it's commands from the shell";
        }

        @Override
        public void execute(CommandContext ctx) throws Exception {
            for (String arg : ctx.getCommandArguments()) {
                ctx.getCommandWriter().printf("Removing plugin: %s\n", arg);
                CLIForce.getInstance().removePlugin(arg);
            }
        }
    }


    public static class HistoryCommand implements Command {


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
            List<String> historyList = CLIForce.getInstance().getHistoryList();
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
            StringBuilder b = new StringBuilder();
            for (String s : ctx.getCommandArguments()) {
                b.append(s).append(" ");
            }

            String[] args;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                args = new String[ctx.getCommandArguments().length + 2];
                args[0] = "cmd";
                args[1] = "/c";
                System.arraycopy(ctx.getCommandArguments(), 0, args, 2, ctx.getCommandArguments().length);
            } else {
                args = ctx.getCommandArguments();
            }
            if (CLIForce.getInstance().isDebug()) {
                ctx.getCommandWriter().printf("sh: Executing: %s\n", Arrays.toString(args));
            }
            Process start = new ProcessBuilder(args).start();
            Thread t = new Thread(new Reader(start.getInputStream(), ctx.getCommandWriter(), format("sh->%s:stdout#", ctx.getCommandArguments()[0])));
            Thread err = new Thread(new Reader(start.getErrorStream(), ctx.getCommandWriter(), format("sh->%s:stderr#", ctx.getCommandArguments()[0])));
            t.setDaemon(true);
            t.start();
            err.setDaemon(true);
            err.start();
            start.waitFor();
            t.interrupt();
            err.interrupt();
            t.join();
            err.join();
        }

        private class Reader implements Runnable {
            InputStream in;
            CommandWriter out;
            private String lineheader;

            private Reader(InputStream in, CommandWriter out, String lineheader) {
                this.in = in;
                this.out = out;
                this.lineheader = lineheader;
            }

            public void run() {
                InputStreamReader reader = new InputStreamReader(in);
                BufferedReader breader = new BufferedReader(reader);
                String output = null;
                try {
                    while ((output = breader.readLine()) != null) {
                        synchronized (out) {
                            out.println(lineheader + output);
                        }
                    }
                } catch (IOException e) {
                    out.println("IOException reading process input");
                }
            }
        }

    }

    public static class VersionCommand implements Command {
        @Override
        public String name() {
            return "version";

        }

        @Override
        public String describe() {
            return "Show the current running version of cliforce";
        }

        @Override
        public void execute(CommandContext ctx) throws Exception {
            Properties cliforceProperties = new Properties();
            cliforceProperties.load(getClass().getClassLoader().getResourceAsStream("cliforce.properties"));
            ctx.getCommandWriter().printf("groupId:%s\n", cliforceProperties.getProperty("groupId"));
            ctx.getCommandWriter().printf("artifactId:%s\n", cliforceProperties.getProperty("artifactId"));
            ctx.getCommandWriter().printf("version:%s\n", cliforceProperties.getProperty("version"));
            ctx.getCommandWriter().printf("builtAt:%s\n", cliforceProperties.getProperty("builtAt"));
        }
    }

    public static class EnvCommand implements Command {
        @Override
        public String name() {
            return "env";
        }

        @Override
        public String describe() {
            return "Display the current environment variables";
        }

        @Override
        public void execute(CommandContext ctx) throws Exception {
            for (Map.Entry<String, String> env : System.getenv().entrySet()) {
                ctx.getCommandWriter().printf("%s = %s\n", env.getKey(), env.getValue());
            }
        }
    }

    public static class SyspropsCommand implements Command {
        @Override
        public String name() {
            return "sysprops";
        }

        @Override
        public String describe() {
            return "Display the current java system properties";
        }

        @Override
        public void execute(CommandContext ctx) throws Exception {
            for (String prop : System.getProperties().stringPropertyNames()) {
                ctx.getCommandWriter().printf("%s = %s\n", prop, System.getProperty(prop));
            }
        }
    }


    public static class ClasspathArg {
        @Parameter(description = "name of the plugin to get the classpath for, or none for the cliforce classpath")
        public List<String> plugins = new ArrayList<String>();


        public String plugin() {
            return plugins.size() > 0 ? plugins.get(0) : null;
        }
    }

    public static class ClasspathCommand extends JCommand<ClasspathArg> {

        @Override
        public String name() {
            return "classpath";
        }

        @Override
        public String describe() {
            return usage("show the classpath for a cliforce plugin, or for cliforce itself. \n " +
                    "Note that the classloader of cliforce is the parent classloader of plugin classloaders");
        }

        @Override
        public void executeWithArgs(CommandContext ctx, ClasspathArg args) {
            List<URL> classpathForCommand = CLIForce.getInstance().getClasspathForPlugin(args.plugin());
            for (URL url : classpathForCommand) {
                ctx.getCommandWriter().println(url.toString());
            }
        }
    }


}
