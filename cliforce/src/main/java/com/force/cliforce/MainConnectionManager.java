package com.force.cliforce;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletException;

import org.apache.commons.httpclient.HttpHost;

import com.force.sdk.connector.ForceConnectorConfig;
import com.force.sdk.connector.ForceServiceConnector;
import com.sforce.ws.ConnectionException;
import com.vmforce.client.VMForceClient;
import com.vmforce.client.connector.RestTemplateConnector;

public class MainConnectionManager implements ConnectionManager {

    private static LazyLogger log = new LazyLogger(MainConnectionManager.class);
    private ConcurrentMap<ForceEnv, EnvConnections> connections = new ConcurrentHashMap<ForceEnv, EnvConnections>();
    private ConcurrentMap<String, ForceEnv> envs = new ConcurrentHashMap<String, ForceEnv>();
    /*key=envName,value=forceUrl*/
    protected Properties envProperties = new Properties();
    protected Properties loginProperties = new Properties();
    private volatile VMForceClient vmForceClient;
    private volatile ForceEnv currentEnv;
    private volatile String currentEnvName;
    private String user;
    private String password;
    private String target;


    @Override
    public VMForceClient getVmForceClient() {
        return vmForceClient;
    }

    @Override
    public ForceEnv getCurrentEnv() {
        return currentEnv;
    }


    @Override
    public void setLogin(String user, String password, String target) {
        this.user = user;
        this.password = password;
        this.target = target;
    }

    @Override
    public String getUser() {
        return this.user;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getTarget() {
        return this.target;
    }

    @Override
    public void saveLogin() throws IOException {
        loginProperties.setProperty(USER, user);
        loginProperties.setProperty(PASSWORD, password);
        loginProperties.setProperty(TARGET, target);
        Util.writeProperties("login", loginProperties);
    }

    @Override
    public void doLogin() {
        VMForceClient forceClient = new VMForceClient();
        RestTemplateConnector restConnector = new RestTemplateConnector();
        restConnector.setTarget(new HttpHost(target));
        restConnector.debug(true);
        forceClient.setHttpConnector(restConnector);
        try {
            forceClient.login(user, password);
        } catch (IOException e) {
            throw new RuntimeException("Failed to login", e);
        } catch (ServletException e) {
            throw new RuntimeException("Failed to login", e);
        }
        vmForceClient = forceClient;
    }

    @Override
    public void loadLogin() throws IOException {
        Util.readProperties("login", loginProperties);
        if (!(loginProperties.containsKey(USER) && loginProperties.containsKey(PASSWORD) && loginProperties.containsKey(TARGET))) {
            throw new IOException("login properties did not contain user, password, and target");
        }
        user = loginProperties.getProperty(USER);
        password = loginProperties.getProperty(PASSWORD);
        target = loginProperties.getProperty(TARGET);
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
                URL purl = new URL(com.sforce.soap.partner.Connector.END_POINT);
                ForceConnectorConfig config = new ForceConnectorConfig();
                config.setAuthEndpoint("https://" + env.getHost() + purl.getPath());
                config.setUsername(env.getUser());
                config.setPassword(env.getPassword());
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
            } catch (MalformedURLException e) {
                log.get().error("MalformedURLException while creating ForceConfig, returning null", e);
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
