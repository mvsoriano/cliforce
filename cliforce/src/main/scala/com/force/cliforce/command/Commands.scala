package com.force.cliforce.command

import com.beust.jcommander.Parameter
import javax.inject.Inject
import com.force.cliforce._


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

  def name = CLIForce.BANNER_CMD
}


class DebugArgs {
  @Parameter(names = Array("--off"), description = "Turns off debug logging to the console")
  val off = false
}

class DebugCommand() extends JCommand[DebugArgs] {

  @Inject
  var cliforce: CLIForce = null;


  def describe = usage("Turns debug output on. Or off with the --off switch")

  def name = "debug"

  def executeWithArgs(ctx: CommandContext, args: DebugArgs) = {
    cliforce.setDebug(!args.off)
  }
}


