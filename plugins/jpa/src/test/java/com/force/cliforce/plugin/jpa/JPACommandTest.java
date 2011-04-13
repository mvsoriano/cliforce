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
        TestCommandContext ctx = createCtxWithJPA(JPAPopulate.class, new TestCommandReader(Lists.newArrayList("3")),
                "-g", "com.force.sdk", "-a", "force-jpa-test", "-v", "21.0.1-SNAPSHOT", "-t");
        validateConnectToOrg(ctx);
        validatePUSelection(ctx);
        
        // try the 'q' quit command
        ctx = createCtxWithJPA(JPAPopulate.class, new TestCommandReader(Lists.newArrayList("q")),
                "-g", "com.force.sdk", "-a", "force-jpa-test", "-v", "21.0.1-SNAPSHOT", "-t");
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
        ctx = createCtxWithJPA(JPAPopulate.class, null,
                "-g", "com.force.sdk", "-a", "force-jpa-test", "-v", "21.0.1-SNAPSHOT", "-t", "-u", "testDNJpaPersistence");
        validateConnectToOrg(ctx);
        Assert.assertTrue(ctx.getCommandWriter().getOutput().endsWith(
                "Running with selected PersistenceUnit: testDNJpaPersistence\n"), ctx.getCommandWriter().getOutput());
    }
    
    @Test
    public void testJPAPluginNegativeParams() throws Exception {
        // Check a missing base param
        TestCommandContext ctx = createCtxWithJPA(JPAPopulate.class, new TestCommandReader(Lists.newArrayList("3")),
                "-g", "-a", "force-jpa-test", "-v", "21.0.1-SNAPSHOT", "-t");
        Assert.assertTrue(ctx.getCommandWriter().getOutput().startsWith(
        "Exception while executing command: populate"), ctx.getCommandWriter().getOutput());
        
        // try no selection followed by invalid selection
        ctx = createCtxWithJPA(JPAPopulate.class, new TestCommandReader(Lists.newArrayList("", "0", "q")),
                "-g", "com.force.sdk", "-a", "force-jpa-test", "-v", "21.0.1-SNAPSHOT", "-t");
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
        // Check all base params
        TestCommandContext ctx = createCtxWithJPA(JPAPopulate.class, new TestCommandReader(Lists.newArrayList("3")),
                "-g", "com.force.sdk", "-a", "force-jpa-test", "-v", "21.0.1-SNAPSHOT", "-t");
        validateConnectToOrg(ctx);
        validatePUSelection(ctx);
    }
    
    @Test
    public void testJPAPluginClean() throws Exception {
        // Check all base params
        TestCommandContext ctx = createCtxWithJPA(JPAClean.class, new TestCommandReader(Lists.newArrayList("3")),
                "-g", "com.force.sdk", "-a", "force-jpa-test", "-v", "21.0.1-SNAPSHOT", "-t");
        validateConnectToOrg(ctx);
        validatePUSelection(ctx);
    }
}
