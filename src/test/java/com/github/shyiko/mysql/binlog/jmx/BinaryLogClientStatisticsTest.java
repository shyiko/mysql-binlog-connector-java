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

import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventHeaderV4;
import com.github.shyiko.mysql.binlog.event.EventType;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class BinaryLogClientStatisticsTest {

    @Test
    public void testInitialState() throws Exception {
        BinaryLogClientStatistics statistics = new BinaryLogClientStatistics();
        assertNull(statistics.getLastEvent());
        assertEquals(statistics.getSecondsSinceLastEvent(), 0L);
        assertEquals(statistics.getSecondsBehindMaster(), -1L);
        assertEquals(statistics.getTotalNumberOfEventsSeen(), 0L);
        assertEquals(statistics.getNumberOfSkippedEvents(), 0L);
        assertEquals(statistics.getNumberOfDisconnects(), 0L);
    }

    @Test
    public void testOnEvent() throws Exception {
        BinaryLogClientStatistics statistics = new BinaryLogClientStatistics() {

            private Queue<Long> responseQueue = new LinkedList<Long>(Arrays.<Long>asList(1L, 1010L, 3030L));

            @Override
            protected long getCurrentTimeMillis() {
                return responseQueue.remove();
            }
        };
        assertNull(statistics.getLastEvent());
        assertEquals(statistics.getSecondsSinceLastEvent(), 0L);
        assertEquals(statistics.getTotalNumberOfEventsSeen(), 0L);
        assertEquals(statistics.getNumberOfSkippedEvents(), 0L);
        assertEquals(statistics.getNumberOfDisconnects(), 0L);
        statistics.onEvent(generateEvent(1L, EventType.FORMAT_DESCRIPTION, 1, 104)); // calls getCurrentTimeMillis
        assertEquals(statistics.getLastEvent(), "FORMAT_DESCRIPTION/1 from server 1");
        assertEquals(statistics.getSecondsSinceLastEvent(), 1); // calls getCurrentTimeMillis
        assertEquals(statistics.getSecondsBehindMaster(), 0);
        assertEquals(statistics.getTotalNumberOfEventsSeen(), 1);
        assertEquals(statistics.getNumberOfSkippedEvents(), 0);
        statistics.onEvent(generateEvent(1L, EventType.FORMAT_DESCRIPTION, 1, 104)); // calls getCurrentTimeMillis
        assertEquals(statistics.getSecondsBehindMaster(), 3);
    }

    @Test
    public void testOnEventDeserializationFailure() throws Exception {
        BinaryLogClientStatistics statistics = new BinaryLogClientStatistics();
        statistics.onEvent(generateEvent(1L, EventType.FORMAT_DESCRIPTION, 1, 104));
        statistics.onEventDeserializationFailure(null, null);
        assertNull(statistics.getLastEvent());
        assertEquals(statistics.getTotalNumberOfEventsSeen(), 2L);
        assertEquals(statistics.getNumberOfSkippedEvents(), 1L);
    }

    @Test
    public void testOnDisconnect() throws Exception {
        BinaryLogClientStatistics statistics = new BinaryLogClientStatistics();
        statistics.onDisconnect(null);
        assertEquals(statistics.getNumberOfDisconnects(), 1L);
    }

    @Test
    public void testReset() throws Exception {
        BinaryLogClientStatistics statistics = new BinaryLogClientStatistics();
        statistics.onEventDeserializationFailure(null, null);
        statistics.onEvent(generateEvent(1L, EventType.FORMAT_DESCRIPTION, 1, 104));
        statistics.onDisconnect(null);
        statistics.reset();
        assertNull(statistics.getLastEvent());
        assertEquals(statistics.getSecondsSinceLastEvent(), 0L);
        assertEquals(statistics.getSecondsBehindMaster(), -1L);
        assertEquals(statistics.getTotalNumberOfEventsSeen(), 0L);
        assertEquals(statistics.getNumberOfSkippedEvents(), 0L);
        assertEquals(statistics.getNumberOfDisconnects(), 0L);
    }

    private Event generateEvent(long timestamp, EventType type, long serverId, long nextPosition) {
        EventHeaderV4 header = new EventHeaderV4();
        header.setTimestamp(timestamp);
        header.setEventType(type);
        header.setServerId(serverId);
        header.setNextPosition(nextPosition);
        return new Event(header, null);
    }
}
