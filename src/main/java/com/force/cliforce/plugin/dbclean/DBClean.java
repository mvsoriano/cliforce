package com.force.cliforce.plugin.dbclean;

import com.force.cliforce.Command;
import com.force.cliforce.CommandContext;
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
        return "dbclean";
    }

    @Override
    public String describe() {
        return "Deletes all custom objects in the current org";
    }


    public void execute(CommandContext ctx) throws IOException, ConnectionException {

        System.out.println("Connected to org " + ctx.getPartnerConnection().getUserInfo().getOrganizationId());

        DescribeGlobalResult objs = ctx.getPartnerConnection().describeGlobal();
        MDPackage destructiveChanges = new MDPackage();
        for (DescribeGlobalSObjectResult s : objs.getSobjects()) {
            if (s.isCustom()) {
                ctx.getCommandWriter().println("Preparing to delete " + s.getName());
                destructiveChanges.addCustomObject(s.getName());
            }
        }

        // A bit of a hack:
        // Use same version as force-metadata-api: This is the last path element
        // in the connector URL

        String[] arr = com.sforce.soap.metadata.Connector.END_POINT.split("/");
        String apiVersion = arr[arr.length - 1];

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

//			log(destructiveChanges.toXML());

//			BufferedOutputStream bo = new BufferedOutputStream(
//					new FileOutputStream("tmp.zip"));
//			bo.write(bout.toByteArray());
//			log("Wrote " + bout.toByteArray().length + " bytes to file tmp.zip");
//			bo.close();

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
            System.out.println("Operation succeeded.");
        } else {
            System.out.println("Operation failed. Messages:");
            for (DeployMessage dm : dr.getMessages()) {
                System.out.println("  " + dm.getFullName() + ": " + dm.getProblem());
            }
        }

    }

}
