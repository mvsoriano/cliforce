package com.force.cliforce.plugin.connection.command

import collection.JavaConversions._
import com.force.cliforce.{ForceEnv, CLIForce, CommandContext, Command}

class ListConnectionsCommand extends Command {
  def execute(ctx: CommandContext) = {
    CLIForce.getInstance.getAvailableEnvironments.foreach{
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

  def describe = "list the currently available connections"

  def name = "list"
}

class CurrentConnectionCommand extends Command {
  def execute(ctx: CommandContext) = {
    ctx.getCommandWriter.printf("Current Connection Name: %s\n", CLIForce.getInstance.getCurrentEnvironment)
    ctx.getCommandWriter.printf("Current User: %s\n", ctx.getForceEnv.getUser);
    ctx.getCommandWriter.printf("Current Endpoint: %s\n", ctx.getForceEnv.getHost);
  }

  def describe = "show the currently selected connection"

  def name = "current"
}


class AddConnectionCommand extends Command {
  def execute(ctx: CommandContext) = {
    if (ctx.getCommandArguments.size != 2) {
      ctx.getCommandWriter.println("Error, command expects exaxtly 2 arguments")
      ctx.getCommandWriter.println(describe)
    } else {
      val name = ctx.getCommandArguments.apply(0)
      if (CLIForce.getInstance.getAvailableEnvironments.containsKey(name)) {
        ctx.getCommandWriter.printf("There is already a connection named %s, please rename or remove it first\n", name)
      } else {
        val env = new ForceEnv(ctx.getCommandArguments.apply(1), "cliforce");
        if (env.isValid) {
          CLIForce.getInstance.setAvailableEnvironment(name, env)
          if (CLIForce.getInstance.getAvailableEnvironments.size == 1) {
            CLIForce.getInstance.setDefaultEnvironment(name)
            CLIForce.getInstance.setCurrentEnvironment(name)
          }

        } else {
          ctx.getCommandWriter.printf("The url entered is invalid, reason:%s\n", env.getMessage)
        }
      }
    }
  }

  def describe = "add an available conenction. Usage connection:add <connectionName> <connectionUrl>"

  def name = "add"
}

class DefaultConnectionCommand extends Command {
  def execute(ctx: CommandContext) = {
    if (ctx.getCommandArguments.size == 0) {
      ctx.getCommandWriter.printf("The currently selected default connection name is: %s\n", CLIForce.getInstance.getDefaultEnvironment)
    } else if (ctx.getCommandArguments.size != 1) {
      ctx.getCommandWriter.println("Error, command expects exaxtly 1 argument")
      ctx.getCommandWriter.println(describe)
    } else {
      val name = ctx.getCommandArguments.apply(0)
      if (CLIForce.getInstance.getAvailableEnvironments.containsKey(name)) {
        CLIForce.getInstance.setDefaultEnvironment(name)
        CLIForce.getInstance.setCurrentEnvironment(name)
      } else {
        ctx.getCommandWriter.printf("There is no such environment: %s avaiable\n", name)
      }
    }
  }

  def describe = "set the current and default connection for cliforce. Usage connection:default <connectionName>"

  def name = "default"
}

class SetConnectionCommand extends Command {
  def execute(ctx: CommandContext) = {
    if (ctx.getCommandArguments.size != 1) {
      ctx.getCommandWriter.println("Error, command expects exaxtly 1 argument")
      ctx.getCommandWriter.println(describe)
    } else {
      val name = ctx.getCommandArguments.apply(0)
      if (CLIForce.getInstance.getAvailableEnvironments.containsKey(name)) {
        CLIForce.getInstance.setCurrentEnvironment(name)
      } else {
        ctx.getCommandWriter.printf("There is no such environment: %s avaiable\n", name)
      }
    }
  }

  def describe = "set the current conenction for cliforce. Usage connection:set <connectionName>"

  def name = "set"
}

class RenameConnectionCommand extends Command {
  def execute(ctx: CommandContext) = {
    if (ctx.getCommandArguments.size != 2) {
      ctx.getCommandWriter.println("Error, command expects exaxtly 2 arguments")
      ctx.getCommandWriter.println(describe)
    } else {
      val name = ctx.getCommandArguments.apply(0)
      val newname = ctx.getCommandArguments.apply(1)
      if (CLIForce.getInstance.getAvailableEnvironments.containsKey(name)) {
        CLIForce.getInstance.renameEnvironment(name, newname)
      } else {
        ctx.getCommandWriter.printf("There is no such environment: %s avaiable\n", name)
      }
    }
  }

  def describe = "rename a connection for cliforce. Usage connection:rename <currentName> <newName>"

  def name = "rename"
}

class RemoveConnectionCommand extends Command {
  def execute(ctx: CommandContext) = {
    if (ctx.getCommandArguments.size != 1) {
      ctx.getCommandWriter.println("Error, command expects exaxtly 1 argument")
      ctx.getCommandWriter.println(describe)
    } else {
      val name = ctx.getCommandArguments.apply(0)
      if (CLIForce.getInstance.getAvailableEnvironments.containsKey(name)) {
        CLIForce.getInstance.removeEnvironment(name)
      } else {
        ctx.getCommandWriter.printf("There is no such environment: %s avaiable\n", name)
      }
    }
  }

  def describe = "remove a conenction from cliforce. Usage connection:remove <connectionName>"

  def name = "remove"
}

