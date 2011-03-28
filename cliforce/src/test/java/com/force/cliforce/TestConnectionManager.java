package com.force.cliforce;


import com.google.inject.name.Named;

import javax.inject.Inject;
import java.io.IOException;

public class TestConnectionManager extends MainConnectionManager {

    public static final String TEST_CREDENTIALS = "test-credentials";

    @Inject
    @Named(TEST_CREDENTIALS)
    private String loginPropsFile;


    @Override
    public void loadLogin() throws IOException {
        loginProperties.clear();
        loginProperties.load(getClass().getClassLoader().getResourceAsStream(loginPropsFile));
        setLogin(loginProperties.getProperty(USER), loginProperties.getProperty(PASSWORD), loginProperties.getProperty(TARGET));
    }

    @Override
    public void saveLogin() throws IOException {

    }
}
