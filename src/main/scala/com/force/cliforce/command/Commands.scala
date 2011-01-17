package com.force.cliforce.command

import com.sforce.soap.metadata.ListMetadataQuery
import com.vmforce.client.bean.ApplicationInfo
import collection.JavaConversions._
import com.beust.jcommander.{JCommander, Parameter}
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{Logger, Level}
import com.force.cliforce.{CLIForce, JCommand, CommandContext, Command}
import java.util.{Collections, ArrayList}
import com.vmforce.client.bean.ApplicationInfo.{StagingBean, ModelEnum, StackEnum, ResourcesBean}

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
  val version: Double = 20.0

  def execute(ctx: CommandContext) = {
    val q = new ListMetadataQuery
    q.setType("CustomObject")

    val objs = ctx.getMetadataConnection.listMetadata(Array(q), version).map(_.getFullName).filter(_.endsWith("__c"))
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
    description = "Memory to allocate to the app, in MB (default 512)")
  var mem: Int = 512

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
    ctx.getCommandWriter.printf("Pushing Application:%s", args.name)
    var appInfo = ctx.getVmForceClient.getApplication(args.name)
    if (appInfo == null) {
      ctx.getCommandWriter.printf("Application %s does not exist, creating\n", args.name)
      appInfo = new ApplicationInfo
      appInfo.setName(args.name)
      val res = new ResourcesBean
      res.setMemory(args.mem)
      appInfo.setInstances(args.instances)
      appInfo.setResources(res)
      appInfo.setUris(Collections.emptyList[String])
      val staging: StagingBean = new StagingBean
      staging.setModel(ModelEnum.SPRING.getRequestValue)
      staging.setStack(StackEnum.JT10.getRequestValue)
      appInfo.setStaging(staging)
      ctx.getVmForceClient.createApplication(appInfo)
    }
    ctx.getVmForceClient.deployApplication(args.name, args.path)
    ctx.getCommandWriter.printf("Application Deployed: %s\n", args.name)
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

class DebugArgs {
  @Parameter(names = Array("--on"), description = "Turns on debug logging to the console")
  val on = false
  @Parameter(names = Array("--off"), description = "Turns off debug logging to the console")
  val off = false

}

class DebugCommand(force: CLIForce) extends JCommand[DebugArgs] {
  def describe = usage("turns debug output on/off")

  def name = "debug"

  def executeWithArgs(ctx: CommandContext, args: DebugArgs) = {
    if (args.on ^ args.off) {
      var level = Level.DEBUG;
      if (args.off) {
        level = Level.INFO
      }
      ctx.getCommandWriter.printf("Setting logger level to %s\n", level.levelStr)
      val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger];
      rootLogger.setLevel(level)
      force.setDebug(args.on)
    }
  }
}

class DeleteAppCommand extends Command {

  object Arg {
    @Parameter(description = "The app to delete", required = true)
    var app = new ArrayList[String]
  }

  def execute(ctx: CommandContext) = {
    new JCommander(Arg, ctx.getCommandArguments.toArray: _*)
    ctx.getCommandWriter.println("Deleting %s".format(Arg.app.get(0)))
    ctx.getVmForceClient.deleteApplication(Arg.app.get(0))
    ctx.getCommandWriter.println("done")
  }

  def describe = "deletes an application from vmforce"

  def name = "delete"
}

