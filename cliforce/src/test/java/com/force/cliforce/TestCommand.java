package com.force.cliforce;

public class TestCommand implements Command {
    @Override
    public String name() {
        return "test";
    }

    @Override
    public String describe() {
        return "command for unit testing the test base classes";
    }

    @Override
    public void execute(CommandContext ctx) throws Exception {
        ctx.getCommandWriter().print("executed");
    }
}
