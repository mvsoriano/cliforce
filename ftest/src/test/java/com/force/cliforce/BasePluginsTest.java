package com.force.cliforce;

import java.io.IOException;

import com.force.cliforce.plugin.app.AppPlugin;
import com.force.cliforce.plugin.connection.ConnectionPlugin;
import com.force.cliforce.plugin.db.DBPlugin;
import com.force.cliforce.plugin.jpa.JPAPlugin;
import com.force.cliforce.plugin.template.TemplatePlugin;

/**
 * 
 * Use this base class if you need to run a test with all plugins installed
 *
 * @author jeffrey.lai
 * @since 
 */
public class BasePluginsTest extends BaseCliforceCommandTest {

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
        c.installPlugin("app", "LATEST", (Plugin)new AppPlugin(), isInternal());
        c.installPlugin("db", "LATEST", new DBPlugin(), isInternal());
        c.installPlugin("connection", "LATEST", new ConnectionPlugin(), isInternal());
        c.installPlugin("template", "LATEST", new TemplatePlugin(), isInternal());
        c.installPlugin("jpa", "LATEST", new JPAPlugin(), isInternal());
    }
    

}
