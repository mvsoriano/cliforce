package com.force.cliforce.plugin.connection

import com.force.cliforce.Plugin
import collection.JavaConversions
import command._

class ConnectionPlugin extends Plugin {
  def getCommands = JavaConversions.asJavaList(List(new ListConnectionsCommand, new CurrentConnectionCommand, new AddConnectionCommand, new DefaultConnectionCommand, new RenameConnectionCommand, new SetConnectionCommand, new RemoveConnectionCommand))
}