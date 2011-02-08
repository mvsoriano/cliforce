package com.force.cliforce;

/**
 * Interface that provides input to commands that need to prompt for user input.
 */
public interface CommandReader {

    /**
     * read and return the raw string entered by the user.
     *
     * @param prompt the prompt presented to the user directing them to input.
     *
     * @return the user entered string.
     */
    String readLine(String prompt);

    /**
     * read and parse the string entered by the user, interpreting quoted strings with whitespace as a single string.
     *
     *
     * @param prompt the prompt presented to the user directing them to input.
     * @return the user entered arguments.
     */
    String[] readAndParseLine(String prompt);

}
