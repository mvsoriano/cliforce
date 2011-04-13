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