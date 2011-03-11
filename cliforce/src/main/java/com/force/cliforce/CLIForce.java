package com.force.cliforce;

import ch.qos.logback.classic.Level;
import com.force.sdk.connector.ForceConnectorConfig;
import com.force.sdk.connector.ForceServiceConnector;
import com.google.inject.Guice;
import com.google.inject.name.Named;
import com.sforce.async.AsyncApiException;
import com.sforce.async.RestConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.vmforce.client.VMForceClient;
import com.vmforce.client.connector.RestTemplateConnector;
import jline.Completor;
import jline.ConsoleReader;
import jline.History;
import jline.SimpleCompletor;
import org.apache.commons.httpclient.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.inject.Inject;
import javax.servlet.ServletException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class CLIForce {


    /**
     * This field is lazy initialized by getLogger, shouldnt use directly.
     */
    private static Logger logger;
    public static final String FORCEPROMPT = "force> ";
    public static final String EXITCMD = "exit";
    public static final String DEFAULT_URL_PROP_NAME = "__default__";
    public static final String TARGET = "target";
    public static final String PASSWORD = "password";
    public static final String USER = "user";
    public static final String INTERNAL_PLUGINS = "internalPlugins";
    private ConcurrentMap<ForceEnv, EnvConnections> connections = new ConcurrentHashMap<ForceEnv, EnvConnections>();
    private ConcurrentMap<String, ForceEnv> envs = new ConcurrentHashMap<String, ForceEnv>();
    /*key=envName,value=forceUrl*/
    private Properties envProperties = new Properties();
    private Properties loginProperties = new Properties();
    private volatile VMForceClient vmForceClient;
    private volatile RestTemplateConnector restTemplateConnector;
    private volatile ForceEnv currentEnv;
    private volatile String currentEnvName;
    private volatile boolean debug = false;
    private ConsoleReader reader;
    private CommandReader commandReader;
    private Completor completor = new CliforceCompletor();
    private CommandWriter writer;
    @Inject
    private PluginManager pluginManager;
    @Inject
    @Named(INTERNAL_PLUGINS)
    private String[] internalPlugins;
    @Inject
    private DefaultPlugin def;


    private ExecutorService startupExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    });
    private List<SetupTask> setupTasks = new ArrayList<SetupTask>(16);
    /*
    used to coordinate background loading of plugins at startup
    we start up and get to the prompt fast, and wait on this latch to actually exec the first command.
    we dont construct it here since we want the latch size to match the number of setupTasks.
    */
    private CountDownLatch initLatch;


    public static void main(String[] args) {

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
            getLogger().error("Connection Exception while initializing cliforce, exiting", e);
            System.exit(1);
        } catch (IOException e) {
            getLogger().error("IOException Exception while initializing cliforce, exiting", e);
            System.exit(1);
        } catch (ServletException e) {
            getLogger().error("ServletException Exception while initializing cliforce, exiting", e);
            System.exit(1);
        } catch (InterruptedException e) {
            getLogger().error("Main Thread Interrupted while waiting for plugin initialization", e);
            System.exit(1);
        } catch (ExitException e) {
            getLogger().error("ExitException->Exiting");
            System.exit(1);
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

    public Map<String, String> getCommandDescriptions() {
        return pluginManager.getCommandDescriptions();
    }

    public List<String> getHistoryList() {
        return (List<String>) reader.getHistory().getHistoryList();
    }

    /**
     * Lazy init the logger so we dont pay init costs at startup time
     *
     * @return
     */
    private static Logger getLogger() {
        if (logger == null) {
            logger = LoggerFactory.getLogger(CLIForce.class);
        }
        return logger;
    }

    public boolean isDebug() {
        return debug;
    }

    public synchronized boolean setLogin(String user, String password, String target) {
        try {
            resetVMForceClient(user, password, target);
        } catch (Exception e) {
            getLogger().debug("Unable to log in", e);
            return false;
        }

        loginProperties.setProperty(USER, user);
        loginProperties.setProperty(PASSWORD, password);
        loginProperties.setProperty(TARGET, target);

        try {
            Util.writeProperties("login", loginProperties);
            return true;
        } catch (IOException e) {
            getLogger().error("Exception persisting new login settings", e);
            return false;
        }
    }

    private void resetVMForceClient(String user, String password, String target) {
        VMForceClient forceClient = new VMForceClient();
        RestTemplateConnector restConnector = new RestTemplateConnector();
        restConnector.setTarget(new HttpHost(target));
        restConnector.debug(false);
        forceClient.setHttpConnector(restConnector);
        try {
            forceClient.login(user, password);
        } catch (IOException e) {
            throw new RuntimeException("Failed to login", e);
        } catch (ServletException e) {
            throw new RuntimeException("Failed to login", e);
        }
        restTemplateConnector = restConnector;
        vmForceClient = forceClient;
    }

    private boolean loadLogin() {
        try {
            if (!Util.readProperties("login", loginProperties)) {
                return false;
            } else {
                if (!(loginProperties.containsKey(USER) && loginProperties.containsKey(PASSWORD) && loginProperties.containsKey(TARGET))) {
                    return false;
                }
            }
            resetVMForceClient(loginProperties.getProperty(USER), loginProperties.getProperty(PASSWORD), loginProperties.getProperty(TARGET));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public Map<String, ForceEnv> getAvailableEnvironments() {
        return Collections.unmodifiableMap(envs);
    }

    public void setAvailableEnvironment(String name, ForceEnv env) {
        if (env.isValid()) {
            envProperties.setProperty(name, env.getUrl());
            ForceEnv old = envs.put(name, env);
            if (old != null) {
                connections.remove(old);
            }
            writeForceUrls();
        }
    }

    public void setDefaultEnvironment(String name) {
        if (envProperties.containsKey(name)) {
            envProperties.setProperty(DEFAULT_URL_PROP_NAME, name);
            writeForceUrls();
        }
    }

    public void renameEnvironment(String name, String newname) {
        if (envProperties.containsKey(name)) {
            envProperties.setProperty(newname, envProperties.getProperty(name));
            envProperties.remove(name);
            if (envProperties.getProperty(DEFAULT_URL_PROP_NAME, "").equals(name)) {
                envProperties.setProperty(DEFAULT_URL_PROP_NAME, newname);
            }
            envs.put(newname, envs.remove(name));
            if (currentEnvName != null && currentEnvName.equals(name)) {
                currentEnvName = newname;
            }
            writeForceUrls();
        }
    }

    public void setCurrentEnvironment(String name) {
        if (envProperties.containsKey(name)) {
            currentEnv = envs.get(name);
            currentEnvName = name;
        }
    }

    public String getCurrentEnvironment() {
        return currentEnvName;
    }

    public String getDefaultEnvironment() {
        return envProperties.getProperty(DEFAULT_URL_PROP_NAME, "<no default selected>");
    }

    public void removeEnvironment(String name) {
        if (envProperties.containsKey(name)) {
            envProperties.remove(name);
            if (envProperties.getProperty(DEFAULT_URL_PROP_NAME, "").equals(name)) {
                envProperties.remove(DEFAULT_URL_PROP_NAME);
            }
            envs.remove(name);
            writeForceUrls();
        }
    }

    private void writeForceUrls() {
        try {
            Util.writeProperties("urls", envProperties);
        } catch (IOException e) {
            getLogger().error("Exception persisting new environment settings", e);
        }
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        for (EnvConnections envConnections : connections.values()) {
            envConnections.config.setTraceMessage(debug);
        }
        if (restTemplateConnector != null) {
            restTemplateConnector.debug(debug);
        }

        Level level = Level.DEBUG;
        if (!debug) {
            level = Level.ERROR;
        }
        writer.printf("Setting logger level to %s\n", level.levelStr);
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(level);
    }

    public void init(InputStream in, PrintWriter out) throws IOException, ConnectionException, ServletException {
        SLF4JBridgeHandler.install();
        injectDefaultPluginAndAddCommands();

        reader = new ConsoleReader(in, out);
        reader.addCompletor(completor);
        writer = new Writer(out);


        File hist = new File(System.getProperty("user.home") + "/.force/history");
        if (!hist.getParentFile().exists()) {
            if (!hist.getParentFile().mkdir()) {
                out.println("cant create .force directory");
            }
        }
        if (!hist.exists()) {
            try {
                if (hist.createNewFile()) {
                    reader.setHistory(new History(hist));
                } else {
                    out.println("can't create history file");
                }
            } catch (IOException e) {
                out.println("can't create history file");
            }

        } else {
            reader.setHistory(new History(hist));
        }

        reader.setBellEnabled(false);
        commandReader = new Reader();

        if (!loadLogin()) {
            try {
                pluginManager.getCommand("login").execute(new Context(null, null, null, new String[0], commandReader, writer));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        addSetupTask(new SetupTask() {
            @Override
            public void setup() {
                try {
                    DefaultPlugin.PluginCommand p = (DefaultPlugin.PluginCommand) pluginManager.getCommand("plugin");
                    for (String defalutPlugin : internalPlugins) {
                        DefaultPlugin.PluginArgs args = new DefaultPlugin.PluginArgs();
                        args.setArtifact(defalutPlugin);
                        args.version = "LATEST";
                        args.internal = true;
                        p.executeWithArgs(getContext(new String[0]), args);
                    }
                    loadInstalledPlugins();
                } catch (IOException e) {
                    getLogger().error("IOException while loading previously installed plugins", e);
                }
            }
        });

        addSetupTask(new SetupTask() {
            @Override
            public void setup() {
                try {
                    Util.readProperties("urls", envProperties);
                    for (String s : envProperties.stringPropertyNames()) {
                        if (s.equals(DEFAULT_URL_PROP_NAME)) continue;
                        String url = envProperties.getProperty(s);
                        ForceEnv env = new ForceEnv(url, ".force_urls");
                        if (env.isValid()) {
                            envs.put(s, env);
                        }
                    }

                    String defaultEnv = envProperties.getProperty(DEFAULT_URL_PROP_NAME);
                    if (defaultEnv != null) {
                        currentEnv = envs.get(defaultEnv);
                        currentEnvName = defaultEnv;
                    } else {
                        if (envProperties.size() == 1) {
                            defaultEnv = envProperties.stringPropertyNames().iterator().next();
                            currentEnvName = defaultEnv;
                            currentEnv = envs.get(defaultEnv);
                            envProperties.setProperty(DEFAULT_URL_PROP_NAME, defaultEnv);
                            Util.writeProperties("urls", envProperties);
                        }
                    }

                } catch (IOException e) {
                    getLogger().error("IOException while loading force urls", e);
                }
            }
        });
        executeSetupTasks();

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


    /**
     * Main run loop.
     *
     * @throws InterruptedException
     */
    public void run() throws InterruptedException {
        try {
            pluginManager.getCommand("banner").execute(getContext(new String[0]));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String[] cmds = commandReader.readAndParseLine(FORCEPROMPT);
        String cmdKey = cmds[0];
        while (!cmdKey.equals(EXITCMD)) {
            initLatch.await();
            executeWithArgs(cmds);
            cmds = commandReader.readAndParseLine(FORCEPROMPT);
            cmdKey = cmds[0];
        }
    }

    private void executeWithArgs(String[] cmds) throws InterruptedException {
        String cmdKey = cmds[0];
        if (!cmdKey.equals(EXITCMD)) {
            //we dont wait on the latch if somone runs cliforce exit.
            //this is useful to measure "startup time to get to the prompt"
            //by running> time cliforce exit
            initLatch.await();
        }
        Command cmd = pluginManager.getCommand(cmdKey);
        String[] args = cmds.length > 1 ? Arrays.copyOfRange(cmds, 1, cmds.length) : new String[0];
        if (!cmdKey.equals("") && !cmdKey.equals(EXITCMD)) {
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
        } catch (NullPointerException e) {
            //todo think about a mechanism for commands to do precondition checks on the context
            //or otherwise indicate they need a force env
            if (currentEnv == null) {
                writer.println("The command failed to execute");
                writer.println("You must add a connection using the connection:add command before executing this command");
                writer.println("run 'help connection:add' for more info");
            } else {
                writer.printf("Exception while executing command %s\n", cmdKey);
                writer.printStackTrace(e);
            }
        } catch (Exception e) {
            writer.printf("Exception while executing command %s\n", cmdKey);
            writer.printStackTrace(e);
        } finally {
            Thread.currentThread().setContextClassLoader(curr);
        }
    }

    public List<URL> getClasspathForPlugin(String plugin) {
        return pluginManager.getClasspathForPlugin(plugin);
    }

    CommandContext getContext(String[] args) {
        EnvConnections envConnections = getEnvConnections(currentEnv);
        if (envConnections != null) {
            return new Context(currentEnv, envConnections.forceServiceConnector, vmForceClient, args, commandReader, writer);
        } else {
            if (initLatch.getCount() == 0) {
                getLogger().warn("Couldn't get a valid connection for the current force url. Executing the command without force service connector or VMforce client");
            }
            return new Context(currentEnv, null, null, args, commandReader, writer);
        }
    }

    private EnvConnections getEnvConnections(ForceEnv env) {
        if (env == null || !env.isValid()) return null;
        EnvConnections current = connections.get(env);
        if (current == null) {
            try {
                URL purl = new URL(com.sforce.soap.partner.Connector.END_POINT);
                ForceConnectorConfig config = new ForceConnectorConfig();
                config.setAuthEndpoint("https://" + env.getHost() + purl.getPath());
                config.setUsername(env.getUser());
                config.setPassword(env.getPassword());
                config.setTraceMessage(false);
                config.setPrettyPrintXml(true);
                ForceServiceConnector connector = new ForceServiceConnector(config);
                current = new EnvConnections(config, connector);
                EnvConnections prev = connections.putIfAbsent(env, current);
                return prev == null ? current : prev;
            } catch (ConnectionException e) {
                getLogger().error("ConnectionException while creating ForceConfig, returning null", e);
                return null;
            } catch (MalformedURLException e) {
                getLogger().error("MalformedURLException while creating ForceConfig, returning null", e);
                return null;
            }
        } else {
            return current;
        }
    }


    private static class EnvConnections {
        public final ForceConnectorConfig config;
        public final ForceServiceConnector forceServiceConnector;

        private EnvConnections(ForceConnectorConfig config, ForceServiceConnector forceServiceConnector) {
            this.config = config;
            this.forceServiceConnector = forceServiceConnector;
        }
    }

    /**
     * cliforce internal impl of CommandContext
     */
    private static class Context implements CommandContext {

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
                getLogger().error("Connection exception while getting metadata connection", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public PartnerConnection getPartnerConnection() {
            try {
                return connector.getConnection();
            } catch (ConnectionException e) {
                getLogger().error("ConnectionException while getting metadata connection", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public RestConnection getRestConnection() {
            try {
                return connector.getRestConnection();
            } catch (AsyncApiException e) {
                getLogger().error("AsyncApiException exception while getting rest connection", e);
                throw new RuntimeException(e);
            } catch (ConnectionException e) {
                getLogger().error("ConnectionException exception while getting rest connection", e);
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
                if (line == null) line = EXITCMD;
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

    private class CliforceCompletor implements Completor {
        @Override
        public int complete(String buffer, int cursor, List candidates) {
            String[] args = Util.parseCommand(buffer);
            int cmd = new SimpleCompletor(pluginManager.getCommandNames().toArray(new String[0])).complete(args[0], cursor, candidates);
            if (candidates.size() == 0 && buffer != null && buffer.length() > 0) {
                getLogger().debug("cliforce completor returning 0, from first if branch");
                return 0;
            } else if (candidates.size() == 1 && (buffer.endsWith(" ") || args.length > 1)) {
                String candidate = (String) candidates.remove(0);
                Command command = pluginManager.getCommand(args[0]);
                if (command != null) {
                    if (command instanceof JCommand) {
                        return ((JCommand<?>) command).complete(buffer, args, cursor, (List<String>) candidates, getContext(args));
                    } else {
                        getLogger().debug("cliforce completor executing standard completion");
                        candidates.add(" ");
                        candidates.add(command.describe());
                        return cursor;
                    }
                } else {
                    getLogger().debug("cliforce completor returning {} from command null branch", cmd);
                    return cmd;
                }

            } else {
                getLogger().debug("cliforce completor returning {}  from last else branch", cmd);
                return cmd;
            }
        }
    }

}
