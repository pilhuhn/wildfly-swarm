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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE_DEPTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * @author hrupp
 */
public class BaseMetricWorker {
  private static final String HEAP_MEMORY_USAGE = "heap-memory-usage";
  private static final String CORE_SERVICE = "core-service";
  private static final String PLATFORM_MBEAN = "platform-mbean";
  private static final String TYPE = "type";
  private static final String QUERY = "query";
  private static BaseMetricWorker worker;
  private ModelControllerClient controllerClient;

  private static final String SELECT = "select";


  private BaseMetricWorker() { /* Singleton */ }

  public static BaseMetricWorker create(ModelControllerClient controllerClient) {

    worker = new BaseMetricWorker();
    worker.controllerClient = controllerClient;
    return worker;
  }

  public static BaseMetricWorker instance() {
    if (worker == null) {
      throw new IllegalStateException("You must first create a worker via create()");
    }
    return worker;
  }

   Map<String,Double> getBaseMetrics() {

     Map<String,Double> outcome = new HashMap<>();

    ModelNode op = new ModelNode();
    op.get(ADDRESS).add(CORE_SERVICE, PLATFORM_MBEAN);
    op.get(ADDRESS).add(TYPE, "threading");
    op.get(OP).set(QUERY);
    op.get(SELECT).add("thread-count");
    op.get(SELECT).add("daemon-thread-count");
    op.get(SELECT).add("peak-thread-count");
    op.get(SELECT).add("total-started-thread-count");

    try {
        ModelNode response = controllerClient.execute(op);
        ModelNode result = unwrap(response);


        outcome.put("thread.count",result.get("thread-count").asDouble());
        outcome.put("thread.daemon.count",result.get("daemon-thread-count").asDouble());
        outcome.put("thread.max.count",result.get("peak-thread-count").asDouble());

    } catch (IOException e) {
        throw new RuntimeException(e); // TODO return some 500 message or such
    }

     op = new ModelNode();
     op.get(ADDRESS).add(CORE_SERVICE, PLATFORM_MBEAN);
     op.get(ADDRESS).add(TYPE, "memory");
     op.get(OP).set(QUERY);
     op.get(SELECT).add(HEAP_MEMORY_USAGE);
     op.get(SELECT).add("non-heap-memory-usage");

     try {
       ModelNode response = controllerClient.execute(op);
       ModelNode result = unwrap(response);
       outcome.put("memory.usedHeap",result.get(HEAP_MEMORY_USAGE).get("used").asDouble());
       outcome.put("memory.maxHeap",result.get(HEAP_MEMORY_USAGE).get("max").asDouble());
       outcome.put("memory.committedHeap",result.get(HEAP_MEMORY_USAGE).get("committed").asDouble());

     } catch (IOException e) {
         throw new RuntimeException(e);
     }

     op = new ModelNode();
     op.get(ADDRESS).add(CORE_SERVICE, PLATFORM_MBEAN);
     op.get(ADDRESS).add(TYPE, "class-loading");
     op.get(OP).set(QUERY);
     op.get(SELECT).add("loaded-class-count");
     op.get(SELECT).add("total-loaded-class-count");
     op.get(SELECT).add("unloaded-class-count");

     try {
         ModelNode response = controllerClient.execute(op);
         ModelNode result = unwrap(response);

       outcome.put("classloader.currentLoadedClass.count",result.get("loaded-class-count").asDouble());
       outcome.put("classloader.totalUnloadedClass.count",result.get("unloaded-class-count").asDouble());
       outcome.put("classloader.totalLoadedClass.count",result.get("total-loaded-class-count").asDouble());

     } catch (IOException e) {
         throw new RuntimeException(e); // TODO return some 500 message or such
     }

     op = new ModelNode();
     op.get(ADDRESS).add(CORE_SERVICE, PLATFORM_MBEAN);
     op.get(ADDRESS).add(TYPE, "operating-system");
     op.get(OP).set(QUERY);
     op.get(SELECT).add("available-processors");
     op.get(SELECT).add("system-load-average");

     try {
         ModelNode response = controllerClient.execute(op);
         ModelNode result = unwrap(response);

         outcome.put("cpu.availableProcessors",result.get("available-processors").asDouble());
         outcome.put("cpu.systemLoadAverage",result.get("system-load-average").asDouble());

     } catch (IOException e) {
         throw new RuntimeException(e); // TODO return some 500 message or such
     }

     op = new ModelNode();
     op.get(ADDRESS).add(CORE_SERVICE, PLATFORM_MBEAN);
     op.get(ADDRESS).add(TYPE, "runtime");
     op.get(OP).set(QUERY);
     op.get(SELECT).add("uptime");

     try {
         ModelNode response = controllerClient.execute(op);
         ModelNode result = unwrap(response);

         outcome.put("jvm.uptime",result.get("uptime").asDouble());

     } catch (IOException e) {
         throw new RuntimeException(e); // TODO return some 500 message or such
     }

     // Get garbage collector info. This is a bit special, as there can be multiple
     // Garbage collectors.
     op = new ModelNode();
     op.get(ADDRESS).add(CORE_SERVICE, PLATFORM_MBEAN);
     op.get(ADDRESS).add(TYPE, "garbage-collector");
     op.get(OP).set(READ_RESOURCE_OPERATION);
     op.get(INCLUDE_RUNTIME).set(true);
     op.get(RECURSIVE).set(true);
     op.get(RECURSIVE_DEPTH).set(1); // If we don't set this, depth=0

     try {
       ModelNode response = controllerClient.execute(op);
       ModelNode result = unwrap(response);


       List<ModelNode> collectors = result.get("name").asList();

       for (ModelNode node : collectors) {
         String collectorName = node.asProperty().getName();
         String baseName = "gc." + collectorName;

         outcome.put(baseName + ".count", node.asProperty().getValue().get("collection-count").asDouble());
         outcome.put(baseName + ".time", node.asProperty().getValue().get("collection-time").asDouble());

       }
     } catch (IOException e) {
       throw new RuntimeException(e); // TODO return some 500 message or such
     }

     return outcome;
   }


  private static ModelNode unwrap(ModelNode response) {
      if (response.get(OUTCOME).asString().equals(SUCCESS)) {
          return response.get(RESULT);
      } else {
          return response;
      }
  }

  void registerBaseMetrics() {

    MetricRegistry baseRegistry = MetricRegistryFactory.get(MetricRegistry.Type.BASE);

    baseRegistry.getMetadata().put("thread.count", new Metadata("thread.count", MetricType.COUNTER));
    baseRegistry.getMetadata().put("thread.daemon.count", new Metadata("thread.daemon.count", MetricType.COUNTER));
    baseRegistry.getMetadata().put("thread.max.count", new Metadata("thread.max.count", MetricType.COUNTER));

    baseRegistry.getMetadata().put("memory.maxHeap", new Metadata("memory.maxHeap", MetricType.GAUGE, "bytes"));
    baseRegistry.getMetadata().put("memory.usedHeap", new Metadata("memory.usedHeap", MetricType.GAUGE, "bytes"));
    baseRegistry.getMetadata().put("memory.committedHeap", new Metadata("memory.committedHeap", MetricType.GAUGE, "bytes"));

    baseRegistry.getMetadata().put("classloader.currentLoadedClass.count",
                                   new Metadata("classloader.currentLoadedClass.count", MetricType.COUNTER, MetricUnits.NONE));
    baseRegistry.getMetadata().put("classloader.totalLoadedClass.count",
                                   new Metadata("classloader.totalLoadedClass.count", MetricType.COUNTER, MetricUnits.NONE));
    baseRegistry.getMetadata().put("classloader.totalUnloadedClass.count",
                                   new Metadata("classloader.totalUnloadedClass.count", MetricType.COUNTER, MetricUnits.NONE));

    baseRegistry.getMetadata().put("cpu.availableProcessors",
                                   new Metadata("cpu.availableProcessors", MetricType.GAUGE, MetricUnits.NONE));
    baseRegistry.getMetadata().put("cpu.systemLoadAverage",
                                   new Metadata("cpu.systemLoadAverage", MetricType.GAUGE, MetricUnits.NONE));
    baseRegistry.getMetadata().put("jvm.uptime",
                                   new Metadata("jvm.uptime", MetricType.GAUGE, MetricUnits.MILLISECONDS));


    List<Metadata> entries = findGarbageCollectors();
    for (Metadata entry : entries) {
      baseRegistry.getMetadata().put(entry.getName(), entry);
    }

  }

  private List<Metadata> findGarbageCollectors() {

    ModelNode op = new ModelNode();
    op.get(ADDRESS).add(CORE_SERVICE, PLATFORM_MBEAN);
    op.get(ADDRESS).add(TYPE, "garbage-collector");
    op.get(OP).set(QUERY);

    try {
      ModelNode response = controllerClient.execute(op);
      ModelNode result = unwrap(response);

      List<ModelNode> collectors = result.get("name").asList();
      List<Metadata> output = new ArrayList<>(2 * collectors.size());
      for (ModelNode node : collectors) {
        String collectorName = node.asProperty().getName();
        String baseName = "gc." + collectorName;
        Metadata m = new Metadata(baseName + ".time", MetricType.GAUGE, MetricUnits.MILLISECONDS);
        output.add(m);
        m = new Metadata(baseName + ".count", MetricType.COUNTER);
        output.add(m);
      }

      return output;

    } catch (IOException e) {
      throw new RuntimeException(e); // TODO return some 500 message or such
    }

  }
}