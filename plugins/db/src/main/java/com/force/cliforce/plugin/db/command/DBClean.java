package com.force.cliforce.plugin.db.command;

import com.force.cliforce.Command;
import com.force.cliforce.CommandContext;
import com.force.cliforce.Util;
import com.sforce.soap.metadata.*;
import com.sforce.soap.partner.DescribeGlobalResult;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.ws.ConnectionException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DBClean implements Command {


    @Override
    public String name() {
        return "clean";
    }

    @Override
    public String describe() {
        return "Deletes all custom objects in the current org";
    }


    public void execute(CommandContext ctx) throws IOException, ConnectionException {
        Util.requireMetadataConnection(ctx);
        Util.requirePartnerConnection(ctx);
        ctx.getCommandWriter().println("Connected to org " + ctx.getPartnerConnection().getUserInfo().getOrganizationId());

        DescribeGlobalResult objs = ctx.getPartnerConnection().describeGlobal();
        MDPackage destructiveChanges = new MDPackage();
        for (DescribeGlobalSObjectResult s : objs.getSobjects()) {
            if (s.isCustom()) {
                ctx.getCommandWriter().println("Preparing to delete " + s.getName());
                destructiveChanges.addCustomObject(s.getName());
            }
        }


        String apiVersion = Util.getApiVersion();
        destructiveChanges.setVersion(apiVersion);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(bout);

        ZipEntry entry = new ZipEntry("destructiveChanges.xml");

        zout.putNextEntry(entry);
        zout.write(destructiveChanges.toXML().getBytes());
        zout.closeEntry();

        MDPackage pkg = new MDPackage();
        pkg.setVersion(apiVersion);

        entry = new ZipEntry("package.xml");
        zout.putNextEntry(entry);
        zout.write(pkg.toXML().getBytes());
        zout.closeEntry();

        zout.close();
        zout.flush();

        DeployOptions options = new DeployOptions();
        options.setSinglePackage(true);
        AsyncResult ar = ctx.getMetadataConnection().deploy(bout.toByteArray(), options);

        while (!ar.getDone()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            ar = ctx.getMetadataConnection().checkStatus(new String[]{ar.getId()})[0];
        }
        DeployResult dr = ctx.getMetadataConnection().checkDeployStatus(ar.getId());
        if (dr.getSuccess()) {
            ctx.getCommandWriter().println("Operation succeeded.");
        } else {
            ctx.getCommandWriter().println("Operation failed. Messages:");
            for (DeployMessage dm : dr.getMessages()) {
                ctx.getCommandWriter().println("  " + dm.getFullName() + ": " + dm.getProblem());
            }
        }

    }

}
