package com.force.cliforce;


import com.sforce.async.RestConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.vmforce.client.VMForceClient;
import org.junit.Test;

import java.io.PrintStream;
import java.io.PrintWriter;

public class PluginTest {

    @Test
    public void testHappy() {
    }

    @Test
    public void testPluginResolution() throws Exception {
        CLIForce f = new CLIForce(new ForceEnv());
        DefaultPlugin.PluginCommand c = new DefaultPlugin.PluginCommand(f);
        PrintWriter p = new PrintWriter(System.out);
        try {
            c.execute(new CommandContext() {
                @Override
                public MetadataConnection getMetadataConnection() {
                    return null;
                }


                @Override
                public VMForceClient getVmForceClient() {
                    return null;
                }

                @Override
                public PartnerConnection getPartnerConnection() {
                    return null;
                }

                @Override
                public RestConnection getRestConnection() {
                    return null;
                }

                @Override
                public String[] getCommandArguments() {
                    return new String[]{"-a", "cliplugin"};
                }

                @Override
                public CommandReader getCommandReader() {
                    return null;
                }

                @Override
                public PrintStream getCommandWriter() {
                    return System.out;
                }
            });
        } finally {
            p.flush();
        }
    }


}
