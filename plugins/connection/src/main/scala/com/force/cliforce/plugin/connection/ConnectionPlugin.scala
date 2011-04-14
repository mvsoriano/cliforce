package com.force.cliforce.plugin.connection

import com.force.cliforce.Plugin
import collection.JavaConversions
import command._

class ConnectionPlugin extends Plugin {
  def getCommands = JavaConversions.asJavaList(List(classOf[ListConnectionsCommand],
    classOf[CurrentConnectionCommand],
    classOf[AddConnectionCommand],
    classOf[DefaultConnectionCommand],
    classOf[RenameConnectionCommand],
    classOf[SetConnectionCommand],
    classOf[TestConnectionCommand],
    classOf[RemoveConnectionCommand]))
}