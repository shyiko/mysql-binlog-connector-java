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
package com.github.shyiko.mysql.binlog.jmx;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventHeader;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class BinaryLogClientStatistics implements BinaryLogClientStatisticsMXBean,
        BinaryLogClient.EventListener, BinaryLogClient.LifecycleListener {

    private AtomicReference<EventHeader> lastEventHeader = new AtomicReference<EventHeader>();
    private AtomicLong timestampOfLastEvent = new AtomicLong();
    private AtomicLong totalNumberOfEventsSeen = new AtomicLong();
    private AtomicLong numberOfSkippedEvents = new AtomicLong();
    private AtomicLong numberOfDisconnects = new AtomicLong();

    public BinaryLogClientStatistics() {
    }

    public BinaryLogClientStatistics(BinaryLogClient binaryLogClient) {
        binaryLogClient.registerEventListener(this);
        binaryLogClient.registerLifecycleListener(this);
    }

    @Override
    public String getLastEvent() {
        EventHeader eventHeader = lastEventHeader.get();
        return eventHeader == null ? null : eventHeader.getEventType() + "/" + eventHeader.getTimestamp() +
                " from server " + eventHeader.getServerId();
    }

    @Override
    public long getSecondsSinceLastEvent() {
        long timestamp = timestampOfLastEvent.get();
        return timestamp == 0 ? 0 : (System.currentTimeMillis() - timestamp) / 1000;
    }

    @Override
    public long getTotalNumberOfEventsSeen() {
        return totalNumberOfEventsSeen.get();
    }

    @Override
    public long getNumberOfSkippedEvents() {
        return numberOfSkippedEvents.get();
    }

    @Override
    public long getNumberOfDisconnects() {
        return numberOfDisconnects.get();
    }

    @Override
    public void reset() {
        lastEventHeader.set(null);
        timestampOfLastEvent.set(0);
        totalNumberOfEventsSeen.set(0);
        numberOfSkippedEvents.set(0);
        numberOfDisconnects.set(0);
    }

    @Override
    public void onEvent(Event event) {
        lastEventHeader.set(event.getHeader());
        timestampOfLastEvent.set(System.currentTimeMillis());
        totalNumberOfEventsSeen.getAndIncrement();
    }

    @Override
    public void onEventDeserializationFailure(BinaryLogClient client, Exception ex) {
        numberOfSkippedEvents.getAndIncrement();
        lastEventHeader.set(null);
        timestampOfLastEvent.set(System.currentTimeMillis());
        totalNumberOfEventsSeen.getAndIncrement();
    }

    @Override
    public void onDisconnect(BinaryLogClient client) {
        numberOfDisconnects.getAndIncrement();
    }

    @Override
    public void onConnect(BinaryLogClient client) {
    }

    @Override
    public void onCommunicationFailure(BinaryLogClient client, Exception ex) {
    }

}
