package com.force.cliforce;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Util {

    public static String getApiVersion() {
        String url = com.sforce.soap.metadata.Connector.END_POINT;
        String apiVersion = url.substring(url.lastIndexOf("/") + 1);
        return apiVersion;
    }

    public static Double getApiVersionAsDouble() {
        return new Double(getApiVersion());
    }

    /**
     * Read a .force_${name} properties file from the current users home directory,
     *
     * @param name       the name to append to the string .force_ to get the properties file name
     * @param properties properties instance into which to read the file
     * @return true if the file was successfully read, false if not.
     */
    public static boolean readProperties(String name, Properties properties) throws IOException {
        File propFile = getForcePropertiesFile(name);
        if (propFile.exists() || propFile.createNewFile()) {
            FileInputStream fileInputStream = new FileInputStream(propFile);
            properties.load(fileInputStream);
            fileInputStream.close();
            return true;
        } else {
            return false;
        }

    }

    /**
     * Write a .force_${name} properties file to the current users home directory.
     *
     * @param name name the name to append to the string .force_ to get the properties file name
     * @return true if the file was successfully written, false if not.
     */
    public static boolean writeProperties(String name, Properties properties) throws IOException {
        File propFile = getForcePropertiesFile(name);
        if (propFile.exists() || propFile.createNewFile()) {
            FileOutputStream fileOutputStream = new FileOutputStream(propFile);
            properties.store(fileOutputStream, "CLIForce " + name);
            fileOutputStream.close();
            return true;
        } else {
            return false;
        }
    }

    static File getForcePropertiesFile(String name) {
        return new File(System.getProperty("user.home") + "/.force_" + name);
    }


}
