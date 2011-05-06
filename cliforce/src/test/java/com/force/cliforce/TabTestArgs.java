/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
