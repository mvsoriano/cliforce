package com.force.cliforce;


import java.util.List;

/**
 * Interface for Command providers.
 *
 * The plugin will be instantiated via java service locator mechanism.
 *
 * There must be a file called META-INF/services/com.force.cliforce.Plugin in the plugin jar file
 * containing the fully qualified class name of the Plugin implementation class.
 */
public interface Plugin {

    List<Class<? extends Command>> getCommands();

}
