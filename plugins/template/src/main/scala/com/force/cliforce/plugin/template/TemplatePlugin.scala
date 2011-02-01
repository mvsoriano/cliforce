package com.force.cliforce.plugin.template

import com.force.cliforce.Plugin
import collection.JavaConversions
import command.NewProjectCommand


class TemplatePlugin extends Plugin{
  def getCommands = JavaConversions.asJavaList(List(new NewProjectCommand))
}