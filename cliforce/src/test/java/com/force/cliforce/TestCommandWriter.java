package com.force.cliforce;


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
    public void print(String msg) {
        builder.append(msg);
    }

    @Override
    public void println(String msg) {
        builder.append(msg).append("\n");
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
