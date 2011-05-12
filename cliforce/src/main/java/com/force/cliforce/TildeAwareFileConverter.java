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

import static com.force.cliforce.Util.withSeparator;

import java.io.File;

import jline.internal.Configuration;

import com.beust.jcommander.IStringConverter;

/**
 * TildeAwareFileConverter
 *
 * @author sclasen
 */
public class TildeAwareFileConverter implements IStringConverter<java.io.File> {

    @Override
    public File convert(String value) {
        File homeDir = new File(System.getProperty("user.home"));
        String translated = value;
        // Special character: ~ maps to the user's home directory
        if (translated.startsWith(withSeparator("~"))) {
            translated = homeDir.getPath() + translated.substring(1);
        } else if (translated.startsWith("~")) {
            translated = homeDir.getParentFile().getAbsolutePath();
        } else if (!startsWithRoot(translated)) {
            String cwd = getUserDir().getAbsolutePath();
            translated = withSeparator(cwd) + translated;
        }
        return new File(translated);
    }

    protected File getUserHome() {
        return Configuration.getUserHome();
    }

    protected File getUserDir() {
        return new File(".");
    }
    
    private boolean startsWithRoot(String filePath) {
        for (File root : File.listRoots()) {
            if (filePath.startsWith(root.getAbsolutePath())) {
                return true;
            }
        }
        
        return false;
    }
}
