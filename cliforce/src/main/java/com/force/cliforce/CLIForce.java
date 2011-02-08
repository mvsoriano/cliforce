package com.force.cliforce;

import ch.qos.logback.classic.Level;
import com.force.sdk.connector.ForceServiceConnector;
import com.sforce.async.AsyncApiException;
import com.sforce.async.RestConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
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
import java.net.URL;
import java.util.*;

public class CLIForce {

    /*package protected fields are accessed by commands in the default plugin*/
    private static Logger logger = LoggerFactory.getLogger(CLIForce.class);
    public static final String FORCEPROMPT = "force> ";
    public static final String EXITCMD = "exit";
    /*package*/ Map<String, Command> commands = new TreeMap<String, Command>();
    /*package*/ Map<String, Plugin> plugins = new TreeMap<String, Plugin>();
    /*package*/ Properties installedPlugins = new Properties();
    /*package*/ ForceEnv forceEnv;
    /*package*/ ConsoleReader reader;
    private CommandReader commandReader;
    private VMForceClient forceClient;
    private Completor completor = new SimpleCompletor(EXITCMD);
    private volatile boolean debug = false;
    private ForceServiceConnector connector;
    private CommandWriter writer;
    private ConnectorConfig config;
    private RestTemplateConnector restConnector;


    public static void main(String[] args) {

        ForceEnv env = new ForceEnv();
        if (!env.isValid()) {
            System.out.println("Could not find a proper configuration.");
            System.out.println("Tried to use config source: " + env.getConfigSource());
            System.out.println("Got the following message: ");
            System.out.println(env.getMessage());
            System.out.println("Found URL: " + env.getUrl());
            return;
        }

        CLIForce cliForce = new CLIForce(env);
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
            logger.error("Connection Exception while initializing cliforce, exiting", e);
            System.exit(1);
        } catch (IOException e) {
            logger.error("IOException Exception while initializing cliforce, exiting", e);
            System.exit(1);
        } catch (ServletException e) {
            logger.error("ServletException Exception while initializing cliforce, exiting", e);
            System.exit(1);
        }
    }


    public CLIForce(ForceEnv env) {
        forceEnv = env;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        config.setTraceMessage(debug);
        restConnector.debug(debug);
        Level level = Level.DEBUG;
        if (!debug) {
            level = Level.INFO;
        }
        writer.printf("Setting logger level to %s\n", level.levelStr);
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(level);
    }

    public void init(InputStream in, PrintWriter out) throws IOException, ConnectionException, ServletException {
        SLF4JBridgeHandler.install();
        URL purl = new URL(com.sforce.soap.partner.Connector.END_POINT);
        config = new ConnectorConfig();
        config.setAuthEndpoint("https://" + forceEnv.getHost() + purl.getPath());
        config.setUsername(forceEnv.getUser());
        config.setPassword(forceEnv.getPassword());
        config.setTraceMessage(false);
        config.setPrettyPrintXml(true);
        connector = new ForceServiceConnector("cliforce", config);

        forceClient = new VMForceClient();
        restConnector = new RestTemplateConnector();
        restConnector.setTarget(new HttpHost("api.alpha.vmforce.com"));
        restConnector.debug(false);
        forceClient.setHttpConnector(restConnector);
        forceClient.login(forceEnv.getUser(), forceEnv.getPassword());


        Plugin def = new DefaultPlugin(this);
        for (Command command : def.getCommands()) {
            commands.put(command.name(), command);
        }


        reader = new ConsoleReader(in, out);
        reader.addCompletor(completor);
        writer = new Writer(out);

        DefaultPlugin.PluginCommand p = (DefaultPlugin.PluginCommand) commands.get("plugin");
        String[] defalutPlugins = {"app", "db", "template"};//TODO externalize
        for (String defalutPlugin : defalutPlugins) {
            DefaultPlugin.PluginArgs args = new DefaultPlugin.PluginArgs();
            args.artifact = defalutPlugin;
            args.version = "LATEST";
            args.internal = true;
            p.executeWithArgs(getContext(new String[0]), args);
        }


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
        loadInstalledPlugins();
    }

    private void loadInstalledPlugins() throws FileNotFoundException {
        DefaultPlugin.PluginCommand p = (DefaultPlugin.PluginCommand) commands.get("plugin");
        File plugins = new File(System.getProperty("user.home") + "/.force_plugins");
        try {
            if (!plugins.exists() && !plugins.createNewFile()) {
                logger.debug(".force_plugins does not exist and was unable to create");
                return;
            }

            FileInputStream in = new FileInputStream(plugins);
            installedPlugins.load(in);
            in.close();
            for (String artifact : installedPlugins.stringPropertyNames()) {
                String version = installedPlugins.getProperty(artifact);
                DefaultPlugin.PluginArgs args = new DefaultPlugin.PluginArgs();
                args.artifact = artifact;
                args.version = version;
                p.executeWithArgs(getContext(new String[0]), args);
            }
        } catch (IOException e) {
            logger.debug("Caught IOException while loading previously installed plugins", e);
        }
    }

    void saveInstalledPlugins(CommandWriter out) {
        File plugins = new File(System.getProperty("user.home") + "/.force_plugins");
        if (plugins.exists()) {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(plugins);
                installedPlugins.store(fileOutputStream, "CLIForce plugins");
                fileOutputStream.close();
            } catch (IOException e) {
                out.println("error persisting installation of plugin, you will have to re-plugin next time you run cliforce");
            }
        }
    }

    void reloadCompletions() {
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

    public void run() {
        try {
            commands.get("banner").execute(getContext(new String[0]));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String[] cmds = commandReader.readAndParseLine(FORCEPROMPT);
        String cmdKey = cmds[0];
        while (!cmdKey.equals(EXITCMD)) {
            executeWithArgs(cmds);
            cmds = commandReader.readAndParseLine(FORCEPROMPT);
            cmdKey = cmds[0];
        }
    }

    private void executeWithArgs(String[] cmds) {
        String cmdKey = cmds[0];
        Command cmd = commands.get(cmdKey);
        String[] args = cmds.length > 1 ? Arrays.copyOfRange(cmds, 1, cmds.length) : new String[0];
        if (!cmdKey.equals("")) {
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
        } catch (Exception e) {
            writer.printf("Exception while executing command %s\n", cmdKey);
            writer.printStackTrace(e);
        } finally {
            Thread.currentThread().setContextClassLoader(curr);
        }
    }


    private CommandContext getContext(String[] args) {
        return new Context(connector, forceClient, args, commandReader, writer);
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


        private Context(ForceServiceConnector c, VMForceClient cl, String[] args, CommandReader reader, CommandWriter writer) {
            this.connector = c;
            this.client = cl;
            this.args = args;
            this.reader = reader;
            this.writer = writer;
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
                logger.error("Connection exception while getting metadata connection", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public PartnerConnection getPartnerConnection() {
            try {
                return connector.getConnection();
            } catch (ConnectionException e) {
                logger.error("Connection exception while getting metadata connection", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public RestConnection getRestConnection() {
            try {
                return connector.getRestConnection();
            } catch (AsyncApiException e) {
                logger.error("AsyncApiException exception while getting rest connection", e);
                throw new RuntimeException(e);
            } catch (ConnectionException e) {
                logger.error("ConnectionException exception while getting rest connection", e);
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
}
