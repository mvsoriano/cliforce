package com.force.cliforce;


import java.io.IOException;

public class TestConnectionManager extends MainConnectionManager {


    @Override
    public boolean loadLogin() {
        loginProperties.clear();
        try {
            loginProperties.load(getClass().getClassLoader().getResourceAsStream("test.login"));
            return loginProperties.size() == 3;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean saveLogin() throws IOException {
        return true;
    }
}
