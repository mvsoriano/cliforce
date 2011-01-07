package com.force.cliforce;


public interface Command {

    public String name();

    public String describe();

    public void execute(CommandContext ctx) throws Exception;

}
