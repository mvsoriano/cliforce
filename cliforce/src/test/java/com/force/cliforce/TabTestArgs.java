/*
 * Copyright, 1999-2010, SALESFORCE.com 
 * All Rights Reserved
 * Company Confidential
 */
package com.force.cliforce;

import com.beust.jcommander.Parameter;

import java.io.File;
import java.net.URISyntaxException;

/**
 * TabTestArgs is an arguments class for tab completion tests. The arguments defined
 * are used e.g. by {@link JCommandTabCompletionTest} for testing the tab completion
 * functionality. New CLIForce argument types should be refelcted with a new test
 * parameter here.
 *
 * @author sclasen
 */
public class TabTestArgs {

    public static final String sLong = "--slong";
    public static final String sShort = "-s";
    public static final String[] sSwitches = new String[]{sLong, sShort};
    public static final String sDesc = "description for switch:s";
    public static final String[] sCompletions = new String[]{"sone", "stwo", "sthree", "fours", "fives", "sixs"};

    public static final String iLong = "--ilong";
    public static final String iShort = "-i";
    public static final String[] iSwitches = new String[]{iLong, iShort};
    public static final String iDesc = "description for switch:i";

    public static final String pLong = "--path";
    public static final String pShort = "-p";
    public static final String[] pSwitches = new String[]{pLong, pShort};
    public static final String pDesc = "description for path switch";
    public final String[] pCompletions;

    public static final String bLong = "--bLong";
    public static final String bShort = "-b";
    public static final String bDesc = "description for boolean switch";
    public static final String[] bCompletions = new String[]{"-b"};
    public static final String[] bCompletionsAfterValue = new String[]{" -b"};

    public TabTestArgs() throws URISyntaxException {
        File sourceDir = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        pCompletions = sourceDir.getParentFile().getParentFile().list();// /.../cliforce/cliforce/
    }


    @Parameter(names = {sLong, sShort}, description = sDesc, required = true)
    public String s;

    @Parameter(names = {iLong, iShort}, description = iDesc)
    public int i;

    @Parameter(names = {pLong, pShort}, description = pDesc, arity = 1, hidden = true)
    public File p;

    @Parameter(names = {bLong, bShort}, description = bDesc)
    public boolean b = false;
}
