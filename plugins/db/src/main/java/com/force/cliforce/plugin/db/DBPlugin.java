package com.force.cliforce.plugin.db;

import com.force.cliforce.Command;
import com.force.cliforce.Plugin;
import com.force.cliforce.plugin.db.command.DBClean;
import com.force.cliforce.plugin.db.command.ListCustomObjects;

import java.util.Arrays;
import java.util.List;

public class DBPlugin implements Plugin {

    @Override
    public List<Class<? extends Command>> getCommands() {
        return Arrays.asList(DBClean.class, ListCustomObjects.class);
    }
}
