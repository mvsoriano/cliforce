package com.force.cliforce.plugin.codegen.command;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
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
        
        public MockDescribeGlobalPartnerConnection(ConnectorConfig config) throws ConnectionException {
            super(config);
        }
        
        public DescribeGlobalResult describeGlobal() {
            return returnedDescribeGlobalResult;
        }
        
        // Convenience method that allows a test to construct
        // a DescribeGlobalResult based on DescribeGlobalSObjectResults
        public void setSObjectsForDescribeGlobalResult(List<DescribeGlobalSObjectResult> descSObjects) {
            returnedDescribeGlobalResult.setSobjects(descSObjects.toArray(new DescribeGlobalSObjectResult[descSObjects.size()]));
        }
        
        public void reset() {
        	returnedDescribeGlobalResult.setSobjects(new DescribeGlobalSObjectResult[0]);
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
    
    @Test
    public void testDescribeAllSchema() {
    	DescribeGlobalSObjectResult standardObj = new DescribeGlobalSObjectResult();
    	standardObj.setName("StandardObject");
    	standardObj.setLabel("Standard Object");
    	
    	DescribeGlobalSObjectResult customObj = new DescribeGlobalSObjectResult();
    	customObj.setName("CustomObject");
    	customObj.setLabel("Custom Object");
    	customObj.setCustom(true);
    	
    	mockConn.setSObjectsForDescribeGlobalResult(Lists.newArrayList(standardObj, customObj));
    	
    	DescribeArgs args = new DescribeArgs();
    	args.all = true;
    	describe.executeWithArgs(ctx, args);
    	
    	assertEquals(cmdWriter.getOutput(), 
    			"Custom Schema:\n" +
    			"  CustomObject (Custom Object)                                           [                                                           ]\n" +
    			"\n" +
    			"Standard Schema:\n" +
    		    "  StandardObject (Standard Object)                                       [                                                           ]\n",
    			"Unexpected describe output");
    }
    
    @Test
    public void testDescribeAllSchemaWithNoSchema() {
    	DescribeArgs args = new DescribeArgs();
    	args.all = true;
    	describe.executeWithArgs(ctx, args);
    	
    	assertEquals(cmdWriter.getOutput(), 
    			"Custom Schema:\n" +
    			"  There is no custom schema in your org\n" +
    			"\n" +
    			"Standard Schema:\n" +
    		    "  There is no standard schema in your org\n",
    			"Unexpected describe output");
    }
    
    @Test
    public void testDescribeNamedSchema() {
    	DescribeGlobalSObjectResult namedObj = new DescribeGlobalSObjectResult();
    	namedObj.setName("NamedObject");
    	namedObj.setLabel("Named Object");
    	
    	mockConn.setSObjectsForDescribeGlobalResult(Lists.newArrayList(namedObj));
    	describe.executeWithArgs(ctx, constructDescribeArgs(namedObj));
    	
    	assertEquals(cmdWriter.getOutput(), 
    			"  NamedObject (Named Object)                                             [                                                           ]\n",
    			"Unexpected describe output");
    }
    
    @Test
    public void testDescribeNamedSchemaOrder() {
    	DescribeGlobalSObjectResult namedObj1 = new DescribeGlobalSObjectResult();
    	namedObj1.setName("NamedObject1");
    	namedObj1.setLabel("Named Object 1");
    	
    	DescribeGlobalSObjectResult namedObj2 = new DescribeGlobalSObjectResult();
    	namedObj2.setName("NamedObject2");
    	namedObj2.setLabel("Named Object 2");
    	
    	// We'll reverse the order of the named objects in the args
    	mockConn.setSObjectsForDescribeGlobalResult(Lists.newArrayList(namedObj1, namedObj2));
    	describe.executeWithArgs(ctx, constructDescribeArgs(namedObj2, namedObj1));
    	
    	// Assert that the describe results are in the same order as they went into the args
    	assertEquals(cmdWriter.getOutput(), 
    			"  NamedObject2 (Named Object 2)                                          [                                                           ]\n" +
    			"  NamedObject1 (Named Object 1)                                          [                                                           ]\n",
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
    	assertEquals(cmdWriter.getOutput(), "No schema described. Please specify the schema object names or use -a\n",
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
    	describe.executeWithArgs(ctx, constructDescribeArgs(dgsr));
    	
    	// Get the CRUD string from the describe results
    	String crudString = cmdWriter.getOutput().substring(cmdWriter.getOutput().indexOf('['), cmdWriter.getOutput().lastIndexOf('\n'));
    	assertEquals(crudString, expectedCRUDString, "Unexpected CRUD string");
    }
    
    private DescribeArgs constructDescribeArgs(DescribeGlobalSObjectResult... dgsrs) {
    	DescribeArgs args = new DescribeArgs();
    	List<String> names = new ArrayList<String>(dgsrs.length);
    	
    	for (DescribeGlobalSObjectResult dgsr : dgsrs) {
    		names.add(dgsr.getName());
    	}
    	
    	args.names = names;
    	return args;
    }
}
