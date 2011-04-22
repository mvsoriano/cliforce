package com.force.cliforce;


import com.sforce.async.BulkConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;


/**
 * Command execution context.
 */
public interface CommandContext {

    /**
     * can be null
     *
     * @return
     */
    MetadataConnection getMetadataConnection();

    /**
     * can be null
     *
     * @return
     */
    PartnerConnection getPartnerConnection();

    /**
     * can be null
     *
     * @return
     */
    BulkConnection getBulkConnection();

    /**
     * can be null
     *
     * @return
     */
    ForceEnv getForceEnv();

    /**
     * This array will have the arguments passed to a given command, but not the actual command.
     * <p/>
     * Example:<p/>
     * user types: "someplugin:somecommand -a somearg -b someotherarg somemainarg
     * getCommandArguments returns {"-a", "somearg", "-b", "someotherarg", "somemainarg"}
     *
     * @return the arguments passed to the command but not the command itself.
     */
    String[] getCommandArguments();

    CommandReader getCommandReader();

    CommandWriter getCommandWriter();
    
    /**
     * can be null
     * 
     * @return
     */
    String getConnectionName();
}
