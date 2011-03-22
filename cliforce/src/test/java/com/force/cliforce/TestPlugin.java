package com.force.cliforce;


import java.util.Arrays;
import java.util.List;

public class TestPlugin implements Plugin {
    @Override
    public List<Class<? extends Command>> getCommands() {
        return Arrays.asList(TestCommand.class, TestInjectedCommand.class);
    }
}
