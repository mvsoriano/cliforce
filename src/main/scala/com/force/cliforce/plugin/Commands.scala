package com.force.cliforce.plugin

import com.sforce.soap.metadata.ListMetadataQuery
import com.force.cliforce.{CommandContext, Command}
import com.vmforce.client.bean.ApplicationInfo
import collection.JavaConversions._
import com.beust.jcommander.{JCommander, Parameter}
import com.vmforce.client.bean.ApplicationInfo.ResourcesBean
import java.util.ArrayList

class AppsCommand extends Command {
  def execute(ctx: CommandContext) = {
    asScalaIterable(ctx.getVmForceClient.getApplications).foreach{
      app: ApplicationInfo => {
        ctx.getCommandWriter.println("============================")
        ctx.getCommandWriter.println("App: %s".format(app.getName))
        ctx.getCommandWriter.println("Instances: %d".format(app.getInstances))
        ctx.getCommandWriter.println("State: %s".format(app.getState.toString))
        ctx.getCommandWriter.println("Memory: %dMB".format(app.getResources.getMemory))
        ctx.getCommandWriter.println("============================\n\n")
      }
    }
  }

  def describe = "lists deployed apps"

  def name = "apps"
}

class ListCustomObjects extends Command {
  def execute(ctx: CommandContext) = {
    val q = new ListMetadataQuery
    q.setType("CustomObject")
    val objs = ctx.getMetadataConnection.listMetadata(Array(q), 20.0).map(_.getFullName).filter(_.endsWith("__c"))
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

class PushCommand extends Command {

  object Args {
    @Parameter(
      names = Array("-n", "--name"),
      description = "Name of the Application to push",
      required = true)
    var name: String = null

    @Parameter(
      names = Array("-m", "--mem"),
      description = "Memory to allocate to the app, in MB (default 256)")
    var mem: Int = 256

    @Parameter(
      names = Array("-i", "--instances"),
      description = "Number of instances to deploy (default 1)")
    var instances: Int = 1

    @Parameter(
      names = Array("-p", "--path"),
      description = "Local path to the deployable app",
      required = true)
    var path: String = null;

  }

  def execute(ctx: CommandContext) = {
    new JCommander(Args, ctx.getCommandArguments.toArray: _*)
    val appInfo = new ApplicationInfo
    appInfo.setName(Args.name)
    val res = new ResourcesBean
    res.setMemory(Args.mem)
    appInfo.setInstances(Args.instances)
    appInfo.setResources(res)
    ctx.getVmForceClient.createApplication(appInfo)
    ctx.getVmForceClient.deployApplication(name, Args.path)
    ctx.getCommandWriter.println(Args.name)
  }

  def describe = {
    val usage = new StringBuilder("push an application to VMForce.\n\tUsage:\n")
    val jc = new JCommander(Args, Array.empty[String]: _*)
    jc.getParameters.foreach{
      p => {
        usage.append("\t").append(p.getNames).append("\t").append(p.getDescription).append("\n")
      }
    }
    usage.toString
  }

  def name = "push"
}

class StartCommand extends Command {

  object Arg {
    @Parameter(description = "The app to start", required = true)
    var app = new ArrayList[String]
  }

  def execute(ctx: CommandContext) = {
    new JCommander(Arg, ctx.getCommandArguments.toArray: _*)
    ctx.getCommandWriter.println("Starting %s".format(Arg.app.get(0)))
    ctx.getVmForceClient.startApplication(Arg.app.get(0))
    ctx.getCommandWriter.println("done")
  }

  def describe = "start an application"

  def name = "start"
}

class StopCommand extends Command {

  object Arg {
    @Parameter(description = "The app to stop", required = true)
    var app = new ArrayList[String]
  }

  def execute(ctx: CommandContext) = {
    new JCommander(Arg, ctx.getCommandArguments.toArray: _*)
    ctx.getCommandWriter.println("Stopping %s".format(Arg.app.get(0)))
    ctx.getVmForceClient.stopApplication(Arg.app.get(0))
    ctx.getCommandWriter.println("done")
  }

  def describe = "Stop an application"

  def name = "stop"
}

class RestartCommand extends Command {

  object Arg {
    @Parameter(description = "The app to restart", required = true)
    var app = new ArrayList[String]
  }

  def execute(ctx: CommandContext) = {
    new JCommander(Arg, ctx.getCommandArguments.toArray: _*)
    ctx.getCommandWriter.println("Stopping %s".format(Arg.app.get(0)))
    ctx.getVmForceClient.stopApplication(Arg.app.get(0))
    ctx.getCommandWriter.println("done")
    ctx.getCommandWriter.println("Starting %s".format(Arg.app.get(0)))
    ctx.getVmForceClient.startApplication(Arg.app.get(0))
    ctx.getCommandWriter.println("done")
  }

  def describe = "restart an application"

  def name = "restart"
}