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

package com.force.cliforce.plugin.jpa.command

import java.util.Map;
import javax.persistence.spi.PersistenceProvider;
import com.beust.jcommander.Parameter;
import com.force.cliforce.CommandContext;
import java.util.{Map => JMap}

class JPAClean extends JPACommand[JPACleanParam] {

  override def name = "clean"

  override def describe = "Deletes schema for all writable JPA entities in the current org"

  override def executeInternal(ctx: CommandContext, args: JPACleanParam, persistenceProvider: PersistenceProvider, persistenceUnit: String, overrideProps: JMap[String, Object]): Unit = {
    overrideProps.put("force.deleteSchema", java.lang.Boolean.TRUE)
    if (args.force) {
      overrideProps.put("datanucleus.autoCreateSchema", java.lang.Boolean.TRUE)
    }
    if (args.purge) {
      overrideProps.put("force.purgeOnDeleteSchema", java.lang.Boolean.TRUE);
    }
    persistenceProvider.createEntityManagerFactory(persistenceUnit, overrideProps);
  }
}

class JPACleanParam extends JPAParam {
    @Parameter(names = Array("-p", "--purge"), description = "Purge on clean")
    var purge: Boolean = false;
    
    @Parameter(names = Array("-f", "--force"), description = "Force schema change even if 'datanucleus.autoCreateSchema' is false")
    var force: Boolean = true;
}