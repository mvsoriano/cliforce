package com.force.cliforce.plugin.db.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import com.beust.jcommander.Parameter;
import com.force.cliforce.CommandContext;
import com.force.cliforce.JCommand;
import com.force.cliforce.LazyLogger;
import com.force.cliforce.Util;
import com.force.cliforce.plugin.db.command.Describe.DescribeArgs;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.sforce.soap.partner.DescribeGlobalResult;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.ws.ConnectionException;

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
public class Describe extends JCommand<DescribeArgs> {

    private static final String NEWLINE = System.getProperty("line.separator");
    private static final int MAX_BATCH_DESCRIBE_SIZE = 100;
    
    // Filters out standard objects
    private static final Predicate<DescribeGlobalSObjectResult> CUSTOM_OBJECT_FILTER = 
        new Predicate<DescribeGlobalSObjectResult>() {
            
            @Override
            public boolean apply(DescribeGlobalSObjectResult input) {
                return input.isCustom();
            }
        };
    
    // Transform SObject describe result to SObject name
    private static final Function<DescribeGlobalSObjectResult, String> OBJECT_NAME_FUNCTION = 
        new Function<DescribeGlobalSObjectResult, String>() {
            
            @Override
            public String apply(DescribeGlobalSObjectResult from) {
                return from.getName();
            }
        };
    
    // Transform printable schema class to SObject name
    private static final Function<Schema, String> SCHEMA_NAME_FUNCTION = 
        new Function<Schema, String>() {
    
            @Override
            public String apply(Schema from) {
                return from.name;
            }
        };
        
    // Transform global SObject describe result to printable schema class
    private static final Function<DescribeGlobalSObjectResult, Schema> GLOBAL_DESCRIBE_SCHEMA_FUNCTION = 
        new Function<DescribeGlobalSObjectResult, Schema>() {
    
            @Override
            public Schema apply(DescribeGlobalSObjectResult from) {
                return new Schema(from);
            }
        };

    // Transform SObject describe result to printable schema class
    private static final Function<DescribeSObjectResult, Schema> DESCRIBE_SCHEMA_FUNCTION = 
        new Function<DescribeSObjectResult, Schema>() {
    
            @Override
            public Schema apply(DescribeSObjectResult from) {
                return new Schema(from);
            }
        };
            
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
        
        if (!args.all && !args.custom && !args.standard && args.names.isEmpty()) {
            ctx.getCommandWriter().println("No schema described. Please specify the schema object names or use -a, -c, -s");
            return;
        }
        
        Util.requirePartnerConnection(ctx);
        
        DescribeGlobalResult dgr;
        try {
            dgr = ctx.getPartnerConnection().describeGlobal();
        } catch (ConnectionException e) {
            ctx.getCommandWriter().print("Unable to describe org schema: ");
            ctx.getCommandWriter().printExceptionMessage(e, true /*newLine*/);
            log.get().debug("Unable to describe org schema: ", e);
            return;
        }
        
        Collection<DescribeGlobalSObjectResult> filteredDgsrs;
        
        // Filter the global describe results according to the options
        // passed in by the user
        if (args.all) {
            filteredDgsrs = Arrays.asList(dgr.getSobjects());
        } else {            
            Collection<DescribeGlobalSObjectResult> dgsrs = Arrays.asList(dgr.getSobjects());
            filteredDgsrs = new ArrayList<DescribeGlobalSObjectResult>(dgsrs.size());
            
            if (!args.names.isEmpty()) {
                ObjectNameFilter namesFilter = new ObjectNameFilter(args.names);
                filteredDgsrs.addAll(Collections2.filter(dgsrs, namesFilter));
            }
            
            if (args.custom) {
                filteredDgsrs.addAll(Collections2.filter(dgsrs, CUSTOM_OBJECT_FILTER));
            }
            
            if (args.standard) {
                filteredDgsrs.addAll(Collections2.filter(dgsrs, Predicates.not(CUSTOM_OBJECT_FILTER)));
            }
        }
        
        SortedMap<String, Schema> schemaObjects;
        
        // If the user wants verbose information then make extra calls to get
        // all the field information
        if (args.verbose) {
            schemaObjects = Maps.newTreeMap(Ordering.natural());
            List<String> objectNames = ImmutableList.<String>copyOf(Collections2.transform(filteredDgsrs, OBJECT_NAME_FUNCTION));
            for (int i = 0; i < objectNames.size(); i += MAX_BATCH_DESCRIBE_SIZE) {
                // Ensure that our range only goes to the end of the objectNames list
                int endIndex = i + MAX_BATCH_DESCRIBE_SIZE;
                if (endIndex > objectNames.size()) endIndex = objectNames.size();
                
                // Batch the SObject names so that we do not exceed the SObject
                // describe call batch limit
                List<String> objectNameBatch = objectNames.subList(i, endIndex);
                try {
                    
                    DescribeSObjectResult[] dsrs = 
                        ctx.getPartnerConnection().describeSObjects(objectNameBatch.toArray(new String[objectNameBatch.size()]));
                    
                    // 1. Transform SObject describe results to printable schema
                    // 2. Create a map of [SObject Name -> Schema]
                    schemaObjects.putAll(
                            ImmutableSortedMap.<String, Schema>copyOf(Maps.uniqueIndex(
                                    Collections2.transform(Arrays.asList(dsrs), DESCRIBE_SCHEMA_FUNCTION), SCHEMA_NAME_FUNCTION)));
                    
                } catch (ConnectionException e) {
                    ctx.getCommandWriter().print("Unable to verbosely describe org schema: ");
                    ctx.getCommandWriter().printExceptionMessage(e, true /*newLine*/);
                    log.get().debug("Unable to verbosely describe org schema: ", e);
                    return;
                }
            }
        } else {
            // 1. Transform global SObject describe results to printable schema
            // 2. Create a map of [SObject Name -> Schema]
            schemaObjects = 
                ImmutableSortedMap.<String, Schema>copyOf(Maps.uniqueIndex(
                    Collections2.transform(filteredDgsrs, GLOBAL_DESCRIBE_SCHEMA_FUNCTION), SCHEMA_NAME_FUNCTION));
        }
        
        printSchema(ctx, schemaObjects, args.verbose, args.names);			
    }

    private void printSchema(CommandContext ctx, SortedMap<String, Schema> schemaObjects, boolean verbose, List<String> names) {
        if (schemaObjects.isEmpty() && names.isEmpty()) {
            ctx.getCommandWriter().println("No schema describe results");
        } else {
            for (Schema schemaObject : schemaObjects.values()) {
                ctx.getCommandWriter().println(schemaObject.toString(verbose));
            }
        }
        
        // If schema has been named in the parameters then maintain the same schema order
        if (names != null && !names.isEmpty()) {
            for (String name : names) {
                if (!schemaObjects.containsKey(name)) {
                    ctx.getCommandWriter().println(new Schema("UNABLE TO FIND", name).toString(false));
                }
            }
        }
    }
    
    /**
     * Arguments for the describe command.
     * 
     * @author Tim Kral
     */
    public static class DescribeArgs {
        @Parameter(description = "names of Force.com schema objects to describe")
        public List<String> names = new ArrayList<String>();
        
        @Parameter(names = {"-a", "--all"}, description = "describe all Force.com schema objects")
        public boolean all = false;
        
        @Parameter(names = {"-c", "--custom"}, description = "describe custom Force.com schema objects")
        public boolean custom = false;
        
        @Parameter(names = {"-s", "--standard"}, description = "describe standard Force.com schema objects")
        public boolean standard = false;
        
        @Parameter(names = {"-v", "--verbose"}, description = "verbosely describe Force.com schema objects")
        public boolean verbose = false;
    }

    /**
     * Filter for a list of global SObject describe results
     * which filters on the given SObject names.
     * 
     * @author Tim Kral
     */
    private static class ObjectNameFilter implements Predicate<DescribeGlobalSObjectResult> {
        private final List<String> names;
        
        ObjectNameFilter(List<String> names) {
            this.names = names;
        }
        
        @Override
        public boolean apply(DescribeGlobalSObjectResult input) {
            return names.contains(input.getName());
        }
    }
    
    /**
     * Printable attributes for a Force.com schema object.
     * 
     * @author Tim Kral
     */
    private static class Schema {
        private final String name;
        private final String label;
        
        private final boolean createable;
        private final boolean readable;
        private final boolean updateable;
        private final boolean deletable;
        
        private final Field[] fields;
        
        Schema(String name, String label) {
            this.name = name;
            this.label = label;
            
            this.createable = this.readable = 
                this.updateable = this.deletable = false;
            
            this.fields = null;
        }
        
        Schema(DescribeGlobalSObjectResult dgsr) {
            this.name = dgsr.getName();
            this.label = dgsr.getLabel();
            
            this.createable = dgsr.isCreateable();
            this.readable = dgsr.isQueryable();
            this.updateable = dgsr.isUpdateable();
            this.deletable = dgsr.isDeletable();
            
            this.fields = null;
        }
        
        Schema(DescribeSObjectResult dsr) {
            this.name = dsr.getName();
            this.label = dsr.getLabel();
            
            this.createable = dsr.isCreateable();
            this.readable = dsr.isQueryable();
            this.updateable = dsr.isUpdateable();
            this.deletable = dsr.isDeletable();
            
            this.fields = dsr.getFields();
        }
        
        public String toString(boolean withFields) {
            StringBuffer sb = new StringBuffer("  ");
            
            // Schema information
            // SchemaName (Schema Label) ... [ <CREATEABLE> <READABLE> <UPDATEABLE> <DELETABLE> ]
            sb.append(String.format("%-70s", name + " (" + label + ")"));
            sb.append(" [ ")
              .append(String.format("%-15s", createable ? "CREATEABLE" : "")).append(' ')
              .append(String.format("%-15s", readable ? "READABLE" : "")).append(' ')
              .append(String.format("%-15s", updateable ? "UPDATEABLE" : "")).append(' ')
              .append(String.format("%-9s", deletable ? "DELETABLE" : ""))
              .append(" ]");
            
            if (withFields && fields != null) {
                // Field information
                // FieldName (Field Label) ... [ <TYPE> ... <NOT NULL> ... <DEFAULTED> ]
                for (Field field : fields) {
                    sb.append(NEWLINE).append("    ");
                    sb.append(String.format("%-70s", field.getName() + " (" + field.getLabel() + ")"));
                    sb.append(" [ ")
                      .append(String.format("%-30s", field.getType())).append(' ')
                      .append(String.format("%-10s", field.isNillable() ? "" : "NOT NULL")).append(' ')
                      .append(String.format("%-10s", field.isDefaultedOnCreate() ? "DEFAULTED" : ""))
                      .append(" ]");
                }
            }
            
            return sb.toString();
        }
    }

}
