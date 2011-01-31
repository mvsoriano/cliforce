package com.force.cliforce.command

import com.sforce.soap.metadata.ListMetadataQuery
import com.vmforce.client.bean.ApplicationInfo
import collection.JavaConversions._
import com.beust.jcommander.Parameter
import com.force.cliforce.{CLIForce, JCommand, CommandContext, Command}
import java.util.{Collections, ArrayList}
import com.vmforce.client.bean.ApplicationInfo.{StagingBean, ModelEnum, StackEnum, ResourcesBean}
import com.force.cliforce.DefaultPlugin.ShellCommand
import java.io.IOException

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


class TailArg {
  @Parameter(names = Array("-a", "--app"), description = "App on which to tail a file", required = true)
  var app: String = null
  @Parameter(names = Array("-i", "--instance"), description = "Instance on which to tail a file, default:0")
  var instance: String = "0"
  @Parameter(names = Array("-p", "--path"), description = "path to file", required = true)
  var path: String = null

}

class TailFileCommand extends JCommand[TailArg] {
  def name = "tail"

  def describe = usage("tail a file within a given app's instance")

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

//  %w[logs/stderr.log logs/stdout.log logs/startup.log]
//cc_url = "#{droplets_uri}/#{appname}/instances/#{instance}/files/#{path}"
//cc_url.gsub!('files//', 'files/')


/*class LogsCommand extends JCommand[AppArg] {
  def executeWithArgs(ctx: CommandContext, args: AppArg) = {
    val token = ctx.getVmForceClient.getFullToken;
    val response = ctx.getRestTemplateConnector.executeRequestRaw("GET", "/apps/" + args.app + "/instances/0/files/logs/stderr.log", "", token)
    val reader = new BufferedReader(new InputStreamReader(response.getEntity.getContent))
    var line = reader.readLine
    while (line != null) {
      ctx.getCommandWriter.println(line)
      line = reader.readLine
    }

  }

  def describe = usage("show the logs for the application")

  def name = "logs"
}*/


class NewProjectArgs {

  @Parameter(names = Array("-g", "--group"), required = true, description = "groupId of the project to create")
  var group: String = null
  @Parameter(names = Array("-a", "--artifact"), required = true, description = "artifactId of the project to create")
  var artifact: String = null
  @Parameter(names = Array("-v", "--version"), description = "version of the project to create, default 1.0-SNAPSHOT")
  var version: String = "1.0-SNAPSHOT"
  @Parameter(names = Array("-p", "--package"), description = "root package for classes in project, defaults to groupId")
  var pkg: String = null
  @Parameter(names = Array("-t", "type"), description = "type of project single|multi defaults to single")
  var typ = "single"

  def getpkg(): String = {
    if (pkg eq null) group else pkg
  }

  def getGroupArtifact(): (String, String) = {
    val ag = "com.vmforce.samples.service.singlemodule"
    val art = if (typ eq "single") "sampleWebApp-archetype" else "multiWebApp?"
    (ag, art)
  }


}

class NewProjectContextWrapper(val ctx: CommandContext, val args: Array[String]) extends CommandContext {
  def getCommandWriter = ctx.getCommandWriter

  def getVmForceClient = ctx.getVmForceClient

  def getCommandReader = ctx.getCommandReader

  def getCommandArguments = args

  def getRestConnection = ctx.getRestConnection

  def getPartnerConnection = ctx.getPartnerConnection

  def getMetadataConnection = ctx.getMetadataConnection
}

class NewProjectCommand extends JCommand[NewProjectArgs] {
  def executeWithArgs(ctx: CommandContext, args: NewProjectArgs) = {
    val shell = new ShellCommand
    val cmd = Array("mvn", "archetype:generate", "-DinteractiveMode=false",
      "-DarchetypeCatalog=http://repo.t.salesforce.com/archiva/repository/snapshots/archetype-catalog.xml",
      "-DarchetypeGroupId=" + args.getGroupArtifact._1,
      "-DarchetypeArtifactId=" + args.getGroupArtifact._2,
      "-DarchetypeVersion=0.0.1-SNAPSHOT",
      "-DgroupId=" + args.group,
      "-DartifactId=" + args.artifact,
      "-Dversion=" + args.version,
      "-Dpackagename=" + args.getpkg
    )
    ctx.getCommandWriter.println("Executing:" + cmd.reduceLeft((acc, str) => acc + " " + str))
    try {
      shell.execute(new NewProjectContextWrapper(ctx, cmd));
    } catch {
      case ioe: IOException => {
        ctx.getCommandWriter.println("It appears you either dont have maven installed or on your path. Both are required to run this command.")
        ctx.getCommandWriter.println(ioe.getMessage)
      }
    }
  }

  def describe = usage("creates a new vmforce maven project from a maven archetype")

  def name = "newproj"
}

