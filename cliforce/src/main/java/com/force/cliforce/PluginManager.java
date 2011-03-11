package com.force.cliforce;


import com.google.inject.Injector;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class PluginManager {

    private ConcurrentMap<String, Command> commands = new ConcurrentSkipListMap<String, Command>();
    private ConcurrentMap<String, Plugin> plugins = new ConcurrentSkipListMap<String, Plugin>();
    private ConcurrentMap<String, Injector> pluginInjectors = new ConcurrentHashMap<String, Injector>();
    private Properties installedPlugins = new Properties();
    @Inject
    private Injector mainInjector;

    public Command getCommand(String key) {
        return commands.get(key);
    }

    public List<Command> getPluginCommands(String plugin) {
        List<Command> cmds = new ArrayList<Command>();
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            if (entry.getKey().startsWith(plugin + ":")) {
                cmds.add(entry.getValue());
            }
        }
        return cmds;
    }

    public Set<String> getCommandNames() {
        return commands.keySet();
    }

    /**
     * Return a map of installed plugins' maven artifactId->version
     *
     * @return
     */
    public Map<String, String> getInstalledPlugins() {
        Map<String, String> plugins = new HashMap<String, String>();
        for (String s : installedPlugins.stringPropertyNames()) {
            plugins.put(s, installedPlugins.getProperty(s));
        }
        return plugins;
    }

    public List<String> getActivePlugins() {
        List<String> pi = new ArrayList<String>();
        pi.addAll(plugins.keySet());
        Collections.sort(pi);
        return pi;
    }

    /**
     * return the currently installed version of a plugin or null if not installed.
     *
     * @param plugin
     * @return
     */
    public String getInstalledPluginVersion(String plugin) {
        return installedPlugins.getProperty(plugin);
    }

    /*package*/ void installPlugin(String artifact, String version, Plugin p, boolean internal) throws IOException {
        plugins.put(artifact, p);
        if (!internal) {
            installedPlugins.setProperty(artifact, version);
            saveInstalledPlugins();
        }
        injectPluginAndAddCommands(artifact, p, internal);
    }

    public void injectDefaultPluginAndAddCommands(Plugin p) {
        PluginModule module = new PluginModule(p);
        Injector injector = mainInjector.createChildInjector(module);
        for (Class<? extends Command> cmdClass : p.getCommands()) {
            Command command = injector.getInstance(cmdClass);
            commands.put(command.name(), command);
        }
    }

    public void injectPluginAndAddCommands(String artifact, Plugin p, boolean internal) {
        PluginModule module = new PluginModule(p);
        Injector injector = mainInjector.createChildInjector(module);
        for (Class<? extends Command> cmdClass : p.getCommands()) {
            Command command = injector.getInstance(cmdClass);
            commands.put(artifact + ":" + command.name(), command);
        }
    }

    public void removePlugin(String artifactId) throws IOException {
        Plugin p = plugins.remove(artifactId);
        Injector injector = pluginInjectors.get(artifactId);
        if (p != null) {
            for (Class<? extends Command> cmdClass : p.getCommands()) {
                Command command = injector.getInstance(cmdClass);
                commands.remove(artifactId + ":" + command.name());
            }
            installedPlugins.remove(artifactId);
            pluginInjectors.remove(artifactId);
            saveInstalledPlugins();
        }
    }

    public Map<String, String> getCommandDescriptions() {
        Map<String, String> descriptions = new HashMap<String, String>();
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            descriptions.put(entry.getKey(), entry.getValue().describe());
        }
        return descriptions;
    }

    public List<URL> getClasspathForPlugin(String plugin) {
        if (plugin == null) {
            return Arrays.asList(((URLClassLoader) CLIForce.class.getClassLoader()).getURLs());
        }
        Plugin p = plugins.get(plugin);
        if (p == null) {
            return null;
        } else {
            return Arrays.asList(((URLClassLoader) p.getClass().getClassLoader()).getURLs());
        }
    }


    public void loadInstalledPlugins() throws IOException {
        if (!Util.readProperties("plugins", installedPlugins)) {
            throw new IOException(".force_plugins does not exist and was unable to create");
        }
    }

    private void saveInstalledPlugins() throws IOException {
        if (!Util.writeProperties("plugins", installedPlugins)) {
            throw new IOException("Unable to create .force_plugins file, can't save installed plugins. You will have to re-plugin next time you run cliforce");
        }
    }

}
