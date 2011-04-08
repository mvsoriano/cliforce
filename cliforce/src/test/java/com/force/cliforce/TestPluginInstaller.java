package com.force.cliforce;


import javax.inject.Inject;
import java.io.IOException;

/**
 * Allows tests to manually install plugins to a pluginManager
 * rather than loading them through the DependencyResolver/ServiceLoader
 * mechanism of the PluginCommand.
 * <p/>
 * The pluginManager used is the same one injected into the cliforce instance if you are using the TestModule guice mod.
 */
public class TestPluginInstaller {

    @Inject
    private PluginManager pluginManager;

    @Inject
    private CLIForce cliForce;

    public void installPlugin(String artifact, String version, Plugin p, boolean internal) throws IOException {
        pluginManager.installPlugin(artifact, version, p, internal);
        if (internal) {
            cliForce.getInternalPlugins().add(artifact);
        }
    }

    public void installDefaultPlugin() {
        pluginManager.injectDefaultPluginAndAddCommands(new DefaultPlugin());
    }

}
