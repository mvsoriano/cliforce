package com.force.cliforce;


import java.io.IOException;

public class TestConnectionManager extends MainConnectionManager {


    @Override
    public void loadLogin() throws IOException {
        loginProperties.clear();
        loginProperties.load(getClass().getClassLoader().getResourceAsStream("test.login"));
    }

    @Override
    public void saveLogin() throws IOException {

    }
}
