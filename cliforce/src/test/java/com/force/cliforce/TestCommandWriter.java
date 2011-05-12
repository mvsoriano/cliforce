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

import static com.force.cliforce.Util.newLine;
import static com.force.cliforce.Util.withNewLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.sforce.soap.partner.fault.ApiFault;

public class TestCommandWriter implements CommandWriter{

    StringBuilder builder = new StringBuilder();

    public void reset(){
        builder.setLength(0);
    }

    public String getOutput(){
        return builder.toString();
    }

    @Override
    public void printf(String format, Object... args) {
        builder.append(String.format(format, args));
    }

    @Override
    public void printfln(String format, Object... args) {
        builder.append(String.format(withNewLine(format), args));
    }
    
    @Override
    public void print(String msg) {
        builder.append(msg);
    }

    @Override
    public void println(String msg) {
        builder.append(msg).append(newLine());
    }

    @Override
    public void printExceptionMessage(Exception e, boolean newLine) {
        String exceptionMessage;
        if (e instanceof ApiFault) {
            ApiFault af = (ApiFault)e;
            exceptionMessage = af.getExceptionMessage();
        } else {
            exceptionMessage = e.getMessage();
        }
        
        if (newLine) {
            println(exceptionMessage);
        } else {
            print(exceptionMessage);
        }
    }
    
    @Override
    public void printStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        builder.append(sw.getBuffer().toString());
    }
}
