package com.force.cliforce;

public class CommandDescriptor {
    public final String name;
    public final Command command;

    public CommandDescriptor(String name, Command c) {
        this.name = name;
        this.command = c;
    }
}
