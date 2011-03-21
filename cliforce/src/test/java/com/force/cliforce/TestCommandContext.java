package com.force.cliforce;

import com.sforce.async.RestConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectorConfig;
import com.vmforce.client.VMForceClient;
import com.vmforce.test.util.SfdcTestingUtil;
import com.vmforce.test.util.TestContext;
import com.vmforce.test.util.UserInfo;


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
    private String[] commandArguments = new String[0];
    private CommandReader commandReader;
    private VMForceClient vmForceClient;
    private TestCommandWriter commandWriter = new TestCommandWriter();
    private ForceEnv forceEnv;

    public TestCommandContext() throws Exception {
        partnerConnection = SfdcTestingUtil.getPartnerConnection(TestContext.get().getUserInfo());        
    }

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
    public TestCommandWriter getCommandWriter() {
        return commandWriter;
    }

    @Override
    public ForceEnv getForceEnv() {
        return forceEnv;
    }

    public TestCommandContext withMetadataConnection(MetadataConnection metadataConnection) throws Exception {
        TestCommandContext copy = copy();
        copy.metadataConnection = metadataConnection;
        return copy;
    }

    public TestCommandContext withPartnerConnection(PartnerConnection partnerConnection) throws Exception {
        TestCommandContext copy = copy();
        copy.partnerConnection = partnerConnection;
        return copy;
    }

    public TestCommandContext withRestConnection(RestConnection restConnection) throws Exception {
        TestCommandContext copy = copy();
        copy.restConnection = restConnection;
        return copy;
    }

    public TestCommandContext withCommandArguments(String[] commandArguments) throws Exception {
        TestCommandContext copy = copy();
        copy.commandArguments = commandArguments;
        return copy;
    }

    public TestCommandContext withCommandReader(CommandReader commandReader) throws Exception {
        TestCommandContext copy = copy();
        copy.commandReader = commandReader;
        return copy;
    }

    public TestCommandContext withVmForceClient(VMForceClient vmForceClient) throws Exception {
        TestCommandContext copy = copy();
        copy.vmForceClient = vmForceClient;
        return copy;
    }

    public TestCommandContext withCommandWriter(TestCommandWriter commandWriter) throws Exception {
        TestCommandContext copy = copy();
        copy.commandWriter = commandWriter;
        return copy;
    }

    public TestCommandContext withForceEnv(ForceEnv env) throws Exception {
        TestCommandContext copy = copy();
        copy.forceEnv = env;
        return copy;
    }

    private TestCommandContext copy() throws Exception {
        TestCommandContext tcc = new TestCommandContext();
        tcc.metadataConnection = metadataConnection;
        tcc.restConnection = restConnection;
        tcc.partnerConnection = partnerConnection;
        tcc.commandArguments = commandArguments;
        tcc.commandReader = commandReader;
        tcc.commandWriter = commandWriter;
        tcc.vmForceClient = vmForceClient;
        tcc.forceEnv = forceEnv;
        return tcc;
    }
}
