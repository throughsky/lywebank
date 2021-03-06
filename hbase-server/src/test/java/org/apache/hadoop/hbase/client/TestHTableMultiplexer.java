/**
 * Copyright The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.LargeTests;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(LargeTests.class)
public class TestHTableMultiplexer {
  final Log LOG = LogFactory.getLog(getClass());
  private final static HBaseTestingUtility TEST_UTIL = new HBaseTestingUtility();
  private static byte[] FAMILY = Bytes.toBytes("testFamily");
  private static byte[] QUALIFIER = Bytes.toBytes("testQualifier");
  private static byte[] VALUE1 = Bytes.toBytes("testValue1");
  private static byte[] VALUE2 = Bytes.toBytes("testValue2");
  private static int SLAVES = 3;
  private static int PER_REGIONSERVER_QUEUE_SIZE = 100000;

  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    TEST_UTIL.startMiniCluster(SLAVES);
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    TEST_UTIL.shutdownMiniCluster();
  }

  @Test
  public void testHTableMultiplexer() throws Exception {
    TableName TABLE =
        TableName.valueOf("testHTableMultiplexer");
    final int NUM_REGIONS = 10;
    final int VERSION = 3;
    List<Put> failedPuts;
    boolean success;
    
    HTableMultiplexer multiplexer = new HTableMultiplexer(TEST_UTIL.getConfiguration(), 
        PER_REGIONSERVER_QUEUE_SIZE);

    HTable ht = TEST_UTIL.createTable(TABLE, new byte[][] { FAMILY }, VERSION,
        Bytes.toBytes("aaaaa"), Bytes.toBytes("zzzzz"), NUM_REGIONS);
    TEST_UTIL.waitUntilAllRegionsAssigned(TABLE);

    byte[][] startRows = ht.getStartKeys();
    byte[][] endRows = ht.getEndKeys();

    // SinglePut case
    for (int i = 0; i < NUM_REGIONS; i++) {
      byte [] row = startRows[i];
      if (row == null || row.length <= 0) continue;
      Put put = new Put(row);
      put.add(FAMILY, QUALIFIER, VALUE1);
      success = multiplexer.put(TABLE, put);
      assertTrue(success);

      LOG.info("Put for " + Bytes.toString(startRows[i]) + " @ iteration " + (i+1));

      // verify that the Get returns the correct result
      Get get = new Get(startRows[i]);
      get.addColumn(FAMILY, QUALIFIER);
      Result r;
      int nbTry = 0;
      do {
        assertTrue(nbTry++ < 50);
        Thread.sleep(100);
        r = ht.get(get);
      } while (r == null || r.getValue(FAMILY, QUALIFIER) == null);
      assertEquals(0, Bytes.compareTo(VALUE1, r.getValue(FAMILY, QUALIFIER)));
    }

    // MultiPut case
    List<Put> multiput = new ArrayList<Put>();
    for (int i = 0; i < NUM_REGIONS; i++) {
      byte [] row = endRows[i];
      if (row == null || row.length <= 0) continue;
      Put put = new Put(row);
      put.add(FAMILY, QUALIFIER, VALUE2);
      multiput.add(put);
    }
    failedPuts = multiplexer.put(TABLE, multiput);
    assertTrue(failedPuts == null);

    // verify that the Get returns the correct result
    for (int i = 0; i < NUM_REGIONS; i++) {
      byte [] row = endRows[i];
      if (row == null || row.length <= 0) continue;
      Get get = new Get(row);
      get.addColumn(FAMILY, QUALIFIER);
      Result r;
      int nbTry = 0;
      do {
        assertTrue(nbTry++ < 50);
        Thread.sleep(100);
        r = ht.get(get);
      } while (r == null || r.getValue(FAMILY, QUALIFIER) == null ||
          Bytes.compareTo(VALUE2, r.getValue(FAMILY, QUALIFIER)) != 0);
    }
  }
}
