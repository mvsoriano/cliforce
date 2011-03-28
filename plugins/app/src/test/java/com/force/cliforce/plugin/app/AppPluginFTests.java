package com.force.cliforce.plugin.app;


import com.force.cliforce.CLIForce;
import com.force.cliforce.Command;
import com.force.cliforce.TestModule;
import com.force.cliforce.TestPluginInstaller;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

public class AppPluginFTests {


    @Test
    public void deletionOfNonExistentApp() throws Exception {
        Injector testInjector = getTestInjector();
        CLIForce cliForce = testInjector.getInstance(CLIForce.class);
        TestPluginInstaller installer = testInjector.getInstance(TestPluginInstaller.class);
        StringWriter stringWriter = new StringWriter();
        cliForce.init(System.in, new PrintWriter(stringWriter));
        installer.installPlugin("app", "LATEST", new AppPlugin());
        cliForce.executeWithArgs("app:delete", "nonexistentapp");
        Assert.assertTrue(stringWriter.getBuffer().toString().contains("the application was not found"), stringWriter.getBuffer().toString());
        Assert.assertFalse(stringWriter.getBuffer().toString().contains("done"), stringWriter.getBuffer().toString());
    }


    private <T extends Command> T getInjectedCommand(Class<T> cmd) {
        return getTestInjector().getInstance(cmd);
    }

    private Injector getTestInjector() {
        return Guice.createInjector(new TestModule());
    }


}
