/*
 * Copyright 2013 Stanley Shyiko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.shyiko.mysql.binlog;

import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class CountDownEventListener implements BinaryLogClient.EventListener {

    private final Map<EventType, AtomicInteger> counters = new HashMap<EventType, AtomicInteger>();

    @Override
    public void onEvent(Event event) {
        EventType eventType = event.getHeader().getEventType();
        AtomicInteger counter = getCounter(eventType);
        synchronized (counter) {
            if (counter.incrementAndGet() == 0) {
                counter.notify();
            }
        }
    }

    private synchronized AtomicInteger getCounter(EventType eventType) {
        AtomicInteger counter = counters.get(eventType);
        if (counter == null) {
            counters.put(eventType, counter = new AtomicInteger());
        }
        return counter;
    }

    public void waitFor(EventType eventType, int numberOfEvents, long timeoutInMilliseconds)
            throws TimeoutException, InterruptedException {
        AtomicInteger counter = getCounter(eventType);
        synchronized (counter) {
            counter.set(counter.get() - numberOfEvents);
            if (counter.get() != 0) {
                counter.wait(timeoutInMilliseconds);
                if (counter.get() != 0) {
                    throw new TimeoutException("Received " + (numberOfEvents + counter.get()) + " " + eventType +
                            " event(s) instead of expected " + numberOfEvents);
                }
            }
        }
    }

    public synchronized void reset() {
        counters.clear();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CountDownEventListener");
        sb.append("{counters=").append(counters);
        sb.append('}');
        return sb.toString();
    }
}

