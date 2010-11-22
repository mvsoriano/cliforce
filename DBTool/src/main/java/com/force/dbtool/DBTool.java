package com.force.dbtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class DBTool {

	static final String[] parms = new String[] { "force.userName", "force.password", "force.apiVersion", "force.endPoint" };
	
	static public void main(String[] args) {

		boolean verbose = "true".equals(System.getProperty("verbose"));

		File config = new File(System.getProperty("user.home")+"/.force_config");

		Properties props = new Properties();
		if(config.exists()) {
			try {
				props.load(new FileInputStream(config));
			} catch (FileNotFoundException e) {} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		boolean missingProps = false;
		for(String s : parms) {
			String value = System.getProperty(s);
			if(value!=null) {
				if(verbose) System.out.println("Property "+s+" was provided as system property. It will take presedence over properties in .force_config file in home directory");
				props.setProperty(s,value);
			} else if(props.getProperty(s)==null) {
				System.out.println("The property "+s+" must be specified, either in the file .force_config in your home directory or as a system property, e.g by passing -D"+s+"=<value> on the command line");
				missingProps = true;
			}
		}
		if(missingProps) {
			return;
		}
					
		try {


			ConnectorConfig partnerConfig = new ConnectorConfig();
			partnerConfig.setAuthEndpoint(props.getProperty("force.endPoint") + "/services/Soap/u/"
					+ props.getProperty("force.apiVersion"));
			partnerConfig.setUsername(props.getProperty("force.userName"));
			partnerConfig.setPassword(props.getProperty("force.password"));
			partnerConfig.setTraceMessage("true".equals(System.getProperty("force.trace")));

			PartnerConnection partner = new PartnerConnection(partnerConfig);

			ConnectorConfig mdConfig = new ConnectorConfig();
			mdConfig.setSessionId(partnerConfig.getSessionId());
			mdConfig.setServiceEndpoint(props.getProperty("force.endPoint") + "/services/Soap/m/"
					+ props.getProperty("force.apiVersion"));
			mdConfig.setTraceMessage("true".equals(System.getProperty("force.trace")));
			MetadataConnection md = new MetadataConnection(mdConfig);
			
			DBClean task = new DBClean();
			task.setPartnerConnection(partner);
			task.setMetadataConnection(md);
			task.execute();
			
		} catch (ConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
