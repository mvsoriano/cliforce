package com.force.cliforce;

import org.testng.Assert;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;

@Guice(modules = TestModule.class)
public class TestPluginInjectorTest {

    @Inject
    TestPluginInjector injector;

    @Test
    public void testCommandWithoutDependencies() throws Exception {
        Plugin testPlugin = new TestPlugin();
        TestCommand testCommand = injector.getInjectedCommand(testPlugin, TestCommand.class);
        TestCommandContext testCommandContext = new TestCommandContext();
        testCommand.execute(testCommandContext);
        Assert.assertEquals(testCommandContext.getCommandWriter().getOutput(), "executed");
    }

    @Test
    public void testCommandWithDependencies() throws Exception {
        Plugin testPlugin = new TestPlugin();
        TestInjectedCommand testCommand = injector.getInjectedCommand(testPlugin, TestInjectedCommand.class);
        TestCommandContext testCommandContext = new TestCommandContext();
        testCommand.execute(testCommandContext);
        Assert.assertEquals(testCommandContext.getCommandWriter().getOutput(), "executed");
    }


}

