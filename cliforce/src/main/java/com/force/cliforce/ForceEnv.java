package com.force.cliforce;

import java.io.File;
import java.util.*;

public class ForceEnv {

    private String user;
    private String password;
    private String host;
    private boolean valid = true;
    private String message;
    private String configSource;
    private String url;

    // This should always end up being "force"
    private String protocol;

    public ForceEnv() {
        this(null);
    }

    public ForceEnv(String url, String source) {
        this.url = url;
        this.configSource = source;
        parseAndValidate();
    }

    public ForceEnv(String host, String user, String password) {
        this.url = String.format("force://%s;user=%s;password=%s", host, user, password);
        this.valid = parseAndValidate();
    }

    public ForceEnv(String namedConfig) {
        String envVar = "FORCE_URL";
        String sysProp = "force.url";
        String envFile = System.getProperty("user.home") + "/.force_url";

        if (namedConfig != null) {
            envVar = "FORCE_" + namedConfig.toUpperCase() + "_URL";
            sysProp = "force." + namedConfig + ".url";
            envFile = System.getProperty("user.home") + "/.force_" + namedConfig + "_url";
        }
        try {
            configSource = "env: " + envVar;
            url = System.getenv(envVar);
            if (url == null) {
                configSource = "sysproperty: " + sysProp;
                url = System.getProperty(sysProp);
                if (url == null) {
                    configSource = "file: " + envFile;
                    // Courtesy of http://stackoverflow.com/questions/3402735/what-is-simplest-way-to-read-a-file-into-string-in-java
                    url = new Scanner(new File(envFile)).useDelimiter("\\Z").next().trim();
                }
            }

            parseAndValidate();


        } catch (Exception e) {
            valid = false;
            message = "Exception: " + e.getMessage();
        }
    }


    public boolean parseAndValidate() {
        //expects force://<host>;user=<user>;password=<password>

        try {
            StringTokenizer t = new StringTokenizer(url);
            Map<String, String> map = new HashMap<String, String>();
            protocol = t.nextToken("://");
            if (!protocol.equalsIgnoreCase("force")) {
                valid = false;
                message = "Unsupported protocol: " + protocol + ". Only 'force' is supported as protocol.";
                return false;
            }

            host = t.nextToken(";").substring(3);
            if (host.length() == 0) {
                valid = false;
                message = "Endpoint could not be found in URL";
                return false;
            }

            while (t.hasMoreTokens()) {
                String key = t.nextToken("=").substring(1);
                String value = t.nextToken(";").substring(1);
                map.put(key, value);
            }

            user = map.get("user");
            if (user == null || user.length() == 0) {
                valid = false;
                message = "User could not be found in URL";
                return false;
            }
            password = map.get("password");
            if (password == null || password.length() == 0) {
                valid = false;
                message = "Password could not be found in URL";
                return false;
            }
            return true;
        } catch (NoSuchElementException e) {
           valid = false;
           message = "Unable to successfully parse the URL";
           return false;
        }
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public String getConfigSource() {
        return configSource;
    }

    public String getUrl() {
        return url;
    }

    public String getProtocol() {
        return protocol;
    }


    /**
     * Equals and hashcode are relied upon so we can use these as a map key to lookup cached connections.
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ForceEnv forceEnv = (ForceEnv) o;

        if (!host.equals(forceEnv.host)) return false;
        if (!password.equals(forceEnv.password)) return false;
        if (!protocol.equals(forceEnv.protocol)) return false;
        if (!user.equals(forceEnv.user)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = user.hashCode();
        result = 31 * result + password.hashCode();
        result = 31 * result + host.hashCode();
        result = 31 * result + protocol.hashCode();
        return result;
    }
}
