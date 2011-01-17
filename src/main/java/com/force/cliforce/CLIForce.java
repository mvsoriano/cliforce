package com.force.cliforce;

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
    /*package*/ ForceEnv forceEnv;
    /*package*/ ConsoleReader reader;
    private CommandReader commandReader;
    private VMForceClient forceClient;
    private Completor completor = new SimpleCompletor(EXITCMD);
    private volatile boolean debug = false;
    private ForceServiceConnector connector;
    private CommandWriter writer = new Writer();
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
            cliForce.init();
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
    }

    public void init() throws IOException, ConnectionException, ServletException {
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
        reader = new ConsoleReader();
        reader.addCompletor(completor);
        File hist = new File(System.getProperty("user.home") + "/.force_history");

        if (!hist.exists()) {
            try {
                if (hist.createNewFile()) {
                    reader.setHistory(new History(hist));
                } else {
                    System.out.println("cant create history file");
                }
            } catch (IOException e) {
                System.out.println("cant create history file");
            }

        } else {
            reader.setHistory(new History(hist));
        }
        reloadCompletions();
        reader.setBellEnabled(false);
        commandReader = new Reader();

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

    public static class Writer implements CommandWriter {
        @Override
        public void printf(String format, Object... args) {
            System.out.printf(format, args);
        }

        @Override
        public void print(String msg) {
            System.out.print(msg);
        }

        @Override
        public void println(String msg) {
            System.out.println(msg);
        }

        @Override
        public void printStackTrace(Exception e) {
            e.printStackTrace(System.out);
        }
    }


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
