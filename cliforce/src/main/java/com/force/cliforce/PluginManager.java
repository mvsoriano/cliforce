package com.force.cliforce;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;


public interface PluginManager {
    Command getCommand(String key);

    List<Command> getPluginCommands(String plugin);

    Set<String> getCommandNames();

    Map<String, String> getInstalledPlugins();

    List<String> getActivePlugins();

    String getInstalledPluginVersion(String plugin);

    void injectDefaultPluginAndAddCommands(Plugin p);

    void injectPluginAndAddCommands(String artifact, Plugin p);

    void removePlugin(String artifactId) throws IOException;

    Map<String, String> getCommandDescriptions();

    List<URL> getClasspathForPlugin(String plugin);

    void loadInstalledPlugins() throws IOException;

    void installPlugin(String artifact, String version, Plugin p, boolean internal) throws IOException;
}
