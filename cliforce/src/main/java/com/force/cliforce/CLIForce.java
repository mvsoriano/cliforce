package com.force.cliforce;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.util.StatusPrinter;
import com.force.sdk.connector.ForceServiceConnector;
import com.google.inject.Guice;
import com.google.inject.name.Named;
import com.sforce.async.AsyncApiException;
import com.sforce.async.RestConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.vmforce.client.VMForceClient;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;
import jline.console.history.History;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.inject.Inject;
import javax.servlet.ServletException;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

public class CLIForce {

    public static final String FORCE_PROMPT = "force> ";
    public static final String INTERNAL_PLUGINS = "internalPlugins";
    public static final String STARTUP_EXECUTOR = "startupExecutor";
    public static final String EXIT_CMD = "exit";
    public static final String BANNER_CMD = "banner";
    public static final String LOGIN_CMD = "login";
    public static final String PLUGIN_CMD = "plugin";
    public static LazyLogger log = new LazyLogger(CLIForce.class);

    private volatile boolean debug = false;
    private volatile boolean loginSucceded = false;

    private ConsoleReader reader;
    private CommandReader commandReader;
    private CommandWriter writer;

    @Inject
    private Completer completor;
    @Inject
    private ConnectionManager connectionManager;
    @Inject
    private PluginManager pluginManager;
    @Inject
    @Named(INTERNAL_PLUGINS)
    private String[] internalPlugins;
    @Inject
    private DefaultPlugin def;
    @Inject
    @Named(STARTUP_EXECUTOR)
    private ExecutorService startupExecutor;

    private List<SetupTask> setupTasks = new ArrayList<SetupTask>(16);
    /*
    used to coordinate background loading of plugins at startup
    we start up and get to the prompt fast, and wait on this latch to actually exec the first command.
    we dont construct it here since we want the latch size to match the number of setupTasks.
    */
    private CountDownLatch initLatch;
    private CountDownLatch loginLatch = new CountDownLatch(1);


    public static void main(String[] args) {
        setupLogback();
        CLIForce cliForce = Guice.createInjector(new MainModule()).getInstance(CLIForce.class);

        try {
            cliForce.init(System.in, new PrintWriter(
                    new OutputStreamWriter(System.out,
                            System.getProperty("jline.WindowsTerminal.output.encoding", System.getProperty("file.encoding"))), true));
            if (args.length == 0) {
                cliForce.run();
            } else {
                cliForce.executeWithArgs(args);
            }
        } catch (ConnectionException e) {
            log.get().error("Connection Exception while initializing cliforce, exiting", e);
            System.exit(1);
        } catch (IOException e) {
            log.get().error("IOException Exception while initializing cliforce, exiting", e);
            System.exit(1);
        } catch (ServletException e) {
            log.get().error("ServletException Exception while initializing cliforce, exiting", e);
            System.exit(1);
        } catch (InterruptedException e) {
            log.get().error("Main Thread Interrupted while waiting for plugin initialization", e);
            System.exit(1);
        } catch (ExitException e) {
            log.get().error("ExitException->Exiting");
            System.exit(1);
        }
    }

    private static void setupLogback() {
        System.setProperty("logback.configurationFile", System.getProperty("logback.configurationFile", "logback.xml"));
        StatusPrinter.setPrintStream(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                //logback 0.9.28 barfs some stuff at startup. This supresses it.
            }
        }));
    }


    public void init(InputStream in, PrintWriter out) throws IOException, ConnectionException, ServletException {
        SLF4JBridgeHandler.install();
        injectDefaultPluginAndAddCommands();

        reader = new ConsoleReader(in, out);
        reader.addCompleter(completor);
        writer = new Writer(out);

        setupHistory(reader, out);

        reader.setBellEnabled(false);
        commandReader = new Reader();


        addSetupTask(new SetupTask() {
            @Override
            public void setup() {
                try {
                    connectionManager.loadLogin();
                    connectionManager.doLogin();
                    loginSucceded = true;
                } catch (Exception e) {
                    log.get().debug("Exception caught while logging in", e);
                } finally {
                    loginLatch.countDown();
                }
            }
        });


        addSetupTask(new SetupTask() {
            @Override
            public void setup() {
                try {
                    DefaultPlugin.PluginCommand p = (DefaultPlugin.PluginCommand) pluginManager.getCommand(PLUGIN_CMD);
                    for (String defalutPlugin : internalPlugins) {
                        DefaultPlugin.PluginArgs args = new DefaultPlugin.PluginArgs();
                        args.setArtifact(defalutPlugin);
                        args.version = "LATEST";
                        args.internal = true;
                        p.executeWithArgs(getContext(new String[0]), args);
                    }
                    loadInstalledPlugins();
                } catch (IOException e) {
                    log.get().error("IOException while loading previously installed plugins", e);
                }
            }
        });

        addSetupTask(new SetupTask() {
            @Override
            public void setup() {
                try {
                    connectionManager.loadUserConnections();
                } catch (IOException e) {
                    log.get().error("IOException while loading force urls", e);
                }
            }
        });
        executeSetupTasks();

    }

    private void setupHistory(ConsoleReader r, PrintWriter o) throws IOException {
        File hist = new File(System.getProperty("user.home") + "/.force/cliforce_history");
        if (!hist.getParentFile().exists()) {
            if (!hist.getParentFile().mkdir()) {
                o.println("can't create .force directory");
            }
        }
        if (!hist.exists()) {
            try {
                if (hist.createNewFile()) {
                    reader.setHistory(new FileHistory(hist));
                } else {
                    o.println("can't create history file");
                }
            } catch (IOException e) {
                o.println("can't create history file");
            }

        } else {
            r.setHistory(new FileHistory(hist));
        }
    }

    private void addSetupTask(SetupTask task) {
        setupTasks.add(task);
    }

    private void executeSetupTasks() {
        initLatch = new CountDownLatch(setupTasks.size());
        for (SetupTask setupTask : setupTasks) {
            startupExecutor.submit(setupTask);
        }
    }

    /**
     * Main run loop.
     *
     * @throws InterruptedException
     */
    public void run() throws InterruptedException {
        try {
            pluginManager.getCommand(BANNER_CMD).execute(getContext(new String[0]));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        loginLatch.await();


        String[] cmds = commandReader.readAndParseLine(FORCE_PROMPT);
        String cmdKey = cmds[0];
        while (!cmdKey.equals(EXIT_CMD)) {
            initLatch.await();
            executeWithArgs(cmds);
            cmds = commandReader.readAndParseLine(FORCE_PROMPT);
            cmdKey = cmds[0];
        }
    }

    private void doLogin() {
        try {
            pluginManager.getCommand(LOGIN_CMD).execute(new Context(null, null, null, new String[0], commandReader, writer));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void executeWithArgs(String[] cmds) throws InterruptedException {
        String cmdKey = cmds[0];
        if (!cmdKey.equals(EXIT_CMD)) {
            loginLatch.await();
            //we dont wait on the latch if somone runs cliforce exit.
            //this is useful to measure "startup time to get to the prompt"
            //by running> time cliforce exit
            initLatch.await();
        }
        Command cmd = pluginManager.getCommand(cmdKey);
        String[] args = cmds.length > 1 ? Arrays.copyOfRange(cmds, 1, cmds.length) : new String[0];
        if (!cmdKey.equals("") && !cmdKey.equals(EXIT_CMD)) {
            if (cmd != null) {
                executeCommand(cmdKey, cmd, args);
            } else {
                writer.printf("Unknown Command %s\n", cmdKey);
            }
        }
    }

    private void executeCommand(String cmdKey, Command cmd, String[] args) {
        ClassLoader curr = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cmd.getClass().getClassLoader());
            cmd.execute(getContext(args));
        } catch (ExitException e) {
            writer.println("Exit Exception thrown, exiting");
            throw e;
        } catch (ResourceException e) {
            writer.println(e.getMessage());
            if(!isDebug()){
                writer.println("execute debug --on and retry to see failure information");
            }
        } catch (Exception e) {
            writer.printf("Exception while executing command %s\n", cmdKey);
            writer.printStackTrace(e);
        } finally {
            Thread.currentThread().setContextClassLoader(curr);
        }
    }


    /**
     * Return a map of installed plugins' maven artifactId->version
     *
     * @return
     */
    public Map<String, String> getInstalledPlugins() {
        return pluginManager.getInstalledPlugins();
    }

    public List<String> getActivePlugins() {
        return pluginManager.getActivePlugins();
    }

    /**
     * return the currently installed version of a plugin or null if not installed.
     *
     * @param plugin
     * @return
     */
    public String getInstalledPluginVersion(String plugin) {
        return pluginManager.getInstalledPluginVersion(plugin);
    }

    /*package*/ void installPlugin(String artifact, String version, Plugin p, boolean internal) throws IOException {
        pluginManager.installPlugin(artifact, version, p, internal);
        if (!internal && initLatch.getCount() == 0) {
            List<Command> pluginCommands = pluginManager.getPluginCommands(artifact);
            writer.printf("Plugin: %s installed\n", artifact);
            writer.println("Adds the following commands");
            for (Command pluginCommand : pluginCommands) {
                writer.println(pluginCommand.name());
            }

        }
    }

    private void injectDefaultPluginAndAddCommands() {
        pluginManager.injectDefaultPluginAndAddCommands(def);
    }


    /*package*/ void removePlugin(String artifactId) throws IOException {
        List<Command> pluginCommands = pluginManager.getPluginCommands(artifactId);
        if (pluginCommands.size() == 0) {
            writer.println("....not found");
        } else {
            for (Command command : pluginCommands) {
                writer.printf("\tremoved command: %s\n", command.name());
            }
            pluginManager.removePlugin(artifactId);
            writer.println("Done");
        }
    }

    public List<URL> getClasspathForPlugin(String plugin) {
        return pluginManager.getClasspathForPlugin(plugin);
    }

    public Map<String, String> getCommandDescriptions() {
        return pluginManager.getCommandDescriptions();
    }

    public List<String> getHistoryList() {
        List<String> strings = new ArrayList<String>();
        ListIterator<History.Entry> entries = reader.getHistory().entries();
        while (entries.hasNext()) {
            History.Entry next = entries.next();
            strings.add(next.value().toString());
        }
        return strings;
    }

    private void loadInstalledPlugins() throws IOException {
        pluginManager.loadInstalledPlugins();
        DefaultPlugin.PluginCommand p = (DefaultPlugin.PluginCommand) pluginManager.getCommand("plugin");
        for (String artifact : pluginManager.getInstalledPlugins().keySet()) {
            String version = pluginManager.getInstalledPluginVersion(artifact);
            DefaultPlugin.PluginArgs args = new DefaultPlugin.PluginArgs();
            args.setArtifact(artifact);
            args.version = version;
            p.executeWithArgs(getContext(new String[0]), args);
        }
    }


    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        connectionManager.setDebugOnConnections(debug);
        Level level = Level.DEBUG;
        if (!debug) {
            level = Level.OFF;
        }
        writer.printf("Setting logger level to %s\n", level.levelStr);
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(level);
    }


    public synchronized boolean setLogin(String user, String password, String target) {
        try {
            connectionManager.setLogin(user, password, target);
            connectionManager.doLogin();
            loginSucceded = true;
        } catch (Exception e) {
            log.get().debug("Unable to log in", e);
            loginSucceded = false;
            return false;
        }

        try {
            connectionManager.saveLogin();
        } catch (IOException e) {
            log.get().error("Exception persisting new login settings, the login will not persist over restarts.", e);
        }
        return true;
    }


    public Map<String, ForceEnv> getAvailableEnvironments() {
        return connectionManager.getAvailableEnvironments();
    }

    public void setAvailableEnvironment(String name, ForceEnv env) {
        connectionManager.setAvailableEnvironment(name, env);
    }

    public void setDefaultEnvironment(String name) {
        connectionManager.setDefaultEnvironment(name);
    }

    public void renameEnvironment(String name, String newname) {
        connectionManager.renameEnvironment(name, newname);
    }

    public void setCurrentEnvironment(String name) {
        connectionManager.setCurrentEnvironment(name);
    }

    public String getCurrentEnvironment() {
        return connectionManager.getCurrentEnvironment();
    }

    public String getDefaultEnvironment() {
        return connectionManager.getDefaultEnvironment();
    }

    public void removeEnvironment(String name) {
        connectionManager.removeEnvironment(name);
    }


    CommandContext getContext(String[] args) {
        ForceEnv currentEnv = connectionManager.getCurrentEnv();
        VMForceClient vmForceClient = connectionManager.getVmForceClient();
        ForceServiceConnector connector = connectionManager.getCurrentConnector();

        if (connector != null) {
            return new Context(currentEnv, connector, vmForceClient, args, commandReader, writer);
        } else {
            if (initLatch.getCount() == 0) {
                log.get().warn("Couldn't get a valid connection for the current force url. Executing the command without force service connector or VMforce client");
            }
            return new Context(currentEnv, null, null, args, commandReader, writer);
        }
    }


    /**
     * cliforce internal impl of CommandContext
     */
    static class Context implements CommandContext {

        ForceServiceConnector connector;
        String[] args;
        CommandReader reader;
        VMForceClient client;
        CommandWriter writer;
        ForceEnv forceEnv;


        private Context(ForceEnv env, ForceServiceConnector conn, VMForceClient cl, String[] args, CommandReader reader, CommandWriter writer) {
            this.client = cl;
            this.args = args;
            this.reader = reader;
            this.writer = writer;
            this.forceEnv = env;
            this.connector = conn;
        }

        @Override
        public ForceEnv getForceEnv() {
            return forceEnv;
        }

        @Override
        public VMForceClient getVmForceClient() {
            return client;
        }

        @Override
        public MetadataConnection getMetadataConnection() {
            try {
                return connector.getMetadataConnection();
            } catch (ConnectionException e) {
                log.get().error("Connection exception while getting metadata connection", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public PartnerConnection getPartnerConnection() {
            try {
                return connector.getConnection();
            } catch (ConnectionException e) {
                log.get().error("ConnectionException while getting metadata connection", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public RestConnection getRestConnection() {
            try {
                return connector.getRestConnection();
            } catch (AsyncApiException e) {
                log.get().error("AsyncApiException exception while getting rest connection", e);
                throw new RuntimeException(e);
            } catch (ConnectionException e) {
                log.get().error("ConnectionException exception while getting rest connection", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public String[] getCommandArguments() {
            return args;
        }

        @Override
        public CommandReader getCommandReader() {
            return reader;
        }

        @Override
        public CommandWriter getCommandWriter() {
            return writer;
        }
    }

    /**
     * cliforce internal impl of CommandWriter. Usually wraps system.out.
     */
    public class Writer implements CommandWriter {

        private PrintWriter out;

        public Writer(PrintWriter out) {
            this.out = out;
        }

        @Override
        public void printf(String format, Object... args) {
            out.printf(format, args);
        }

        @Override
        public void print(String msg) {
            out.print(msg);
            out.flush();
        }

        @Override
        public void println(String msg) {
            out.println(msg);
        }

        @Override
        public void printStackTrace(Exception e) {
            e.printStackTrace(out);
        }
    }

    /**
     * cliforce internal impl of CommandReader, uses apache commons-cli to parse command lines
     */
    private class Reader implements CommandReader {
        @Override
        public String readLine(final String prompt) {
            try {
                return reader.readLine(prompt);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String[] readAndParseLine(String prompt) {
            try {
                String line = reader.readLine(prompt);
                if (line == null) line = EXIT_CMD;
                return Util.parseCommand(line);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String readLine(String prompt, Character mask) {
            try {
                return reader.readLine(prompt, mask);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * base class for tasks that should be run asynchronously on startup.
     * <p/>
     * need to instantiate these and add them with addSetupTask(task) in init(), sometime before
     * executeSetupTasks is called.
     */
    private abstract class SetupTask implements Runnable {

        public abstract void setup();

        @Override
        public void run() {
            try {
                setup();
            } finally {
                initLatch.countDown();
            }
        }
    }

}
