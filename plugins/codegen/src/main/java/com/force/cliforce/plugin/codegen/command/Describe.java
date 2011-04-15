package com.force.cliforce.plugin.codegen.command;

import java.util.ArrayList;
import java.util.List;

import scala.actors.threadpool.Arrays;

import com.beust.jcommander.Parameter;
import com.force.cliforce.CommandContext;
import com.force.cliforce.JCommand;
import com.force.cliforce.LazyLogger;
import com.force.cliforce.Util;
import com.force.cliforce.plugin.codegen.command.Describe.DescribeArgs;
import com.sforce.soap.partner.DescribeGlobalResult;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.ws.ConnectionException;

/**
 * Describes Force.com schema for a given org.
 * 
 * This may be useful during code generation
 * as it will allow a user to see what can be 
 * or will be generated.
 * 
 * Usage: describe <names of Force.com schema objects to describe>
 * 
 * @author Tim Kral
 */
public class Describe extends JCommand<DescribeArgs> {

    public static LazyLogger log = new LazyLogger(Describe.class);

    @Override
    public String name() {
        return "describe";
    }

    @Override
    public String describe() {
        return usage("Describes Force.com schema in the current org");
    }

    @Override
    public void executeWithArgs(CommandContext ctx, DescribeArgs args) {
        Util.requirePartnerConnection(ctx);
        
        DescribeGlobalResult dgr;
        try {
            dgr = ctx.getPartnerConnection().describeGlobal();
        } catch (ConnectionException e) {
            log.get().debug("Unable to retrieve org schema", e);
            return;
        }
        
        if (args.all) {
            describeAllSchema(ctx, dgr);
        } else if (!args.names.isEmpty()) {
            describeSchema(ctx, dgr, args.names);			
        } else {
            ctx.getCommandWriter().println("No schema described. Please specify the schema object names or use -a");
        }
    }

    private void describeAllSchema(CommandContext ctx, DescribeGlobalResult dgr) {
        
        List<DescribeGlobalSObjectResult> standardSchema = new ArrayList<DescribeGlobalSObjectResult>();
        List<DescribeGlobalSObjectResult> customSchema = new ArrayList<DescribeGlobalSObjectResult>();
        
        // Separate custom and standard schema
        for (DescribeGlobalSObjectResult dgsr : dgr.getSobjects()) {
            if (dgsr.isCustom()) {
                customSchema.add(dgsr);
            } else {
                standardSchema.add(dgsr);
            }
        }
        
        ctx.getCommandWriter().println("Custom Schema:");
        if (customSchema.isEmpty()) {
            ctx.getCommandWriter().println("  There is no custom schema in your org");
        } else {
            printSchema(ctx, customSchema);
        }
        
        ctx.getCommandWriter().println("");
        ctx.getCommandWriter().println("Standard Schema:");
        if (standardSchema.isEmpty()) {
            ctx.getCommandWriter().println("  There is no standard schema in your org");
        } else {
            printSchema(ctx, standardSchema);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void describeSchema(CommandContext ctx, DescribeGlobalResult dgr, List<String> names) {
        DescribeGlobalSObjectResult[] schema = new DescribeGlobalSObjectResult[names.size()];
        
        // Filter in schema named in the parameters
        for (DescribeGlobalSObjectResult dgsr : dgr.getSobjects()) {
            int index;
            if ((index = names.indexOf(dgsr.getName())) > -1) {
                schema[index] = dgsr; // Maintain the same schema order as the parameters
            }
        }
        
        for (int i = 0; i < schema.length; i++) {
            // A null in the schema array means that we were unable to find the named schema
            // (e.g. it was misspelled)
            if (schema[i] == null) {
                // A little hacky. Create a DescribeGlobalSObjectResult that
                // will format as: UNABLE TO FIND (<objectName>)
                DescribeGlobalSObjectResult dgsr = new DescribeGlobalSObjectResult();
                dgsr.setName("UNABLE TO FIND");
                dgsr.setLabel(names.get(i));
                
                schema[i] = dgsr;
            }
        }
        
        printSchema(ctx, Arrays.asList(schema));
    }
    
    private void printSchema(CommandContext ctx, List<DescribeGlobalSObjectResult> schema) {
        for (DescribeGlobalSObjectResult object : schema) {
            ctx.getCommandWriter().println(String.format("  %-70s [ %-15s %-15s %-15s %-9s ]",
                    object.getName() + " (" + object.getLabel() + ")",
                    object.isCreateable() ? "CREATEABLE" : "",
                    object.isQueryable() ? "READABLE" : "",
                    object.isUpdateable() ? "UPDATEABLE" : "",
                    object.isDeletable() ? "DELETABLE" : ""));
        }
    }

    public static class DescribeArgs {
        @Parameter(description = "names of Force.com schema objects to describe")
        public List<String> names = new ArrayList<String>();
        
        @Parameter(names = {"-a", "--all"}, description = "describe all Force.com schema objects")
        public boolean all = false;
    }

}
