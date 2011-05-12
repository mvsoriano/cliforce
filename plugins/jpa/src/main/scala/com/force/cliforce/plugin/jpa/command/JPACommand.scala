/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.force.cliforce.plugin.jpa.command

import com.force.cliforce.Boot;
import com.force.cliforce.Util.requireResolver

import javax.inject.Inject
import javax.persistence.spi.PersistenceProvider
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPathExpressionException

import org.datanucleus.OMFContext
import org.datanucleus.PersistenceConfiguration
import org.datanucleus.metadata._
import org.xml.sax.SAXException

import com.beust.jcommander.Parameter
import com.beust.jcommander.internal.Maps
import com.force.cliforce.CommandContext
import com.force.cliforce.JCommand
import com.force.cliforce.Util._
import com.force.cliforce.dependency.OutputAdapter
import com.force.cliforce.dependency.DependencyResolver
import com.force.cliforce.dependency.ZipUtil
import com.force.cliforce.dependency.DependencyResolver.Scope
import com.force.sdk.jpa.PersistenceProviderImpl
import collection.JavaConversions._
import java.io.File;
import java.util.{Map => JMap}
import java.net.{URL, URLClassLoader}

/**
 * 
 * All JPA commands inherit from here. The class provides basic ability to select a persistenceunit and then apply command to that PU
 *
 * @author fhossain
 */
abstract class JPACommand[P <: JPAParam] extends JCommand[P] {

  @Inject
  var resolver: DependencyResolver = null

  /**
   * Individual commands implement this to do actual work
   * 
   * @param ctx
   * @param args
   * @param persistenceProvider - Loaded avax.persistence.spi.PersistenceProvider
   * @param persistenceUnit - Name of the persistenceUnit
   * @param overrideProps - Any override property to pass on to EntityManagerFactory
   */
  def executeInternal(ctx: CommandContext, args: P, persistenceProvider: PersistenceProvider, persistenceUnit: String, overrideProps: JMap[String, Object]): Unit
    
  override def executeWithArgs(ctx: CommandContext, args: P): Unit = {
    requireMetadataConnection(ctx)
    requirePartnerConnection(ctx)
    requireResolver(resolver)

    ctx.getCommandWriter().println("Connected to org " + ctx.getPartnerConnection().getUserInfo().getOrganizationId())
    try {    	
    	executeWithClasspath(ctx, args)
    } finally {
    	val tempWarDir = new File(Boot.getCliforceHome() + "/" + ZipUtil.TEMP_SUB_DIR_NAME)
    	if(tempWarDir.exists()) {
    		ZipUtil.deleteDir(tempWarDir)
    	}
    }
  }

  private def getPersistenceUnit(ctx: CommandContext, persistenceXmlFiles: List[PersistenceFileMetaData]): String = {
    def getPersistenceUnit(ctx: CommandContext, persistenceXmlFile: PersistenceFileMetaData): String = {
      if (persistenceXmlFile.getNoOfPersistenceUnits() == 0) {
        ctx.getCommandWriter().println("WARNING: No persistence-unit found in the persistence.xml file")
        return null
      }

      val names = persistenceXmlFile.getPersistenceUnits.toList.map(s => s.getName())
      val sel = selectOne(ctx, names, "Select PersistenceUnit:", true)
      if (sel._1 >= 0) {
        return sel._2(sel._1)
      }
      return null
    }
    
    def selectOne(ctx: CommandContext, names: List[String], message:String, doSort: Boolean):(Int, List[String]) = {
      if (names.length == 1) return (0, names)
      ctx.getCommandWriter().println(message)
      val sortedNames = if (doSort) names.sort((e1, e2) => (e1 compareTo e2) < 0) else names
      //val result = sortedNames.zipWithIndex map {case (i, name) => i +". " + name}
      var i = 1
      for (name <- sortedNames) {
        ctx.getCommandWriter().println(i + ". " + name)
        i += 1
      }
      var selectedValue = -1
      while(selectedValue < 0) {
        val values = ctx.getCommandReader().readAndParseLine("[1-" + sortedNames.length + "] q to quit? ").toList;
        if (values.length == 1) {
          if ("q" == values.get(0)) return (-1, sortedNames)
          try {
            val v = values.get(0)
            if (v.toInt > 0 && v.toInt <= names.length) {
              selectedValue = v.toInt - 1;
            }
          } catch {
            case ne: NumberFormatException =>
          }
        }
      }
      (selectedValue, sortedNames)
    }

    val sel = selectOne(ctx, persistenceXmlFiles.map(m => m.getFilename()), "Select persistence.xml file:", false)
    if (sel._1 >= 0) {
      return getPersistenceUnit(ctx, persistenceXmlFiles(sel._1))
    }
    return null
  }
    
  protected def executeWithClasspath(ctx: CommandContext, args: P): Unit = {
    val curr = Thread.currentThread().getContextClassLoader()
    val oa = new OutputAdapter() {
      override def println(msg: String) = {
        ctx.getCommandWriter().println(msg)
      }
      
      override def println(e: Exception, msg: String) = {
        ctx.getCommandWriter().printf("%s: %s", msg, e.toString())
      }
    }
    var pcl: ClassLoader = resolver.createClassLoaderFor(args.group, args.artifact, args.packaging, args.version,
      if (args.searchTestJars) Scope.TEST else Scope.RUNTIME, curr, oa)

    Thread.currentThread().setContextClassLoader(pcl)
    try {
      // Create PersistenceProvider
      val clr = Thread.currentThread().getContextClassLoader()
      val clazz = clr.loadClass("com.force.sdk.jpa.PersistenceProviderImpl")
      val impl = clazz.newInstance().asInstanceOf[PersistenceProviderImpl]

      // Find all "META-INF/persistence.xml" files in the current thread loader CLASSPATH
      val omfCtx = new OMFContext(new PersistenceConfiguration(){});
      omfCtx.setApi("JPA")
      val metadataMgr = omfCtx.getMetaDataManager()
      val files = metadataMgr.parsePersistenceFiles()
      if (files == null || files.length == 0) {
          ctx.getCommandWriter().println("No persistence.xml file found in the selected artifact.")
          return
      }
        
      val persistenceUnit =
        if (args.persistenceUnit != null) args.persistenceUnit
        else getPersistenceUnit(ctx, files.toList)
      if (persistenceUnit == null) {
        return
      }
      ctx.getCommandWriter().println("Running with selected PersistenceUnit: " + persistenceUnit)
      val props: JMap[String, Object] = Maps.newHashMap().asInstanceOf[JMap[String, Object]]
      props.put("force.schemaCreateClient", java.lang.Boolean.TRUE)
      // Override the connection name that comes from persistence.xml
      props.put("force.ConnectionName", ctx.getConnectionName())
      executeInternal(ctx, args, impl, persistenceUnit, props)
    } finally {
        Thread.currentThread().setContextClassLoader(curr)
    }
  }
}

class JPAParam {
  @Parameter(names = Array("-g", "--group"), description = "Group name", required = true)
  var group: String = null
    
  @Parameter(names = Array("-a", "--artifact"), description = "Artifact id", required = true)
  var artifact: String = null
    
  @Parameter(names = Array("-type", "--type"), description = "Artifact packaging type")
  var packaging: String = "jar"
  
  @Parameter(names = Array("-v", "--version"), description = "Version number", required = true)
  var version: String = null
    
  @Parameter(names = Array("-t", "--test"), description = "Search test jars")
  var searchTestJars: Boolean = false
    
  @Parameter(names = Array("-u", "--persistenceunit"), description = "PersistenceUnit name")
  var persistenceUnit: String = null
}