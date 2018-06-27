/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.wildfly.swarm.microprofile.metrics.runtime;

import java.util.Collection;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.ParallelCounter;
import org.wildfly.swarm.microprofile.metrics.runtime.app.ParallelCounterImpl;

/**
 * @author hrupp
 */
public class ParallelCounterResetPeakTask implements Runnable {

  @Override
  public void run() {

    System.err.println("PCWmF: run");

    for (MetricRegistry.Type type : MetricRegistry.Type.values()) {
      MetricRegistry registry = MetricRegistries.get(type);
      Collection<ParallelCounter> pcs = registry.getParallelCounters().values();
      for (ParallelCounter pc : pcs) {
        ParallelCounterImpl pci = (ParallelCounterImpl) pc;
        pci.resetRecentPeak();
      }
    }
  }
}
