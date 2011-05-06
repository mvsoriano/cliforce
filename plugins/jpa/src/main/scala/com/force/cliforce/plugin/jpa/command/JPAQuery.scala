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

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.spi.PersistenceProvider
import com.sforce.soap.partner.sobject.SObject
import com.force.cliforce.CommandContext
import collection.JavaConversions._
import java.util.{Map => JMap, List => JList}

class JPAQuery extends JPACommand[JPAParam] {

  def name = "query"
      
  def describe = "Run JPQL (or SOQL) against the current org"
      
  override def executeInternal(ctx: CommandContext, args: JPAParam, persistenceProvider: PersistenceProvider, persistenceUnit: String, overrideProps: JMap[String, Object]): Unit = {
    overrideProps.put("datanucleus.autoCreateSchema", java.lang.Boolean.FALSE)

    val emf = persistenceProvider.createEntityManagerFactory(persistenceUnit, overrideProps)
    val em = emf.createEntityManager
    while (true) {
      try {
        val line = ctx.getCommandReader.readLine("jpql (q to quit) > ").trim()
        if (line.length > 0) {
          if ("q".equals(line)) return
          if (line.startsWith("soql:")) {
            // skip soql:
            printSObjectResult(ctx, em.createNativeQuery(line.substring(5)).getResultList)
          } else {
            printObjectResult(ctx, em.createQuery(line).getResultList);
          }
        }
      } catch {
        case ie: IllegalStateException => throw ie
        case e: Exception => ctx.getCommandWriter.println("" + e)
      }
    }
  }
  
  private def printSObjectResult(ctx: CommandContext, result: JList[_]) = {
    if (result.size == 0) {
      ctx.getCommandWriter.println("No data found")
    } else {
      val f = sObjectFields(result.get(0).asInstanceOf[SObject])
      ctx.getCommandWriter.println(f.reduceLeft[String](_ + "\t" + _))
      result.foreach(v => ctx.getCommandWriter.println(f.map(fi => v.asInstanceOf[SObject].getField(fi)).mkString("\t")))
      ctx.getCommandWriter.println("Total rows: " + result.size())
    }
    
    def sObjectFields(x: SObject) = {
      // get rid of first two fields (type, id)
      x.getChildren.
        toList.
        drop(2).
        map(m => m.getName.getLocalPart)
    }
  }
   
  private def printObjectResult(ctx: CommandContext, result: JList[_]) = {
    if (result.size == 0) {
      ctx.getCommandWriter.println("No data found")
    } else {
      val f = fields(result.get(0))
      ctx.getCommandWriter.println(f.reduceLeft[String](_ + "\t" + _))
      result.foreach(v => ctx.getCommandWriter.println(f.map(fi => "" + get(fi, v.asInstanceOf[AnyRef])).mkString("\t")))
      ctx.getCommandWriter.println("Total rows: " + result.size())
    }
    
    def fields(x: Any) = wrapped(x).getClass.
      getDeclaredFields.
      toList.
      map(m => m.toString.replaceFirst("^.*\\.", "")).
      filter(s => !(s.startsWith("jdo")))
      
    def wrapped(x: Any): AnyRef = x match {
      case x: Byte => byte2Byte(x)
      case x: Short => short2Short(x)
      case x: Char => char2Character(x)
      case x: Int => int2Integer(x)
      case x: Long => long2Long(x)
      case x: Float => float2Float(x)
      case x: Double => double2Double(x)
      case x: Boolean => boolean2Boolean(x)
      case _ => x.asInstanceOf[AnyRef]
    }

    def get[T](s: String, a: AnyRef): T = {
      val f = a.getClass.getDeclaredField(s)
      f.setAccessible(true)
      f.get(a).asInstanceOf[T]
    }
  }
}