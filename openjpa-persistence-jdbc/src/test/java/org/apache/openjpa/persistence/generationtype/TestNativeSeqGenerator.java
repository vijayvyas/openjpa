/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.persistence.generationtype;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestNativeSeqGenerator extends SQLListenerTestCase {
    OpenJPAEntityManager em;
    JDBCConfiguration conf;
    DBDictionary dict;
    boolean supportsNativeSequence = false;
    
    EntityE2 entityE2;
    
    @Override
    public void setUp() throws Exception {
        super.setUp(EntityE2.class,DROP_TABLES);
        assertNotNull(emf);
        conf = (JDBCConfiguration) emf.getConfiguration();
        dict = conf.getDBDictionaryInstance();
        supportsNativeSequence = dict.nextSequenceQuery != null;       
        if (supportsNativeSequence) {
            em = emf.createEntityManager();
            assertNotNull(em);
        }
    }
    
    public void createEntityE2() {
        entityE2 = new EntityE2("e name");
    }
    
    public void testGetIdGeneratorSeqGen() {
        if (!supportsNativeSequence) {
            System.out.println("Does not support native sequence");
            return;
        }
        createEntityE2();
        em.getTransaction().begin();
        em.persist(entityE2);
        em.getTransaction().commit();
        int genId = entityE2.getId();        
        int nextId = (int)((Long)em.getIdGenerator(EntityE2.class).next()).longValue();
        assertTrue("Next value should depend on previous genid", nextId >= genId + 1);
        em.close();
    }

    /**
     * Asserts native sequence generator allocates values in memory
     * and requests sequence values from database only when necessary.
     */
    public void testAllocationSize() {
        //Run this test only if the user has elected to not use the Native Sequence Cache.
    	if (supportsNativeSequence && !dict.useNativeSequenceCache){
	        // Turn off statement batching for easier INSERT counting.
	        dict.setBatchLimit(0);
	        em.getTransaction().begin();
	        resetSQL();
	        for (int i = 0; i < 51; i++) {
	            createEntityE2();
	            em.persist(entityE2);
	        }
	        em.getTransaction().commit();
	
	        // Since allocationSize has a default of 50, we expect 2 sequence fetches and 51 INSERTs.
	        assertEquals("53 statements should be executed.", 53, getSQLCount());
	        String[] statements = new String[53];
	        statements[0] = ".*";
	        statements[1] = ".*";
	        for (int i = 2; i < 53; i++) {
	            statements[i] = "INSERT .*";
	        }
	        assertAllExactSQLInOrder(statements);
	        em.close();
    	}
    }
}
