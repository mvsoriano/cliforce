package com.force.cliforce.plugin.connection.command

import collection.JavaConversions._
import javax.inject.Inject
import com.force.cliforce.Util._
import com.beust.jcommander.Parameter
import com.force.cliforce._

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
Name:     %s
Host:     %s
User:     %s
Password: %s
Valid:    %s
Message:  %s
==========================="""
            .format(name, env.getHost, env.getUser, mask(env.getPassword), env.isValid.toString, Option(env.getMessage).toString))
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
    ctx.getCommandWriter.printf("Current Connection Name: %s\n", cliforce.getCurrentEnvironment)
    ctx.getCommandWriter.printf("Current User: %s\n", ctx.getForceEnv.getUser);
    ctx.getCommandWriter.printf("Current Endpoint: %s\n", ctx.getForceEnv.getHost);
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

  @Parameter(names = Array("-h", "--host"), description = "Host to connect to, defaults to vmf01.t.salesforce.com")
  var host = "vmf01.t.salesforce.com"

  @Parameter(names = Array("-t", "--token"), description = "security token with which to connect")
  var token: String = ""

  @Parameter(names = Array("--notoken"), description = "set this flag if security tokens are turned off in your org")
  var notoken = false

  def url = "force://" + host + ";user=" + user + ";password=" + password + token

}


class AddConnectionCommand extends JCommand[AddConnectionArgs] {
  @Inject
  var cliforce: CLIForce = null

  def executeWithArgs(ctx: CommandContext, args: AddConnectionArgs) = {
    requireCliforce(cliforce)
    if (args.name != null && (cliforce.getAvailableEnvironments.containsKey(args.name))) {
      ctx.getCommandWriter.printf("There is already a connection named %s, please rename or remove it first\n", name)
      args.name = null
    }
    while (args.name == null || (args.name eq "")) {
      args.name = ctx.getCommandReader.readLine("connection name: ")
      if (args.name != null && (cliforce.getAvailableEnvironments.containsKey(args.name))) {
        ctx.getCommandWriter.printf("There is already a connection named %s, please rename or remove it first\n", name)
        args.name = null
      }
    }
    while (args.user == null || (args.user eq "")) {
      args.user = ctx.getCommandReader.readLine("user: ")
    }
    while (args.password == null || (args.password eq "")) {
      args.password = ctx.getCommandReader.readLine("password: ", '*')
    }

    if (args.token == "" && !args.notoken) {
      args.token = ctx.getCommandReader.readLine("security token: ")
    }

    val env = new ForceEnv(args.url, "cliforce");
    if (env.isValid) {
      cliforce.setAvailableEnvironment(args.name, env)
      if (cliforce.getAvailableEnvironments.size == 1) {
        cliforce.setDefaultEnvironment(args.name)
        cliforce.setCurrentEnvironment(args.name)
      }
      ctx.getCommandWriter.printf("Connection: %s added\n", args.name)
    } else {
      ctx.getCommandWriter.printf("The url entered is invalid, reason:%s\n", env.getMessage)
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
      ctx.getCommandWriter.printf("The currently selected default connection name is: %s\n", cliforce.getDefaultEnvironment)
    } else if (ctx.getCommandArguments.size != 1) {
      ctx.getCommandWriter.println("Error, command expects exactly one argument")
      ctx.getCommandWriter.println(describe)
    } else {
      val name = ctx.getCommandArguments.apply(0)
      if (cliforce.getAvailableEnvironments.containsKey(name)) {
        cliforce.setDefaultEnvironment(name)
        cliforce.setCurrentEnvironment(name)
      } else {
        ctx.getCommandWriter.printf("There is no such connection: %s available\n", name)
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
        ctx.getCommandWriter.printf("There is no such connection: %s available\n", name)
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
        ctx.getCommandWriter.printf("There is already a connection named %s, please rename or delete it first\n", newname)
      } else if (cliforce.getAvailableEnvironments.containsKey(name)) {
        cliforce.renameEnvironment(name, newname)
        ctx.getCommandWriter.printf("Renamed connection %s to %s\n", name, newname)
      } else {
        ctx.getCommandWriter.printf("There is no such connection: %s avaiable\n", name)
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
        ctx.getCommandWriter.printf("Connection: %s removed\n", name)
      } else {
        ctx.getCommandWriter.printf("There is no such connection: %s available\n", name)
      }
    }
  }

  def describe = "remove a connection from cliforce. Usage connection:remove <connectionName>"

  def name = "remove"
}

