package com.force.cliforce.plugin

import com.sforce.soap.metadata.ListMetadataQuery
import com.vmforce.client.bean.ApplicationInfo
import collection.JavaConversions._
import com.beust.jcommander.{JCommander, Parameter}
import com.vmforce.client.bean.ApplicationInfo.ResourcesBean
import java.util.ArrayList
import com.force.cliforce.{JCommand, CommandContext, Command}

class AppsCommand extends Command {
  def execute(ctx: CommandContext) = {
    asScalaIterable(ctx.getVmForceClient.getApplications).foreach{
      app: ApplicationInfo => {
        ctx.getCommandWriter.println("""
===========================
App:       %s
Instances: %d
State:     %s
Memory:    %dMB
==========================="""
          .format(app.getName, app.getInstances, app.getState, app.getResources.getMemory))
      }
    }
  }

  def describe = "lists deployed apps"

  def name = "apps"
}

class BannerCommand extends Command {
  //http://www.network-science.de/ascii/  -> big font
  def execute(ctx: CommandContext) = {
    ctx.getCommandWriter.print("""
  _____ _      _____ ______
 / ____| |    |_   _|  ____|
| |    | |      | | | |__ ___  _ __ ___ ___
| |    | |      | | |  __/ _ \| '__/ __/ _ \
| |____| |____ _| |_| | | (_) | | | (_|  __/
 \_____|______|_____|_|  \___/|_|  \___\___|

""")
  }

  def describe = "print the banner"

  def name = "banner"
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

class PushArgs {
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

class PushCommand extends JCommand[PushArgs] {

  def executeWithArgs(ctx: CommandContext, args: PushArgs) = {
    val appInfo = new ApplicationInfo
    appInfo.setName(args.name)
    val res = new ResourcesBean
    res.setMemory(args.mem)
    appInfo.setInstances(args.instances)
    appInfo.setResources(res)
    ctx.getVmForceClient.createApplication(appInfo)
    ctx.getVmForceClient.deployApplication(name, args.path)
    ctx.getCommandWriter.println(args.name)
  }

  def describe = {
    usage("push an application to VMForce.")
  }

  def name = "push"
}

class UpdateArgs {
  @Parameter(names = Array("-n", "--name"), description = "The name of the app to update", required = true)
  var appName = null;
}

class UpdateCommand extends JCommand[UpdateArgs] {
  def describe = "update an already deployed app"

  def name = "update"

  def executeWithArgs(ctx: CommandContext, args: UpdateArgs) = {

  }
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
    ctx.getCommandWriter.println("Restarting %s".format(Arg.app.get(0)))
    ctx.getVmForceClient.restartApplication(Arg.app.get(0))
    ctx.getCommandWriter.println("done")
  }

  def describe = "restart an application"

  def name = "restart"
}

