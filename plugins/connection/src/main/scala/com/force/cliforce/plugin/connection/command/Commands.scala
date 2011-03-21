package com.force.cliforce.plugin.connection.command

import collection.JavaConversions._
import javax.inject.Inject
import com.force.cliforce.{ForceEnv, CLIForce, CommandContext, Command}
import com.force.cliforce.Util._

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
URL:      %s
Host:     %s
User:     %s
Password: %s
Valid:    %s
Message:  %s
==========================="""
            .format(name, env.getUrl, env.getHost, env.getUser, env.getPassword, env.isValid.toString, Option(env.getMessage).toString))
        }
      }
    }
  }

  def describe = "list the currently available connections"

  def name = "list"
}

class CurrentConnectionCommand extends Command {
  @Inject
  var cliforce: CLIForce = null

  def execute(ctx: CommandContext) = {
    requireCliforce(cliforce, ctx)
    requireForceEnv(ctx)
    ctx.getCommandWriter.printf("Current Connection Name: %s\n", cliforce.getCurrentEnvironment)
    ctx.getCommandWriter.printf("Current User: %s\n", ctx.getForceEnv.getUser);
    ctx.getCommandWriter.printf("Current Endpoint: %s\n", ctx.getForceEnv.getHost);
  }

  def describe = "show the currently selected connection"

  def name = "current"
}


class AddConnectionCommand extends Command {
  @Inject
  var cliforce: CLIForce = null

  def execute(ctx: CommandContext) = {
    requireCliforce(cliforce,ctx)
    if (ctx.getCommandArguments.size != 2) {
      ctx.getCommandWriter.println("Error, command expects exactly two arguments")
      ctx.getCommandWriter.println(describe)
    } else {
      val name = ctx.getCommandArguments.apply(0)
      if (cliforce.getAvailableEnvironments.containsKey(name)) {
        ctx.getCommandWriter.printf("There is already a connection named %s, please rename or remove it first\n", name)
      } else {
        val env = new ForceEnv(ctx.getCommandArguments.apply(1), "cliforce");
        if (env.isValid) {
          cliforce.setAvailableEnvironment(name, env)
          if (cliforce.getAvailableEnvironments.size == 1) {
            cliforce.setDefaultEnvironment(name)
            cliforce.setCurrentEnvironment(name)
          }

        } else {
          ctx.getCommandWriter.printf("The url entered is invalid, reason:%s\n", env.getMessage)
        }
      }
    }
  }

  def describe = "add an available connection. Usage connection:add <connectionName> <connectionUrl>"

  def name = "add"
}

class DefaultConnectionCommand extends Command {
  @Inject
  var cliforce: CLIForce = null

  def execute(ctx: CommandContext) = {
    requireCliforce(cliforce,ctx)
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
        ctx.getCommandWriter.printf("There is no such environment: %s available\n", name)
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
    requireCliforce(cliforce,ctx)
    if (ctx.getCommandArguments.size != 1) {
      ctx.getCommandWriter.println("Error, command expects exactly one argument")
      ctx.getCommandWriter.println(describe)
    } else {
      val name = ctx.getCommandArguments.apply(0)
      if (cliforce.getAvailableEnvironments.containsKey(name)) {
        cliforce.setCurrentEnvironment(name)
      } else {
        ctx.getCommandWriter.printf("There is no such environment: %s available\n", name)
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
    requireCliforce(cliforce,ctx)
    if (ctx.getCommandArguments.size != 2) {
      ctx.getCommandWriter.println("Error, command expects exactly two arguments")
      ctx.getCommandWriter.println(describe)
    } else {
      val name = ctx.getCommandArguments.apply(0)
      val newname = ctx.getCommandArguments.apply(1)
      if (cliforce.getAvailableEnvironments.containsKey(name)) {
        cliforce.renameEnvironment(name, newname)
      } else {
        ctx.getCommandWriter.printf("There is no such environment: %s avaiable\n", name)
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
    requireCliforce(cliforce,ctx)
    if (ctx.getCommandArguments.size != 1) {
      ctx.getCommandWriter.println("Error, command expects exactly one argument")
      ctx.getCommandWriter.println(describe)
    } else {
      val name = ctx.getCommandArguments.apply(0)
      if (cliforce.getAvailableEnvironments.containsKey(name)) {
        cliforce.removeEnvironment(name)
        ctx.getCommandWriter.printf("Connection: %s removed\n", name)
      } else {
        ctx.getCommandWriter.printf("There is no such environment: %s available\n", name)
      }
    }
  }

  def describe = "remove a connection from cliforce. Usage connection:remove <connectionName>"

  def name = "remove"
}

