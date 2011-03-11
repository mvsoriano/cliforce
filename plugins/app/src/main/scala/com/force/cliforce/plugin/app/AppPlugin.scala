package com.force.cliforce.plugin.app

import com.force.cliforce.Plugin
import collection.JavaConversions
import command._

class AppPlugin extends Plugin {
  def getCommands = JavaConversions.asJavaList(List(classOf[AppsCommand],
    classOf[DeleteAppCommand],
    classOf[PushCommand],
    classOf[RestartCommand],
    classOf[StartCommand],
    classOf[StopCommand],
    classOf[TailFileCommand]))
}