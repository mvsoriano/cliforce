package com.force.cliforce;


public class Util {

    public static String getApiVersion() {
        String url = com.sforce.soap.metadata.Connector.END_POINT;
        String apiVersion = url.substring(url.lastIndexOf("/") + 1);
        return apiVersion;
    }

    public static Double getApiVersionAsDouble() {
        return new Double(getApiVersion());
    }

}
