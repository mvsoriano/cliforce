package com.force.cliforce.plugin.template.command

import com.beust.jcommander.Parameter
import com.force.cliforce._
import java.util.ArrayList
import com.force.cliforce.DefaultPlugin.ShellExecutor
import java.io.{File, IOException}

class NewProjectArgs {

  @Parameter(names = Array("-g", "--group"), description = "groupId of the project to create, defaults to org name(reversed, ie my.org = org.my).artifactId")
  var group: String = null
  @Parameter(required = true, description = "artifactId/name of the project to create")
  var artifacts = new ArrayList[String]

  def artifact = artifacts.get(0)

  @Parameter(names = Array("-v", "--version"), description = "version of the project to create, default 1.0-SNAPSHOT")
  var version: String = "1.0-SNAPSHOT"
  @Parameter(names = Array("-p", "--package"), description = "root package for classes in project, defaults to groupId")
  var pkg: String = null
  /* uncomment when there is more than one choice
  @Parameter(names = Array("-t", "type"), description = "type of project default:springmvc")
  */
  @Parameter(names = Array("-d", "--dir"), description = "directory to create the project in, defaults to current dir.")
  var dir: File = null

  var typ = "springmvc"

  def getpkg(): String = {
    if (pkg eq null) group else pkg
  }

  def getGroupArtifact(): (String, String) = {
    val ag = "com.force.sdk"
    val art = typ + "-archetype"
    (ag, art)
  }


}

class NewProjectContextWrapper(val ctx: CommandContext, val args: Array[String]) extends CommandContext {
  def getCommandWriter = ctx.getCommandWriter

  def getVmForceClient = ctx.getVmForceClient

  def getCommandReader = ctx.getCommandReader

  def getForceEnv = ctx.getForceEnv

  def getCommandArguments = args

  def getBulkConnection = ctx.getBulkConnection

  def getPartnerConnection = ctx.getPartnerConnection

  def getMetadataConnection = ctx.getMetadataConnection

}

class NewProjectCommand extends JCommand[NewProjectArgs] {


  def getGroupFromEnv(forceEnv: ForceEnv, artifact: String): String = {
    val pkg = forceEnv.getUser.substring(forceEnv.getUser.indexOf("@") + 1)
    pkg.split("\\.").reverse.reduceLeft((acc, str) => acc + "." + str) + "." + artifact
  }

  def executeWithArgs(ctx: CommandContext, args: NewProjectArgs): Unit = {

    if (args.group eq null) {
      if (ctx.getForceEnv ne null) {
        args.group = getGroupFromEnv(ctx.getForceEnv, args.artifact)
      } else {
        args.group = "org.example." + args.artifact
      }
    }
    val shell = new ShellExecutor
    if (args.dir ne null) {
      if (args.dir.exists && args.dir.isDirectory) {
        shell.setWorkingDir(args.dir)
      } else {
        ctx.getCommandWriter.printf("%s does not exist or is not a directory\n", args.dir.getAbsolutePath)
        return
      }

    }
    val cmd = Array("mvn", "archetype:generate", "-DinteractiveMode=false",
      "-DarchetypeCatalog=http://repo.t.salesforce.com/archiva/repository/snapshots/archetype-catalog.xml",
      "-DarchetypeGroupId=" + args.getGroupArtifact._1,
      "-DarchetypeArtifactId=" + args.getGroupArtifact._2,
      "-DarchetypeVersion=LATEST",
      "-DgroupId=" + args.group,
      "-DartifactId=" + args.artifact,
      "-Dversion=" + args.version,
      "-Dpackage=" + args.getpkg
    )
    ctx.getCommandWriter.println("Executing:" + cmd.reduceLeft((acc, str) => acc + " " + str))
    try {
      shell.execute(cmd, ctx.getCommandWriter);
    } catch {
      case ioe: IOException => {
        ctx.getCommandWriter.println("It appears you either don't have maven installed, or it is not on your path. Both are required to run this command.")
        ctx.getCommandWriter.println(ioe.getMessage)
      }
    }
  }

  def describe = usage("creates a new VMforce maven project from a maven archetype")

  def name = "create"
}

class ListTemplatesCommand extends Command {
  def name = "list"

  def describe = "list the available project templates"

  def execute(ctx: CommandContext) = {
    ctx.getCommandWriter.println("springmvc:  A simple maven-based VMforce project using spring, springmvc and jpa")
  }
}

