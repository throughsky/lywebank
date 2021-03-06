/**
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
package org.apache.hadoop.hbase.index.coprocessor.regionserver;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.hbase.regionserver.RegionScanner;

public interface SeekAndReadRegionScanner extends RegionScanner {

  void addSeekPoints(List<byte[]> seekPoints);

  byte[] getLatestSeekpoint();

  /**
   * This will make the scanner to reseek to the next seek point.
   * @return True when seeked to the next point. False when there is no further seek points.
   * @throws IOException
   */
  boolean seekToNextPoint() throws IOException;

  public boolean isClosed();

}
