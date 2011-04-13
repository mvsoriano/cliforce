package com.force.cliforce.plugins;

import java.io.IOException;

import org.testng.annotations.Test;

import scala.actors.threadpool.Arrays;

import com.force.cliforce.BaseCliforceCommandTest;
import com.force.cliforce.CLIForce;
import com.force.cliforce.Plugin;
import com.force.cliforce.plugin.app.AppPlugin;
import com.force.cliforce.plugin.connection.ConnectionPlugin;
import com.force.cliforce.plugin.db.DBPlugin;
import com.force.cliforce.plugin.template.TemplatePlugin;

/**
 * Test command completion for commands that behave differently if plugins are loaded.
 * @author dhain
 * @since javasdk-21.0.2-BETA
 */
public class CommandsCompletorWithPluginsTest extends BaseCliforceCommandTest {

	@Override
	public String getPluginArtifact() {
		return null;
	}

	@Override
	public Plugin getPlugin() {
		return null;
	}
	
	@Override
    public void setupCLIForce(CLIForce c) throws IOException {
		//we load the plugin we need in the individual tests
	}

    @Test
    public void testClasspathCommandLoadedPlugins() throws IOException {
    	testPluginInstaller.installPlugin("app", "LATEST", (Plugin)new AppPlugin(), true);
    	testPluginInstaller.installPlugin("db", "LATEST", new DBPlugin(), true);
    	testPluginInstaller.installPlugin("connection", "LATEST", new ConnectionPlugin(), true);
    	testPluginInstaller.installPlugin("template", "LATEST", new TemplatePlugin(), true);
    	runCompletorTestCase("classpath --sort ", 16, Arrays.asList(new String[] {"app", "connection", "db", "template", "<or none for the cliforce classpath>"}));
    }  
}
