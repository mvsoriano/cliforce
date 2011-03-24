package com.force.cliforce;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.force.cliforce.command.BannerCommand;
import com.force.cliforce.command.DebugCommand;
import com.force.cliforce.dependency.DependencyResolutionException;
import com.force.cliforce.dependency.DependencyResolver;
import com.force.cliforce.dependency.OutputAdapter;
import com.google.common.base.Joiner;
import jline.console.completer.StringsCompleter;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import static com.force.cliforce.Util.*;

/**
 * The default cliforce plugin, provides the sh, banner, history, debug,
 * info, help, plugin, unplug, version and require commands
 */
public class DefaultPlugin implements Plugin {


    @Override
    public List<Class<? extends Command>> getCommands() {
        List<Class<? extends Command>> commands = Arrays.asList(
                ShellCommand.class,
                BannerCommand.class,
                HistoryCommand.class,
                DebugCommand.class,
                HelpCommand.class,
                PluginCommand.class,
                RequirePluginCommand.class,
                UnplugCommand.class,
                VersionCommand.class,
                EnvCommand.class,
                SyspropsCommand.class,
                ClasspathCommand.class,
                LoginCommand.class,
                ExitCommand.class
        );

        return commands;
    }


    public static class ExitCommand implements Command {
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
    }

    public static class LoginArgs {
        @Parameter(names = "--target", description = "The controller endpoint to authenticate against")
        public String target = "api.alpha.vmforce.com";
    }

    public static class LoginCommand extends JCommand<LoginArgs> {

        @Inject
        private CLIForce cliForce;

        @Override
        public void executeWithArgs(CommandContext ctx, LoginArgs args) {
            String go = "Y";
            while (!login(ctx, args)) {
                go = ctx.getCommandReader().readLine("Enter Y to try again, anything else to cancel.");
                if (!go.toUpperCase().startsWith("Y")) {
                    return;
                }
            }
        }

        private boolean login(CommandContext ctx, LoginArgs args) {
            requireCliforce(cliForce);
            ctx.getCommandWriter().println("Please log in");
            String target = ctx.getCommandReader().readLine(String.format("Target login server [%s]:", args.target));
            if ("".equals(target)) target = args.target;
            ctx.getCommandWriter().printf("Login server: %s\n", target);
            String user = ctx.getCommandReader().readLine("Username:");
            String password = ctx.getCommandReader().readLine("Password:", '*');
            if (cliForce.setLogin(user, password, target)) {
                ctx.getCommandWriter().println("Login successful.");
                return true;
            } else {
                ctx.getCommandWriter().println("Unable to log in with provided credentials");
                return false;
            }
        }

        @Override
        public String name() {
            return CLIForce.LOGIN_CMD;
        }

        @Override
        public String describe() {
            return usage("login to a controller endpoint and save the login info for future use");
        }
    }


    public static class HelpCommand implements Command {

        @Inject
        private CLIForce cliForce;

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
            requireCliforce(cliForce);
            Map<String, String> descs = cliForce.getCommandDescriptions();
            descs = new TreeMap<String, String>(descs);
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

        @Inject
        DependencyResolver resolver;

        @Inject
        CLIForce cliForce;

        @Override
        public String name() {
            return CLIForce.PLUGIN_CMD;
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
            requireCliforce(cliForce);
            requireResolver(resolver);
            if (cliForce.getInternalPlugins().contains(arg.artifact()) && !arg.internal) {
                ctx.getCommandWriter().printf("Manually installing internal plugins [%s] is not suported\n", Joiner.on(", ").join(cliForce.getInternalPlugins()));
                return;
            }
            CommandWriter output = ctx.getCommandWriter();
            if (arg.artifact() == null) {
                output.println("Listing plugins...");
                for (Map.Entry<String, String> e : cliForce.getInstalledPlugins().entrySet()) {
                    output.printf("Plugin: %s (%s)\n", e.getKey(), e.getValue());
                }
                output.println("Done.");
            } else {

                if (cliForce.getActivePlugins().contains(arg.artifact())) {
                    output.printf("Plugin %s is already installed. Please execute 'unplug %s' before running this command\n", arg.artifact(), arg.artifact());
                    return;
                }

                ClassLoader curr = Thread.currentThread().getContextClassLoader();
                OutputAdapter oa = new OutputAdapter() {
                    @Override
                    public void println(String msg) {
                        if (cliForce.isDebug()) {
                            ctx.getCommandWriter().println(msg);
                        }
                    }

                    @Override
                    public void println(Exception e, String msg) {
                        if (cliForce.isDebug()) {
                            ctx.getCommandWriter().printf("%s: %s", msg, e.toString());
                        }
                    }
                };
                ClassLoader pcl = null;
                try {
                    if (arg.version != null) {
                        pcl = resolver.createClassLoaderFor(PLUGIN_GROUP, arg.artifact(), arg.version, curr, oa);
                    } else {
                        pcl = resolver.createClassLoaderFor(PLUGIN_GROUP, arg.artifact(), curr, oa);
                    }
                } catch (DependencyResolutionException e) {
                    ctx.getCommandWriter().println("The maven artifact associated with the plugin could not be found.");
                    return;
                }
                try {
                    Thread.currentThread().setContextClassLoader(pcl);
                    ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, pcl);
                    Iterator<Plugin> iterator = loader.iterator();
                    if (!iterator.hasNext()) {
                        output.printf("Error: %s doesn't declare a Plugin in META-INF/services/com.force.cliforce.Plugin\n", arg.artifact());
                        return;
                    }
                    Plugin p = iterator.next();

                    cliForce.installPlugin(arg.artifact(), arg.version, p, arg.internal);

                    while (iterator.hasNext()) {
                        Plugin ignore = iterator.next();
                        output.printf("only one plugin per artifact is supported, %s will not be registered\n", ignore.getClass().getName());
                    }
                    loader.reload();
                } catch (Exception e) {
                    output.println("Unable to load plugin");
                    output.printStackTrace(e);
                } finally {
                    Thread.currentThread().setContextClassLoader(curr);
                }

            }
        }
    }


    public static class RequirePluginCommand extends JCommand<PluginArgs> {

        @Inject
        private CLIForce cliForce;

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
            requireCliforce(cliForce);
            String version = cliForce.getInstalledPluginVersion(arg.artifact());
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

        @Inject
        private CLIForce cliForce;

        @Override
        public String name() {
            return "unplug";
        }

        @Override
        public String describe() {
            return "removes a plugin and its commands from the shell";
        }

        @Override
        public void execute(CommandContext ctx) throws Exception {
            requireCliforce(cliForce);

            for (String arg : ctx.getCommandArguments()) {
                if (cliForce.getInternalPlugins().contains(arg)) {
                    ctx.getCommandWriter().printf("Removing internal plugins [%s] is not suported\n", Joiner.on(", ").join(cliForce.getInternalPlugins()));
                } else {
                    ctx.getCommandWriter().printf("Removing plugin: %s\n", arg);
                    cliForce.removePlugin(arg);
                }
            }
        }
    }


    public static class HistoryCommand implements Command {

        @Inject
        private CLIForce cliForce;

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
            requireCliforce(cliForce);
            List<String> historyList = cliForce.getHistoryList();
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
            if (ctx.getCommandArguments().length == 0) {
                ctx.getCommandWriter().println("The sh command expects a command which you would like to execute");
                return;
            }

            try {
                new ShellExecutor().execute(ctx.getCommandArguments(), ctx.getCommandWriter());
            } catch (IOException e) {
                ctx.getCommandWriter().println("The command failed to execute. Please check the path to the executable you provided");
                log.get().debug("IOEXception", e);
            }

        }


    }

    public static class ShellExecutor {

        public void execute(String[] args, CommandWriter writer) throws IOException {


            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                String[] nargs = new String[args.length + 2];
                nargs[0] = "cmd";
                nargs[1] = "/c";
                System.arraycopy(args, 0, nargs, 2, args.length);
                args = nargs;
            }

            writer.printf("sh: Executing: %s\n", Joiner.on(" ").join(args));
            Process start = new ProcessBuilder(args).start();
            Thread t = new Thread(new Reader(start.getInputStream(), writer, "  "));
            Thread err = new Thread(new Reader(start.getErrorStream(), writer, "  "));
            t.setDaemon(true);
            t.start();
            err.setDaemon(true);
            err.start();
            try {
                start.waitFor();
            } catch (InterruptedException e) {

            }
            t.interrupt();
            err.interrupt();
            try {
                t.join();
            } catch (InterruptedException e) {

            }
            try {
                err.join();
            } catch (InterruptedException e) {

            }
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
            return "Display the current Java system properties";
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

        @Parameter(names = {"-s", "--sort"}, description = "sort the returned list of files on the classpath alphabetically")
        public Boolean sort = false;


        public String plugin() {
            return plugins.size() > 0 ? plugins.get(0) : null;
        }
    }

    public static class ClasspathCommand extends JCommand<ClasspathArg> {

        @Inject
        private CLIForce cliForce;

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
            requireCliforce(cliForce);
            Collection<URL> classpathForCommand;
            List<URL> classpathForPlugin = cliForce.getClasspathForPlugin(args.plugin());
            if (classpathForPlugin == null) {
                ctx.getCommandWriter().printf("No such plugin: %s\n", args.plugin());
                return;
            }
            if (args.sort) {
                classpathForCommand = new TreeSet<URL>(new Comparator<URL>() {
                    @Override
                    public int compare(URL o1, URL o2) {
                        return o1.toString().compareTo(o2.toString());
                    }
                });
                classpathForCommand.addAll(classpathForPlugin);
            } else {
                classpathForCommand = classpathForPlugin;
            }

            for (URL url : classpathForCommand) {
                ctx.getCommandWriter().println(url.toString());
            }
        }

        @Override
        protected List<CharSequence> getCompletionsForSwitch(String switchForCompletion, String partialValue, ParameterDescription parameterDescription, CommandContext context) {
            if (switchForCompletion.equals(MAIN_PARAM)) {
                List<CharSequence> candidates = new ArrayList<CharSequence>();
                List<String> activePlugins = cliForce.getActivePlugins();
                new StringsCompleter(activePlugins.toArray(new String[0])).complete(partialValue, partialValue.length(), candidates);
                if (candidates.size() > 1) {
                    candidates.add("<or none for the cliforce classpath>");
                }
                return candidates;
            } else {
                return super.getCompletionsForSwitch(switchForCompletion, partialValue, parameterDescription, context);
            }
        }
    }


}
