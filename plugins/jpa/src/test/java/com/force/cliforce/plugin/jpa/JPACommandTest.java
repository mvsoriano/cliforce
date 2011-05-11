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

package com.force.cliforce.plugin.jpa;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.force.cliforce.Command;
import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestCommandReader;
import com.force.cliforce.plugin.jpa.command.*;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * All tests for JPA plugin
 *
 * @author fhossain
 * @since javasdk-22.0.0-BETA
 */
public class JPACommandTest extends JPAPluginBaseTest {

    private static final String TEST_ARTIFACT = "jpa-test-fixture";
    private static final String TEST_VERSION = "22.0.0-SNAPSHOT";
    private static final String TEST_GROUP = "com.force.cliforce";

    @DataProvider
    public Object[][] jpaCommandWithArgs() {
        String[] testFixtureProjectArgs = new String[]{ "-g", TEST_GROUP, "-a", TEST_ARTIFACT, "-v", TEST_VERSION, "-t" };

        return new Object[][] {
              // format: command, ordered input, query, result, command args, expected result
              /*  POSITIVE TESTS  */
                {
                        JPAPopulate.class, null, null, null, new String[]{ "-g", TEST_GROUP, "-a", TEST_ARTIFACT, "-v", TEST_VERSION, "-t", "-u", "testDNJpaPersistence" },
                        "Running with selected PersistenceUnit: testDNJpaPersistence"
                }
              , { JPAPopulate.class, getReader("1", "3"), null, null, testFixtureProjectArgs, "[1-5] q to quit? 3\nRunning with selected PersistenceUnit: testDNJpaPersistence" }
              , { JPAPopulate.class, getReader("1", "q"), null, null, testFixtureProjectArgs, "[1-5] q to quit? q" }
              , { JPAPopulate.class, getReader("1", "", "q"), null, null, testFixtureProjectArgs, "[1-5] q to quit? \n[1-5] q to quit? q\n" }
              , { JPAPopulate.class, getReader("1", "0", "q"), null, null, testFixtureProjectArgs, "[1-5] q to quit? 0\n[1-5] q to quit? q\n" }
              , { JPAClean.class, getReader("1", "3"), null, null, testFixtureProjectArgs, "[1-5] q to quit? 3\nRunning with selected PersistenceUnit: testDNJpaPersistence" }
              , { JPAQuery.class, getReader("1", "3", "select o from Account o", "q"), "select o from Account o", Lists.newArrayList(), testFixtureProjectArgs, "jpql (q to quit) > select o from Account o\nNo data found\njpql (q to quit) > q"}
              /*  NEGATIVE TESTS  */

                // TODO: force a persistenceexception when the whole thing is mocked
//              , { JPAQuery.class, getReader("1", "3", "select o from Account o", "q"), "select o from Account o", Lists.newArrayList(), testFixtureProjectArgs, "javax.persistence.PersistenceException: Class Account for query has not been resolved. Check the query and any imports specification"}
        };
    }

    @Test(dataProvider = "jpaCommandWithArgs")
    public void testJPACommand(Class<? extends Command> command, TestCommandReader reader, String query, List<?> queryResult, String[] testFixtureProjectArgs, String expectedSubstring) throws Exception {
        TestCommandContext testContext = createCtxWithJPA(command, reader, query, queryResult, testFixtureProjectArgs);
        verifyConnection(testContext);
        assertStringContains(testContext.out(), expectedSubstring);
    }

    @Test
    public void testInvalidMavenArgs() throws Exception {
        TestCommandContext testContext = createCtxWithJPA(JPAPopulate.class, getReader("1", "3"), null, null, "-g", "-a", TEST_ARTIFACT, "-v", TEST_VERSION, "-t");
        assertStringContains(testContext.out(), "Exception while executing command: populate");
    }

    private void assertStringContains(String actual, String expectedSubstring) {
        Assert.assertTrue(
                actual.contains(expectedSubstring)
                , "Expected substring: " + expectedSubstring + "\nActual substring: " + actual
        );
    }

    private TestCommandReader getReader(String... inputs) {
        ArrayList inputList = Lists.newArrayList(inputs);
        return new TestCommandReader(inputList);
    }

    private void verifyConnection(TestCommandContext ctx) {
        Assert.assertTrue(ctx.getCommandWriter().getOutput().contains("Connected to org mock_organization_id"), ctx.getCommandWriter().getOutput());
    }

    private void validatePUSelection(TestCommandContext ctx) {
        System.out.println(ctx.getCommandWriter().getOutput());
        Assert.assertTrue(ctx.getCommandWriter().getOutput().contains(
                "Select PersistenceUnit:\n" +
                        "1. SchemaLoadInvocationFTest\n" +
                        "2. extPersCtxPU\n" +
                        "3. testDNJpaPersistence\n" +
                        "4. testDNJpaPersistence2\n" +
                        "5. testDNJpaPersistence3\n" +
                        "[1-5] q to quit? 3\n" +
                        "Running with selected PersistenceUnit: testDNJpaPersistence"), ctx.getCommandWriter().getOutput());
    }

}
