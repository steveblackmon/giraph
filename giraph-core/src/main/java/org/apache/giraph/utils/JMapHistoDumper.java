/*
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

package org.apache.giraph.utils;

import org.apache.giraph.conf.GiraphConstants;
import org.apache.giraph.conf.ImmutableClassesGiraphConfiguration;
import org.apache.giraph.master.MasterObserver;
import org.apache.giraph.worker.WorkerObserver;
import org.apache.log4j.Logger;

/**
 * An observer for both worker and master that periodically dumps the memory
 * usage using jmap tool.
 */
public class JMapHistoDumper implements MasterObserver, WorkerObserver {
  /** Logger */
  private static final Logger LOG = Logger.getLogger(JMapHistoDumper.class);

  /** How many msec to sleep between calls */
  private int sleepMillis;
  /** How many lines of output to print */
  private int linesToPrint;
  /** Should only print live objects */
  private boolean liveObjectsOnly;

  /** The jmap printing thread */
  private Thread thread;
  /** Halt jmap thread */
  private boolean stop = false;

  @Override
  public void preLoad() {
    // This is called by both WorkerObserver and MasterObserver
    startJMapThread();
  }

  @Override
  public void postSave() {
    // This is called by both WorkerObserver and MasterObserver
    joinJMapThread();
  }

  @Override
  public void preApplication() {
  }

  @Override
  public void postApplication() {
  }

  /**
   * Join the jmap thread
   */
  private void joinJMapThread() {
    stop = true;
    try {
      thread.join(sleepMillis + 5000);
    } catch (InterruptedException e) {
      LOG.error("Failed to join jmap thread");
    }
  }

  /**
   * Start the jmap thread
   */
  public void startJMapThread() {
    stop = false;
    thread = new Thread(new Runnable() {
      @Override
      public void run() {
        while (!stop) {
          JMap.heapHistogramDump(linesToPrint, liveObjectsOnly);
          try {
            Thread.sleep(sleepMillis);
          } catch (InterruptedException e) {
            LOG.warn("JMap histogram sleep interrupted", e);
          }
        }
      }
    });
    thread.start();
  }

  @Override
  public void preSuperstep(long superstep) { }

  @Override
  public void postSuperstep(long superstep) { }

  @Override
  public void applicationFailed(Exception e) { }

  @Override
  public void setConf(ImmutableClassesGiraphConfiguration configuration) {
    sleepMillis = GiraphConstants.JMAP_SLEEP_MILLIS.get(configuration);
    linesToPrint = GiraphConstants.JMAP_PRINT_LINES.get(configuration);
    liveObjectsOnly = GiraphConstants.JMAP_LIVE_ONLY.get(configuration);
  }

  @Override
  public ImmutableClassesGiraphConfiguration getConf() {
    return null;
  }
}
