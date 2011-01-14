package com.force.cliforce;

/**
 *
 */
public interface CommandWriter {

    void printf(String format, Object... args);

    void print(String msg);

    void println(String msg);

}
