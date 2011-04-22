package com.force.cliforce;

import com.sforce.async.BulkConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;



/**
 * CommandContext for testing, with fluent/immutable interface.
 * Example Usage for a test that only needs a partner conenction and wants to vary command args.
 * <pre>
 * TestCommandContext prototype = new TestCommandContext().withPartnerConnection(myPartnerConnetction);
 * TestCommandContext test1Context = prototype.withCommandArguments(new String[]{"foo","bar"});
 * //...do test 1...
 * TestCommandContext test2Context = prototype.withCommandArguments(new String[]{"foo2","bar2"});
 * //...do test 2...
 * </pre>
 */
public class TestCommandContext implements CommandContext {


    private MetadataConnection metadataConnection;
    private PartnerConnection partnerConnection;
    private BulkConnection bulkConnection;
    private String[] commandArguments = new String[0];
    private CommandReader commandReader;
    private TestCommandWriter commandWriter = new TestCommandWriter();
    private ForceEnv forceEnv;
    private String connectionName;

    @Override
    public MetadataConnection getMetadataConnection() {
        return metadataConnection;
    }

    @Override
    public PartnerConnection getPartnerConnection() {
        return partnerConnection;
    }

    @Override
    public BulkConnection getBulkConnection() {
        return bulkConnection;
    }

    @Override
    public String[] getCommandArguments() {
        return commandArguments;
    }

    @Override
    public CommandReader getCommandReader() {
        return commandReader;
    }


    @Override
    public TestCommandWriter getCommandWriter() {
        return commandWriter;
    }

    @Override
    public ForceEnv getForceEnv() {
        return forceEnv;
    }
    
    @Override
    public String getConnectionName() {
        return connectionName;
    }

    public TestCommandContext withMetadataConnection(MetadataConnection metadataConnection) {
        TestCommandContext copy = copy();
        copy.metadataConnection = metadataConnection;
        return copy;
    }

    public TestCommandContext withPartnerConnection(PartnerConnection partnerConnection) {
        TestCommandContext copy = copy();
        copy.partnerConnection = partnerConnection;
        return copy;
    }

    public TestCommandContext withBulkConnection(BulkConnection bulkConnection) {
        TestCommandContext copy = copy();
        copy.bulkConnection = bulkConnection;
        return copy;
    }

    public TestCommandContext withCommandArguments(String... commandArguments) {
        TestCommandContext copy = copy();
        if (commandArguments != null) {
            copy.commandArguments = commandArguments;
        }
        return copy;
    }

    public void setCommandArguments(String... commandArguments) {
        this.commandArguments = commandArguments;
    }

    public TestCommandContext withCommandReader(CommandReader commandReader) {
        TestCommandContext copy = copy();
        copy.commandReader = commandReader;
        return copy;
    }

    public TestCommandContext withTestCommandReader(TestCommandReader commandReader) {
        TestCommandContext copy = copy();
        copy.commandReader = commandReader;
        copy.commandWriter = new TestCommandWriter();
        commandReader.setCommandWriter(copy.commandWriter);
        return copy;
    }

    public TestCommandContext withCommandWriter(TestCommandWriter commandWriter) {
        TestCommandContext copy = copy();
        copy.commandWriter = commandWriter;
        return copy;
    }

    public TestCommandContext withForceEnv(ForceEnv env) {
        TestCommandContext copy = copy();
        copy.forceEnv = env;
        return copy;
    }

    public TestCommandContext withConnectionName(String connectionName) {
        TestCommandContext copy = copy();
        copy.connectionName = connectionName;
        return copy;
    }
    
    private TestCommandContext copy() {
        TestCommandContext tcc = new TestCommandContext();
        tcc.metadataConnection = metadataConnection;
        tcc.bulkConnection = bulkConnection;
        tcc.partnerConnection = partnerConnection;
        tcc.commandArguments = commandArguments;
        tcc.commandReader = commandReader;
        tcc.forceEnv = forceEnv;
        tcc.connectionName = connectionName;
        return tcc;
    }

    public String out() {
        return commandWriter.getOutput();
    }
}
