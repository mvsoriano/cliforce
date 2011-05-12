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

package com.force.cliforce.plugin.jpa.command;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import mockit.*;

import org.datanucleus.jpa.EntityManagerFactoryImpl;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.force.cliforce.*;
import com.force.cliforce.plugin.jpa.JPAPlugin;
import com.force.sdk.connector.ForceConnectorConfig;
import com.force.sdk.connector.ForceServiceConnector;
import com.force.sdk.connector.threadlocal.ForceThreadLocalStore;
import com.force.sdk.jpa.ForceEntityManagerFactory;
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
    String query;
    List<?> result;

    protected static final Properties jpaTestFixtureProperties = new Properties();
    static {
        try {
            jpaTestFixtureProperties.load(ClassLoader.getSystemResource("jpa-test-fixture.properties").openStream());
        } catch (Exception e) {
            Assert.fail("Unable to retrieve properties for jpa test fixture.", e);
        }
    }

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

//        @Mock
//        public QueryResult query(String query) {
//            final QueryResult queryResult = new QueryResult();
//            queryResult.setDone(true);
//            queryResult.setRecords();
//            queryResult.setSize();
//            return queryResult;
//        }
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

    @MockClass(realClass = ForceEntityManagerFactory.class, instantiation = Instantiation.PerMockInvocation)
    public static final class MockForceEntityManagerFactory {
        @Mock
        public void $init(String unitName, Map properties) {}
    }
    
    // This is yucky but could not get instance based mocking to work after classloader action on actual implementation of JPAPlugin
    private volatile static JPAPluginBaseTest current;
    
    @MockClass(realClass = com.force.sdk.jpa.PersistenceProviderImpl.class, instantiation = Instantiation.PerMockInvocation)
    public static class MockPersistenceProviderImpl extends org.datanucleus.jpa.PersistenceProviderImpl {
        @Override
        @Mock
        public EntityManagerFactory createEntityManagerFactory(String unitName, Map properties) {
            // Could not implement this with an anonymous class. Had to make it static.
            current.validateCreateEntityManagerFactory(unitName, properties);
            return new ForceEntityManagerFactory(unitName, properties);
        }
    }

    @MockClass(realClass = EntityManagerFactoryImpl.class, instantiation = Instantiation.PerMockInvocation)
    public static class MockEntityManagerFactory {
        @Mock
        public EntityManager createEntityManager() {
            return new DummyEntityManager(current.query, current.result);
        }
    }
    
    @BeforeClass
    public void classSetup() throws Exception {
        Mockit.setUpMocks(MockPartnerConnection.class,
        MockMetadataConnection.class,
        MockPersistenceProviderImpl.class,
        MockEntityManagerFactory.class,
        MockForceEntityManagerFactory.class
        );

    }
    
    @BeforeMethod
    public void methodSetup() throws Exception {
        injector = Guice.createInjector(new TestModule()).getInstance(TestPluginInjector.class);
    }
    
    protected TestCommandContext createCtxWithJPA (Class<? extends Command> commandClazz, TestCommandReader reader, String query, List<?> result, String... args) throws Exception {
        Command cmd = injector.getInjectedCommand(jpaPlugin, commandClazz);
        ForceConnectorConfig cfg = new ForceConnectorConfig();
        cfg.setUsername("foobar@a.com");
        cfg.setAuthEndpoint("https://fake.login.salesforce.com");
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
            current.query = query;
            current.result = result;
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
