package com.force.cliforce.plugin.codegen.command;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.force.cliforce.CommandContext;
import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestCommandWriter;
import com.force.cliforce.plugin.codegen.command.Describe.DescribeArgs;
import com.google.common.collect.Lists;
import com.sforce.soap.partner.DescribeGlobalResult;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.FieldType;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

/**
 * Unit tests for the describe command.
 * 
 * @author Tim Kral
 */
public class DescribeTest {

    static class MockDescribeGlobalPartnerConnection extends PartnerConnection {
        
        private final DescribeGlobalResult returnedDescribeGlobalResult = new DescribeGlobalResult();
        private final List<DescribeSObjectResult> returnedDescribeSObjectResults = new ArrayList<DescribeSObjectResult>();
        
        public MockDescribeGlobalPartnerConnection(ConnectorConfig config) throws ConnectionException {
            super(config);
        }
        
        @Override
        public DescribeGlobalResult describeGlobal() {
            return returnedDescribeGlobalResult;
        }
        
        @Override
        public DescribeSObjectResult[] describeSObjects(String[] sObjectType) {
            return returnedDescribeSObjectResults.toArray(new DescribeSObjectResult[returnedDescribeSObjectResults.size()]);
        }
        
        // Convenience method that allows a test to construct
        // a DescribeGlobalResult based on DescribeGlobalSObjectResults
        public void setSObjectsForDescribeGlobalResult(List<DescribeGlobalSObjectResult> descSObjects) {
            returnedDescribeGlobalResult.setSobjects(descSObjects.toArray(new DescribeGlobalSObjectResult[descSObjects.size()]));
        }
        
        public void setDescribeSObjectResults(List<DescribeSObjectResult> descSObjects) {
            returnedDescribeSObjectResults.addAll(descSObjects);
        }
        
        public void reset() {
        	returnedDescribeGlobalResult.setSobjects(new DescribeGlobalSObjectResult[0]);
        	returnedDescribeSObjectResults.clear();
        }
    }
    
    
    private final Describe describe = new Describe();
    
    private MockDescribeGlobalPartnerConnection mockConn;
    private TestCommandWriter cmdWriter;
    private CommandContext ctx;
    
    @BeforeClass
    public void classSetUp() throws ConnectionException {
        // This config will require no validation
        ConnectorConfig config = new ConnectorConfig();
        config.setManualLogin(true);
        
        mockConn = new MockDescribeGlobalPartnerConnection(config);
        cmdWriter = new TestCommandWriter();
        ctx = new TestCommandContext().withPartnerConnection(mockConn).withCommandWriter(cmdWriter);
    }

    @AfterMethod
    public void methodTearDown() {
    	mockConn.reset();
    	cmdWriter.reset();
    }
    
    @DataProvider
    public Object[][] describeArgsProvider() {

        return new Object[][]{
                {true/*all*/, false/*custom*/, false/*standard*/, Collections.<String>emptyList(), 
                    "  CustomObject (Custom Object)                                           [                                                           ]\n" +
                    "  StandardObject1 (Standard Object1)                                     [                                                           ]\n" +
                    "  StandardObject2 (Standard Object2)                                     [                                                           ]\n"},
                {false/*all*/, true/*custom*/, false/*standard*/, Collections.<String>emptyList(), 
                    "  CustomObject (Custom Object)                                           [                                                           ]\n"},
                {false/*all*/, false/*custom*/, true/*standard*/, Collections.<String>emptyList(), 
                    "  StandardObject1 (Standard Object1)                                     [                                                           ]\n" +
                    "  StandardObject2 (Standard Object2)                                     [                                                           ]\n"},
                {false/*all*/, false/*custom*/, false/*standard*/, Lists.<String>newArrayList("StandardObject1"), 
                    "  StandardObject1 (Standard Object1)                                     [                                                           ]\n"},
                {false/*all*/, true/*custom*/, false/*standard*/, Lists.<String>newArrayList("StandardObject2"),
                    "  CustomObject (Custom Object)                                           [                                                           ]\n" +
                    "  StandardObject2 (Standard Object2)                                     [                                                           ]\n"},
        };
    }
    
    @Test(dataProvider = "describeArgsProvider")
    public void testDescribeSchema(boolean all, boolean custom, boolean standard, List<String> names, 
            String expectedDescribeString) {
        
    	DescribeGlobalSObjectResult standardObj1 = new DescribeGlobalSObjectResult();
    	standardObj1.setName("StandardObject1");
    	standardObj1.setLabel("Standard Object1");
    	
        DescribeGlobalSObjectResult standardObj2 = new DescribeGlobalSObjectResult();
        standardObj2.setName("StandardObject2");
        standardObj2.setLabel("Standard Object2");
    	
    	DescribeGlobalSObjectResult customObj = new DescribeGlobalSObjectResult();
    	customObj.setName("CustomObject");
    	customObj.setLabel("Custom Object");
    	customObj.setCustom(true);
    	
    	mockConn.setSObjectsForDescribeGlobalResult(Lists.newArrayList(standardObj1, standardObj2, customObj));
    	
    	DescribeArgs args = new DescribeArgs();
    	args.all = all;
    	args.custom = custom;
    	args.standard = standard;
    	args.names = names;
    	describe.executeWithArgs(ctx, args);
    	
    	assertEquals(cmdWriter.getOutput(), expectedDescribeString, "Unexpected describe output");
    }
    
    @Test
    public void testDescribeSchemaVerbose() {
        DescribeGlobalSObjectResult dgsr = new DescribeGlobalSObjectResult();
        dgsr.setName("VerboseObject");
        dgsr.setLabel("Verbose Object");
        
        DescribeSObjectResult dsr = new DescribeSObjectResult();
        dsr.setName("VerboseObject");
        dsr.setLabel("Verbose Object");
        
        Field field = new Field();
        field.setName("VerboseField");
        field.setLabel("Verbose Field");
        field.setType(FieldType.string);
        field.setNillable(true);
        dsr.setFields(new Field[] { field });
        
        mockConn.setSObjectsForDescribeGlobalResult(Collections.<DescribeGlobalSObjectResult>singletonList(dgsr));
        mockConn.setDescribeSObjectResults(Collections.<DescribeSObjectResult>singletonList(dsr));
        
        DescribeArgs args = new DescribeArgs();
        args.all = true;
        args.verbose = true;
        describe.executeWithArgs(ctx, args);
        
        assertEquals(cmdWriter.getOutput(),
                "  VerboseObject (Verbose Object)                                         [                                                           ]\n" +
                "    VerboseField (Verbose Field)                                           [ string                                               ]\n",
                "Unexpected describe output");
    }
    
    @Test
    public void testDescribeAllSchemaWithNoSchema() {
    	DescribeArgs args = new DescribeArgs();
    	args.all = true;
    	describe.executeWithArgs(ctx, args);
    	
    	assertEquals(cmdWriter.getOutput(), 
    			"No schema describe results\n",
    			"Unexpected describe output");
    }
    
    @Test
	public void testDescribeMisnamedSchema() {
		
		// Simulate the case where we can't find the schema
		// that the user has named
		DescribeArgs args = new DescribeArgs();
		args.names = Lists.newArrayList("MisnamedSchema");
		describe.executeWithArgs(ctx, args);
		
		assertEquals(cmdWriter.getOutput(),
				"  UNABLE TO FIND (MisnamedSchema)                                        [                                                           ]\n",
				"Unexpected describe output");
	}

    @Test
    public void testDescribeWithNoSchemaNamed() {
    	describe.executeWithArgs(ctx, new DescribeArgs());
    	assertEquals(cmdWriter.getOutput(), "No schema described. Please specify the schema object names or use -a, -c, -s\n",
    			"Unexpected describe output");
    }
    
	@DataProvider
    public Object[][] schemaCRUDProvider() {

        return new Object[][]{
                {true , false, false, false, "[ CREATEABLE                                                ]"},
                {false, false, false, true , "[                                                 DELETABLE ]"},
                {true , false, true , false, "[ CREATEABLE                      UPDATEABLE                ]"},
                {false, true , false, true , "[                 READABLE                        DELETABLE ]"},
                {true , true , true , true , "[ CREATEABLE      READABLE        UPDATEABLE      DELETABLE ]"},
        };
    }
    
    @Test(dataProvider = "schemaCRUDProvider")
    public void testCRUDPrintFormat(boolean createable, boolean readable, boolean updateable, boolean deletable,
    		String expectedCRUDString) {
    	DescribeGlobalSObjectResult dgsr = new DescribeGlobalSObjectResult();
    	dgsr.setName("testCRUDPrintFormat");
    	dgsr.setLabel("testCRUDPrintFormat");
    	dgsr.setCreateable(createable);
    	dgsr.setQueryable(readable);
    	dgsr.setUpdateable(updateable);
    	dgsr.setDeletable(deletable);
    	
    	mockConn.setSObjectsForDescribeGlobalResult(Lists.newArrayList(dgsr));
    	DescribeArgs args = new DescribeArgs();
    	args.names = Collections.<String>singletonList("testCRUDPrintFormat");
    	describe.executeWithArgs(ctx, args);
    	
    	// Get the CRUD string from the describe results
    	String crudString = cmdWriter.getOutput().substring(cmdWriter.getOutput().indexOf('['), cmdWriter.getOutput().lastIndexOf('\n'));
    	assertEquals(crudString, expectedCRUDString, "Unexpected CRUD string");
    }

    @DataProvider
    public Object[][] fieldInfoProvider() {

        return new Object[][]{
                {FieldType.string, true , false, 
                    "[ string                                               ]"},
                {FieldType.string, false, false, 
                    "[ string                         NOT NULL              ]"},
                {FieldType.string, false, true , 
                    "[ string                         NOT NULL   DEFAULTED  ]"},
                {FieldType.datacategorygroupreference, false, true,
                    "[ datacategorygroupreference     NOT NULL   DEFAULTED  ]"},
        };
    }
    
    @Test(dataProvider = "fieldInfoProvider")
    public void testFieldPrintFormat(FieldType fieldType, boolean nillable, boolean defaultedOnCreate,
            String expectedFieldString) {
        DescribeGlobalSObjectResult dgsr = new DescribeGlobalSObjectResult();
        dgsr.setName("VerboseObject");
        dgsr.setLabel("Verbose Object");
        
        DescribeSObjectResult dsr = new DescribeSObjectResult();
        dsr.setName("VerboseObject");
        dsr.setLabel("Verbose Object");
        
        Field field = new Field();
        field.setName("VerboseField");
        field.setLabel("Verbose Field");
        field.setType(fieldType);
        field.setNillable(nillable);
        field.setDefaultedOnCreate(defaultedOnCreate);
        dsr.setFields(new Field[] { field });
        
        mockConn.setSObjectsForDescribeGlobalResult(Collections.<DescribeGlobalSObjectResult>singletonList(dgsr));
        mockConn.setDescribeSObjectResults(Collections.<DescribeSObjectResult>singletonList(dsr));

        DescribeArgs args = new DescribeArgs();
        args.all = true;
        args.verbose = true;
        describe.executeWithArgs(ctx, args);
        
        String fieldString = cmdWriter.getOutput().substring(cmdWriter.getOutput().lastIndexOf('['), cmdWriter.getOutput().lastIndexOf('\n'));
        assertEquals(fieldString, expectedFieldString, "Unexpected describe output");
    }
}
