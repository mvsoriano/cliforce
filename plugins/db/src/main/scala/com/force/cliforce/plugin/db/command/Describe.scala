package com.force.cliforce.plugin.db.command

import com.beust.jcommander.Parameter
import com.force.cliforce._
import com.sforce.soap.partner.{Field, DescribeGlobalResult, DescribeSObjectResult, DescribeGlobalSObjectResult}
import com.sforce.ws.ConnectionException
import java.util.{ArrayList, List => JList}
import scala.collection.JavaConversions._

/**
 * Arguments for the describe command.
 * 
 * @author Tim Kral
 */
class DescribeArgs {
  
    @Parameter(description = "names of Force.com schema objects to describe")
    var names: JList[String] = new ArrayList()
    
    @Parameter(names = Array("-a", "--all"), description = "describe all Force.com schema objects")
    var all = false
    
    @Parameter(names = Array("-c", "--custom"), description = "describe custom Force.com schema objects")
    var custom = false
    
    @Parameter(names = Array("-s", "--standard"), description = "describe standard Force.com schema objects")
    var standard = false
    
    @Parameter(names = Array("-v", "--verbose"), description = "verbosely describe Force.com schema objects")
    var verbose = false
}

/**
 * Describes Force.com schema for a given org.
 * 
 * Usage: describe [args] <names of Force.com schema objects to describe>
 *      args:
 *      -a, --all       describe all Force.com schema objects
 *      -c, --custom    describe custom Force.com schema objects
 *      -s, --standard  describe standard Force.com schema objects
 *      -v, --verbose   verbosely describe Force.com schema objects
 * 
 * @author Tim Kral
 */
class Describe extends JCommand[DescribeArgs] {
    
    private val MAX_BATCH_DESCRIBE_SIZE = 100
    private var log = new LazyLogger(classOf[Describe])
    
    def name = "describe"
        
    def describe = usage("Describes Force.com schema in the current org")

    def executeWithArgs(ctx: CommandContext, args: DescribeArgs): Unit = {
        
        if (!args.all && !args.custom && !args.standard && args.names.isEmpty) {
            ctx.getCommandWriter.println("No schema described. Please specify the schema object names or use -a, -c, -s")
            return
        }
        
        Util.requirePartnerConnection(ctx)
        
        // Run a global describe on the org
        var dgr: DescribeGlobalResult = null
        try {
            dgr = ctx.getPartnerConnection.describeGlobal
        } catch {
            case ce: ConnectionException => {
                ctx.getCommandWriter.print("Unable to describe org schema: ")
                ctx.getCommandWriter.printExceptionMessage(ce, true /*newLine*/)
                log.get.debug("Unable to describe org schema: ", ce)
                return
            }
        }
        
        var dgsrs = dgr.getSobjects
        var dgsrsFiltered: List[DescribeGlobalSObjectResult] = null
        
        // Filter the global describe results according to the options
        // passed in by the user
        if (args.all) {
            dgsrsFiltered = dgsrs.elements.toList
        } else {
            dgsrsFiltered = List()
            if (!args.names.isEmpty) {
                // So we don't confuse ourselves, dedupe the names list
                args.names = dedupe(args.names.elements.toList)
                dgsrsFiltered = dgsrsFiltered:::dgsrs.filter(dgsr => args.names.contains(dgsr.getName)).elements.toList
            }
            
            if (args.custom) {
                dgsrsFiltered = dgsrsFiltered:::dgsrs.filter(dgsr => dgsr.isCustom).elements.toList
            }
            
            if (args.standard) {
                dgsrsFiltered = dgsrsFiltered:::dgsrs.filter(dgsr => !dgsr.isCustom).elements.toList
            }
        }
        
        var schemaObjects: List[Schema] = null
        
        // If the user wants verbose information then make extra calls to get
        // all the field information
        if (args.verbose) {
            schemaObjects = List()
            
            var objectNames = dgsrsFiltered.map(dgsr => dgsr.getName)
            for (i <- 0 until objectNames.size by MAX_BATCH_DESCRIBE_SIZE) {
                // Ensure that our range only goes to the end of the objectNames list
                var endIndex = i + MAX_BATCH_DESCRIBE_SIZE
                if (endIndex > objectNames.size) endIndex = objectNames.size
                
                // Batch the SObject names so that we do not exceed the SObject
                // describe call batch limit
                var dsrs = ctx.getPartnerConnection.describeSObjects(objectNames.slice(i, endIndex).elements.toArray)
                schemaObjects = schemaObjects:::dsrs.map(dsr => new Schema(dsr)).elements.toList
            }
        } else {
            // Transform global SObject describe results to printable schema
            schemaObjects = dgsrsFiltered.map(dgsr => new Schema(dgsr))
        }
        
        // Dedupe the schemaObjects list in case we've filtered in multiple copies of the same schema
        // (e.g. db:describe -c <CustomObjectName>)
        schemaObjects = dedupe(schemaObjects)
        
        // Print the schema in sorted order according to schema object name
        printSchema(
            schemaObjects.sort((schema1, schema2) => (schema1.getName compareTo schema2.getName) < 0),
            ctx, args.names
        )
    }
    
    private def dedupe[T](elements: List[T]): List[T] = {
        if (elements.isEmpty) {
            elements
        } else {
            elements.head::dedupe(for (e <- elements.tail if e != elements.head) yield e)
        }
    }
    
    private def printSchema(schemaObjects: List[Schema], ctx: CommandContext, names: JList[String]) {
        var printedNames = new ArrayList[String]
        if (schemaObjects.isEmpty && names.isEmpty) {
            ctx.getCommandWriter.println("No schema describe results")
        } else {
            for (schemaObject <- schemaObjects) {
                ctx.getCommandWriter.println(schemaObject.toString)
                printedNames.add(schemaObject.getName)
            }
        }
        
        var missingNames = names.filter(name => !printedNames.contains(name))
        missingNames.foreach(missingName => ctx.getCommandWriter.println(new Schema("UNABLE TO FIND", missingName).toString))
    }
}

/**
 * Printable attributes for a Force.com schema object.
 * 
 * @author Tim Kral
 */
class Schema private (name: String, label: String, 
                      createable: Boolean, readable: Boolean, 
                      updateable: Boolean, deletable: Boolean,
                      fields : Array[Field]) {

    def this(name: String, label: String) = this(name, label, 
                                                 false, false, 
                                                 false, false, 
                                                 null)
    
    def this(dgsr: DescribeGlobalSObjectResult) = this(dgsr.getName, dgsr.getLabel, 
                                                       dgsr.isCreateable, dgsr.isQueryable, 
                                                       dgsr.isUpdateable, dgsr.isDeletable,
                                                       null)
                                                         
    def this(dsr: DescribeSObjectResult) = this(dsr.getName, dsr.getLabel,
                                                dsr.isCreateable, dsr.isQueryable,
                                                dsr.isUpdateable, dsr.isDeletable,
                                                dsr.getFields)
                                                
    def getName = name
    override def equals(that: Any) = {
        that.isInstanceOf[Schema] && this.name == that.asInstanceOf[Schema].getName
    }
                                                  
    override def toString: String = {
        val sb = new StringBuffer
        
        // Schema information
        // SchemaName (Schema Label) ... [ <CREATEABLE> <READABLE> <UPDATEABLE> <DELETABLE> ]
        sb.append("  %-70s" format (name + " (" + label + ")"))
        sb.append(" [ ")
        sb.append("%-15s" format (if (createable) "CREATEABLE" else "")).append(' ')
        sb.append("%-15s" format (if (readable) "READABLE" else "")).append(' ')
        sb.append("%-15s" format (if (updateable) "UPDATEABLE" else "")).append(' ')
        sb.append("%-9s" format (if (deletable) "DELETABLE" else ""))
        sb.append(" ]")
        
        if (fields ne null) {
            // Field information
            // FieldName (Field Label) ... [ <TYPE> ... <NOT NULL> ... <DEFAULTED> ]
            for (field <- fields) {
                sb.append("\n")
                sb.append("    %-70s" format (field.getName + " (" + field.getLabel + ")"))
                sb.append(" [ ")
                sb.append("%-30s" format (field.getType)).append(' ')
                sb.append("%-10s" format (if (field.isNillable) "" else "NOT NULL")).append(' ')
                sb.append("%-10s" format (if (field.isDefaultedOnCreate) "DEFAULTED" else ""))
                sb.append(" ]")
            }
        }
        
        return sb.toString
    }
}