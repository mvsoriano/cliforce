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
  @Parameter(names = Array("--on"), description = "Turns on debug logging to the console")
  val on = false
  @Parameter(names = Array("--off"), description = "Turns off debug logging to the console")
  val off = false

}

class DebugCommand() extends JCommand[DebugArgs] {

  @Inject
  var cliforce:CLIForce=null;


  def describe = usage("turns debug output on/off")

  def name = "debug"

  def executeWithArgs(ctx: CommandContext, args: DebugArgs) = {
    if (args.on ^ args.off) {
      cliforce.setDebug(args.on)
    } else{
      ctx.getCommandWriter.println(describe)
    }
  }
}


