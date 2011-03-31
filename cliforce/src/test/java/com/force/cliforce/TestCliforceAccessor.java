/*
 * Copyright, 1999-2010, SALESFORCE.com 
 * All Rights Reserved
 * Company Confidential
 */
package com.force.cliforce;

import javax.inject.Inject;

/**
 * TestCliforceAccessor, allows tests not in cliforce package to call package private methods on cliforce.
 *
 * @author sclasen
 */
public class TestCliforceAccessor {

    @Inject
    CLIForce cliForce;

    public void setWriter(CommandWriter writer) {
        cliForce.setWriter(writer);
    }

    public void setReader(CommandReader reader) {
        cliForce.setReader(reader);
    }
}
