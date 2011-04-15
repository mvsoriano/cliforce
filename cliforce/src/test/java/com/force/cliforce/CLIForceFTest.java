package com.force.cliforce;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Scanner;

/**
 *
 * @author naamannewbold
 * @since javasdk-22.0.0-BETA
 */
public class CLIForceFTest {

    @Test
    public void testStandardErrOutputRedirectedToErrorLogFile() throws FileNotFoundException {
        CLIForce.setupLogging();

        long timestamp = Calendar.getInstance().getTimeInMillis();
        String timestampString = String.valueOf(timestamp);
        System.err.println(timestamp);

        File errFile = new File(Util.getCliforceHome() + "/.force/cliforce.errors");
        Scanner scanner = new Scanner(errFile);

        Assert.assertEquals(scanner.findWithinHorizon(timestampString, 0), timestampString, "Expected cliforce.errors to contain System.err output");

    }

}
