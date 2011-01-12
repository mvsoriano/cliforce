#Writing plugins for the CLIForce shell

The simplest example of a CLIForce plugin is available on github at[http://github.com/sclasen/cliplugin](http://github.com/sclasen/cliplugin).

Your plugin must currently be published (even locally) as a maven artifact for you to be able to install it in the shell. The current naming
convention for CLIForce plugins is that the maven groupId must be com.force.cliforce.plugin. The artifactId of your plugin is used to prefix the name of commands
that your plugin provides.

The two important java interfaces involved in creating a plugin are com.force.cliforce.Plugin

        package com.force.cliforce;


        import java.util.List;

        public interface Plugin {

            List<Command> getCommands();

        }


and com.force.cliforce.Command.

        package com.force.cliforce;


        public interface Command {

            public String name();

            public String describe();

            public void execute(CommandContext ctx) throws Exception;
        }


Plugin instances are discovered and instantiated using java's built in service discovery mechanism, java.util.ServiceLocator

Your plugin artifact should contain a text file called META-INF/services/com.force.cliforce.Plugin, with a single line of text with the fully qualified class name of your Plugin implementation.

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

force> plugin -a cliplugin -v 1.0
Adding Plugin: cliplugin (HelloWorldPlugin)
  -> adds command cliplugin:hello (HelloWorldPlugin$HelloWorldCommand)
force> cliplugin:hello
Hello World
force> unplug cliplugin
attempting to remove plugin: cliplugin
removed command: hello

Done
force>


