/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

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

public class MainPluginManager implements PluginManager {

    private ConcurrentMap<String, Command> commands = new ConcurrentSkipListMap<String, Command>();
    private ConcurrentMap<String, Plugin> plugins = new ConcurrentSkipListMap<String, Plugin>();
    private ConcurrentMap<String, Injector> pluginInjectors = new ConcurrentHashMap<String, Injector>();
    private Properties installedPlugins = new Properties();
    @Inject
    private Injector mainInjector;

    @Override
    public Command getCommand(String key) {
        return commands.get(key);
    }

    @Override
    public List<Command> getPluginCommands(String plugin) {
        List<Command> cmds = new ArrayList<Command>();
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            if (entry.getKey().startsWith(plugin + ":")) {
                cmds.add(entry.getValue());
            }
        }
        return cmds;
    }

    @Override
    public Set<String> getCommandNames() {
        return commands.keySet();
    }

    /**
     * Return a map of installed plugins' maven artifactId->version
     *
     * @return
     */
    @Override
    public Map<String, String> getInstalledPlugins() {
        Map<String, String> plugins = new HashMap<String, String>();
        for (String s : installedPlugins.stringPropertyNames()) {
            plugins.put(s, installedPlugins.getProperty(s));
        }
        return plugins;
    }

    @Override
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
    @Override
    public String getInstalledPluginVersion(String plugin) {
        return installedPlugins.getProperty(plugin);
    }

    @Override
    public void installPlugin(String artifact, String version, Plugin p, boolean internal) throws IOException {
        plugins.put(artifact, p);
        if (!internal) {
            installedPlugins.setProperty(artifact, version);
            saveInstalledPlugins();
        }
        injectPluginAndAddCommands(artifact, p);
    }

    @Override
    public void injectDefaultPluginAndAddCommands(DefaultPlugin p) {
        PluginModule module = new PluginModule(p);
        Injector injector = mainInjector.createChildInjector(module);
        for (Class<? extends Command> cmdClass : p.getCommands()) {
            Command command = injector.getInstance(cmdClass);
            commands.put(command.name(), command);
        }
    }

    @Override
    public void injectPluginAndAddCommands(String artifact, Plugin p) {
        PluginModule module = new PluginModule(p);
        Injector injector = mainInjector.createChildInjector(module);
        pluginInjectors.put(artifact, injector);
        for (Class<? extends Command> cmdClass : p.getCommands()) {
            Command command = injector.getInstance(cmdClass);
            commands.put(artifact + ":" + command.name(), command);
        }
    }


    @Override
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

    @Override
    public Map<String, String> getCommandDescriptions() {
        Map<String, String> descriptions = new HashMap<String, String>();
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            descriptions.put(entry.getKey(), entry.getValue().describe());
        }
        return descriptions;
    }

    @Override
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


    @Override
    public void loadInstalledPlugins() throws IOException {
        Util.readProperties("plugins", installedPlugins);
    }

    private void saveInstalledPlugins() throws IOException {
        Util.writeProperties("plugins", installedPlugins);
    }

}
