package com.force.cliforce;


import com.force.cliforce.CLIForce;
import com.force.cliforce.DefaultPlugin;
import org.junit.Test;

import java.io.PrintWriter;

public class PluginTest {

    @Test public void testHappy(){}

   @Test
    public void testPluginResolution() throws Exception {
        CLIForce f = new CLIForce();
        DefaultPlugin.PluginCommand c = new DefaultPlugin.PluginCommand(f);
        PrintWriter p = new PrintWriter(System.out);
        try {
            c.execute(new String[]{"HelloWorldPlugin", "cliplugin:cliplugin:1.0"}, null, null, p);
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
