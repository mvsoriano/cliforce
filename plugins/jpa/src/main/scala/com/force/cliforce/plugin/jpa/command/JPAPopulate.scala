package com.force.cliforce.plugin.jpa.command

import java.util.Map
import javax.persistence.spi.PersistenceProvider
import com.beust.jcommander.Parameter
import com.force.cliforce.CommandContext
import java.util.{Map => JMap}

class JPAPopulate extends JPACommand[JPAPopulateParam] {

  override def name = "populate"

  override def describe = "Populate schema for all writable JPA entities in the current org"

  override def executeInternal(ctx: CommandContext, args: JPAPopulateParam, persistenceProvider: PersistenceProvider, persistenceUnit: String, overrideProps: JMap[String, Object]): Unit = {
    if (args.force) {
      overrideProps.put("datanucleus.autoCreateSchema", java.lang.Boolean.TRUE)
    }
    // All we do is create an EntityManagerFactory. The rest of the magic happens as part of JPA initialization from the Provider
    persistenceProvider.createEntityManagerFactory(persistenceUnit, overrideProps);
  }
}

class JPAPopulateParam extends JPAParam {
  @Parameter(names = Array("-f", "--force"),
    description = "Force schema change even if 'datanucleus.autoCreateSchema' is false")
    var force: Boolean = false
}
