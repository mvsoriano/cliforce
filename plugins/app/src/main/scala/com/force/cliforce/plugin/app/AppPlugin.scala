package com.force.cliforce.plugin.app

import com.force.cliforce.Plugin
import collection.JavaConversions
import command._

class AppPlugin extends Plugin{
  def getCommands = JavaConversions.asJavaList(List(new AppsCommand, new DeleteAppCommand, new PushCommand, new RestartCommand, new StartCommand, new StopCommand, new TailFileCommand))
}