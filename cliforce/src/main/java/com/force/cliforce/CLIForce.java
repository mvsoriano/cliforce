package com.force.cliforce;

import ch.qos.logback.classic.Level;
import com.force.sdk.connector.ForceConnectorConfig;
import com.force.sdk.connector.ForceServiceConnector;
import com.sforce.async.AsyncApiException;
import com.sforce.async.RestConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.vmforce.client.VMForceClient;
import com.vmforce.client.connector.RestTemplateConnector;
import jline.*;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.httpclient.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.servlet.ServletException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class CLIForce {

    /*package protected fields are accessed by commands in the default plugin*/
    /**
     * This field is lazy initialized by getLogger, shouldnt use directly.
     */
    private static CLIForce cliForce;
    private static Logger logger;
    public static final String FORCEPROMPT = "force> ";
    public static final String EXITCMD = "exit";
    public static final String DEFAULT_URL_PROP_NAME = "__default__";
    private ConcurrentMap<String, Command> commands = new ConcurrentSkipListMap<String, Command>();
    private ConcurrentMap<String, Plugin> plugins = new ConcurrentSkipListMap<String, Plugin>();
    private ConcurrentMap<ForceEnv, EnvConnections> connections = new ConcurrentHashMap<ForceEnv, EnvConnections>();
    private ConcurrentMap<String, ForceEnv> envs = new ConcurrentHashMap<String, ForceEnv>();
    /*key=envName,value=forceUrl*/
    private Properties envProperties = new Properties();
    /*key=pluginArtifactId,value=pluginVersion*/
    private Properties installedPlugins = new Properties();
    private ConsoleReader reader;
    private volatile ForceEnv currentEnv;
    private volatile String currentEnvName;
    private CommandReader commandReader;
    private Completor completor = new SimpleCompletor(EXITCMD);
    private volatile boolean debug = false;
    private CommandWriter writer;

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

        cliForce = new CLIForce();
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
        }
    }


    public static CLIForce getInstance() {
        return cliForce;
    }

    /**
     * Return a map of installed plugins' maven artifactId->version
     *
     * @return
     */
    public Map<String, String> getInstalledPlugins() {
        Map<String, String> plugins = new HashMap<String, String>();
        for (String s : installedPlugins.stringPropertyNames()) {
            plugins.put(s, installedPlugins.getProperty(s));
        }
        return plugins;
    }

    /**
     * return the currently installed version of a plugin or null if not installed.
     *
     * @param plugin
     * @return
     */
    public String getInstalledPluginVersion(String plugin) {
        return installedPlugins.getProperty(plugin);
    }

    void installPlugin(String artifact, String version, Plugin p, boolean internal) {
        List<Command> pcommands = p.getCommands();
        plugins.put(artifact, p);
        if (!internal) {
            installedPlugins.setProperty(artifact, version);
            saveInstalledPlugins(writer);
            writer.printf("Adding Plugin: %s (%s)\n", artifact, p.getClass().getName());
        }

        for (Command command : pcommands) {
            if (!internal) {
                writer.printf("\tadds command %s:%s (%s)\n", artifact, command.name(), command.getClass().getName());
            }
            commands.put(artifact + ":" + command.name(), command);
        }
        reloadCompletions();
    }

    void removePlugin(String artifactId) {
        Plugin p = plugins.remove(artifactId);
        if (p == null) {
            writer.println("....not found");
        } else {
            for (Command command : p.getCommands()) {
                commands.remove(artifactId + ":" + command.name());
                writer.printf("\tremoved command: %s\n", command.name());
            }
            reloadCompletions();
            installedPlugins.remove(artifactId);
            saveInstalledPlugins(writer);
            writer.println("Done");
        }
    }

    public Map<String, String> getCommandDescriptions() {
        Map<String, String> descriptions = new HashMap<String, String>();
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            descriptions.put(entry.getKey(), entry.getValue().describe());
        }
        return descriptions;
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

    private CLIForce() {
    }


    public boolean isDebug() {
        return debug;
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
            envConnections.restConnector.debug(debug);
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


        Plugin def = new DefaultPlugin(this);
        for (Command command : def.getCommands()) {
            commands.put(command.name(), command);
        }


        reader = new ConsoleReader(in, out);
        reader.addCompletor(completor);
        writer = new Writer(out);


        File hist = new File(System.getProperty("user.home") + "/.force_history");

        if (!hist.exists()) {
            try {
                if (hist.createNewFile()) {
                    reader.setHistory(new History(hist));
                } else {
                    out.println("cant create history file");
                }
            } catch (IOException e) {
                out.println("cant create history file");
            }

        } else {
            reader.setHistory(new History(hist));
        }
        reloadCompletions();
        reader.setBellEnabled(false);
        commandReader = new Reader();

        addSetupTask(new SetupTask() {
            @Override
            public void setup() {
                try {
                    DefaultPlugin.PluginCommand p = (DefaultPlugin.PluginCommand) commands.get("plugin");
                    String[] defalutPlugins = {"connection", "app", "db", "template"};//TODO externalize
                    for (String defalutPlugin : defalutPlugins) {
                        DefaultPlugin.PluginArgs args = new DefaultPlugin.PluginArgs();
                        args.setArtifact(defalutPlugin);
                        args.version = "LATEST";
                        args.internal = true;
                        p.executeWithArgs(getContext(new String[0]), args);
                    }
                    loadInstalledPlugins();
                } catch (FileNotFoundException e) {
                    getLogger().error("FileNotFoundException while loading previously installed plugins", e);
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

    private void loadInstalledPlugins() throws FileNotFoundException {
        DefaultPlugin.PluginCommand p = (DefaultPlugin.PluginCommand) commands.get("plugin");
        try {
            if (Util.readProperties("plugins", installedPlugins)) {
                for (String artifact : installedPlugins.stringPropertyNames()) {
                    String version = installedPlugins.getProperty(artifact);
                    DefaultPlugin.PluginArgs args = new DefaultPlugin.PluginArgs();
                    args.setArtifact(artifact);
                    args.version = version;
                    p.executeWithArgs(getContext(new String[0]), args);
                }
            } else {
                getLogger().debug(".force_plugins does not exist and was unable to create");
                return;
            }
        } catch (IOException e) {
            getLogger().debug("Caught IOException while loading previously installed plugins", e);
        }
    }

    void saveInstalledPlugins(CommandWriter out) {
        try {
            if (!Util.writeProperties("plugins", installedPlugins)) {
                out.println("Unable to create .force_plugins file cannot save installed plugins, you will have to re-plugin next time you run cliforce");
            }
        } catch (IOException e) {
            out.println("error persisting installation of plugin, you will have to re-plugin next time you run cliforce");
        }

    }

    private synchronized void reloadCompletions() {
        reader.removeCompletor(completor);
        List<Completor> completors = new ArrayList<Completor>();
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            List<Completor> cmdCompletors = new ArrayList<Completor>();
            cmdCompletors.add(new SimpleCompletor(entry.getKey()));
            cmdCompletors.add(new SimpleCompletor(new String[]{" ", entry.getValue().describe()}));
            completors.add(new ArgumentCompletor(cmdCompletors));
        }
        completor = new MultiCompletor(completors);
        reader.addCompletor(completor);
    }

    public void run() throws InterruptedException {
        try {
            commands.get("banner").execute(getContext(new String[0]));
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
        Command cmd = commands.get(cmdKey);
        String[] args = cmds.length > 1 ? Arrays.copyOfRange(cmds, 1, cmds.length) : new String[0];
        if (!cmdKey.equals("") && !cmdKey.equals(EXITCMD)) {
            if (cmd != null) {
                initLatch.await();
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
        } catch (Exception e) {
            writer.printf("Exception while executing command %s\n", cmdKey);
            writer.printStackTrace(e);
        } finally {
            Thread.currentThread().setContextClassLoader(curr);
        }
    }


    private CommandContext getContext(String[] args) {
        EnvConnections envConnections = getEnvConnections(currentEnv);
        if (envConnections != null) {
            return new Context(currentEnv, envConnections.forceServiceConnector, envConnections.vmForceClient, args, commandReader, writer);
        } else {
            if (initLatch.getCount() == 0) {
                getLogger().warn("Could not get a valid connection for the current force url. Executing the command without force service connector or vmforce client");
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
                ForceServiceConnector connector = new ForceServiceConnector("cliforce", config);
                VMForceClient forceClient = new VMForceClient();
                RestTemplateConnector restConnector = new RestTemplateConnector();
                try {
                    restConnector.setTarget(new HttpHost("api.alpha.vmforce.com"));
                    restConnector.debug(false);
                    forceClient.setHttpConnector(restConnector);
                    forceClient.login(env.getUser(), env.getPassword());
                } catch (Exception e) {
                    System.out.println("Couldn't authenticate with controller. Continuing. Error: " + e);
                }

                current = new EnvConnections(config, connector, forceClient, restConnector);
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
        public final VMForceClient vmForceClient;
        public final RestTemplateConnector restConnector;

        private EnvConnections(ForceConnectorConfig config, ForceServiceConnector forceServiceConnector, VMForceClient vmForceClient, RestTemplateConnector restConnector) {
            this.config = config;
            this.forceServiceConnector = forceServiceConnector;
            this.vmForceClient = vmForceClient;
            this.restConnector = restConnector;
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
                getLogger().error("Connection exception while getting metadata connection", e);
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
                if (line.equals("")) return new String[]{""};
                CommandLine c = CommandLine.parse(line);
                String exe = c.getExecutable();
                String[] args = c.getArguments();
                String[] all = new String[args.length + 1];
                all[0] = exe;
                System.arraycopy(args, 0, all, 1, args.length);
                return all;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

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
