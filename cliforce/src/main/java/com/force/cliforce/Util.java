package com.force.cliforce;


import com.force.cliforce.dependency.DependencyResolver;
import com.sforce.async.BulkConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import org.apache.commons.exec.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Util {

    static LazyLogger log = new LazyLogger(Util.class);


    public static String getCliforceHome() {
        return Boot.getCliforceHome();
    }

    public static String getApiVersion() {
        String url = com.sforce.soap.metadata.Connector.END_POINT;
        String apiVersion = url.substring(url.lastIndexOf("/") + 1);
        return apiVersion;
    }

    public static Double getApiVersionAsDouble() {
        return new Double(getApiVersion());
    }

    /**
     * Read a .force/cliforce_${name} properties file from the current users home directory,
     *
     * @param name       the name to append to the string .force_ to get the properties file name
     * @param properties properties instance into which to read the file
     * @return true if the file was successfully read, false if not.
     */
    public static void readProperties(String name, Properties properties) throws IOException {
        File propFile = getForcePropertiesFile(name);
        if (!propFile.getParentFile().exists()) {
            throw new IOException("Unable to create " + propFile.getCanonicalPath() + " directory");
        }
        if (propFile.exists() || propFile.createNewFile()) {
            FileInputStream fileInputStream = new FileInputStream(propFile);
            properties.load(fileInputStream);
            fileInputStream.close();
        } else {
            throw new IOException("Unable to create file:" + propFile.getAbsolutePath());
        }

    }

    /**
     * Write a .force/cliforce_${name} properties file to the current users home directory.
     *
     * @param name name the name to append to the string .force_ to get the properties file name
     * @return true if the file was successfully written, false if not.
     */
    public static void writeProperties(String name, Properties properties) throws IOException {
        File propFile = getForcePropertiesFile(name);
        if (!propFile.getParentFile().exists()) {
            if (!propFile.getParentFile().mkdir()) {
                throw new IOException("Unable to create ~/.force/ directory");
            }
        }
        if (propFile.exists() || propFile.createNewFile()) {
            FileOutputStream fileOutputStream = new FileOutputStream(propFile);
            properties.store(fileOutputStream, "CLIForce " + name);
            fileOutputStream.close();
        } else {
            throw new IOException("Unable to create file:" + propFile.getAbsolutePath());
        }
    }

    static File getForcePropertiesFile(String name) {
        return new File(getCliforceHome() + "/.force/cliforce_" + name);
    }

    /**
     * parse a command string into a string array, interpreting quoted values as a single string.
     *
     * @param cmd
     * @return
     */
    public static String[] parseCommand(String cmd) {
        if (cmd == null || cmd.equals("")) return new String[]{""};
        CommandLine c = CommandLine.parse(cmd);
        String exe = c.getExecutable();
        String[] args = c.getArguments();
        String[] all = new String[args.length + 1];
        all[0] = exe;
        System.arraycopy(args, 0, all, 1, args.length);
        return all;
    }

    public static void requireForceEnv(CommandContext context) throws ResourceException {
        if (context.getForceEnv() == null) {
            String msg = "Unable to execute the command, since the current force connection is null.\nPlease add a valid connection using connection:add";
            throw new ResourceException(msg);
        }
    }

    public static void requireMetadataConnection(CommandContext context) throws ResourceException {

        String msg = "Unable to execute the command, since the current metadata connection is ";
        MetadataConnection metadataConnection = null;
        try {
            metadataConnection = context.getMetadataConnection();
        } catch (Exception e) {
            log.get().debug("Exception getting metadata connection", e);
            throw new ResourceException(msg + "invalid.\nPlease add a valid connection using connection:add", e);
        }

        if (metadataConnection == null) {
            throw new ResourceException(msg + "null.\nPlease add a valid connection using connection:add");
        }
    }

    public static void requirePartnerConnection(CommandContext context) throws ResourceException {
        String msg = "Unable to execute the command, since the partner connection is ";
        PartnerConnection partnerConnection = null;
        try {
            partnerConnection = context.getPartnerConnection();
        } catch (Exception e) {
            log.get().debug("Exception getting partner conenction", e);
            throw new ResourceException(msg + "invalid.\nPlease add a valid connection using connection:add", e);
        }
        if (partnerConnection == null) {
            throw new ResourceException(msg + "null\nPlease add a valid connection using connection:add");
        }
    }

    public static void requireBulkConnection(CommandContext context) throws ResourceException {
        String msg = "Unable to execute the command, since the bulk connection is ";
        BulkConnection bulkConnection = null;
        try {
            bulkConnection = context.getBulkConnection();
        } catch (Exception e) {
            log.get().debug("Exception getting rest conenction", e);
            throw new ResourceException(msg + "invalid.\nPlease add a valid connection using connection:add", e);
        }
        if (bulkConnection == null) {
            throw new ResourceException(msg + "null.\nPlease add a valid connection using connection:add");
        }
    }

    public static void requireCliforce(CLIForce cliForce) throws ResourceException {
        String msg = "Unable to execute the command, since the injected cliforce instance is null";
        if (cliForce == null) {
            throw new ResourceException(msg);
        }
    }

    public static void requireResolver(DependencyResolver resolver) throws ResourceException {
        String msg = "Unable to execute the command, since the injected dependency resolver instance is null";
        if (resolver == null) {
            throw new ResourceException(msg);
        }
    }


}
