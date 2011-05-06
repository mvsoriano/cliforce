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


import com.sforce.async.BulkConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;


/**
 * Command execution context.
 */
public interface CommandContext {

    /**
     * can be null
     *
     * @return
     */
    MetadataConnection getMetadataConnection();

    /**
     * can be null
     *
     * @return
     */
    PartnerConnection getPartnerConnection();

    /**
     * can be null
     *
     * @return
     */
    BulkConnection getBulkConnection();

    /**
     * can be null
     *
     * @return
     */
    ForceEnv getForceEnv();

    /**
     * This array will have the arguments passed to a given command, but not the actual command.
     * <p/>
     * Example:<p/>
     * user types: "someplugin:somecommand -a somearg -b someotherarg somemainarg
     * getCommandArguments returns {"-a", "somearg", "-b", "someotherarg", "somemainarg"}
     *
     * @return the arguments passed to the command but not the command itself.
     */
    String[] getCommandArguments();

    CommandReader getCommandReader();

    CommandWriter getCommandWriter();
    
    /**
     * can be null
     * 
     * @return
     */
    String getConnectionName();
}
