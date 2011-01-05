package com.force.cliforce;

import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.PartnerConnection;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class DefaultPlugin implements Plugin {


    private CLIForce force;

    public DefaultPlugin(CLIForce it) {
        force = it;
    }

    @Override
    public List<CommandDescriptor> getCommands() {
        return Arrays.asList(new CommandDescriptor("list", new ListCustomObjects()),
                new CommandDescriptor("dbclean", new DBClean()),
                new CommandDescriptor("info", new InfoCommand(force.forceEnv)),
                new CommandDescriptor("help", new HelpCommand(force.commands)),
                new CommandDescriptor("exit", new Command() {
                    @Override
                    public String describe() {
                        return "Exit this shell";
                    }

                    @Override
                    public void execute(PartnerConnection partner, MetadataConnection metadata, PrintWriter output) throws Exception {
                        //No-op, will exit
                    }
                }));
    }

    public static class ListCustomObjects implements Command {

        @Override
        public String describe() {
            return "list the existing custom objects and their fields";
        }

        @Override
        public void execute(PartnerConnection partner, MetadataConnection metadata, PrintWriter out) throws Exception {
            ListMetadataQuery q = new ListMetadataQuery();
            q.setType("CustomObject");
            FileProperties[] fpa = metadata.listMetadata(new ListMetadataQuery[]{q}, 20.0);
            List<String> sobjs = new ArrayList<String>();
            for (FileProperties fileProperties : fpa) {
                if (fileProperties.getFullName().endsWith("__c")) {
                    sobjs.add(fileProperties.getFullName());
                }
            }
            DescribeSObjectResult[] describeSObjectResults = partner.describeSObjects(sobjs.toArray(new String[0]));
            for (DescribeSObjectResult describeSObjectResult : describeSObjectResults) {
                out.printf("\n{\nCustom Object-> %s \n", describeSObjectResult.getName());
                for (Field field : describeSObjectResult.getFields()) {
                    out.printf("       field -> %s (type: %s)\n", field.getName(), field.getType().toString());
                }
                out.print("}\n");
            }
        }
    }

    public static class HelpCommand implements Command {
        private Map<String, CommandDescriptor> commands;

        public HelpCommand(Map<String, CommandDescriptor> cmds) {
            this.commands = cmds;
        }

        @Override
        public String describe() {
            return "Display this help message";
        }

        @Override
        public void execute(PartnerConnection partner, MetadataConnection metadata, PrintWriter log) throws Exception {
            for (Map.Entry<String, CommandDescriptor> entry : commands.entrySet()) {
                log.printf("%s: %s\n", entry.getKey(), entry.getValue().command.describe());
            }
        }
    }

    public static class InfoCommand implements Command {

        private ForceEnv forceEnv;

        public InfoCommand(ForceEnv env) {
            this.forceEnv = env;
        }

        @Override
        public String describe() {
            return "Show the current connection info:";
        }

        @Override
        public void execute(PartnerConnection partner, MetadataConnection metadata, PrintWriter log) throws Exception {
            log.printf("Current User: %s\n", forceEnv.getUser());
            log.printf("Current Endpoint: %s\n", forceEnv.getHost());
        }
    }
}
