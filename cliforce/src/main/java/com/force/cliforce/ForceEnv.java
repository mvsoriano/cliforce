package com.force.cliforce;

import com.force.sdk.connector.ForceConnectionProperty;
import com.force.sdk.connector.ForceConnectorUtils;

import java.util.Map;
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


        try {
            if (!url.startsWith("force://")) {
                StringTokenizer t = new StringTokenizer(url);
                protocol = t.nextToken("://");
                valid = false;
                message = "Unsupported protocol: " + protocol + ". Only 'force' is supported as protocol.";
                return false;
            } else {
                protocol = "force";
            }

            Map<ForceConnectionProperty, String> parsed = ForceConnectorUtils.loadConnectorPropsFromUrl(url);


            host = parsed.get(ForceConnectionProperty.ENDPOINT);
            if (host.length() == 0) {
                valid = false;
                message = "Endpoint could not be found in URL";
                return false;
            }

            user = parsed.get(ForceConnectionProperty.USER);
            if (user == null || user.length() == 0) {
                valid = false;
                message = "User could not be found in URL";
                return false;
            }

            password = parsed.get(ForceConnectionProperty.PASSWORD);
            if (password == null || password.length() == 0) {
                valid = false;
                message = "Password could not be found in URL";
                return false;
            }

            oauthKey = parsed.get(ForceConnectionProperty.OAUTH_KEY);
            oauthSecret = parsed.get(ForceConnectionProperty.OAUTH_SECRET);
            if (oauthKey == null ^ oauthSecret == null) {
                valid = false;
                message = "Both oauth_key and oauth_secret are required";
                return false;
            }

            return true;
        } catch (Exception e) {
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
        if (oauthKey == null ^ forceEnv.oauthKey == null) return false;
        if (oauthSecret == null ^ forceEnv.oauthSecret == null) return false;
        if (oauthKey != null && !oauthKey.equals(forceEnv.oauthKey)) return false;
        if (oauthSecret != null && !oauthSecret.equals(forceEnv.oauthSecret)) return false;

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
