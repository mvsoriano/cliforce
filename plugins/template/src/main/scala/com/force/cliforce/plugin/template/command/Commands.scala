package com.force.cliforce.plugin.template.command

import java.io.IOException
import com.beust.jcommander.Parameter
import com.force.cliforce.DefaultPlugin.ShellCommand
import com.force.cliforce._
import java.util.ArrayList

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
  @Parameter(names = Array("-t", "type"), description = "type of project single|multi defaults to single")
  var typ = "springmvc"

  def getpkg(): String = {
    if (pkg eq null) group else pkg
  }

  def getGroupArtifact(): (String, String) = {
    val ag = "com.force.sdk"
    val art = typ+"-archetype"
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

class NewProjectCommand extends JCommand[NewProjectArgs] with ForceEnvAware {

  var forceEnv: ForceEnv = null

  def setForceEnv(env: ForceEnv) = {
    forceEnv = env
  }

  def getGroupFromEnv(artifact: String) = {
    val pkg = forceEnv.getUser.substring(forceEnv.getUser.indexOf("@") + 1)
    pkg.split("\\.").reverse.reduceLeft((acc, str) => acc + "." + str) + "." + artifact
  }

  def executeWithArgs(ctx: CommandContext, args: NewProjectArgs) = {
    if (args.group eq null) {
      args.group = getGroupFromEnv(args.artifact)
    }
    val shell = new ShellCommand
    val cmd = Array("mvn", "archetype:generate", "-DinteractiveMode=false",
      "-DarchetypeCatalog=http://repo.t.salesforce.com/archiva/repository/snapshots/archetype-catalog.xml",
      "-DarchetypeGroupId=" + args.getGroupArtifact._1,
      "-DarchetypeArtifactId=" + args.getGroupArtifact._2,
      "-DarchetypeVersion=LATEST",
      "-DgroupId=" + args.group,
      "-DartifactId=" + args.artifact,
      "-Dversion=" + args.version,
      "-Dpackagename=" + args.getpkg,
      "-DforceUrl=" + forceEnv.getUrl
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

  def name = "create"
}

class ListTemplatesCommand extends Command {
  def name = "list"

  def describe = "list the available project templates"

  def execute(ctx: CommandContext) = {
    ctx.getCommandWriter.println("single:  A simple maven based vmforce project using spring and jpa")
  }
}

