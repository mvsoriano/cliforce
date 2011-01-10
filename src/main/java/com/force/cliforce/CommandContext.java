package com.force.cliforce;


import com.sforce.async.RestConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;

import java.io.PrintStream;

public interface CommandContext {

    MetadataConnection getMetadataConnection();

    PartnerConnection getPartnerConnection();

    RestConnection getRestConnection();

    String[] getCommandArguments();

    CommandReader getCommandReader();

    PrintStream getCommandWriter();

}
