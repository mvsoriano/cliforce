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

import com.force.cliforce.plugin.app.command.AppsCommand;
import com.force.cliforce.plugin.app.command.DeleteAppCommand;
import com.force.cliforce.plugin.app.command.PushCommand;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.vmforce.client.VMForceClient;

/**
 * Tests for the behavior of cliforce's app command.
 *
 * @author nnewbold
 * @since vmforce.beta1
 */
public class AppCommandTest {

    Injector injector;
    final String appName = "myAppName";
	final String appPath = "testspringmvc-1.0-SNAPSHOT.war";
	File appFileDummy = new File(appPath);

    // ensure the client can connect to an org
    @BeforeClass
    public void setupEnvironment() throws IOException, ServletException {
    	injector = Guice.createInjector(new TestModule());
    	Mockit.setUpMock(VMForceClient.class, new MockVMForceClient());
    	if(!appFileDummy.exists()){
    		Assert.assertTrue(appFileDummy.createNewFile(), "Could not create dummy app file " + appFileDummy.getAbsolutePath());
    	}
    }
    
    @AfterClass(alwaysRun = true)
    public void cleanupEnvironment() {
    	appFileDummy.delete();
    }

    @DataProvider(name = "expectedInput")
    public Object[][] appCommandExpectedInput() {
        return new Object[][]{
                {DeleteAppCommand.class, "Exception while executing command: delete -> Main parameters are required (\"the name of the application\")\n", null, true}
                , {DeleteAppCommand.class, "Deleting nonexistantappname\nthe application was not found\n", new String[]{"nonexistantappname"}, true}
                , {AppsCommand.class, "No applications have been deployed\n", null, true}
                , {PushCommand.class, "The path given: /no/such/path.war does not exist", new String[]{"-p", "/no/such/path.war", "pushfailapp"}, false}
        };
    }

    @Test(dataProvider = "expectedInput")
    public void testAppOutput(Class<? extends Command> commandClass, String expectedOutput, String[] args, boolean exactOutput) throws Exception {
    	TestCommandContext context = new TestCommandContext().withCommandArguments(args).withVmForceClient(new VMForceClient());
        Command command = injector.getInstance(commandClass);
        command.execute(context);
        String actualOutput = context.out();
        if (exactOutput) {
            Assert.assertEquals(actualOutput, expectedOutput, "Unexpected output for " + command + ": " + actualOutput);
        } else {
            Assert.assertTrue(actualOutput.contains(expectedOutput), "Unexpected output for " + command + ": " + actualOutput);
        }
    }

    @Test
    public void testAppPush() throws Exception{
    	TestCommandContext pushctx = createCtxWithApp(appName, appPath);
    	String[] output = pushctx.out().split("\\n");
    	Assert.assertTrue(output[0].contains(appName), "Did not find " + appName + "on first output line.");
    	Assert.assertTrue(output[2].contains("Deployed: " + appName), "Command did not report successful deploy on line 3: " + output[2]);
    	Assert.assertNotNull(pushctx.getVmForceClient().getApplication(appName), "The application was not created in the mock.");
    }
    
    @Test
    public void testAppPushShortName() throws Exception{
    	TestCommandContext pushctx = createCtxWithApp("short", appPath);
    	String[] output = pushctx.out().split("\\n");
    	Assert.assertTrue(output[0].contains("Your application name is invalid, it must be 6 or more characters long"), "Incorrect error message with app name that is less than 6 characters long");
    }
    
    @Test
    public void testAppApps() throws Exception {
    	TestCommandContext appsctx = createCtxWithApp(appName, appPath);
    	appsctx.getCommandWriter().reset();
    	Command apps = injector.getInstance(AppsCommand.class);
    	appsctx.setCommandArguments(new String[]{});
    	apps.execute(appsctx);
    	String[] output = appsctx.out().split("\\n");
    	Assert.assertTrue(output[2].contains(appName), appName + " was not shown in app:apps line 2.");
    }
    
    @Test
    public void testAppDelete() throws Exception {
    	TestCommandContext delctx = createCtxWithApp(appName, appPath);
    	delctx.getCommandWriter().reset();
    	Command del = injector.getInstance(DeleteAppCommand.class);
    	delctx.setCommandArguments(new String[]{appName});
    	del.execute(delctx);
    	String[] output = delctx.out().split("\\n");
    	Assert.assertTrue(output[0].contains(appName), "Delete message did not contain " + appName);
    	Assert.assertEquals(output[1], "done", "Delete was not successful.");
    }
    
    /**
     * Create a TestCommandContext with a fake deployed app. The helper requires a (possibly) empty app file
     * to be present at appPath.
     * @param appName The name to use for the (possibly fake) app
     * @param appPath The location of the (possibly fake) app file
     * @return {@link TestCommandContext} 
     * @throws Exception 
     */
    private TestCommandContext createCtxWithApp (String appName, String appPath) throws Exception {
    	Assert.assertTrue(new File(appPath).exists(), "The app file " +appPath+ " does not exist.");
    	Command cmd = injector.getInstance(PushCommand.class);
		TestCommandContext ctx = 
    		new TestCommandContext().withCommandArguments(appName, "--path", appPath)
    			.withVmForceClient(new VMForceClient()); //uses mock VMForceClient defined in setup
    	cmd.execute(ctx);
    	return ctx;
    }
}
