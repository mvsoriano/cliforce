package com.force.cliforce.listener;

import java.io.IOException;

import org.testng.ISuite;
import org.testng.ISuiteListener;

import com.vmforce.test.util.PropsUtil;
import com.vmforce.test.util.TestContext;
import com.vmforce.test.util.UserInfo;

/**
 * 
 * This listener loads Test Context required for cliforce tests.
 *
 * @author jeffrey.lai
 * @since javasdk-21.0.2-BETA
 */
public class CliforceTestContextSuiteListener implements ISuiteListener {

    @Override
    public void onFinish(ISuite suite) {
        try {
            TestContext.get().addTestProps(PropsUtil.load("test.vmf01.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Error loading property files", e);
        }
        
        UserInfo user = new UserInfo(null, null, TestContext.get().getTestProps().getProperty("test.username"), 
                TestContext.get().getTestProps().getProperty("test.password"), 
                "https://" + TestContext.get().getTestProps().getProperty("test.sfdc.endpoint") + "/services/Soap/u/21");
        TestContext.get().setUserInfo(null, user);
    }

    @Override
    public void onStart(ISuite suite) {
        
    }

}
