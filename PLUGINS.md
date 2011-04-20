#Writing plugins for the CLIForce shell

Writing a plugin enables you to add commands that execute your own Java code that is passed a CommandContext as a parameter. This context gives you access to MetadataConnection, PartnerConnection, RestConnection, and VMForceClient connections.

The simplest example of a CLIForce plugin is available on github at [http://github.com/sclasen/cliplugin](http://github.com/sclasen/cliplugin).

Your plugin must currently be published (even locally) as a Maven artifact for you to be able to install it in the shell. The current naming
convention for CLIForce plugins is that the Maven groupId must be com.force.cliforce.plugin. The artifactId of your plugin is used to prefix the name of commands
that your plugin provides.

The two important Java interfaces involved in creating a plugin are com.force.cliforce.Plugin
(testing new syntax highlighting)
'''
        package com.force.cliforce;


        import java.util.List;

        public interface Plugin {

            List<Class<? extends Command>> getCommands();

        }
'''

and com.force.cliforce.Command.

        package com.force.cliforce;


        public interface Command {

            public String name();

            public String describe();

            public void execute(CommandContext ctx) throws Exception;
        }


Plugin instances are discovered and instantiated using Java's built-in service discovery mechanism, java.util.ServiceLocator.

Your plugin artifact should contain a text file called META-INF/services/com.force.cliforce.Plugin with a single line of text containing the fully qualified class name of your Plugin implementation.

#Generics help for Plugins that have a single command.

Due to the way java generics work, plugins that only have a single can be created like this

    @Override
    public List<Class<? extends Command>> getCommands() {
         return Arrays.<Class<? extends Command>>asList(HelloWorldCommand.class);
    }


#Accessing the CLIForce or DependencyResolver instance

If your command needs access to the current CLIForce or DependencyResolver instance, simply declare a field of the proper type, annotated with a
javax.inject.Inject annotation. (JSR-330, Dependency Injection for Java)

For example

    public class MyCommand implements Command
    @Inject
    private CLIForce cliforce;
    @Inject
    private DependencyResolver resolver;


# Prefer extending JCommand instead of implementing Command directly.

In general, if your commands take arguments you should prefer implementing them by extending the JCommand class.
This base class uses JCommander (http://jcommander.org) to parse command arguments on your behalf, and also provides
context-aware tab completion out of the box.


#Installing a plugin

Using the sample plugin provide, run the following commands.

        git clone http://github.com/sclasen/cliplugin
        cd cliplugin
        mvn install
        cliforce

          _____ _      _____ ______
         / ____| |    |_   _|  ____|
        | |    | |      | | | |__ ___  _ __ ___ ___
        | |    | |      | | |  __/ _ \| '__/ __/ _ \
        | |____| |____ _| |_| | | (_) | | | (_|  __/
         \_____|______|_____|_|  \___/|_|  \___\___|

        force> plugin cliplugin
        Adding Plugin: cliplugin (HelloWorldPlugin)
          -> adds command cliplugin:hello (HelloWorldPlugin$HelloWorldCommand)
        force> cliplugin:hello
        Hello World
        force> unplug cliplugin
        attempting to remove plugin: cliplugin
        removed command: hello

        Done
        force>


