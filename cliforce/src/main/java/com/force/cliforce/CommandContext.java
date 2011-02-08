package com.force.cliforce;


import com.sforce.async.RestConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.vmforce.client.VMForceClient;

/**
 * Command execution context.
 */
public interface CommandContext {

    MetadataConnection getMetadataConnection();

    PartnerConnection getPartnerConnection();

    RestConnection getRestConnection();

    /**
     * This array will have the arguments passed to a given command, but not the actual command.
     *
     * Example:<p/>
     * user types: "someplugin:somecommand -a somearg -b someotherarg somemainarg
     * getCommandArguments returns {"-a", "somearg", "-b", "someotherarg", "somemainarg"}
     *
     * @return the arguments passed to the command but not the command itself.
     */
    String[] getCommandArguments();

    CommandReader getCommandReader();

    VMForceClient getVmForceClient();

    CommandWriter getCommandWriter();

}
