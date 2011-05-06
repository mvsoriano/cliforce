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

import java.util.*;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

import org.testng.Assert;

/**
 * 
 * Dummy class for JPA Query testing
 *
 * @author fhossain
 * @since javasdk-22.0.0-BETA
 */
public class DummyEntityManager implements EntityManager {

    private String query;
    private List<?> result;
    
    public DummyEntityManager(String query, List<?> result) {
        this.query = query;
        this.result = result;
    }
    
    @Override
    public void persist(Object entity) {
        // TODO Auto-generated method stub

    }

    @Override
    public <T> T merge(T entity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void remove(Object entity) {
        // TODO Auto-generated method stub

    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void flush() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setFlushMode(FlushModeType flushMode) {
        // TODO Auto-generated method stub

    }

    @Override
    public FlushModeType getFlushMode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void lock(Object entity, LockModeType lockMode) {
        // TODO Auto-generated method stub

    }

    @Override
    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        // TODO Auto-generated method stub

    }

    @Override
    public void refresh(Object entity) {
        // TODO Auto-generated method stub

    }

    @Override
    public void refresh(Object entity, Map<String, Object> properties) {
        // TODO Auto-generated method stub

    }

    @Override
    public void refresh(Object entity, LockModeType lockMode) {
        // TODO Auto-generated method stub

    }

    @Override
    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        // TODO Auto-generated method stub

    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub

    }

    @Override
    public void detach(Object entity) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean contains(Object entity) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public LockModeType getLockMode(Object entity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setProperty(String propertyName, Object value) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Object> getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Query createQuery(String qlString) {
        Assert.assertEquals(qlString, query, "Query does not match");
        return new Query() {
            
            @Override
            public <T> T unwrap(Class<T> cls) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Query setParameter(int position, Date value, TemporalType temporalType) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Query setParameter(int position, Calendar value, TemporalType temporalType) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Query setParameter(String name, Date value, TemporalType temporalType) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Query setParameter(String name, Calendar value, TemporalType temporalType) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Query setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Query setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Query setParameter(int position, Object value) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Query setParameter(String name, Object value) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public <T> Query setParameter(Parameter<T> param, T value) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Query setMaxResults(int maxResult) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Query setLockMode(LockModeType lockMode) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Query setHint(String hintName, Object value) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Query setFlushMode(FlushModeType flushMode) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Query setFirstResult(int startPosition) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public boolean isBound(Parameter<?> param) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public Object getSingleResult() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public List getResultList() {
                return result;
            }
            
            @Override
            public Set<Parameter<?>> getParameters() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Object getParameterValue(int position) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Object getParameterValue(String name) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public <T> T getParameterValue(Parameter<T> param) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public <T> Parameter<T> getParameter(int position, Class<T> type) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public <T> Parameter<T> getParameter(String name, Class<T> type) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Parameter<?> getParameter(int position) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Parameter<?> getParameter(String name) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public int getMaxResults() {
                // TODO Auto-generated method stub
                return 0;
            }
            
            @Override
            public LockModeType getLockMode() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Map<String, Object> getHints() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public FlushModeType getFlushMode() {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public int getFirstResult() {
                // TODO Auto-generated method stub
                return 0;
            }
            
            @Override
            public int executeUpdate() {
                // TODO Auto-generated method stub
                return 0;
            }
        };
    }

    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Query createNamedQuery(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Query createNativeQuery(String sqlString) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Query createNativeQuery(String sqlString, Class resultClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void joinTransaction() {
        // TODO Auto-generated method stub

    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getDelegate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isOpen() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public EntityTransaction getTransaction() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Metamodel getMetamodel() {
        // TODO Auto-generated method stub
        return null;
    }

}
