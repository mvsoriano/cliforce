package com.force.cliforce;


import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;

import java.io.OutputStream;
import java.io.PrintStream;

public interface CommandContext {

    MetadataConnection getMetadataConnection();

    PartnerConnection getPartnerConnection();

    String[] getCommandArguments();

    CommandReader getCommandReader();

    PrintStream getCommandWriter();

}
