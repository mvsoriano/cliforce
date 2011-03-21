package com.force.cliforce.plugin.db.command

import com.sforce.soap.metadata.ListMetadataQuery
import com.force.cliforce.{Util, Command, CommandContext}


class ListCustomObjects extends Command {


  def execute(ctx: CommandContext) = {
    Util.requireMetadataConnection(ctx)
    Util.requirePartnerConnection(ctx)
    val q = new ListMetadataQuery
    q.setType("CustomObject")

    val objs = ctx.getMetadataConnection.listMetadata(Array(q), Util.getApiVersionAsDouble.doubleValue).map(_.getFullName).filter(_.endsWith("__c"))
    ctx.getPartnerConnection.describeSObjects(objs).foreach{
      res => {
        ctx.getCommandWriter.printf("\n{\nCustom Object-> %s \n", res.getName());
        res.getFields.foreach{
          field => ctx.getCommandWriter().printf("       field -> %s (type: %s)\n", field.getName(), field.getType().toString());
        }
        ctx.getCommandWriter.print("}\n")
      }
    }
  }

  def describe = "list custom objects"

  def name = "list"
}
