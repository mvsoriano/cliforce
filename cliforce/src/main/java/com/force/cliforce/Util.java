package com.force.cliforce;


public class Util {

    public static String getApiVersion() {
        String[] arr = com.sforce.soap.metadata.Connector.END_POINT.split("/");
        String apiVersion = arr[arr.length - 1];
        return apiVersion;
    }

    public static Double getApiVersionAsDouble() {
        return new Double(getApiVersion());
    }

}
