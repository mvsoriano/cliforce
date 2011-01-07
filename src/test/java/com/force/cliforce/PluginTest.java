package com.force.cliforce;


import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import org.junit.Test;

import java.io.PrintStream;
import java.io.PrintWriter;

public class PluginTest {

    @Test
    public void testHappy() {
    }

    @Test
    public void testPluginResolution() throws Exception {
        CLIForce f = new CLIForce();
        DefaultPlugin.PluginCommand c = new DefaultPlugin.PluginCommand(f);
        PrintWriter p = new PrintWriter(System.out);
        try {
            c.execute(new CommandContext() {
                @Override
                public MetadataConnection getMetadataConnection() {
                    return null;
                }

                @Override
                public PartnerConnection getPartnerConnection() {
                    return null;
                }

                @Override
                public String[] getCommandArguments() {
                    return new String[]{"HelloWorldPlugin", "cliplugin:cliplugin:1.0"};
                }

                @Override
                public CommandReader getCommandReader() {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                public PrintStream getCommandWriter() {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }
            });
        } finally {
            p.flush();
        }
    }

    @Test
    public void testPluginResolutionOnly() throws Exception {
        CLIForce f = new CLIForce();
        DefaultPlugin.PluginCommand c = new DefaultPlugin.PluginCommand(f);
        c.resolveWithDependencies("cliplugin", "cliplugin", "1.0");
    }

}
