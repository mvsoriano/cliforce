package com.force.cliforce;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For classes that need to be initialized in the main startup thread,
 * use this to get a logger, and we dont pay the logging init penalty till we actually need to log a message
 */
public class LazyLogger {


    private Logger logger;
    Class clazz;

    public LazyLogger(Class c) {
        clazz = c;
    }

    public LazyLogger(Object o) {
        clazz = o.getClass();
    }

    public Logger get() {
        if (logger == null) {
            logger = LoggerFactory.getLogger(clazz);
        }
        return logger;
    }


}
