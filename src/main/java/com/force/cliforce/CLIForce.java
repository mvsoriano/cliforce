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
import jline.Completor;
import jline.ConsoleReader;
import jline.History;
import jline.SimpleCompletor;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.httpclient.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class CLIForce {


    public static final String FORCEPROMPT = "force> ";
    public static final String EXITCMD = "exit";
    /*package*/ ConsoleReader reader;
    private VMForceClient forceClient;
    private Completor completor = new SimpleCompletor(EXITCMD);
    /*package*/ Map<String, Command> commands = new TreeMap<String, Command>();
    /*package*/ Map<String, Plugin> plugins = new TreeMap<String, Plugin>();
    /*package*/ ForceEnv forceEnv;
    private ForceServiceConnector connector;
    private static Logger logger = LoggerFactory.getLogger(CLIForce.class);
    private PrintWriter out = new PrintWriter(System.out);


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
            cliForce.run();
        } catch (ConnectionException e) {
            System.out.println("Connection Exception while initializing cliforce, exiting");
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            System.out.println("IOException Exception while initializing cliforce, exiting");
            e.printStackTrace();
            System.exit(1);
        } catch (ServletException e) {
            System.out.println("ServletException Exception while initializing cliforce, exiting");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public CLIForce(ForceEnv env) {
        forceEnv = env;
    }

    public void init() throws IOException, ConnectionException, ServletException {
        URL purl = new URL(com.sforce.soap.partner.Connector.END_POINT);
        ConnectorConfig config = new ConnectorConfig();
        config.setAuthEndpoint("https://" + forceEnv.getHost() + purl.getPath());
        config.setUsername(forceEnv.getUser());
        config.setPassword(forceEnv.getPassword());
        config.setTraceMessage("true".equals(System.getProperty("force.trace")));
        connector = new ForceServiceConnector("cliforce", config);

        forceClient = new VMForceClient();
        RestTemplateConnector connector = new RestTemplateConnector();
        connector.setTarget(new HttpHost("api.alpha.vmforce.com"));
        connector.debug(true);
        forceClient.setHttpConnector(connector);
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

    }

    void reloadCompletions() {
        reader.removeCompletor(completor);
        completor = new SimpleCompletor(commands.keySet().toArray(new String[0]));
        reader.addCompletor(completor);
    }

    public void run() {
        CommandReader cmdr = new CommandReader() {
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
        };
        try {
            commands.get("banner").execute(getContext(new String[0], cmdr));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String[] cmds = cmdr.readAndParseLine(FORCEPROMPT);
        String cmdKey = cmds[0];
        while (!cmdKey.equals(EXITCMD)) {
            Command cmd = commands.get(cmdKey);
            String[] args = cmds.length > 1 ? Arrays.copyOfRange(cmds, 1, cmds.length) : new String[0];
            if (cmd != null) {
                ClassLoader curr = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(cmd.getClass().getClassLoader());
                    cmd.execute(getContext(args, cmdr));
                } catch (Exception e) {
                    out.printf("Exception while executing command %s\n", cmdKey);
                    e.printStackTrace(out);
                } finally {
                    Thread.currentThread().setContextClassLoader(curr);
                    out.flush();
                }

            } else {
                out.printf("Unknown Command %s\n", cmdKey);
                out.flush();
            }
            cmds = cmdr.readAndParseLine(FORCEPROMPT);
        }

    }


    private CommandContext getContext(String[] args, CommandReader reader) {
        return new Context(connector, forceClient, args, reader);
    }

    private static class Context implements CommandContext {

        ForceServiceConnector connector;
        String[] args;
        CommandReader reader;
        VMForceClient client;


        private Context(ForceServiceConnector c, VMForceClient cl, String[] args, CommandReader reader) {
            this.connector = c;
            this.client = cl;
            this.args = args;
            this.reader = reader;
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
        public PrintStream getCommandWriter() {
            return System.out;
        }
    }


}
