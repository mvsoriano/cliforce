package com.force.cliforce.plugin.db;

import com.force.cliforce.Command;
import com.force.cliforce.Plugin;
import com.force.cliforce.plugin.db.command.Describe;

import java.util.Arrays;
import java.util.List;

public class DBPlugin implements Plugin {

    @Override
    public List<Class<? extends Command>> getCommands() {
        return Arrays.<Class<? extends Command>>asList(Describe.class);
    }
}
