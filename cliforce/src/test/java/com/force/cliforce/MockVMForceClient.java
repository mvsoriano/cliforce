package com.force.cliforce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import mockit.Mock;
import mockit.MockClass;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import com.vmforce.client.VMForceClient;
import com.vmforce.client.bean.ApplicationInfo;
import com.vmforce.client.bean.ApplicationInfo.ModelEnum;


@MockClass(realClass = VMForceClient.class)
public class MockVMForceClient {

	private HashMap<String, ApplicationInfo> apps;
	
	@Mock
	public void $init(){
		apps = new HashMap<String, ApplicationInfo>();
	}

	@Mock
	public Map createApplication (ApplicationInfo info) {
		if(apps.containsKey(info.getName())){
			apps.remove(info.getName());
		}
		apps.put(info.getName(), info);
		return new HashMap();
	}
	
	@Mock
	public ApplicationInfo getApplication (String appName) {
		if(apps.containsKey(appName)){
			return apps.get(appName);
		}
		return null;
	}
	
	@Mock
	public List<ApplicationInfo> getApplications() {
		return new ArrayList<ApplicationInfo>(apps.values());		
	}
	
	@Mock
	public void deployApplication(String appName, String localPathToAppFile) throws IOException, ServletException {
		if(!apps.containsKey(appName)){
			apps.put(appName, new ApplicationInfo(appName, 1, 512, Collections.singletonList("dummyURL"), ModelEnum.SPRING));			
		}
	}
	
	@Mock
	public void deleteApplication(String appName){
		if(apps.containsKey(appName)){
			apps.remove(appName);
		} else {
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND); //what VMForceClient would do
		}
	}
	
	@Mock
	public void deleteAllApplications() {
		if(apps != null){
			apps.clear();
		}
	}	
}