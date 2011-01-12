package com.force.cliforce;


public interface CommandReader {

    String readLine(String prompt);

    String[] readAndParseLine(String prompt);

}
