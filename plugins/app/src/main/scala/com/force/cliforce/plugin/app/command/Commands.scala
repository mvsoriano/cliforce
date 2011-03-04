package com.force.cliforce.plugin.app.command

import com.beust.jcommander.Parameter
import com.vmforce.client.bean.ApplicationInfo
import com.vmforce.client.bean.ApplicationInfo.{StackEnum, ModelEnum, StagingBean, ResourcesBean}
import java.util.{Collections, ArrayList}
import collection.JavaConversions._
import com.force.cliforce.{CommandWriter, JCommand, CommandContext, Command}

class AppArg {
  @Parameter(description = "the name of the application", required = true)
  private var apps = new ArrayList[String]

  def app = apps.get(0)
}

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

class DeleteAppCommand extends JCommand[AppArg] {


  def executeWithArgs(ctx: CommandContext, arg: AppArg) = {
    ctx.getCommandWriter.println("Deleting %s".format(arg.app))
    ctx.getVmForceClient.deleteApplication(arg.app)
    ctx.getCommandWriter.println("done")
  }

  def describe = usage("deletes an application from vmforce")

  def name = "delete"
}

class PushArgs {
  @Parameter(
    description = "Name of the Application to push",
    required = true)
  var names = new ArrayList[String]

  def name = names.get(0)

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
      appInfo = ctx.getVmForceClient.getApplication(args.name)
    }
    ctx.getVmForceClient.deployApplication(args.name, args.path)
    ctx.getCommandWriter.printf("Application Deployed: %s\n", args.name)
    ctx.getCommandWriter.printf("Instances: %s\n", appInfo.getInstances.toString)
    ctx.getCommandWriter.printf("Memory: %sMB\n", appInfo.getResources.getMemory.toString)
    appInfo.getUris.foreach{
      ctx.getCommandWriter.printf("URI: http://%s\n", _)
    }

    ctx.getCommandWriter.printf("Note that push does not start/restart apps, please run 'app:start %s' or 'app:restart %s' as appropriate\n", args.name, args.name)
  }

  def describe = {
    usage("push an application to VMForce.")
  }

  def name = "push"
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


class TailArg {
  @Parameter(description = "App on which to tail a file", required = true)
  var apps = new ArrayList[String]

  def app = apps.get(0)

  @Parameter(names = Array("-i", "--instance"), description = "Instance on which to tail a file, default:0")
  var instance: String = "0"
  @Parameter(names = Array("-p", "--path"), description = "path to file", required = true)
  var path: String = null

}

class TailFileCommand extends JCommand[TailArg] {
  def name = "tail"

  def describe = usage("tail a file within a given app's instance. Note that the app must be running.")

  def executeWithArgs(ctx: CommandContext, args: TailArg) = {
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