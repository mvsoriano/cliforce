package com.force.cliforce.command

import com.sforce.soap.metadata.ListMetadataQuery
import com.vmforce.client.bean.ApplicationInfo
import collection.JavaConversions._
import com.beust.jcommander.Parameter
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


class AppArg {
  @Parameter(description = "the name of the application", required = true)
  private var apps = new ArrayList[String]

  def app = apps(0);
}

class StartCommand extends JCommand[AppArg] {


  def executeWithArgs(ctx: CommandContext, arg: AppArg) = {
    ctx.getCommandWriter.println("Starting %s".format(arg.app))
    ctx.getVmForceClient.startApplication(arg.app)
    ctx.getCommandWriter.println("done")
  }

  def describe = usage("start an application")

  def name = "start"
}

class StopCommand extends JCommand[AppArg] {


  def executeWithArgs(ctx: CommandContext, arg: AppArg) = {
    ctx.getCommandWriter.println("Stopping %s".format(arg.app))
    ctx.getVmForceClient.stopApplication(arg.app)
    ctx.getCommandWriter.println("done")
  }


  def describe = usage("Stop an application")

  def name = "stop"
}

class RestartCommand extends JCommand[AppArg] {


  def executeWithArgs(ctx: CommandContext, arg: AppArg) = {
    ctx.getCommandWriter.println("Restarting %s".format(arg.app))
    ctx.getVmForceClient.restartApplication(arg.app)
    ctx.getCommandWriter.println("done")
  }

  def describe = usage("restart an application")

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
      force.setDebug(args.on)
    }
  }
}

class DeleteAppCommand extends JCommand[AppArg] {


  def executeWithArgs(ctx: CommandContext, arg: AppArg) = {
    ctx.getCommandWriter.println("Deleting %s".format(arg.app))
    ctx.getVmForceClient.deleteApplication(arg.app)
    ctx.getCommandWriter.println("done")
  }

  def describe = usage("deletes an application from vmforce")

  def name = "delete"
}

