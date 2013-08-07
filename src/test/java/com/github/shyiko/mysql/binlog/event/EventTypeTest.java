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
package com.github.shyiko.mysql.binlog.event;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class EventTypeTest {

    @Test
    public void testIsWrite() throws Exception {
        List<EventType> writeEventTypes =
            Arrays.asList(EventType.PRE_GA_WRITE_ROWS, EventType.WRITE_ROWS, EventType.EXT_WRITE_ROWS);
        for (EventType writeEventType : writeEventTypes) {
            assertTrue(EventType.isWrite(writeEventType));
        }
        EnumSet<EventType> eventTypes = EnumSet.allOf(EventType.class);
        eventTypes.removeAll(writeEventTypes);
        for (EventType eventType : eventTypes) {
            assertFalse(EventType.isWrite(eventType));
        }
    }

    @Test
    public void testIsUpdate() throws Exception {
        List<EventType> writeEventTypes =
            Arrays.asList(EventType.PRE_GA_UPDATE_ROWS, EventType.UPDATE_ROWS, EventType.EXT_UPDATE_ROWS);
        for (EventType writeEventType : writeEventTypes) {
            assertTrue(EventType.isUpdate(writeEventType));
        }
        EnumSet<EventType> eventTypes = EnumSet.allOf(EventType.class);
        eventTypes.removeAll(writeEventTypes);
        for (EventType eventType : eventTypes) {
            assertFalse(EventType.isUpdate(eventType));
        }
    }

    @Test
    public void testIsDelete() throws Exception {
        List<EventType> writeEventTypes =
            Arrays.asList(EventType.PRE_GA_DELETE_ROWS, EventType.DELETE_ROWS, EventType.EXT_DELETE_ROWS);
        for (EventType writeEventType : writeEventTypes) {
            assertTrue(EventType.isDelete(writeEventType));
        }
        EnumSet<EventType> eventTypes = EnumSet.allOf(EventType.class);
        eventTypes.removeAll(writeEventTypes);
        for (EventType eventType : eventTypes) {
            assertFalse(EventType.isDelete(eventType));
        }
    }
}
