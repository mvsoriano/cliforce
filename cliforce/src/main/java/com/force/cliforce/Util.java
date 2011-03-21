package com.force.cliforce;


import com.force.cliforce.dependency.DependencyResolver;
import org.apache.commons.exec.CommandLine;

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
     * Read a .force/cliforce_${name} properties file from the current users home directory,
     *
     * @param name       the name to append to the string .force_ to get the properties file name
     * @param properties properties instance into which to read the file
     * @return true if the file was successfully read, false if not.
     */
    public static void readProperties(String name, Properties properties) throws IOException {
        File propFile = getForcePropertiesFile(name);
        if (!propFile.getParentFile().exists()) {
            throw new IOException("Unable to create ~/.force/ directory");
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
        return new File(System.getProperty("user.home") + "/.force/cliforce_" + name);
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

    public static void requireVMForceClient(CommandContext context) throws ResourceException {
        String msg = "Unable to execute the command, since the VMForceClient is null";
        if (context.getVmForceClient() == null) {
            if (context.getCommandWriter() != null) {
                context.getCommandWriter().println(msg);
            }
            throw new ResourceException(msg);
        }
    }

    public static void requireForceEnv(CommandContext context) throws ResourceException {
        String msg = "Unable to execute the command, since the ForceEnv is null";
        if (context.getForceEnv() == null) {
            if (context.getCommandWriter() != null) {
                context.getCommandWriter().println(msg);
            }
            throw new ResourceException(msg);
        }
    }

    public static void requireMetadataConnection(CommandContext context) throws ResourceException {
        String msg = "Unable to execute the command, since the metadata connection is null";
        if (context.getMetadataConnection() == null) {
            if (context.getCommandWriter() != null) {
                context.getCommandWriter().println(msg);
            }
            throw new ResourceException(msg);
        }
    }

    public static void requirePartnerConnection(CommandContext context) throws ResourceException {
        String msg = "Unable to execute the command, since the partner connection is null";
        if (context.getPartnerConnection() == null) {
            if (context.getCommandWriter() != null) {
                context.getCommandWriter().println(msg);
            }
            throw new ResourceException(msg);
        }
    }

    public static void requireRestConnection(CommandContext context) throws ResourceException {
        String msg = "Unable to execute the command, since the rest connection is null";
        if (context.getRestConnection() == null) {
            if (context.getCommandWriter() != null) {
                context.getCommandWriter().println(msg);
            }
            throw new ResourceException(msg);
        }
    }

    public static void requireCliforce(CLIForce cliForce, CommandContext context) throws ResourceException {
        String msg = "Unable to execute the command, since the injected cliforce instance is null";
        if (cliForce == null) {
            if (context.getCommandWriter() != null) {
                context.getCommandWriter().println(msg);
            }
            throw new ResourceException(msg);
        }
    }

    public static void requireResolver(DependencyResolver resolver, CommandContext context) throws ResourceException {
        String msg = "Unable to execute the command, since the injected dependency resolver instance is null";
        if (resolver == null) {
            if (context.getCommandWriter() != null) {
                context.getCommandWriter().println(msg);
            }
            throw new ResourceException(msg);
        }
    }


}
