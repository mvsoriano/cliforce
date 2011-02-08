package com.force.cliforce.dependency;


public class DependencyResolutionException extends RuntimeException{

    public DependencyResolutionException() {
        super();
    }

    public DependencyResolutionException(String s) {
        super(s);
    }

    public DependencyResolutionException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public DependencyResolutionException(Throwable throwable) {
        super(throwable);
    }
}
