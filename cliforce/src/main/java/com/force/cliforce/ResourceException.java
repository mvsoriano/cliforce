package com.force.cliforce;


/**
 * Thrown when a command context is missing a resource necessary to execute a command.
 */
public class ResourceException extends RuntimeException {

    public ResourceException() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public ResourceException(String message) {
        super(message);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public ResourceException(String message, Throwable cause) {
        super(message, cause);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public ResourceException(Throwable cause) {
        super(cause);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
