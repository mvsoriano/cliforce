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
import mockit.Instantiation;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import com.vmforce.client.VMForceClient;
import com.vmforce.client.bean.ApplicationInfo;
import com.vmforce.client.bean.ApplicationInfo.ModelEnum;

@MockClass(realClass = VMForceClient.class, instantiation = Instantiation.PerMockSetup)
public class MockVMForceClient extends VMForceClient {
    
	private HashMap<String, ApplicationInfo> apps = new HashMap<String, ApplicationInfo>();

	@SuppressWarnings("rawtypes")
	@Override
    @Mock
	public Map createApplication (ApplicationInfo info) {
		if(apps.containsKey(info.getName())){
			apps.remove(info.getName());
		}
		apps.put(info.getName(), info);
		return new HashMap();
	}
	
	@Override
	@Mock
	public ApplicationInfo getApplication (String appName) {
		if(apps.containsKey(appName)){
			return apps.get(appName);
		}
		return null;
	}
	
	@Override
	@Mock
	public List<ApplicationInfo> getApplications() {
		return new ArrayList<ApplicationInfo>(apps.values());		
	}
	
	@Override
	@Mock
	public void deployApplication(String appName, String localPathToAppFile) throws IOException, ServletException {
		if(!apps.containsKey(appName)){
			apps.put(appName, new ApplicationInfo(appName, 1, 512, Collections.singletonList("dummyURL"), ModelEnum.SPRING));			
		}
	}
	
	@Override
	@Mock
	public void deleteApplication(String appName){
		if(apps.containsKey(appName)){
			apps.remove(appName);
		} else {
			throw new HttpClientErrorException(HttpStatus.NOT_FOUND); //what VMForceClient would do
		}
	}
	
	@Override
	@Mock
	public void deleteAllApplications() {
		if(apps != null){
			apps.clear();
		}
	}
	
}