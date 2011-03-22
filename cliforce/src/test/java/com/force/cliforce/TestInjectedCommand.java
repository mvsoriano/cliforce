package com.force.cliforce;


import com.force.cliforce.dependency.DependencyResolver;
import junit.framework.Assert;

import javax.inject.Inject;

public class TestInjectedCommand implements Command {

    @Inject
    CLIForce cliForce;

    @Inject
    DependencyResolver dependencyResolver;

    @Override
    public String name() {
        return "testinjected";
    }

    @Override
    public String describe() {
        return "command for unit testing the test base classes, that has a cliforce and dependency resolver injected";
    }

    @Override
    public void execute(CommandContext ctx) throws Exception {
        Assert.assertNotNull("cliForce was not injected", cliForce);
        Assert.assertNotNull("dependencyResolver was not injected", cliForce);
        ctx.getCommandWriter().print("executed");
    }
}

