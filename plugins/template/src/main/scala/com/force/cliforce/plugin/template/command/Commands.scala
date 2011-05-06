/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

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

  def getCommandReader = ctx.getCommandReader

  def getForceEnv = ctx.getForceEnv

  def getCommandArguments = args

  def getBulkConnection = ctx.getBulkConnection

  def getPartnerConnection = ctx.getPartnerConnection

  def getMetadataConnection = ctx.getMetadataConnection

  def getConnectionName = ctx.getConnectionName
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
    val repos = Boot.getRepositories
    //forcesnap only defined if we are in a SNAPSHOT
    val repo = repos.getProperty("forcesnap", repos.getProperty("force"))
    val catalog = repo + "/archetype-catalog.xml"
    val rev = Boot.getCLIForceProperties.getProperty("version") match {
      case s if s.contains("SNAPSHOT") => "LATEST"
      case _ => "RELEASE"
    }

    val cmd = Array("mvn", "archetype:generate", "-DinteractiveMode=false",
      "-DarchetypeCatalog=" + catalog,
      "-DarchetypeGroupId=" + args.getGroupArtifact._1,
      "-DarchetypeArtifactId=" + args.getGroupArtifact._2,
      "-DarchetypeVersion=" + rev,
      "-DgroupId=" + args.group,
      "-DartifactId=" + args.artifact,
      "-Dversion=" + args.version,
      "-Dpackage=" + args.getpkg
    )
    //printed by sh => ctx.getCommandWriter.println("Executing:" + cmd.reduceLeft((acc, str) => acc + " " + str))
    try {
      shell.execute(cmd, ctx.getCommandWriter);
    } catch {
      case ioe: IOException => {
        ctx.getCommandWriter.println("It appears you either don't have maven installed, or it is not on your path. Both are required to run this command.")
        ctx.getCommandWriter.println(ioe.getMessage)
      }
    }
  }

  def describe = usage("creates a new forcesdk maven project from a maven archetype")

  def name = "create"
}

class ListTemplatesCommand extends Command {
  def name = "list"

  def describe = "list the available project templates"

  def execute(ctx: CommandContext) = {
    ctx.getCommandWriter.println("springmvc:  A simple maven-based forcesdk project using spring, springmvc and jpa")
  }
}

