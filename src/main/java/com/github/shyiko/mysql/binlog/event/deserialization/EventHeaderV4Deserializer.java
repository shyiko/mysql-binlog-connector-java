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
package com.github.shyiko.mysql.binlog.event.deserialization;

import com.github.shyiko.mysql.binlog.event.EventHeaderV4;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class EventHeaderV4Deserializer implements EventHeaderDeserializer<EventHeaderV4> {


    private static final Map<Integer,EventType> EVENT_TYPES;

    static {
        // Mutable map but will never update, so it's safe.
        EVENT_TYPES = new HashMap<Integer, EventType>();
        for (EventType type : EventType.values())
        {
            EVENT_TYPES.put(type.get(),type);
        }
    }
    @Override
    public EventHeaderV4 deserialize(ByteArrayInputStream inputStream) throws IOException {
        EventHeaderV4 header = new EventHeaderV4();
        header.setTimestamp(inputStream.readLong(4) * 1000L);
        header.setEventType(getEventType(inputStream.readInteger(1)));
        header.setServerId(inputStream.readLong(4));
        header.setEventLength(inputStream.readLong(4));
        header.setNextPosition(inputStream.readLong(4));
        header.setFlags(inputStream.readInteger(2));
        return header;
    }

    private static EventType getEventType(int ordinal) throws IOException {
        EventType type = EVENT_TYPES.get(ordinal);
        if (type == null) {
            throw new IOException("Unknown event type " + ordinal);
        }
        return type;
    }

}
