package com.force.cliforce.plugin.jpa

import com.force.cliforce.Plugin
import collection.JavaConversions
import command._

class JPAPlugin extends Plugin {
  override def getCommands = JavaConversions.asJavaList(List(classOf[JPAPopulate],
    classOf[JPAClean],
    classOf[JPAQuery],
    classOf[ListCustomObjects]))
}