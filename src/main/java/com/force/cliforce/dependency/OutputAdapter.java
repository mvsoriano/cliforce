package com.force.cliforce.dependency;


/*
Used to isolate Logger dependency from the Dependency resolver.
Logger jar wont be loaded at when Boot is executing so it uses an OutputAdapter to System.out
Other users of com.force.cliforce.dependency.DependencyResolver will use an OutputAdapter that forwards to a logger
 */
public interface OutputAdapter {

    public void println(String msg);

    public void println(Exception e, String msg);

}
