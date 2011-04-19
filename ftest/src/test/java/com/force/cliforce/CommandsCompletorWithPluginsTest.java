package com.force.cliforce;

import java.io.IOException;

import org.testng.annotations.Test;

import scala.actors.threadpool.Arrays;


/**
 * Test command completion for commands that behave differently if plugins are loaded.
 * @author dhain
 * @since javasdk-21.0.2-BETA
 */
public class CommandsCompletorWithPluginsTest extends BasePluginsTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testClasspathCommandLoadedPlugins() throws IOException {
    	runCompletorTestCase("classpath --sort ", 16, Arrays.asList(new String[] {"app", "codegen", "connection", "db", "jpa", "template", "<or none for the cliforce classpath>"}));
    }  
}
