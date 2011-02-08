package com.force.cliforce;

/**
 * Commands that implement this interface will have the current force env set on them.
 */
public interface ForceEnvAware {

    void setForceEnv(ForceEnv env);

}
