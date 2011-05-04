package com.force.cliforce;

import com.force.sdk.connector.ForceConnectionProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class ForceEnv {

    private String user;
    private String password;
    private String host;
    private String oauthKey;
    private String oauthSecret;
    private boolean valid = true;
    private String message;
    private String configSource;
    private String url;

    // This should always end up being "force"
    private String protocol;

    public ForceEnv(String url, String source) {
        this.url = url;
        this.configSource = source;
        parseAndValidate();
    }

    public ForceEnv(String host, String user, String password) {
        this.url = String.format("force://%s;user=%s;password=%s", host, user, password);
        this.valid = parseAndValidate();
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

            user = map.get(ForceConnectionProperty.USER.getPropertyName());
            if (user == null || user.length() == 0) {
                valid = false;
                message = "User could not be found in URL";
                return false;
            }
            password = map.get(ForceConnectionProperty.PASSWORD.getPropertyName());
            if (password == null || password.length() == 0) {
                valid = false;
                message = "Password could not be found in URL";
                return false;
            }

            oauthKey = map.get(ForceConnectionProperty.OAUTH_KEY.getPropertyName());
            oauthSecret = map.get(ForceConnectionProperty.OAUTH_SECRET.getPropertyName());
            if (oauthKey == null ^ oauthSecret == null) {
                valid = false;
                message = "Both oauth_key and oauth_secret are required";
                return false;
            }

            return true;
        } catch (NoSuchElementException e) {
            valid = false;
            message = "Unable to successfully parse the URL";
            return false;
        }
    }

    public String getOauthKey() {
        return oauthKey;
    }

    public String getOauthSecret() {
        return oauthSecret;
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
     *
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
        if(oauthKey == null ^ forceEnv.oauthKey == null) return false;
        if(oauthSecret == null ^ forceEnv.oauthSecret == null) return false;
        if(oauthKey != null && !oauthKey.equals(forceEnv.oauthKey)) return false;
        if(oauthSecret != null && !oauthSecret.equals(forceEnv.oauthSecret)) return false;

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
