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


import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


import com.force.sdk.connector.ForceConnectorConfig;
import com.force.sdk.connector.ForceServiceConnector;
import com.sforce.ws.ConnectionException;

public class MainConnectionManager implements ConnectionManager {

    private static LazyLogger log = new LazyLogger(MainConnectionManager.class);
    private ConcurrentMap<ForceEnv, EnvConnections> connections = new ConcurrentHashMap<ForceEnv, EnvConnections>();
    private ConcurrentMap<String, ForceEnv> envs = new ConcurrentHashMap<String, ForceEnv>();
    /*key=envName,value=forceUrl*/
    protected Properties envProperties = new Properties();
    protected Properties loginProperties = new Properties();
    private volatile ForceEnv currentEnv;
    private volatile String currentEnvName;


    @Override
    public ForceEnv getCurrentEnv() {
        return currentEnv;
    }

     @Override
    public Map<String, ForceEnv> getAvailableEnvironments() {
        return Collections.unmodifiableMap(envs);
    }

    @Override
    public void setAvailableEnvironment(String name, ForceEnv env) {
        if (env.isValid()) {
            envProperties.setProperty(name, env.getUrl());
            ForceEnv old = envs.put(name, env);
            if (old != null) {
                connections.remove(old);
            }
            writeForceUrls();
        }
    }

    @Override
    public void setDefaultEnvironment(String name) {
        if (envProperties.containsKey(name)) {
            envProperties.setProperty(DEFAULT_URL_PROP_NAME, name);
            writeForceUrls();
        }
    }

    @Override
    public void renameEnvironment(String name, String newname) {
        if (envProperties.containsKey(name)) {
            envProperties.setProperty(newname, envProperties.getProperty(name));
            envProperties.remove(name);
            if (envProperties.getProperty(DEFAULT_URL_PROP_NAME, "").equals(name)) {
                envProperties.setProperty(DEFAULT_URL_PROP_NAME, newname);
            }
            envs.put(newname, envs.remove(name));
            if (currentEnvName != null && currentEnvName.equals(name)) {
                currentEnvName = newname;
            }
            writeForceUrls();
        }
    }

    @Override
    public void setCurrentEnvironment(String name) {
        if (envProperties.containsKey(name)) {
            currentEnv = envs.get(name);
            currentEnvName = name;
        }
    }

    @Override
    public String getCurrentEnvironment() {
        return currentEnvName;
    }

    @Override
    public String getDefaultEnvironment() {
        return envProperties.getProperty(DEFAULT_URL_PROP_NAME, "<no default selected>");
    }

    @Override
    public void removeEnvironment(String name) {
        if (envProperties.containsKey(name)) {
            envProperties.remove(name);
            if (envProperties.getProperty(DEFAULT_URL_PROP_NAME, "").equals(name)) {
                envProperties.remove(DEFAULT_URL_PROP_NAME);
            }
            ForceEnv env = envs.remove(name);
            connections.remove(env);
            writeForceUrls();
            if (name != null && name.equals(currentEnvName)) {
                currentEnv = null;
                currentEnvName = null;
            }

        }
    }

    private void writeForceUrls() {
        try {
            Util.writeProperties("urls", envProperties);
        } catch (IOException e) {
            log.get().error("Exception persisting new environment settings", e);
        }
    }

    @Override
    public void setDebugOnConnections(boolean debug) {
        for (EnvConnections envConnections : connections.values()) {
            envConnections.config.setTraceMessage(debug);
        }
    }

    @Override
    public void loadUserConnections() throws IOException {
        Util.readProperties("urls", envProperties);
        for (String s : envProperties.stringPropertyNames()) {
            if (s.equals(DEFAULT_URL_PROP_NAME)) continue;
            String url = envProperties.getProperty(s);
            ForceEnv env = new ForceEnv(url, ".force_urls");
            if (env.isValid()) {
                envs.put(s, env);
            }
        }

        String defaultEnv = envProperties.getProperty(DEFAULT_URL_PROP_NAME);
        if (defaultEnv != null) {
            currentEnv = envs.get(defaultEnv);
            currentEnvName = defaultEnv;
        } else {
            if (envProperties.size() == 1) {
                defaultEnv = envProperties.stringPropertyNames().iterator().next();
                currentEnvName = defaultEnv;
                currentEnv = envs.get(defaultEnv);
                envProperties.setProperty(DEFAULT_URL_PROP_NAME, defaultEnv);
                Util.writeProperties("urls", envProperties);
            }
        }
    }

    @Override
    public ForceServiceConnector getCurrentConnector() {
        ForceEnv env = currentEnv;
        if (env == null || !env.isValid()) return null;
        EnvConnections current = connections.get(env);
        if (current == null) {
            try {
                ForceConnectorConfig config = new ForceConnectorConfig();
                config.setConnectionUrl(env.getUrl());
                config.setTraceMessage(false);
                config.setPrettyPrintXml(true);
                ForceServiceConnector connector = new ForceServiceConnector(config);
                connector.setConnectionName(getCurrentEnvironment());
                current = new EnvConnections(config, connector);
                EnvConnections prev = connections.putIfAbsent(env, current);
                return prev == null ? current.forceServiceConnector : prev.forceServiceConnector;
            } catch (ConnectionException e) {
                log.get().error("ConnectionException while creating ForceConfig, returning null", e);
                return null;
            }
        } else {
            return current.forceServiceConnector;
        }
    }

    private static class EnvConnections {
        public final ForceConnectorConfig config;
        public final ForceServiceConnector forceServiceConnector;

        private EnvConnections(ForceConnectorConfig config, ForceServiceConnector forceServiceConnector) {
            this.config = config;
            this.forceServiceConnector = forceServiceConnector;
        }
    }


}
