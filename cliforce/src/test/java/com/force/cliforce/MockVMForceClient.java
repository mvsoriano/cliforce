package com.force.cliforce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import com.vmforce.client.VMForceClient;
import com.vmforce.client.bean.ApplicationInfo;
import com.vmforce.client.bean.ApplicationInfo.ModelEnum;

public class MockVMForceClient extends VMForceClient {

	private HashMap<String, ApplicationInfo> apps;
	
	public MockVMForceClient() {
		apps = new HashMap<String, ApplicationInfo>();
	}

	public Map createApplication (ApplicationInfo info) {
		if(apps.containsKey(info.getName())){
			apps.remove(info.getName());
		}
		apps.put(info.getName(), info);
		return new HashMap();
	}
	
	public ApplicationInfo getApplication (String appName) {
		if(apps.containsKey(appName)){
			return apps.get(appName);
		}
		return null;
	}
	
	public List<ApplicationInfo> getApplications() {
		return new ArrayList<ApplicationInfo>(apps.values());		
	}
	
	public void deployApplication(String appName, String localPathToAppFile) throws IOException, ServletException {
		if(!apps.containsKey(appName)){
			apps.put(appName, new ApplicationInfo(appName, 1, 512, Collections.singletonList("dummyURL"), ModelEnum.SPRING));			
		}
	}
	
	public void deleteApplication(String appName){
		if(apps.containsKey(appName)){
			apps.remove(appName);
		} else {
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND); //what VMForceClient would do
		}
	}
	
	public void deleteAllApplications() {
		if(apps != null){
			apps.clear();
		}
	}	
}