package com.force.cliforce.plugin.codegen.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.force.cliforce.CommandContext;
import com.force.cliforce.JCommand;
import com.force.cliforce.LazyLogger;
import com.force.cliforce.Util;
import com.force.cliforce.plugin.codegen.command.JPAClass.JPAClassArgs;
import com.force.sdk.codegen.ForceJPAClassGenerator;

/**
 * Code Generator for Force.com JPA enabled Java Classes. 
 * 
 * @author Tim Kral
 */
public class JPAClass extends JCommand<JPAClassArgs> {

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final String DEFAULT_DEST_DIR = 
        "." + FILE_SEPARATOR + "src" + FILE_SEPARATOR + "main" + FILE_SEPARATOR + "java"; // e.g. ./src/main/java
    
    public static LazyLogger log = new LazyLogger(JPAClass.class);
    
    @Override
    public String name() {
        return "jpaClass";
    }

    @Override
    public String describe() {
        return usage("Generates Force.com JPA enabled Java classes");
    }

    @Override
    public void executeWithArgs(CommandContext ctx, JPAClassArgs args) {
        Util.requirePartnerConnection(ctx);
        
        String fullPath = args.projectDir + FILE_SEPARATOR + args.destDir;
        fullPath.replaceAll(FILE_SEPARATOR + FILE_SEPARATOR, FILE_SEPARATOR); // Remove double file separators
        
        ForceJPAClassGenerator generator = new ForceJPAClassGenerator(ctx.getPartnerConnection(), new File(fullPath));
        generator.setPackageName(args.packageName);
        
        int numGeneratedFiles;
        try {
            if (args.all) {
                numGeneratedFiles = generator.generateJPAClasses(Collections.<String>singletonList("*"));
            } else if (!args.names.isEmpty()){
                numGeneratedFiles = generator.generateJPAClasses(args.names);
            } else {
                ctx.getCommandWriter().println("No Java classes generated. Please specify the schema object names or use -a");
                return;
            }
        } catch (Exception e) {
            ctx.getCommandWriter().print("Unable to generate JPA classes: ");
            ctx.getCommandWriter().printExceptionMessage(e, true /*newLine*/);
            log.get().debug("Unable to generate JPA classes", e);
            return;
        }
        
        ctx.getCommandWriter().println("Successfully generated " + numGeneratedFiles + " JPA classes");
    }

    public static class JPAClassArgs {
        @Parameter(description = "names of Force.com schema objects to generate as JPA classes")
        public List<String> names = new ArrayList<String>();
        
        @Parameter(names = {"-a", "--all"}, description="generate all Force.com schema objects as JPA classes")
        public boolean all = false;
        
        @Parameter(names = {"-d", "--destDir"}, description = "destination directory for generated JPA classes (within project)")
        public File destDir = new File(DEFAULT_DEST_DIR);
        
        @Parameter(names = {"-p", "--package"}, description = "java package name for generated JPA classes")
        public String packageName = null;
        
        @Parameter(names = {"--projectDir"}, description = "root directory for project")
        public File projectDir = new File(System.getProperty("user.dir")); // Current working directory
    }
}
