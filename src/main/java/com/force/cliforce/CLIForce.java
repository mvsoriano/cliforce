package com.force.cliforce;

import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import jline.Completor;
import jline.ConsoleReader;
import jline.History;
import jline.SimpleCompletor;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

public class CLIForce {

    public static final String FORCEPROMPT = "force> ";
    public static final String EXITCMD = "exit";
    private ConsoleReader reader;
    private Completor completor = new SimpleCompletor(EXITCMD);
    Map<String, Command> commands = new TreeMap<String, Command>();
    Map<String, Plugin> plugins = new TreeMap<String, Plugin>();
    private MetadataConnection metadataConnection;
    private PartnerConnection partnerConnection;
    private PrintWriter out = new PrintWriter(System.out);
    ForceEnv forceEnv;


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
        }
    }

    public CLIForce(ForceEnv env) {
        forceEnv = env;
    }

    public void init() throws IOException, ConnectionException {

        URL purl = new URL(com.sforce.soap.partner.Connector.END_POINT);
        URL murl = new URL(com.sforce.soap.metadata.Connector.END_POINT);

        ConnectorConfig partnerConfig = new ConnectorConfig();
        partnerConfig.setAuthEndpoint("https://" + forceEnv.getHost() + purl.getPath());
        partnerConfig.setUsername(forceEnv.getUser());
        partnerConfig.setPassword(forceEnv.getPassword());
        partnerConfig.setTraceMessage("true".equals(System.getProperty("force.trace")));
        partnerConnection = new PartnerConnection(partnerConfig);

        ConnectorConfig mdConfig = new ConnectorConfig();
        mdConfig.setSessionId(partnerConfig.getSessionId());
        mdConfig.setServiceEndpoint("https://" + forceEnv.getHost() + murl.getPath());
        mdConfig.setTraceMessage("true".equals(System.getProperty("force.trace")));
        metadataConnection = new MetadataConnection(mdConfig);

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
        };

        String[] cmdsplit = cmdr.readLine(FORCEPROMPT).trim().split("\\s+", 2);
        while (!cmdsplit[0].equals(EXITCMD)) {
            Command cmd = commands.get(cmdsplit[0]);
            String[] args = cmdsplit.length == 2 ? cmdsplit[1].split("\\s+") : new String[0];
            if (cmd != null) {
                ClassLoader curr = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(cmd.getClass().getClassLoader());
                    cmd.execute(new Context(metadataConnection, partnerConnection, args, cmdr));
                } catch (Exception e) {
                    out.printf("Exception while executing command %s", cmdsplit[0]);
                    e.printStackTrace(out);
                } finally {
                    Thread.currentThread().setContextClassLoader(curr);
                    out.flush();
                }

            } else {
                out.printf("Unknown Command %s\n", cmdsplit[0]);
                out.flush();
            }
            cmdsplit = cmdr.readLine(FORCEPROMPT).trim().split("\\s+", 2);
        }

    }

    private static class Context implements CommandContext {

        MetadataConnection mc;
        PartnerConnection pc;
        String[] args;
        CommandReader reader;


        private Context(MetadataConnection mc, PartnerConnection pc, String[] args, CommandReader reader) {
            this.mc = mc;
            this.pc = pc;
            this.args = args;
            this.reader = reader;
        }

        @Override
        public MetadataConnection getMetadataConnection() {
            return mc;
        }

        @Override
        public PartnerConnection getPartnerConnection() {
            return pc;
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
