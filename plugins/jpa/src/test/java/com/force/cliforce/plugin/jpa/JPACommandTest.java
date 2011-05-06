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
import org.testng.annotations.Test;

import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestCommandReader;
import com.force.cliforce.plugin.jpa.command.*;
import com.google.common.collect.Lists;

/**
 * 
 * All tests for JPA plugin
 *
 * @author fhossain
 * @since javasdk-22.0.0-BETA
 */
public class JPACommandTest extends JPAPluginBaseTest {

    private void validateConnectToOrg(TestCommandContext ctx) {
        Assert.assertTrue(ctx.getCommandWriter().getOutput().contains("Connected to org mock_organization_id"), ctx.getCommandWriter().getOutput());
    }
    
    private void validatePUSelection(TestCommandContext ctx) {
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
    
    /**
     * This test will check basic PersistenceUnit selection that is common to all JPA commands
     * @throws Exception
     */
    @Test
    public void testJPAPluginCommandParams() throws Exception {
        // Check all base params
        TestCommandContext ctx = createCtxWithJPA(JPAPopulate.class, new TestCommandReader(Lists.newArrayList("3")), null, null,
                "-g", "com.force.sdk", "-a", "force-jpa-test", "-v", "22.0.0-SNAPSHOT", "-t");
        validateConnectToOrg(ctx);
        validatePUSelection(ctx);
        
        // try the 'q' quit command
        ctx = createCtxWithJPA(JPAPopulate.class, new TestCommandReader(Lists.newArrayList("q")), null, null,
                "-g", "com.force.sdk", "-a", "force-jpa-test", "-v", "22.0.0-SNAPSHOT", "-t");
        validateConnectToOrg(ctx);
        Assert.assertTrue(ctx.getCommandWriter().getOutput().endsWith(
                "Select PersistenceUnit:\n" +
                "1. SchemaLoadInvocationFTest\n" +
                "2. extPersCtxPU\n" +
                "3. testDNJpaPersistence\n" +
                "4. testDNJpaPersistence2\n" +
                "5. testDNJpaPersistence3\n" +
                "[1-5] q to quit? q\n"), ctx.getCommandWriter().getOutput());
        
        // Pass in persistenceunit and expect no selection
        ctx = createCtxWithJPA(JPAPopulate.class, null, null, null,
                "-g", "com.force.sdk", "-a", "force-jpa-test", "-v", "22.0.0-SNAPSHOT", "-t", "-u", "testDNJpaPersistence");
        validateConnectToOrg(ctx);
        Assert.assertTrue(ctx.getCommandWriter().getOutput().endsWith(
                "Running with selected PersistenceUnit: testDNJpaPersistence\n"), ctx.getCommandWriter().getOutput());
    }
    
    @Test
    public void testJPAPluginNegativeParams() throws Exception {
        // Check a missing base param
        TestCommandContext ctx = createCtxWithJPA(JPAPopulate.class, new TestCommandReader(Lists.newArrayList("3")), null, null,
                "-g", "-a", "force-jpa-test", "-v", "22.0.0-SNAPSHOT", "-t");
        Assert.assertTrue(ctx.getCommandWriter().getOutput().startsWith(
        "Exception while executing command: populate"), ctx.getCommandWriter().getOutput());
        
        // try no selection followed by invalid selection
        ctx = createCtxWithJPA(JPAPopulate.class, new TestCommandReader(Lists.newArrayList("", "0", "q")), null, null,
                "-g", "com.force.sdk", "-a", "force-jpa-test", "-v", "22.0.0-SNAPSHOT", "-t");
        validateConnectToOrg(ctx);
        Assert.assertTrue(ctx.getCommandWriter().getOutput().endsWith(
                "Select PersistenceUnit:\n" +
                "1. SchemaLoadInvocationFTest\n" +
                "2. extPersCtxPU\n" +
                "3. testDNJpaPersistence\n" +
                "4. testDNJpaPersistence2\n" +
                "5. testDNJpaPersistence3\n" +
                "[1-5] q to quit? \n" +
                "[1-5] q to quit? 0\n" +
                "[1-5] q to quit? q\n"), ctx.getCommandWriter().getOutput());
    }
    
    @Test
    public void testJPAPluginPopulate() throws Exception {
        TestCommandContext ctx = createCtxWithJPA(JPAPopulate.class, new TestCommandReader(Lists.newArrayList("3")), null, null,
                "-g", "com.force.sdk", "-a", "force-jpa-test", "-v", "22.0.0-SNAPSHOT", "-t");
        validateConnectToOrg(ctx);
        validatePUSelection(ctx);
    }
    
    @Test
    public void testJPAPluginClean() throws Exception {
        TestCommandContext ctx = createCtxWithJPA(JPAClean.class, new TestCommandReader(Lists.newArrayList("3")), null, null,
                "-g", "com.force.sdk", "-a", "force-jpa-test", "-v", "21.0.1-SNAPSHOT", "-t");
        validateConnectToOrg(ctx);
        validatePUSelection(ctx);
    }
    
    @Test
    public void testJPAPluginQuery() throws Exception {
        // Run some JPQL
        TestCommandContext ctx = createCtxWithJPA(JPAQuery.class, new TestCommandReader(Lists.newArrayList("3", "select o from Account o", "q")),
                "select o from Account o",
                Lists.newArrayList(),
                "-g", "com.force.sdk", "-a", "force-jpa-test", "-v", "22.0.0-SNAPSHOT", "-t");
        validateConnectToOrg(ctx);
        validatePUSelection(ctx);
    }
}
