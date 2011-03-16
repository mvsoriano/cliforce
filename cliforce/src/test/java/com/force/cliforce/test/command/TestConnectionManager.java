package com.force.cliforce.test.command;

import com.force.cliforce.MainConnectionManager;

public class TestConnectionManager extends MainConnectionManager {
    @Override
    public boolean loadLoginProperties() {
        return true;
    }
}
