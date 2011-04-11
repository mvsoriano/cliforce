package com.force.cliforce;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import mockit.Mockit;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import scala.actors.threadpool.Arrays;

import com.force.cliforce.plugin.app.AppPlugin;
import com.sforce.ws.ConnectionException;
import com.vmforce.client.VMForceClient;

/**
 * 
 * Tests for tab completion in app commands
 *
 * @author jeffrey.lai
 * @since javasdk-21.0.2-BETA
 */
public class AppCommandCompletorTest extends BaseCommandCompletorTest {
    
    private final String appPath = "testspringmvc-1.0-SNAPSHOT.war";
    private File appFileDummy = new File(appPath);
    
    @Override
    @BeforeClass
    public void classSetup() throws InterruptedException, IOException, ConnectionException, ServletException {
        super.classSetup();
        Mockit.setUpMock(VMForceClient.class, MockVMForceClient.class);
        if(!appFileDummy.exists()){
            Assert.assertTrue(appFileDummy.createNewFile(), "Could not create dummy app file " + appFileDummy.getAbsolutePath());
        }
    }
    
    @AfterClass(alwaysRun=true)
    public void classTeardown() {
        appFileDummy.delete();
        Mockit.tearDownMocks();
    }

    @Override
    public String getPluginArtifact() {
        return "app";
    }

    @Override
    public Plugin getPlugin() {
        return new AppPlugin();
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testBasicAppCommandCompletion() {
        runCompletorTestCase("app", 0, Arrays.asList(new String[] {"app:apps", "app:delete", "app:push", "app:restart", "app:start", "app:stop", "app:tail"}));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAppsCommandHelpText() {
        runCompletorTestCase("app:apps ", 9, Arrays.asList(new String[] {" ", "lists deployed apps"}));
    }
    
    @DataProvider(name="allAppCommandsNoSpace")
    public Object[][] allAppCommandsNoSpace() {
        return new Object[][] {
                new String[] {"app:apps"},
                new String[] {"app:delete"},
                new String[] {"app:push"},
                new String[] {"app:restart"},
                new String[] {"app:start"},
                new String[] {"app:stop"},
                new String[] {"app:tail"}
        };
    }
    
    @SuppressWarnings("unchecked")
    @Test(dataProvider="allAppCommandsNoSpace")
    public void testCommandNoSpace(String cmd) {
        runCompletorTestCase(cmd, 0, Arrays.asList(new String[] {cmd + " "}));
    }
    
    @DataProvider(name="commandsWithOnlyAppNameParam")
    public Object[][] commandsWithOnlyAppNameParam() {
        return new Object[][] {
                new String[] {"app:delete "},
                new String[] {"app:restart "},
                new String[] {"app:start "},
                new String[] {"app:stop "}
        };
    }
    
    @SuppressWarnings("unchecked")
    @Test(dataProvider = "commandsWithOnlyAppNameParam")
    public void testCommandWithAppNameCompletionHelpText(String cmd) {
        runCompletorTestCase(cmd, cmd.length(), Arrays.asList(new String[] {" main param: the name of the application", " "}));
    }
    
    @SuppressWarnings("unchecked")
    @Test(dataProvider = "commandsWithOnlyAppNameParam")
    public void testCommandWithAppNameCompletionOneApp(String cmd) throws IOException, ConnectionException, ServletException, InterruptedException {
        pushFakeApp("california");
        runCompletorTestCase(cmd, cmd.length(), Arrays.asList(new String[] {"california "}));
        runCommand("app:delete california");
    }
    
    @SuppressWarnings("unchecked")
    @Test(dataProvider = "commandsWithOnlyAppNameParam")
    public void testCommandWithAppNameCompletionTwoApps(String cmd) throws IOException, ConnectionException, ServletException, InterruptedException {
        pushFakeApp("california");
        pushFakeApp("arizona");
        runCompletorTestCase(cmd, cmd.length(), Arrays.asList(new String[] {"arizona", "california"}));
        runCommand("app:delete california");
        runCommand("app:delete arizona");
    }
    
    private void pushFakeApp(String appName) throws IOException, ConnectionException, ServletException, InterruptedException {
        runCommand("app:push " + appName + " -p " + appPath);
    }

}
