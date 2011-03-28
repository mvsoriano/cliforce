package com.force.cliforce;


import javax.inject.Inject;
import java.io.IOException;

public class TestPluginInstaller {

    @Inject
    private PluginManager pluginManager;

    public void installPlugin(String artifact, String version, Plugin p) throws IOException {
        pluginManager.installPlugin(artifact, version, p, true);
    }

}
