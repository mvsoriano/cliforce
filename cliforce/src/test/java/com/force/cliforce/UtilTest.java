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


import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class UtilTest {

    @Test
    public void getVersionAsDouble() {
        Assert.assertEquals(22.0, Util.getApiVersionAsDouble());
        Assert.assertEquals("22.0", Util.getApiVersion());
    }

    @Test
    public void testPropertiesReadWrite() throws IOException {
        Properties p = new Properties();
        p.setProperty("test", "123");
        String id = Long.toString(System.currentTimeMillis());
        File f = Util.getForcePropertiesFile(id);
        if(!f.getParentFile().exists()){
            f.getParentFile().mkdir();
        }
        f.deleteOnExit();
        p.store(new FileOutputStream(f), "utilTest");
        Properties read = new Properties();
        Util.readProperties(id, read);
        Assert.assertEquals(read.getProperty("test"), "123");
        Properties write = new Properties();
        write.setProperty("test", "234");
        Util.writeProperties(id, write);
        p = new Properties();
        p.load(new FileInputStream(f));
        Assert.assertEquals(p.getProperty("test"), "234");
    }


}
