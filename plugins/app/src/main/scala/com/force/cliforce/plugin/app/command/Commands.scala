package com.force.cliforce.plugin.app.command

import com.vmforce.client.bean.ApplicationInfo
import com.vmforce.client.bean.ApplicationInfo.{StackEnum, ModelEnum, StagingBean, ResourcesBean}
import java.util.{Collections, ArrayList}
import collection.JavaConversions._
import java.io.File
import com.beust.jcommander.converters.FileConverter
import collection.mutable.HashMap
import com.force.cliforce.{ForceEnv, JCommand, CommandContext, Command}
import com.beust.jcommander.{ParameterDescription, Parameter}
import java.lang.String
import jline.console.completer.StringsCompleter
import com.force.cliforce.Util._

object AppNameCache {
  lazy val cache = new HashMap[ForceEnv, List[String]];

  def populate(ctx: CommandContext): List[String] = {
    val apps = asScalaIterable(ctx.getVmForceClient.getApplications).map(_.getName).toList
    cache += ctx.getForceEnv -> apps
    apps
  }

  def getApps(ctx: CommandContext): List[String] = {
    cache.get(ctx.getForceEnv).getOrElse{
      populate(ctx)
    }
  }
}

abstract class AppCommand extends JCommand[AppArg] {
  override def getCompletionsForSwitch(switchForCompletion: String, partialValue: String, parameterDescription: ParameterDescription, ctx: CommandContext) = {
    if (switchForCompletion eq JCommand.MAIN_PARAM) {
      val apps = AppNameCache.getApps(ctx)
      val candidates = new ArrayList[CharSequence]
      val cursor = new StringsCompleter(apps).complete(partialValue, partialValue.length, candidates)
      candidates
    } else {
      super.getCompletionsForSwitch(switchForCompletion, partialValue, parameterDescription, ctx)
    }
  }
}

class AppArg {
  @Parameter(description = "the name of the application", required = true)
  private var apps = new ArrayList[String]

  def app = apps.get(0)
}

class AppsCommand extends Command {
  def execute(ctx: CommandContext) = {
    requireVMForceClient(ctx)
    asScalaIterable(ctx.getVmForceClient.getApplications).foreach{
      app: ApplicationInfo => {
        val health = ctx.getVmForceClient.getApplicationHealth(app)
        ctx.getCommandWriter.println("""
===========================
App:              %s
Running Instances:%d
Total Instances:  %d
Health:           %s
State:            %s
Memory:           %dMB
==========================="""
          .format(app.getName, app.getRunningInstances, app.getInstances, health.name, app.getState, app.getResources.getMemory))
      }
    }
  }

  def describe = "lists deployed apps"

  def name = "apps"
}

class DeleteAppCommand extends AppCommand {


  def executeWithArgs(ctx: CommandContext, arg: AppArg) = {
    requireVMForceClient(ctx)
    ctx.getCommandWriter.println("Deleting %s".format(arg.app))
    ctx.getVmForceClient.deleteApplication(arg.app)
    AppNameCache.populate(ctx)
    ctx.getCommandWriter.println("done")
  }

  def describe = usage("deletes an application from VMforce")

  def name = "delete"
}

class PushArgs {
  @Parameter(
    description = "Name of the application to push",
    required = true)
  var names = new ArrayList[String]

  def name = names.get(0)

  @Parameter(
    names = Array("-m", "--mem"),
    description = "Memory to allocate to the application, in MB (default 512)")
  var mem: Int = 512

  @Parameter(
    names = Array("-i", "--instances"),
    description = "Number of instances to deploy (default 1)")
  var instances: Int = 1

  @Parameter(
    names = Array("-p", "--path"),
    description = "Local path to the deployable application",
    required = true,
    converter = classOf[FileConverter])
  var path: File = null;
}

class PushCommand extends JCommand[PushArgs] {

  def executeWithArgs(ctx: CommandContext, args: PushArgs) = {
    requireVMForceClient(ctx)
    ctx.getCommandWriter.printf("Pushing Application: %s\n", args.name)
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
      AppNameCache.populate(ctx)
      appInfo = ctx.getVmForceClient.getApplication(args.name)
    }
    ctx.getVmForceClient.deployApplication(args.name, args.path.getAbsolutePath)
    ctx.getCommandWriter.printf("Application Deployed: %s\n", args.name)
    ctx.getCommandWriter.printf("Instances: %s\n", appInfo.getInstances.toString)
    ctx.getCommandWriter.printf("Memory: %sMB\n", appInfo.getResources.getMemory.toString)
    appInfo.getUris.foreach{
      ctx.getCommandWriter.printf("URI: http://%s\n", _)
    }

    ctx.getCommandWriter.printf("Note that push does not start/restart applications, please run 'app:start %s' or 'app:restart %s' as appropriate\n", args.name, args.name)
  }

  def describe = {
    usage("push an application to VMforce.")
  }

  def name = "push"
}


class StartCommand extends AppCommand {


  def executeWithArgs(ctx: CommandContext, arg: AppArg) = {
    requireVMForceClient(ctx)
    ctx.getCommandWriter.println("Starting %s".format(arg.app))
    ctx.getVmForceClient.startApplication(arg.app)
    ctx.getCommandWriter.println("done")
  }

  def describe = usage("start an application")

  def name = "start"
}

class StopCommand extends AppCommand {


  def executeWithArgs(ctx: CommandContext, arg: AppArg) = {
    requireVMForceClient(ctx)
    ctx.getCommandWriter.println("Stopping %s".format(arg.app))
    ctx.getVmForceClient.stopApplication(arg.app)
    ctx.getCommandWriter.println("done")
  }


  def describe = usage("Stop an application")

  def name = "stop"
}

class RestartCommand extends AppCommand {


  def executeWithArgs(ctx: CommandContext, arg: AppArg) = {
    requireVMForceClient(ctx)
    ctx.getCommandWriter.println("Restarting %s".format(arg.app))
    ctx.getVmForceClient.restartApplication(arg.app)
    ctx.getCommandWriter.println("done")
  }

  def describe = usage("restart an application")

  def name = "restart"
}


class TailArg {
  @Parameter(description = "Application on which to tail a file", required = true)
  var apps = new ArrayList[String]

  def app = apps.get(0)

  @Parameter(names = Array("-i", "--instance"), description = "Instance on which to tail a file, default:0")
  var instance: String = "0"
  @Parameter(names = Array("-p", "--path"), description = "path to file", required = true)
  var path: String = null

}

class TailFileCommand extends JCommand[TailArg] {
  def name = "tail"

  def describe = usage("tail a file within a given application's instance. Note that the application must be running.")

  def executeWithArgs(ctx: CommandContext, args: TailArg) = {
    requireVMForceClient(ctx)
    val tailer = ctx.getVmForceClient.getTailFile(args.app, args.instance, args.path);
    @volatile var go = true;
    var dot = false;
    val r = new Runnable() {
      def run = {
        while (go) {
          var tail = tailer.tail()
          if (tail == null || tail.length == 0) {
            ctx.getCommandWriter.print(".")
            dot = true;
          } else {
            if (dot) {
              ctx.getCommandWriter.println(".")
            }
            ctx.getCommandWriter.print(tail)
          }
          Thread.sleep(2000)
        }
      }
    }
    val thread = new Thread(r);
    ctx.getCommandWriter.printf("Tailing %s on app: %s instance %s, press enter to interrupt\n", args.path, args.app, args.instance);
    thread.start
    ctx.getCommandReader.readLine("")
    go = false
    thread.join
  }
}