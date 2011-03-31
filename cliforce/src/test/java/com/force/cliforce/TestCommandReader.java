package com.force.cliforce;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author nnewbold
 * @since javasdk-21.0.2-BETA
 */
public class TestCommandReader implements CommandReader {

    Iterator<String> inputs;
    CommandWriter out;
    String nextInput;

    public TestCommandReader(List<String> inputs) {
        this.inputs = inputs.iterator();
    }

    public void setCommandWriter(CommandWriter out) {
        this.out = out;
    }

    @Override
    public String readLine(String prompt) {
        handleReadLine(prompt);
        return nextInput;
    }

    @Override
    public String[] readAndParseLine(String prompt) {
        handleReadLine(prompt);
        return Util.parseCommand(nextInput);
    }

    @Override
    public String readLine(String prompt, Character mask) {
        handleReadLine(prompt, mask);
        return nextInput;
    }

    private void handleReadLine(String prompt) {
        handleReadLine(prompt, null);
    }

    private void handleReadLine(String prompt, Character mask) {
        assertHasWriter();
        assertHasMoreInput();
        nextInput = inputs.next();
        out.print(prompt + maskedOutput(nextInput, mask) + "\n");
    }

    private String maskedOutput(String input, Character mask) {
        if (mask != null) {
            char[] maskedOutput = new char[input.length()];
            Arrays.fill(maskedOutput, mask);
            return String.valueOf(maskedOutput);
        } else {
            return input;
        }
    }

    private void assertHasWriter() {
        if (out == null) {
            throw new IllegalStateException("No CommandWriter has been provided.");
        }
    }

    private void assertHasMoreInput() {
        if (!inputs.hasNext()) {
            throw new IllegalStateException("No more input available.");
        }
    }
}
