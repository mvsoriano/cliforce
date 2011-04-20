package com.force.cliforce;

import com.force.sdk.connector.ForceServiceConnector;


import java.io.IOException;
import java.util.Map;


public interface ConnectionManager {
    String TARGET = "target";
    String PASSWORD = "password";
    String USER = "user";
    String DEFAULT_URL_PROP_NAME = "__default__";

    ForceEnv getCurrentEnv();

    Map<String, ForceEnv> getAvailableEnvironments();

    void setAvailableEnvironment(String name, ForceEnv env);

    void setDefaultEnvironment(String name);

    void renameEnvironment(String name, String newname);

    void setCurrentEnvironment(String name);

    String getCurrentEnvironment();

    String getDefaultEnvironment();

    void removeEnvironment(String name);

    void setDebugOnConnections(boolean debug);

    void loadUserConnections() throws IOException;

    ForceServiceConnector getCurrentConnector();
}
