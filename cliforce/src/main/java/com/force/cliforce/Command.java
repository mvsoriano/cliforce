package com.force.cliforce;

/**
 * Interface implemented by classes that want to add behavior to cliforce
 */
public interface Command {

    /**
     * the name of the command. The user will be able to execute the command when it is installed by typing
     * pluginName:commandName
     *
     * @return name of the command
     */
    public String name();

    /**
     * a description of what the command does and the arguments it expects. Displayed by the help command and
     * by tab completion.
     *
     * @return description of the command.
     */
    public String describe();

    /**
     * Execute the command with the given context.
     *
     * @param ctx the execution context provided by cliforce.
     * @throws Exception
     */
    public void execute(CommandContext ctx) throws Exception;
}
