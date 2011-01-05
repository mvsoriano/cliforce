package com.force.cliforce;


import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;

import java.io.PrintWriter;

public interface Command {

    public String describe();

    public void execute(PartnerConnection partner, MetadataConnection metadata, PrintWriter output) throws Exception;

}
