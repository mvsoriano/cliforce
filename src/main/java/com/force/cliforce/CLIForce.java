package com.force.cliforce;

import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import jline.Completor;
import jline.ConsoleReader;
import jline.History;
import jline.SimpleCompletor;
import org.apache.ivy.Ivy;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

public class CLIForce {

    private ConsoleReader reader;
    private Completor completor = new SimpleCompletor("exit");
    Map<String, Command> commands = new TreeMap<String, Command>();
    Map<String, Plugin> plugins = new TreeMap<String, Plugin>();
    private MetadataConnection metadataConnection;
    private PartnerConnection partnerConnection;
    private PrintWriter out = new PrintWriter(System.out);
    ForceEnv forceEnv;
    Ivy ivy = Ivy.newInstance();


    static public void main(String[] args) {

        ForceEnv env = new ForceEnv();


        if (!env.isValid()) {
            System.out.println("Could not find a proper configuration.");
            System.out.println("Tried to use config source: " + env.getConfigSource());
            System.out.println("Got the following message: ");
            System.out.println(env.getMessage());
            System.out.println("Found URL: " + env.getUrl());
            return;
        }

        try {

            URL purl = new URL(com.sforce.soap.partner.Connector.END_POINT);
            URL murl = new URL(com.sforce.soap.metadata.Connector.END_POINT);

            ConnectorConfig partnerConfig = new ConnectorConfig();
            partnerConfig.setAuthEndpoint("https://" + env.getHost() + purl.getPath());
            partnerConfig.setUsername(env.getUser());
            partnerConfig.setPassword(env.getPassword());
            partnerConfig.setTraceMessage("true".equals(System.getProperty("force.trace")));

            PartnerConnection partner = new PartnerConnection(partnerConfig);

            ConnectorConfig mdConfig = new ConnectorConfig();
            mdConfig.setSessionId(partnerConfig.getSessionId());
            mdConfig.setServiceEndpoint("https://" + env.getHost() + murl.getPath());
            mdConfig.setTraceMessage("true".equals(System.getProperty("force.trace")));
            MetadataConnection md = new MetadataConnection(mdConfig);

            CLIForce cliForce = new CLIForce();
            cliForce.metadataConnection = md;
            cliForce.partnerConnection = partner;
            cliForce.forceEnv = env;
            Plugin def = new DefaultPlugin(cliForce);
            for (Command command : def.getCommands()) {
                cliForce.commands.put(command.name(), command);
            }


            cliForce.run();

        } catch (ConnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void reloadCompletions() {
        reader.removeCompletor(completor);
        completor = new SimpleCompletor(commands.keySet().toArray(new String[0]));
        reader.addCompletor(completor);
    }

    public void run() throws IOException {
        reader = new ConsoleReader();
        reader.addCompletor(completor);
        File hist = new File(System.getProperty("user.home") + "/.force_history");

        if (!hist.exists()) {
            try {
                hist.createNewFile();
                reader.setHistory(new History(hist));
            } catch (IOException e) {
                System.out.println("cant create history file");
            }

        } else {
            reader.setHistory(new History(hist));
        }
        reloadCompletions();

        reader.setBellEnabled(false);


        String[] cmdsplit = reader.readLine("force> ").trim().split("\\s+", 2);
        while (!cmdsplit[0].equals("exit")) {
            Command cmd = commands.get(cmdsplit[0]);
            String[] args = cmdsplit.length == 2 ? cmdsplit[1].split("\\s+") : new String[0];
            if (cmd != null) {
                ClassLoader curr = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(cmd.getClass().getClassLoader());
                    cmd.execute(new Context(metadataConnection, partnerConnection, args, new CommandReader() {
                        @Override
                        public String readLine(final String prompt) {
                            try {
                                return reader.readLine(prompt);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }));
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
            cmdsplit = reader.readLine("force> ").trim().split("\\s+", 2);
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
