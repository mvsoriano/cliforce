package com.force.cliforce.plugin.codegen.command;

import com.force.cliforce.TestCommandContext;
import com.force.cliforce.TestCommandWriter;
import com.force.cliforce.TestModule;
import com.force.cliforce.plugin.codegen.command.JPAClass.JPAClassArgs;
import com.force.sdk.codegen.ForceJPAClassGenerator;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import mockit.*;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Unit tests for the describe command.
 * 
 * @author Tim Kral
 */
public class JPAClassTest {

    @MockClass(realClass = ForceJPAClassGenerator.class, instantiation = Instantiation.PerMockSetup)
    public static class MockForceJPAClassGenerator {

        private List<String> expectedObjectNames = new ArrayList<String>();
        
        @Mock
        public int generateJPAClasses(List<String> objectNames) throws ConnectionException, IOException {
            // Assert that the object names are as expected
            for (int i = 0; i < objectNames.size(); i++) {
                assertEquals(objectNames.get(i), expectedObjectNames.get(i),
                        "Unexpected object names at index " + i);
            }

            // Assert that there are any extra names expected or in the objectNames argument
            assertEquals(objectNames.size(), expectedObjectNames.size(),
                    "Unexpected number of object names in generator");
            
            return objectNames.size();
        }
        
        public void setExpectedObjectNames(String... objectNames) {
            for (String objectName : objectNames) {
                expectedObjectNames.add(objectName);
            }
        }
    }
    
    private final JPAClass jpaClass = new JPAClass();
    
    private MockForceJPAClassGenerator mockGenerator;
    private TestCommandWriter cmdWriter;
    private TestCommandContext ctx;
    private Injector injector;
    
    @BeforeClass
    public void classSetUp() throws ConnectionException {
        injector = Guice.createInjector(new TestModule());
        // This config will require no validation
        ConnectorConfig config = new ConnectorConfig();
        config.setManualLogin(true);
        
        cmdWriter = new TestCommandWriter();
        ctx = new TestCommandContext().withPartnerConnection(new PartnerConnection(config)).withCommandWriter(cmdWriter);
        
    }
    
    @BeforeMethod
    public void methodSetUp() {
        // Since we use a local MockUp class (see testDestDirFilePath)
        // We need to setup and tear down the mocks for each test
        mockGenerator = new MockForceJPAClassGenerator();
        Mockit.setUpMock(ForceJPAClassGenerator.class, mockGenerator);        
    }
    
    @AfterMethod
    public void methodTearDown() {
        cmdWriter.reset();
        ctx.setCommandArguments(new String[]{});
        
        // Since we use a local MockUp class (see testDestDirFilePath)
        // We need to setup and tear down the mocks for each test
        Mockit.tearDownMocks(ForceJPAClassGenerator.class);
    }
    
    @DataProvider
    public Object[][] filePathProvider() {

        return new Object[][]{
                {null, null, System.getProperty("user.dir") + "/./src/main/java"},
                {"/projectDir", null, "/projectDir/./src/main/java"},
                {null, "destDir", System.getProperty("user.dir") + "/destDir"},
                {"/projectDir", "/destDir", "/projectDir/destDir"},
                {"~/projectDir", "/destDir", System.getProperty("user.home") + "/projectDir/destDir"}
        };
    }
    
    @Test(dataProvider = "filePathProvider")
    public void testDestDirFilePath(String projectDir, String destDir, final String expectedFilePath) throws Exception {
        new MockUp<ForceJPAClassGenerator>() {
            
            @SuppressWarnings("unused")
            @Mock
            public void $init(PartnerConnection conn, File destDir) {
                // Assert the file path in the generator constructor
                assertEquals(destDir.getAbsolutePath(), expectedFilePath,
                        "Unexpected file path in generator");
            }
        };

        JPAClass jpaClassCommand = injector.getInstance(JPAClass.class);
        List<String> args = new ArrayList<String>();

        if (projectDir != null) {
            args.add("--projectDir");
            args.add(projectDir);
        }

        if (destDir != null) {
            args.add("-d");
            args.add(destDir);
        }

        ctx.setCommandArguments((String[]) args.toArray(new String[0]));
        jpaClassCommand.execute(ctx);
    }
    
    @Test
    public void testGenerateAllSchemaObjects() {
        mockGenerator.setExpectedObjectNames("*");
        
        JPAClassArgs args = new JPAClassArgs();
        args.all = true;
        jpaClass.executeWithArgs(ctx, args);
    }
    
    @Test
    public void testGenerateNamedSchemaObjects() {
        mockGenerator.setExpectedObjectNames("Object1", "Object2");
        
        JPAClassArgs args = new JPAClassArgs();
        args.names = Lists.<String>newArrayList("Object1", "Object2");
        jpaClass.executeWithArgs(ctx, args);
        
        assertEquals(cmdWriter.getOutput(),
                "Successfully generated 2 JPA classes\n",
                "Unexpected jpaClass output");
    }
    
    @Test
    public void testGenerateWithNoSchemaNamed() {
        jpaClass.executeWithArgs(ctx, new JPAClassArgs());
        assertEquals(cmdWriter.getOutput(), "No Java classes generated. Please specify the schema object names or use -a\n",
                "Unexpected jpaClass output");
    }
}
