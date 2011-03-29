package com.force.cliforce;

import com.force.cliforce.plugin.app.command.DeleteAppCommand;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Tests for the behavior of cliforce's app command.
 * @author nnewbold
 * @since vmforce.beta1
 */
public class AppCommandTest {

    Injector injector;
    ConnectionManager connection;
    TestCommandContext context;

    // ensure the client can connect to an org
    @BeforeClass
    public void login() throws IOException {
        injector = Guice.createInjector(new TestModule());

        connection = injector.getInstance(TestConnectionManager.class);
        connection.loadLogin();
        connection.doLogin();

        context = new TestCommandContext().withVmForceClient(connection.getVmForceClient());
    }

    @DataProvider(name = "expectedInput")
    public Object[][] appCommandExpectedInput() {
        return new Object[][]{
                { DeleteAppCommand.class, "Exception while executing command: delete -> Main parameters are required (\"the name of the application\")\n", null}
              , { DeleteAppCommand.class, "Deleting nonexistantappname\nthe application was not found\n", new String[]{"nonexistantappname"} }
        };
    }

    @Test(dataProvider = "expectedInput")
    public void testAppOutput(Class<? extends Command> commandClass, String expectedOutput, String[] args) throws Exception {
        context = context.withCommandArguments(args);
        Command command = injector.getInstance(commandClass);
        command.execute(context);
        String actualOutput = context.out();
        Assert.assertEquals(actualOutput, expectedOutput, "Unexpected output for " + command + ": " + actualOutput);
    }


}
