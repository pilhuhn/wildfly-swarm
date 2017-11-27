/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.swarm.microprofile_metrics.runtime;

import java.util.Map;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricRegistry;

/**
 * @author hrupp
 */
public class PeriodicGCReader implements Runnable {
  @Override
  public void run() {
    try {
      JmxWorker jmxWorker = JmxWorker.instance();

      MetricRegistry baseRegistry = MetricRegistryFactory.getBaseRegistry();
      for (Map.Entry<String,Histogram> entry : baseRegistry.getHistograms().entrySet()) {
        String name = entry.getKey();
        Histogram hist = entry.getValue();

        ExtendedMetadata metadata = (ExtendedMetadata) baseRegistry.getMetadata().get(name);

        Number value = jmxWorker.getValue(metadata.getMbean());

        hist.update(value.longValue());
      }
    } catch (Throwable t) {
      System.err.println(t.getMessage());
      t.printStackTrace();
    }
  }
}
