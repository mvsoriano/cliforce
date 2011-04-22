package com.force.cliforce.plugin.db;

import java.util.Arrays;
import java.util.List;

import com.force.cliforce.Command;
import com.force.cliforce.Plugin;
import com.force.cliforce.plugin.db.command.DBClean;
import com.force.cliforce.plugin.db.command.Describe;

public class DBPlugin implements Plugin {

    @Override
    public List<Class<? extends Command>> getCommands() {
        return Arrays.asList(DBClean.class, Describe.class);
    }
}
