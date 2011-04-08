package com.force.cliforce.plugin.jpa.command

import com.sforce.soap.metadata.ListMetadataQuery
import com.force.cliforce.{Util, Command, CommandContext}
import com.sforce.soap.partner.DescribeSObjectResult


class ListCustomObjects extends Command {


  def execute(ctx: CommandContext) = {
    Util.requireMetadataConnection(ctx)
    Util.requirePartnerConnection(ctx)
    val q = new ListMetadataQuery
    q.setType("CustomObject")

    val objs = ctx.getMetadataConnection.listMetadata(Array(q), Util.getApiVersionAsDouble.doubleValue).map(_.getFullName).filter(_.endsWith("__c"))

    val results: Array[DescribeSObjectResult] = ctx.getPartnerConnection.describeSObjects(objs)
    if (results.size == 0) {
       ctx.getCommandWriter.println("There are no custom objects in your org")
    } else {
      results.foreach {
        res => {
          ctx.getCommandWriter.printf("\n{\nCustom Object-> %s \n", res.getName());
          res.getFields.foreach {
            field => ctx.getCommandWriter().printf("       field -> %s (type: %s)\n", field.getName(), field.getType().toString());
          }
          ctx.getCommandWriter.print("}\n")
        }
      }
    }
  }

  def describe = "list custom objects"

  def name = "list"
}
