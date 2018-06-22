/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * *******************************************************************************
 * Copyright 2010-2013 Coda Hale and Yammer, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.swarm.microprofile.metrics.runtime.app;

import java.util.concurrent.atomic.LongAdder;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.ParallelCounter;

/**
 * @author hrupp
 */
public class ParallelCounterImpl implements ParallelCounter, Counter {

    private final LongAdder count;
    private long max;
    private long[] lastLong = new long[10];

    public ParallelCounterImpl() {
        count = new LongAdder();
        max = 0;
    }

    @Override
    public void inc() {
        count.increment();
        if (count.longValue() > max) {
            max = count.longValue();
        }
    }

    @Override
    public void inc(long n) {
        count.add(n);
        if (count.longValue() > max) {
            max = count.longValue();
        }
    }

    @Override
    public void dec() {
        count.decrement();
    }

    @Override
    public void dec(long n) {
        count.add(-n);
    }

    @Override
    public long getCount() {
        return count.sum();
    }

    @Override
    public long getMax() {
        return max;
    }

    @Override
    public void resetMax() {
        max = 0;
    }

    @Override
    public long[] maxLast10Minutes() {
        return lastLong;
    }

    public void moveItemsAndReset() {
        System.arraycopy(lastLong, 0, lastLong, 1, 9);
        lastLong[0] = max;
        max = 0;
    }
}
