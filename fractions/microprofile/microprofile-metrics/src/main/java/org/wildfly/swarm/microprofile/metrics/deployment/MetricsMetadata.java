/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.wildfly.swarm.microprofile.metrics.deployment;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.HitCounted;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.ParallelCounted;
import org.eclipse.microprofile.metrics.annotation.Timed;

class MetricsMetadata {

    private MetricsMetadata() {
    }

    static <E extends Member & AnnotatedElement> void registerMetrics(MetricRegistry registry, MetricResolver resolver, Class<?> bean, E element) {
        MetricResolver.Of<Counted> counted = resolver.counted(bean, element);
        if (counted.isPresent()) {
            Counted t = counted.metricAnnotation();
            Metadata metadata = getMetadata(counted.metricName(), t.unit(), t.description(), t.displayName(), MetricType.COUNTER, t.tags());
            registry.counter(metadata);
        }
        MetricResolver.Of<HitCounted> hitCountedOf = resolver.hitCounted(bean, element);
        if (hitCountedOf.isPresent()) {
            HitCounted t = hitCountedOf.metricAnnotation();
            Metadata metadata = getMetadata(hitCountedOf.metricName(), t.unit(),t.description(),t.displayName(),
                                            MetricType.HIT_COUNTER, t.tags());

            registry.hitCounter(metadata);
        }

        MetricResolver.Of<ParallelCounted> parallelCountedOf = resolver.parallelCounted(bean, element);
        if (parallelCountedOf.isPresent()) {
            ParallelCounted t = parallelCountedOf.metricAnnotation();
            Metadata metadata = getMetadata(parallelCountedOf.metricName(), t.unit(),t.description(),t.displayName(),
                                            MetricType.PARALLEL_COUNTER, t.tags());

            registry.parallelCounter(metadata);
        }

        MetricResolver.Of<Metered> metered = resolver.metered(bean, element);
        if (metered.isPresent()) {
            Metered t = metered.metricAnnotation();
            Metadata metadata = getMetadata(metered.metricName(), t.unit(), t.description(), t.displayName(), MetricType.METERED, t.tags());
            registry.meter(metadata);
        }
        MetricResolver.Of<Timed> timed = resolver.timed(bean, element);
        if (timed.isPresent()) {
            Timed t = timed.metricAnnotation();
            Metadata metadata = getMetadata(timed.metricName(), t.unit(), t.description(), t.displayName(), MetricType.TIMER, t.tags());
            registry.timer(metadata);
        }
    }

    static Metadata getMetadata(String name, String unit, String description, String displayName, MetricType type, String... tags) {

        MetadataBuilder builder = Metadata.builder().withName(name).withType(type);
        if (!unit.isEmpty()) {
            builder.withUnit(unit);
        }
        if (!description.isEmpty()) {
            builder.withDescription(description);
        }
        if (!displayName.isEmpty()) {
            builder.withDisplayName(displayName);
        }
        if (tags != null && tags.length > 0) {
            for (String tag : tags) {
                builder.addTags(tag);
            }
        }
        return builder.build();
    }

}
