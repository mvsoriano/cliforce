/*
 * Copyright, 1999-2010, SALESFORCE.com 
 * All Rights Reserved
 * Company Confidential
 */
package com.force.cliforce;

import com.beust.jcommander.Parameter;

/**
 * TabTestArgs
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


    @Parameter(names = {sLong, sShort}, description = sDesc)
    public String s;

    @Parameter(names = {iLong, iShort}, description = iDesc)
    public int i;
}
