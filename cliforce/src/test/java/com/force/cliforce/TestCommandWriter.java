package com.force.cliforce;


import java.io.PrintWriter;
import java.io.StringWriter;

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
    public void printStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        builder.append(sw.getBuffer().toString());
    }
}
