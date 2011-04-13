package com.force.cliforce.plugin.jpa.command;

import java.util.Map;

import javax.persistence.EntityManagerFactory;

import mockit.*;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.force.cliforce.*;
import com.force.cliforce.plugin.jpa.JPAPlugin;
import com.force.sdk.connector.ForceConnectorConfig;
import com.force.sdk.connector.ForceServiceConnector;
import com.force.sdk.connector.threadlocal.ForceThreadLocalStore;
import com.force.sdk.jpa.ForceEntityManagerFactory;
import com.force.sdk.jpa.PersistenceProviderImpl;
import com.google.inject.Guice;
import com.sforce.soap.metadata.DescribeMetadataResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.*;

/**
 * 
 * Basic functionality for JPA plugin test
 *
 * @author fhossain
 * @since javasdk-22.0.0-BETA
 */
public class JPAPluginBaseTest {
	
    Plugin jpaPlugin = new JPAPlugin();
    TestPluginInjector injector;
    
    @MockClass(realClass = PartnerConnection.class, instantiation = Instantiation.PerMockInvocation)
    public static class MockPartnerConnection {
        @Mock
        public GetUserInfoResult getUserInfo()
        throws com.sforce.ws.ConnectionException {
            GetUserInfoResult res = new GetUserInfoResult();
            res.setOrganizationId("mock_organization_id");
            return res;
        }
        
        @Mock
        public DescribeSObjectResult[] describeSObjects(String[] sObjectType)
        throws com.sforce.ws.ConnectionException {
            return new DescribeSObjectResult[0];
        }
        
        @Mock
        public DescribeSObjectResult describeSObject(String sObjectType)
        throws com.sforce.ws.ConnectionException {
            DescribeSObjectResult res = new DescribeSObjectResult();
            res.setFields(new Field[0]);
            return res;
        }
    }
    
    @MockClass(realClass = MetadataConnection.class, instantiation = Instantiation.PerMockInvocation)
    public static class MockMetadataConnection {
        @Mock
        public DescribeMetadataResult describeMetadata(double asOfVersion)
        throws com.sforce.ws.ConnectionException {
            DescribeMetadataResult res = new DescribeMetadataResult();
            res.setOrganizationNamespace("mock_namespace");
            return res;
        }
    }
    
    // This is yucky but could not get instance based mocking to work after classloader action on actual implementation of JPAPlugin
    private volatile static JPAPluginBaseTest current;
    
    @MockClass(realClass = PersistenceProviderImpl.class, instantiation = Instantiation.PerMockInvocation)
    public static class MockPersistenceProviderImpl {
        @Mock
        public EntityManagerFactory createEntityManagerFactory(String unitName, Map properties) {
            // Could not implement this with an anonymous class. Had to make it static.
            current.validateCreateEntityManagerFactory(unitName, properties);
            return new ForceEntityManagerFactory();
        }
    }

    @BeforeClass
    public void classSetup() throws Exception {
        Mockit.setUpMocks(MockPartnerConnection.class,
        MockMetadataConnection.class,
        MockPersistenceProviderImpl.class);
    }
    
    @BeforeMethod
    public void methodSetup() throws Exception {
        injector = Guice.createInjector(new TestModule()).getInstance(TestPluginInjector.class);
    }
    
    protected TestCommandContext createCtxWithJPA (Class<? extends Command> commandClazz, TestCommandReader reader, String... args) throws Exception {
        Command cmd = injector.getInjectedCommand(jpaPlugin, commandClazz);
        ForceConnectorConfig cfg = new ForceConnectorConfig();
        cfg.setUsername("foobar@a.com");
        cfg.setAuthEndpoint("https://login.salesforce.com");
        cfg.setServiceEndpoint("http://someservice.com/services/Soap/u/");
        cfg.setSessionId("mocksession");
        ForceServiceConnector connector = new ForceServiceConnector(cfg);
        connector.setConnectionName("mockconnection");
        ForceThreadLocalStore.setConnectorConfig(cfg);
        TestCommandContext ctx = 
            new TestCommandContext()
            .withConnectionName(connector.getConnectionName())
            .withCommandArguments(args)
            .withMetadataConnection(connector.getMetadataConnection())
            .withPartnerConnection(connector.getConnection());
        if (reader != null) {
            ctx = ctx.withTestCommandReader(reader);
        }
        try {
            current = this;
            cmd.execute(ctx);
        } finally {
            current = null;
        }
        return ctx;
    }
    
    protected void validateCreateEntityManagerFactory(String unitName, Map properties) {
        Assert.assertEquals(unitName, "testDNJpaPersistence", "PersistenceUnit name did not match");
        Assert.assertEquals(properties.get("force.schemaCreateClient"), Boolean.TRUE);
        Assert.assertEquals(properties.get("force.ConnectionName"), "mockconnection");
    }
}
