/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.force.cliforce;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.servlet.ServletException;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;
import jline.console.history.History;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.util.StatusPrinter;

import com.force.sdk.connector.ForceServiceConnector;
import com.google.inject.Guice;
import com.google.inject.name.Named;
import com.sforce.async.AsyncApiException;
import com.sforce.async.BulkConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.ws.ConnectionException;


public class CLIForce {

    public static final String FORCE_PROMPT = "force> ";
    public static final String INTERNAL_PLUGINS = "internalPlugins";
    public static final String STARTUP_EXECUTOR = "startupExecutor";
    public static final String EXIT_CMD = "exit";
    public static final String BANNER_CMD = "banner";
    public static final String LOGIN_CMD = "login";
    public static final String PLUGIN_CMD = "plugin";
    public static LazyLogger log = new LazyLogger(CLIForce.class);

    private volatile boolean debug = false;

    private ConsoleReader reader;
    private CommandReader commandReader;
    private CommandWriter writer;

    @Inject
    private Completer completor;
    @Inject
    private ConnectionManager connectionManager;
    @Inject
    private PluginManager pluginManager;
    @Inject
    @Named(INTERNAL_PLUGINS)
    private Set<String> internalPlugins;
    @Inject
    private DefaultPlugin def;
    @Inject
    @Named(STARTUP_EXECUTOR)
    private ExecutorService startupExecutor;

    private List<SetupTask> setupTasks = new ArrayList<SetupTask>(16);
    /*
    used to coordinate background loading of plugins at startup
    we start up and get to the prompt fast, and wait on this latch to actually exec the first command.
    we dont construct it here since we want the latch size to match the number of setupTasks.
    */
    private CountDownLatch initLatch;


    public static void main(String[] args) {
        setupLogging();
        CLIForce cliForce = Guice.createInjector(new MainModule()).getInstance(CLIForce.class);

        try {
            cliForce.init(System.in, new PrintWriter(
                    new OutputStreamWriter(System.out,
                            System.getProperty("jline.WindowsTerminal.output.encoding", System.getProperty("file.encoding"))), true));
            if (args.length == 0) {
                cliForce.run();
            } else {
                cliForce.executeWithArgs(args);
            }
        } catch (ConnectionException e) {
            log.get().error("Connection Exception while initializing cliforce, exiting", e);
            System.exit(1);
        } catch (IOException e) {
            log.get().error("IOException Exception while initializing cliforce, exiting", e);
            System.exit(1);
        } catch (ServletException e) {
            log.get().error("ServletException Exception while initializing cliforce, exiting", e);
            System.exit(1);
        } catch (InterruptedException e) {
            log.get().error("Main Thread Interrupted while waiting for plugin initialization", e);
            System.exit(1);
        } catch (ExitException e) {
            log.get().error("ExitException->Exiting");
            System.exit(1);
        } catch (Throwable t) {
            log.get().error("Caught generic error", t);
            System.exit(1);
        }
    }

    /* package */ static void setupLogging() {
        System.setProperty("logback.configurationFile", System.getProperty("logback.configurationFile", "logback.xml"));
        StatusPrinter.setPrintStream(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                //logback 0.9.28 barfs some stuff at startup. This supresses it.
            }
        }));
        //we redirect stderr to a file because slf4j can sometimes decide to write to
        //system.err on startup due to timing issues during init.
        try {
            File errors = new File(Util.getCliforceHome() + "/.force/cliforce.errors");
            if (errors.exists() || errors.createNewFile()) {
                System.setErr(new PrintStream(new FileOutputStream(errors, true))); // append errors to existing file
            }
        } catch (IOException e) {
            //Swallow, if this happens, there is a possibility we will get SLF4J output during startup on the console.
        }

    }


    /*package*/ void setWriter(CommandWriter writer) {
        this.writer = writer;
    }

    /*package*/ void setReader(CommandReader reader) {
        this.commandReader = reader;
    }


    public void init(InputStream in, PrintWriter out) throws IOException, ConnectionException, ServletException {
        SLF4JBridgeHandler.install();
        injectDefaultPluginAndAddCommands();

        reader = new ConsoleReader(in, out);
        reader.addCompleter(completor);
        writer = new Writer(out);

        setupHistory(reader, out);

        reader.setBellEnabled(false);
        commandReader = new Reader();


        addSetupTask(new SetupTask() {
            @Override
            public void setup() {
                try {
                    DefaultPlugin.PluginCommand p = (DefaultPlugin.PluginCommand) pluginManager.getCommand(PLUGIN_CMD);
                    for (String defalutPlugin : internalPlugins) {
                        DefaultPlugin.PluginArgs args = new DefaultPlugin.PluginArgs();
                        args.setArtifact(defalutPlugin);
                        args.version = "LATEST";
                        args.internal = true;
                        p.executeWithArgs(getContext(new String[0]), args);
                    }
                    loadInstalledPlugins();
                } catch (IOException e) {
                    log.get().error("IOException while loading previously installed plugins", e);
                }
            }
        });

        addSetupTask(new SetupTask() {
            @Override
            public void setup() {
                try {
                    connectionManager.loadUserConnections();
                } catch (IOException e) {
                    log.get().error("IOException while loading force urls", e);
                }
            }
        });
        executeSetupTasks();

    }

    private void setupHistory(ConsoleReader r, PrintWriter o) throws IOException {
        File hist = new File(Util.getCliforceHome() + "/.force/cliforce_history");
        if (!hist.getParentFile().exists()) {
            if (!hist.getParentFile().mkdir()) {
                o.println("can't create .force directory");
            }
        }
        if (!hist.exists()) {
            try {
                if (hist.createNewFile()) {
                    reader.setHistory(new FileHistory(hist));
                } else {
                    o.println("can't create history file");
                }
            } catch (IOException e) {
                o.println("can't create history file");
            }

        } else {
            final FileHistory history = new FileHistory(hist);
            Runtime.getRuntime().addShutdownHook(new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                history.flush();
                            } catch (IOException e) {
                                writer.println("Unable to save command history.");
                            }
                        }
                    }
            ));
            r.setHistory(history);
        }
    }

    private void addSetupTask(SetupTask task) {
        setupTasks.add(task);
    }

    private void executeSetupTasks() {
        initLatch = new CountDownLatch(setupTasks.size());
        for (SetupTask setupTask : setupTasks) {
            startupExecutor.submit(setupTask);
        }
    }

    /**
     * Main run loop.
     *
     * @throws InterruptedException
     */
    public void run() throws InterruptedException {
        try {
            pluginManager.getCommand(BANNER_CMD).execute(getContext(new String[0]));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        while (true) {
            String[] cmds = commandReader.readAndParseLine(FORCE_PROMPT);
            if (cmds.length == 0 || EXIT_CMD.equals(cmds[0])) break;//exit
            initLatch.await();
            executeWithArgs(cmds);
        }
    }


    public void executeWithArgs(String... cmds) throws InterruptedException {
        String cmdKey = cmds[0];
        if (!cmdKey.equals(EXIT_CMD)) {
            //we dont wait on the latch if somone runs cliforce exit.
            //this is useful to measure "startup time to get to the prompt"
            //by running> time cliforce exit
            initLatch.await();
        }
        Command cmd = pluginManager.getCommand(cmdKey);
        String[] args = cmds.length > 1 ? Arrays.copyOfRange(cmds, 1, cmds.length) : new String[0];
        if (!cmdKey.equals("") && !cmdKey.equals(EXIT_CMD)) {
            if (cmd != null) {
                executeCommand(cmdKey, cmd, args);
            } else {
                writer.printf("Unknown Command %s\n", cmdKey);
            }
        }
    }

    private void executeCommand(String cmdKey, Command cmd, String[] args) {
        ClassLoader curr = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cmd.getClass().getClassLoader());
            cmd.execute(getContext(args));
        } catch (ExitException e) {
            writer.println("Exit Exception thrown, exiting");
            throw e;
        } catch (ResourceException e) {
            writer.println(e.getMessage());
            log.get().debug("ResourceException while executing command", e);
            if (!isDebug()) {
                writer.println("execute debug and retry to see failure information");
            }
        } catch (Exception e) {
            writer.printf("Exception while executing command %s\n", cmdKey);
            writer.printStackTrace(e);
        } finally {
            Thread.currentThread().setContextClassLoader(curr);
        }
    }


    /**
     * Return a map of installed plugins' maven artifactId->version
     *
     * @return
     */
    public Map<String, String> getInstalledPlugins() {
        return pluginManager.getInstalledPlugins();
    }

    public List<String> getActivePlugins() {
        return pluginManager.getActivePlugins();
    }

    public Set<String> getInternalPlugins() {
        return internalPlugins;
    }


    /**
     * return the currently installed version of a plugin or null if not installed.
     *
     * @param plugin
     * @return
     */
    public String getInstalledPluginVersion(String plugin) {
        return pluginManager.getInstalledPluginVersion(plugin);
    }

    /*package*/ void installPlugin(String artifact, String version, Plugin p, boolean internal) throws IOException {

        pluginManager.installPlugin(artifact, version, p, internal);
        if (!internal && initLatch.getCount() == 0) {
            List<Command> pluginCommands = pluginManager.getPluginCommands(artifact);
            writer.printf("Plugin: %s installed\n", artifact);
            writer.println("Adds the following commands");
            for (Command pluginCommand : pluginCommands) {
                writer.println(pluginCommand.name());
            }

        }
    }

    private void injectDefaultPluginAndAddCommands() {
        pluginManager.injectDefaultPluginAndAddCommands(def);
    }


    /*package*/ void removePlugin(String artifactId) throws IOException {

        List<Command> pluginCommands = pluginManager.getPluginCommands(artifactId);
        if (pluginCommands.size() == 0) {
            writer.println("....not found");
        } else {
            for (Command command : pluginCommands) {
                writer.printf("\tremoved command: %s\n", command.name());
            }
            pluginManager.removePlugin(artifactId);
            writer.println("Done");
        }

    }

    public List<URL> getClasspathForPlugin(String plugin) {
        return pluginManager.getClasspathForPlugin(plugin);
    }

    public Map<String, String> getCommandDescriptions() {
        return pluginManager.getCommandDescriptions();
    }

    public List<String> getHistoryList() {
        List<String> strings = new ArrayList<String>();
        ListIterator<History.Entry> entries = reader.getHistory().entries();
        while (entries.hasNext()) {
            History.Entry next = entries.next();
            strings.add(next.value().toString());
        }
        return strings;
    }

    private void loadInstalledPlugins() throws IOException {
        pluginManager.loadInstalledPlugins();
        DefaultPlugin.PluginCommand p = (DefaultPlugin.PluginCommand) pluginManager.getCommand("plugin");
        for (String artifact : pluginManager.getInstalledPlugins().keySet()) {
            String version = pluginManager.getInstalledPluginVersion(artifact);
            DefaultPlugin.PluginArgs args = new DefaultPlugin.PluginArgs();
            args.setArtifact(artifact);
            args.version = version;
            p.executeWithArgs(getContext(new String[0]), args);
        }
    }


    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        connectionManager.setDebugOnConnections(debug);
        Level level = Level.DEBUG;
        if (!debug) {
            level = Level.OFF;
        }
        writer.printf("Setting logger level to %s\n", level.levelStr);
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(level);
    }

    public Map<String, ForceEnv> getAvailableEnvironments() {
        return connectionManager.getAvailableEnvironments();
    }

    public void setAvailableEnvironment(String name, ForceEnv env) {
        connectionManager.setAvailableEnvironment(name, env);
    }

    public void setDefaultEnvironment(String name) {
        connectionManager.setDefaultEnvironment(name);
    }

    public void renameEnvironment(String name, String newname) {
        connectionManager.renameEnvironment(name, newname);
    }

    public void setCurrentEnvironment(String name) {
        connectionManager.setCurrentEnvironment(name);
    }

    public String getCurrentEnvironment() {
        return connectionManager.getCurrentEnvironment();
    }

    public String getDefaultEnvironment() {
        return connectionManager.getDefaultEnvironment();
    }

    public void removeEnvironment(String name) {
        connectionManager.removeEnvironment(name);
    }


    CommandContext getContext(String[] args) {
        ForceEnv currentEnv = connectionManager.getCurrentEnv();
        ForceServiceConnector connector = connectionManager.getCurrentConnector();

        if (connector != null) {
            return new Context(currentEnv, connector,  args, commandReader, writer);
        } else {
            if (initLatch.getCount() == 0) {
                log.get().warn("Couldn't get a valid connection for the current force url. Executing the command without force service connector");
            }
            return new Context(currentEnv, null, args, commandReader, writer);
        }
    }


    /**
     * cliforce internal impl of CommandContext
     */
    static class Context implements CommandContext {

        ForceServiceConnector connector;
        String[] args;
        CommandReader reader;
        CommandWriter writer;
        ForceEnv forceEnv;


        private Context(ForceEnv env, ForceServiceConnector conn, String[] args, CommandReader reader, CommandWriter writer) {
            this.args = args;
            this.reader = reader;
            this.writer = writer;
            this.forceEnv = env;
            this.connector = conn;
        }

        @Override
        public ForceEnv getForceEnv() {
            return forceEnv;
        }

        @Override
        public MetadataConnection getMetadataConnection() {
            try {
                return connector.getMetadataConnection();
            } catch (ConnectionException e) {
                log.get().error("Connection exception while getting metadata connection", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public PartnerConnection getPartnerConnection() {
            try {
                return connector.getConnection();
            } catch (ConnectionException e) {
                log.get().error("ConnectionException while getting metadata connection", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public BulkConnection getBulkConnection() {
            try {
                return connector.getBulkConnection();
            } catch (AsyncApiException e) {
                log.get().error("AsyncApiException exception while getting rest connection", e);
                throw new RuntimeException(e);
            } catch (ConnectionException e) {
                log.get().error("ConnectionException exception while getting rest connection", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public String[] getCommandArguments() {
            return args;
        }

        @Override
        public CommandReader getCommandReader() {
            return reader;
        }

        @Override
        public CommandWriter getCommandWriter() {
            return writer;
        }
        
        @Override
        public String getConnectionName() {
            return connector.getConnectionName();
        }
    }

    /**
     * cliforce internal impl of CommandWriter. Usually wraps system.out.
     */
    public class Writer implements CommandWriter {

        private PrintWriter out;

        public Writer(PrintWriter out) {
            this.out = out;
        }

        @Override
        public void printf(String format, Object... args) {
            out.printf(format, args);
        }

        @Override
        public void print(String msg) {
            out.print(msg);
            out.flush();
        }

        @Override
        public void println(String msg) {
            out.println(msg);
        }

        @Override
        public void printExceptionMessage(Exception e, boolean newLine) {
            String exceptionMessage;
            if (e instanceof ApiFault) {
                ApiFault af = (ApiFault)e;
                exceptionMessage = af.getExceptionMessage();
            } else {
                exceptionMessage = e.getMessage();
            }
            
            if (newLine) {
                println(exceptionMessage);
            } else {
                print(exceptionMessage);
            }
        }
        
        @Override
        public void printStackTrace(Exception e) {
            e.printStackTrace(out);
        }
    }

    /**
     * cliforce internal impl of CommandReader, uses apache commons-cli to parse command lines
     */
    private class Reader implements CommandReader {
        @Override
        public String readLine(final String prompt) {
            try {
                while (true) {
                    try {
                        return reader.readLine(prompt);
                    } catch (IllegalArgumentException e) {
                        writer.println(e.getMessage());
                        reader.getCursorBuffer().clear();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String[] readAndParseLine(String prompt) {
            try {
                while (true) {
                    try {
                        String line = reader.readLine(prompt);
                        if (line == null) line = EXIT_CMD;
                        return Util.parseCommand(line);
                    } catch (IllegalArgumentException e) {
                        writer.println(e.getMessage());
                        reader.getCursorBuffer().clear();
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String readLine(String prompt, Character mask) {
            try {
                while (true) {
                    try {
                        return reader.readLine(prompt, mask);
                    } catch (IllegalArgumentException e) {
                        writer.println(e.getMessage());
                        reader.getCursorBuffer().clear();
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * base class for tasks that should be run asynchronously on startup.
     * <p/>
     * need to instantiate these and add them with addSetupTask(task) in init(), sometime before
     * executeSetupTasks is called.
     */
    private abstract class SetupTask implements Runnable {

        public abstract void setup();

        @Override
        public void run() {
            try {
                setup();
            } finally {
                initLatch.countDown();
            }
        }
    }

}
