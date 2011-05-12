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

package com.force.cliforce.plugin.connection.command

import collection.JavaConversions._
import javax.inject.Inject
import com.force.cliforce.Util._
import com.beust.jcommander.Parameter
import com.force.cliforce._
import com.sforce.soap.partner.GetUserInfoResult

class ListConnectionsCommand extends Command {
  @Inject
  var cliforce: CLIForce = null

  def execute(ctx: CommandContext) = {
    if (cliforce.getAvailableEnvironments.size == 0) {
      ctx.getCommandWriter.println("There are no connections configured. Please use connection:add to add one.")
    } else {
      cliforce.getAvailableEnvironments.foreach {
        kv => kv match {
          case (name, env) => ctx.getCommandWriter.println("""
===========================
Name:         %s
Host:         %s
User:         %s
Password:     %s
OAuth Key:    %s
OAuth Secret: %s
Valid:        %s
Message:      %s
==========================="""
            .format(name, env.getHost, env.getUser, mask(env.getPassword), Option(env.getOauthKey).getOrElse("None"), Option(env.getOauthSecret).getOrElse("None"), env.isValid.toString, Option(env.getMessage).toString))
        }
      }
    }
  }

  def mask(password: String): String = {
    val build = new java.lang.StringBuilder
    for (i <- (1 to password.size)) {
      build.append("*")
    }
    build.toString
  }

  def describe = "list the currently available connections"

  def name = "list"
}

class CurrentConnectionCommand extends Command {
  @Inject
  var cliforce: CLIForce = null

  def execute(ctx: CommandContext) = {
    requireCliforce(cliforce)
    requireForceEnv(ctx)
    ctx.getCommandWriter.printfln("Current Connection Name: %s", cliforce.getCurrentEnvironment)
    ctx.getCommandWriter.printfln("Current User: %s", ctx.getForceEnv.getUser);
    ctx.getCommandWriter.printfln("Current Endpoint: %s", ctx.getForceEnv.getHost);
  }

  def describe = "show the currently selected connection"

  def name = "current"
}

class AddConnectionArgs {
  @Parameter(names = Array("-n", "--name"), description = "name of the connection")
  var name: String = null

  @Parameter(names = Array("-u", "--user"), description = "username with which to connect, youruser@your.org")
  var user: String = null

  @Parameter(names = Array("-p", "--password"), description = "password with which to connect")
  var password: String = null

  @Parameter(names = Array("-h", "--host"), description = "Host to connect to, defaults to login.salesforce.com")
  var host = "login.salesforce.com"

  @Parameter(names = Array("-t", "--token"), description = "security token with which to connect")
  var token: String = ""

  @Parameter(names = Array("--notoken"), description = "set this flag if security tokens are turned off in your org")
  var notoken = false

  @Parameter(names = Array("-k", "--oauth-key"), description = "oauth key (optional, if defined oauth secret is required too)")
  var oauthKey: String = null

  @Parameter(names = Array("-s", "--oauth-secret"), description = "oauth secret (optional, if defined oauth key is required too)")
  var oauthSecret: String = null


  def url = {
    var u = "force://" + host + ";user=" + user + ";password=" + password + token
    if (oauthKey != null) {
      u = u + ";oauth_key=" + oauthKey
    }
    if (oauthSecret != null) {
      u = u + ";oauth_secret=" + oauthSecret
    }
    u
  }

}


class AddConnectionCommand extends JCommand[AddConnectionArgs] {
  @Inject
  var cliforce: CLIForce = null

  def executeWithArgs(ctx: CommandContext, args: AddConnectionArgs) : Unit = {
    var interactive = false
    requireCliforce(cliforce)

    def duplicateNameCheck: Boolean = {
      if (args.name != null && (cliforce.getAvailableEnvironments.containsKey(args.name))) {
        ctx.getCommandWriter.printfln("There is already a connection named %s, please rename or remove it first", args.name)
        args.name = null
        return false
      }

      return true
    }

    def spaceCheck: Boolean = {
      if (args.name!= null && (args.name.contains(" ") || args.name.contains("\t"))) {
        ctx.getCommandWriter.printfln("Space and tab are not allowed in connection name.")
        args.name = null
        return false;
      }

      return true;
    }

    if (!duplicateNameCheck) return
    if(!spaceCheck) return

    while (args.name == null || args.name.trim.length == 0) {
      args.name = ctx.getCommandReader.readLine("connection name: ")
      spaceCheck
      args.name = if (args.name == null) null else args.name.trim
      duplicateNameCheck
    }
    while (args.user == null || (args.user eq "")) {
      args.user = ctx.getCommandReader.readLine("user: ")
      interactive = true
    }
    while (args.password == null || (args.password eq "")) {
      args.password = ctx.getCommandReader.readLine("password: ", '*')
      interactive = true
    }

    if (args.token == "" && !args.notoken) {
      args.token = ctx.getCommandReader.readLine("security token: ")
      interactive = true
    }

    if (args.host == null || (args.host eq "login.salesforce.com")) {
      args.host = ctx.getCommandReader.readLine("host (defaults to login.salesforce.com): ")
      interactive = true
      if (args.host == "") {
        args.host = new AddConnectionArgs().host
      }
    }

    if (interactive && args.oauthKey == null && args.oauthSecret == null) {
       val go = ctx.getCommandReader.readLine("Enter oauth key and secret? (Y to enter, anything else to skip): ")
      if ("Y".equalsIgnoreCase(go)) {
        val key = ctx.getCommandReader.readLine("oauth key:")
        if (key != null && (key ne "")) {
          args.oauthKey = key
        }
        val secret = ctx.getCommandReader.readLine("oauth secret:")
        if (secret != null && (secret ne "")) {
          args.oauthSecret = secret
        }
      }
    }


    val env = new ForceEnv(args.url, "cliforce");
    if (env.isValid) {
      cliforce.setAvailableEnvironment(args.name, env)
      if (cliforce.getAvailableEnvironments.size == 1) {
        cliforce.setDefaultEnvironment(args.name)
        cliforce.setCurrentEnvironment(args.name)
      }
      ctx.getCommandWriter.printfln("Connection: %s added", args.name)
    } else {
      ctx.getCommandWriter.printfln("The url entered is invalid, reason:%s", env.getMessage)
    }

  }

  def describe = usage("add a database connection")

  def name = "add"
}

class DefaultConnectionCommand extends Command {
  @Inject
  var cliforce: CLIForce = null

  def execute(ctx: CommandContext) = {
    requireCliforce(cliforce)
    if (ctx.getCommandArguments.size == 0) {
      ctx.getCommandWriter.printfln("The currently selected default connection name is: %s", cliforce.getDefaultEnvironment)
    } else if (ctx.getCommandArguments.size != 1) {
      ctx.getCommandWriter.println("Error, command expects exactly one argument")
      ctx.getCommandWriter.println(describe)
    } else {
      val name = ctx.getCommandArguments.apply(0)
      if (cliforce.getAvailableEnvironments.containsKey(name)) {
        cliforce.setDefaultEnvironment(name)
        cliforce.setCurrentEnvironment(name)
      } else {
        ctx.getCommandWriter.printfln("There is no such connection: %s available", name)
      }
    }
  }

  def describe = "set the current and default connection for cliforce. Usage connection:default <connectionName>"

  def name = "default"
}

class SetConnectionCommand extends Command {
  @Inject
  var cliforce: CLIForce = null

  def execute(ctx: CommandContext) = {
    requireCliforce(cliforce)
    if (ctx.getCommandArguments.size != 1) {
      ctx.getCommandWriter.println("Error, command expects exactly one argument")
      ctx.getCommandWriter.println(describe)
    } else {
      val name = ctx.getCommandArguments.apply(0)
      if (cliforce.getAvailableEnvironments.containsKey(name)) {
        cliforce.setCurrentEnvironment(name)
      } else {
        ctx.getCommandWriter.printfln("There is no such connection: %s available", name)
      }
    }
  }

  def describe = "set the current connection for cliforce. Usage connection:set <connectionName>"

  def name = "set"
}

class RenameConnectionCommand extends Command {
  @Inject
  var cliforce: CLIForce = null

  def execute(ctx: CommandContext) = {
    requireCliforce(cliforce)
    if (ctx.getCommandArguments.size != 2) {
      ctx.getCommandWriter.println("Error, command expects exactly two arguments")
      ctx.getCommandWriter.println(describe)
    } else {
      val name = ctx.getCommandArguments.apply(0)
      val newname = ctx.getCommandArguments.apply(1)
      if (cliforce.getAvailableEnvironments.containsKey(newname)) {
        ctx.getCommandWriter.printfln("There is already a connection named %s, please rename or delete it first", newname)
      } else if (cliforce.getAvailableEnvironments.containsKey(name)) {
        cliforce.renameEnvironment(name, newname)
        ctx.getCommandWriter.printfln("Renamed connection %s to %s", name, newname)
      } else {
        ctx.getCommandWriter.printfln("There is no such connection: %s avaiable", name)
      }
    }
  }

  def describe = "rename a connection for cliforce. Usage connection:rename <currentName> <newName>"

  def name = "rename"
}

class RemoveConnectionCommand extends Command {
  @Inject
  var cliforce: CLIForce = null

  def execute(ctx: CommandContext) = {
    requireCliforce(cliforce)
    if (ctx.getCommandArguments.size != 1) {
      ctx.getCommandWriter.println("Error, command expects exactly one argument")
      ctx.getCommandWriter.println(describe)
    } else {
      val name = ctx.getCommandArguments.apply(0)
      if (cliforce.getAvailableEnvironments.containsKey(name)) {
        cliforce.removeEnvironment(name)
        ctx.getCommandWriter.printfln("Connection: %s removed", name)
      } else {
        ctx.getCommandWriter.printfln("There is no such connection: %s available", name)
      }
    }
  }

  def describe = "remove a connection from cliforce. Usage connection:remove <connectionName>"

  def name = "remove"
}

class TestConnectionCommand extends Command {
  val log = new LazyLogger(classOf[TestConnectionCommand])

  def execute(ctx: CommandContext) = {
    try {
      requirePartnerConnection(ctx)
      val info: GetUserInfoResult = ctx.getPartnerConnection.getUserInfo
      log.get.debug("UserInfoResult" + info, info)
      info.toString
      ctx.getCommandWriter.println("connection valid")
    } catch {
      case e: Exception => {
        ctx.getCommandWriter.println("connection invalid")
        ctx.getCommandWriter.println("execute debug and retry to see failure information")
        log.get.debug("connection invalid", e)
      }
    }
  }

  def describe = "test the current connection. Usage connection:test <connectionName>"

  def name = "test"
}

