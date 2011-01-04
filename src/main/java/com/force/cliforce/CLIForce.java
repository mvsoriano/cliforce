package com.force.cliforce;

import java.io.IOException;
import java.net.URL;

import com.sforce.soap.metadata.Connector;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class CLIForce {

	static public void main(String[] args) {

		ForceEnv env = new ForceEnv();
		
		if(!env.isValid()) {
			System.out.println("Could not find a proper configuration.");
			System.out.println("Tried to use config source: "+env.getConfigSource());
			System.out.println("Got the following message: ");
			System.out.println(env.getMessage());
			System.out.println("Found URL: "+env.getUrl());
			return;
		}
		
		try {

			URL purl = new URL(com.sforce.soap.partner.Connector.END_POINT);
			URL murl = new URL(com.sforce.soap.metadata.Connector.END_POINT);
			
			ConnectorConfig partnerConfig = new ConnectorConfig();
			partnerConfig.setAuthEndpoint("https://"+env.getHost()+ purl.getPath());
			partnerConfig.setUsername(env.getUser());
			partnerConfig.setPassword(env.getPassword());
			partnerConfig.setTraceMessage("true".equals(System.getProperty("force.trace")));

			PartnerConnection partner = new PartnerConnection(partnerConfig);

			ConnectorConfig mdConfig = new ConnectorConfig();
			mdConfig.setSessionId(partnerConfig.getSessionId());
			mdConfig.setServiceEndpoint("https://"+env.getHost() + murl.getPath());
			mdConfig.setTraceMessage("true".equals(System.getProperty("force.trace")));
			MetadataConnection md = new MetadataConnection(mdConfig);

			// TODO: Now we just have the dbclean task, so we hardcode everything here. 
			// But it should be expanded to a generic task execution environment.

			if(args.length>0 && args[0].equals("dbclean")) {
			
				DBClean task = new DBClean();
				task.setPartnerConnection(partner);
				task.setMetadataConnection(md);
				task.execute();

			} else {
				System.out.println("Only one task supported:\n");
				System.out.println("\tdbclean\t\tDeletes all entity definitions in the database (warning: it will delete without asking for confirmation)");
			}
			
		} catch (ConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
