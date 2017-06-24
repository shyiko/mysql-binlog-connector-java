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
    private AtomicLong totalBytesReceived = new AtomicLong();
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
        return timestamp == 0 ? 0 : (getCurrentTimeMillis() - timestamp) / 1000;
    }

    @Override
    public long getSecondsBehindMaster() {
        // because lastEventHeader and timestampOfLastEvent are not guarded by the common lock
        // we may get some "distorted" results, though shouldn't be a problem given the nature of the final value
        long timestamp = timestampOfLastEvent.get();
        EventHeader eventHeader = lastEventHeader.get();
        if (timestamp == 0 || eventHeader == null) {
            return -1;
        }
        return (timestamp - eventHeader.getTimestamp()) / 1000;
    }

    @Override
    public long getTotalNumberOfEventsSeen() {
        return totalNumberOfEventsSeen.get();
    }

    @Override
    public long getTotalBytesReceived() {
        return totalBytesReceived.get();
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
        totalBytesReceived.set(0);
        numberOfSkippedEvents.set(0);
        numberOfDisconnects.set(0);
    }

    @Override
    public void onEvent(Event event) {
        EventHeader header = event.getHeader();
        lastEventHeader.set(header);
        timestampOfLastEvent.set(getCurrentTimeMillis());
        totalNumberOfEventsSeen.getAndIncrement();
        totalBytesReceived.getAndAdd(header.getHeaderLength() + header.getDataLength());
    }

    @Override
    public void onEventDeserializationFailure(BinaryLogClient client, Exception ex) {
        numberOfSkippedEvents.getAndIncrement();
        lastEventHeader.set(null);
        timestampOfLastEvent.set(getCurrentTimeMillis());
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

    protected long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

}
