package com.force.cliforce;

import com.sforce.async.RestConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.vmforce.client.VMForceClient;


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
    private RestConnection restConnection;
    private String[] commandArguments;
    private CommandReader commandReader;
    private VMForceClient vmForceClient;
    private CommandWriter commandWriter;


    @Override
    public MetadataConnection getMetadataConnection() {
        return metadataConnection;
    }

    @Override
    public PartnerConnection getPartnerConnection() {
        return partnerConnection;
    }

    @Override
    public RestConnection getRestConnection() {
        return restConnection;
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
    public VMForceClient getVmForceClient() {
        return vmForceClient;
    }

    @Override
    public CommandWriter getCommandWriter() {
        return commandWriter;
    }


    public TestCommandContext withMetadataConnection(MetadataConnection metadataConnection) {
        TestCommandContext copy = copy();
        copy.metadataConnection = metadataConnection;
        return copy;
    }

    public TestCommandContext withPartnerConnection(PartnerConnection partnerConnection) {
        TestCommandContext copy = copy();
        this.partnerConnection = partnerConnection;
        return copy;
    }

    public TestCommandContext withRestConnection(RestConnection restConnection) {
        TestCommandContext copy = copy();
        this.restConnection = restConnection;
        return copy;
    }

    public TestCommandContext withCommandArguments(String[] commandArguments) {
        TestCommandContext copy = copy();
        this.commandArguments = commandArguments;
        return copy;
    }

    public TestCommandContext withCommandReader(CommandReader commandReader) {
        TestCommandContext copy = copy();
        this.commandReader = commandReader;
        return copy;
    }

    public TestCommandContext withVmForceClient(VMForceClient vmForceClient) {
        TestCommandContext copy = copy();
        this.vmForceClient = vmForceClient;
        return copy;
    }

    public TestCommandContext withCommandWriter(CommandWriter commandWriter) {
        TestCommandContext copy = copy();
        this.commandWriter = commandWriter;
        return copy;
    }

    private TestCommandContext copy() {
        TestCommandContext tcc = new TestCommandContext();
        tcc.metadataConnection = metadataConnection;
        tcc.restConnection = restConnection;
        tcc.partnerConnection = partnerConnection;
        tcc.commandArguments = commandArguments;
        tcc.commandReader = commandReader;
        tcc.commandWriter = commandWriter;
        tcc.vmForceClient = vmForceClient;
        return tcc;
    }
}
