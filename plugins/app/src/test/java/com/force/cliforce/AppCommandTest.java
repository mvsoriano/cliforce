package com.force.cliforce;

import com.force.cliforce.plugin.app.command.*;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.management.RuntimeErrorException;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

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
    File appFileNotWarDummmy = new File(appName + ".notwar");

    // ensure the client can connect to an org
    @BeforeClass
    public void setupEnvironment() throws IOException, ServletException {
        injector = Guice.createInjector(new TestModule());
        if (!appFileDummy.exists()) {
            Assert.assertTrue(appFileDummy.createNewFile(), "Could not create dummy app file " + appFileDummy.getAbsolutePath());
        }
        if (!appFileNotWarDummmy.exists()) {
            Assert.assertTrue(appFileNotWarDummmy.createNewFile(), "Could not createnot war dummy app file " + appFileNotWarDummmy.getAbsolutePath());
        }

    }

    @AfterClass(alwaysRun = true)
    public void cleanupEnvironment() {
        appFileDummy.delete();
        appFileNotWarDummmy.delete();
    }

    @DataProvider(name = "expectedInput")
    public Object[][] appCommandExpectedInput() {
        return new Object[][]{
                {DeleteAppCommand.class, "Exception while executing command: delete -> Main parameters are required (\"the name of the application\")\n", null, true},
                {DeleteAppCommand.class, "Deleting nonexistantappname\nthe application was not found\n", new String[]{"nonexistantappname"}, true},
                {AppsCommand.class, "No applications have been deployed\n", null, true},
                {PushCommand.class, "The path given: /no/such/path.war does not exist", new String[]{"-p", "/no/such/path.war", "pushfailapp"}, false},
                {PushCommand.class, "The path given: " + appFileNotWarDummmy.getAbsolutePath() + "  does not have a .war extension.\n", new String[]{"-p", appFileNotWarDummmy.getAbsolutePath(), "pushfailapp"}, false},
                {StopCommand.class, "No such app nonexistantappname\n", new String[]{"nonexistantappname"}, true},
                {StopCommand.class, "Exception while executing command: stop -> Main parameters are required (\"the name of the application\")\n", new String[]{}, true}
        };
    }

    @Test(dataProvider = "expectedInput")
    public void testAppOutput(Class<? extends Command> commandClass, String expectedOutput, String[] args, boolean exactOutput) throws Exception {
        TestCommandContext context = new TestCommandContext().withCommandArguments(args).withVmForceClient(new MockVMForceClient());
        Command command = injector.getInstance(commandClass);
        command.execute(context);
        assertCorrectOutput(context, expectedOutput, command, exactOutput);
    }

    @Test
    public void testAppAppsWithValidDeployedApplication() throws Exception {
        TestCommandContext appsctx = createCtxWithApp(appName, appPath);
        appsctx.getCommandWriter().reset();
        Command apps = injector.getInstance(AppsCommand.class);
        appsctx.setCommandArguments(new String[]{});
        apps.execute(appsctx);
        String[] output = appsctx.out().split("\\n");
        Assert.assertTrue(output[2].contains(appName), appName + " was not shown in app:apps line 2.");
    }

    @Test
    public void testAppPush() throws Exception {
        TestCommandContext pushctx = createCtxWithApp(appName, appPath);
        String[] output = pushctx.out().split("\\n");
        Assert.assertTrue(output[0].contains(appName), "Did not find " + appName + "on first output line.");
        Assert.assertTrue(output[2].contains("Deployed: " + appName), "Command did not report successful deploy on line 3: " + output[2]);
        Assert.assertNotNull(pushctx.getVmForceClient().getApplication(appName), "The application was not created in the mock.");
    }

    @Test
    public void testAppPushInvalidMem() throws Exception {
        Command cmd = injector.getInstance(PushCommand.class);
        TestCommandContext ctx =
                new TestCommandContext().withCommandArguments(appName, "--path", appPath, "--mem", "2000")
                        .withVmForceClient(new MockVMForceClient());
        cmd.execute(ctx);
        String output = ctx.out();
        Assert.assertTrue(output.contains("valid values for app memory are: 64, 128, 256, 512, 1024"), "Wrong error message thrown" +
                "for invalid memory limit.");
    }

    @Test
    public void testAppPushFromHomeDirUsingTilde() throws Exception {
        String fileName = appFileDummy.getName();
        String appNameInHomeDir = appName + "HomeDir";

        File appInHomeDir = copyFile(appFileDummy.getCanonicalPath(), System.getProperty("user.home") + "/" + fileName);
        appInHomeDir.getCanonicalPath();

        TestCommandContext pushctx = new TestCommandContext().withVmForceClient(new MockVMForceClient()).withCommandArguments(appNameInHomeDir, "-p", "~/" + fileName);
        Command pushCommand = injector.getInstance(PushCommand.class);
        pushCommand.execute(pushctx);

        String[] output = pushctx.out().split("\\n");
        Assert.assertTrue(output[0].contains(appNameInHomeDir), "Did not find " + appNameInHomeDir + "on first output line.");
        Assert.assertTrue(output[2].contains("Deployed: " + appNameInHomeDir), "Command did not report successful deploy on line 3: " + output[2]);
        Assert.assertNotNull(pushctx.getVmForceClient().getApplication(appNameInHomeDir), "The application was not created in the mock.");
        appInHomeDir.delete();
    }

    private File copyFile(String sourcePath, String resultPath) throws IOException {
        File source = new File(sourcePath);
        File result = new File(resultPath);

        if (!result.exists())
            result.createNewFile();

        FileReader s = new FileReader(source);
        FileWriter r = new FileWriter(result);
        int i;

        while ((i = s.read()) != -1) {
            r.write(i);
        }

        s.close();
        r.close();

        return result;
    }

    @Test
    public void testAppPushShortName() throws Exception {
        TestCommandContext pushctx = createCtxWithApp("short", appPath);
        String[] output = pushctx.out().split("\\n");
        Assert.assertTrue(output[0].contains("Your application name is invalid, it must be 6 or more characters long"), "Incorrect error message with app name that is less than 6 characters long");
    }

    @Test
    public void testAppPushWithDeploymentException() throws Exception {
        Assert.assertTrue(new File(appPath).exists(), "The app file " + appPath + " does not exist.");
        Command cmd = injector.getInstance(PushCommand.class);
        TestCommandContext ctx =
                new TestCommandContext().withCommandArguments(appName, "--path", appPath)
                        .withVmForceClient(new MockVMForceClientDeploymentException());
        cmd.execute(ctx);
        //check that the application was deleted
        Assert.assertNull(ctx.getVmForceClient().getApplication(appName), "The application should have been deleted after a deployment failure.");
    }

    @DataProvider(name = "expectedOutputForCommandsWithDeployedApp")
    public Object[][] provideCommandsForVerification() {
        return new Object[][]{
                {StartCommand.class, "Starting " + appName + "\ndone\n", new String[]{appName}, new String[]{}, true}
                , {RestartCommand.class, "Restarting " + appName + "\ndone\n", new String[]{appName}, new String[]{}, true}
                , {DeleteAppCommand.class, "Deleting " + appName + "\ndone\n", new String[]{appName}, new String[]{}, true}
                , {StopCommand.class, "Stopping " + appName + "\ndone\n", new String[]{appName}, new String[]{}, true}
                , {TailFileCommand.class, "Tailing logs on app: " + appName + " instance 0, press enter to interrupt", new String[]{appName, "-p", "logs"}, new String[]{"\n"}, false}
        };
    }

    @Test(dataProvider = "expectedOutputForCommandsWithDeployedApp")
    public void testCommandWithDeployedApp(Class<? extends Command> commandClass, String expectedOutput, String[] args, String[] inputs, boolean exactOutput) throws Exception {
        TestCommandContext ctxWithApp = createCtxWithApp(appName, appPath).withTestCommandReader(new TestCommandReader(Arrays.asList(inputs)));
        ctxWithApp.getCommandWriter().reset();

        Command command = injector.getInstance(commandClass);
        ctxWithApp.setCommandArguments(args);
        command.execute(ctxWithApp);

        assertCorrectOutput(ctxWithApp, expectedOutput, command, exactOutput);
    }

    private void assertCorrectOutput(TestCommandContext contextWithOutput, String expectedOutput, Command command, boolean exactOutput) {
        String actualOutput = contextWithOutput.out();
        if (exactOutput) {
            Assert.assertEquals(actualOutput, expectedOutput, "Unexpected output for " + command + ": " + actualOutput);
        } else {
            Assert.assertTrue(actualOutput.contains(expectedOutput), "Unexpected output for " + command + ": " + actualOutput);
        }
    }

    /**
     * Create a TestCommandContext with a fake deployed app. The helper requires a (possibly) empty app file
     * to be present at appPath.
     *
     * @param appName The name to use for the (possibly fake) app
     * @param appPath The location of the (possibly fake) app file
     * @return {@link TestCommandContext}
     * @throws Exception
     */
    private TestCommandContext createCtxWithApp(String appName, String appPath) throws Exception {
        Assert.assertTrue(new File(appPath).exists(), "The app file " + appPath + " does not exist.");
        Command cmd = injector.getInstance(PushCommand.class);
        TestCommandContext ctx =
                new TestCommandContext().withCommandArguments(appName, "--path", appPath)
                        .withVmForceClient(new MockVMForceClient());
        cmd.execute(ctx);
        return ctx;
    }

    private class MockVMForceClientDeploymentException extends MockVMForceClient {

        @Override
        public void deployApplication(String appName, String localPathToAppFile)
                throws IOException, ServletException {
            throw new RuntimeErrorException(null);
        }

    }

}
