package com.force.cliforce;


/**
 * An exception that causes CLIForce to break out of its repl.
 */
public class ExitException extends RuntimeException{
    public ExitException(String message) {
        super(message);
    }
}
