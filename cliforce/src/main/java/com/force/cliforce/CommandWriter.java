package com.force.cliforce;

/**
 * Interface that provides a mechanism to output messages visible to the user.
 */
public interface CommandWriter {

    /**
     * Format the output message with String.format
     *
     * @param format String.format stype format string
     * @param args   with which to format format.
     */
    void printf(String format, Object... args);

    void print(String msg);

    void println(String msg);

    void printStackTrace(Exception e);

}
