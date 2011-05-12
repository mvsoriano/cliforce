/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

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
        out.println(prompt + maskedOutput(nextInput, mask));
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
