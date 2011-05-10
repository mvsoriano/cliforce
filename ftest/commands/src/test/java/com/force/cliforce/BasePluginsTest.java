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

import java.io.IOException;

import com.force.cliforce.plugin.codegen.CodeGenPlugin;
import com.force.cliforce.plugin.connection.ConnectionPlugin;
import com.force.cliforce.plugin.db.DBPlugin;
import com.force.cliforce.plugin.jpa.JPAPlugin;
import com.force.cliforce.plugin.template.TemplatePlugin;

/**
 * 
 * Use this base class if you need to run a test with all plugins installed
 *
 * @author jeffrey.lai
 * @since 
 */
public class BasePluginsTest extends BaseCliforceCommandTest {

    @Override
    public String getPluginArtifact() {
        return null;
    }

    @Override
    public Plugin getPlugin() {
        return null;
    }
    
    /**
     * Override this method if only want certain plugins installed
     */
    @Override
    public void setupCLIForce(CLIForce c) throws IOException {
        c.installPlugin("db", "LATEST", new DBPlugin(), isInternal());
        c.installPlugin("connection", "LATEST", new ConnectionPlugin(), isInternal());
        c.installPlugin("template", "LATEST", new TemplatePlugin(), isInternal());
        c.installPlugin("jpa", "LATEST", new JPAPlugin(), isInternal());
        c.installPlugin("codegen", "LATEST", new CodeGenPlugin(), isInternal());
    }
    

}
