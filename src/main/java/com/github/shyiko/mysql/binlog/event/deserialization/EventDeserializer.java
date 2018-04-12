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

import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventData;
import com.github.shyiko.mysql.binlog.event.EventHeader;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class EventDeserializer {

    private final EventHeaderDeserializer eventHeaderDeserializer;
    private final EventDataDeserializer defaultEventDataDeserializer;
    private final Map<EventType, EventDataDeserializer> eventDataDeserializers;

    private EnumSet<CompatibilityMode> compatibilitySet = EnumSet.noneOf(CompatibilityMode.class);
    private int checksumLength;

    private final Map<Long, TableMapEventData> tableMapEventByTableId;

    private EventDataDeserializer tableMapEventDataDeserializer;

    public EventDeserializer() {
        this(new EventHeaderV4Deserializer(), new NullEventDataDeserializer());
    }

    public EventDeserializer(EventHeaderDeserializer eventHeaderDeserializer) {
        this(eventHeaderDeserializer, new NullEventDataDeserializer());
    }

    public EventDeserializer(EventDataDeserializer defaultEventDataDeserializer) {
        this(new EventHeaderV4Deserializer(), defaultEventDataDeserializer);
    }

    public EventDeserializer(
            EventHeaderDeserializer eventHeaderDeserializer,
            EventDataDeserializer defaultEventDataDeserializer
    ) {
        this.eventHeaderDeserializer = eventHeaderDeserializer;
        this.defaultEventDataDeserializer = defaultEventDataDeserializer;
        this.eventDataDeserializers = new IdentityHashMap<EventType, EventDataDeserializer>();
        this.tableMapEventByTableId = new HashMap<Long, TableMapEventData>();
        registerDefaultEventDataDeserializers();
        afterEventDataDeserializerSet(null);
    }

    public EventDeserializer(
            EventHeaderDeserializer eventHeaderDeserializer,
            EventDataDeserializer defaultEventDataDeserializer,
            Map<EventType, EventDataDeserializer> eventDataDeserializers,
            Map<Long, TableMapEventData> tableMapEventByTableId
    ) {
        this.eventHeaderDeserializer = eventHeaderDeserializer;
        this.defaultEventDataDeserializer = defaultEventDataDeserializer;
        this.eventDataDeserializers = eventDataDeserializers;
        this.tableMapEventByTableId = tableMapEventByTableId;
        afterEventDataDeserializerSet(null);
    }

    private void registerDefaultEventDataDeserializers() {
        eventDataDeserializers.put(EventType.FORMAT_DESCRIPTION,
                new FormatDescriptionEventDataDeserializer());
        eventDataDeserializers.put(EventType.ROTATE,
                new RotateEventDataDeserializer());
        eventDataDeserializers.put(EventType.INTVAR,
            new IntVarEventDataDeserializer());
        eventDataDeserializers.put(EventType.QUERY,
                new QueryEventDataDeserializer());
        eventDataDeserializers.put(EventType.TABLE_MAP,
                new TableMapEventDataDeserializer());
        eventDataDeserializers.put(EventType.XID,
                new XidEventDataDeserializer());
        eventDataDeserializers.put(EventType.WRITE_ROWS,
                new WriteRowsEventDataDeserializer(tableMapEventByTableId));
        eventDataDeserializers.put(EventType.UPDATE_ROWS,
                new UpdateRowsEventDataDeserializer(tableMapEventByTableId));
        eventDataDeserializers.put(EventType.DELETE_ROWS,
                new DeleteRowsEventDataDeserializer(tableMapEventByTableId));
        eventDataDeserializers.put(EventType.EXT_WRITE_ROWS,
                new WriteRowsEventDataDeserializer(tableMapEventByTableId).
                        setMayContainExtraInformation(true));
        eventDataDeserializers.put(EventType.EXT_UPDATE_ROWS,
                new UpdateRowsEventDataDeserializer(tableMapEventByTableId).
                        setMayContainExtraInformation(true));
        eventDataDeserializers.put(EventType.EXT_DELETE_ROWS,
                new DeleteRowsEventDataDeserializer(tableMapEventByTableId).
                        setMayContainExtraInformation(true));
        eventDataDeserializers.put(EventType.ROWS_QUERY,
                new RowsQueryEventDataDeserializer());
        eventDataDeserializers.put(EventType.GTID,
                new GtidEventDataDeserializer());
       eventDataDeserializers.put(EventType.PREVIOUS_GTIDS,
               new PreviousGtidSetDeserializer());
        eventDataDeserializers.put(EventType.XA_PREPARE,
                new XAPrepareEventDataDeserializer());
    }

    public void setEventDataDeserializer(EventType eventType, EventDataDeserializer eventDataDeserializer) {
        ensureCompatibility(eventDataDeserializer);
        eventDataDeserializers.put(eventType, eventDataDeserializer);
        afterEventDataDeserializerSet(eventType);
    }

    private void afterEventDataDeserializerSet(EventType eventType) {
        if (eventType == null || eventType == EventType.TABLE_MAP) {
            EventDataDeserializer eventDataDeserializer = getEventDataDeserializer(EventType.TABLE_MAP);
            if (eventDataDeserializer.getClass() != TableMapEventDataDeserializer.class &&
                eventDataDeserializer.getClass() != EventDataWrapper.Deserializer.class) {
                tableMapEventDataDeserializer = new EventDataWrapper.Deserializer(
                    new TableMapEventDataDeserializer(), eventDataDeserializer);
            } else {
                tableMapEventDataDeserializer = null;
            }
        }
    }

    public void setChecksumType(ChecksumType checksumType) {
        this.checksumLength = checksumType.getLength();
    }

    /**
     * @see CompatibilityMode
     */
    public void setCompatibilityMode(CompatibilityMode first, CompatibilityMode... rest) {
        this.compatibilitySet = EnumSet.of(first, rest);
        for (EventDataDeserializer eventDataDeserializer : eventDataDeserializers.values()) {
            ensureCompatibility(eventDataDeserializer);
        }
    }

    private void ensureCompatibility(EventDataDeserializer eventDataDeserializer) {
        if (eventDataDeserializer instanceof AbstractRowsEventDataDeserializer) {
            AbstractRowsEventDataDeserializer deserializer =
                (AbstractRowsEventDataDeserializer) eventDataDeserializer;
            boolean deserializeDateAndTimeAsLong =
                compatibilitySet.contains(CompatibilityMode.DATE_AND_TIME_AS_LONG) ||
                compatibilitySet.contains(CompatibilityMode.DATE_AND_TIME_AS_LONG_MICRO);
            deserializer.setDeserializeDateAndTimeAsLong(deserializeDateAndTimeAsLong);
            deserializer.setMicrosecondsPrecision(
                compatibilitySet.contains(CompatibilityMode.DATE_AND_TIME_AS_LONG_MICRO)
            );
            if (compatibilitySet.contains(CompatibilityMode.INVALID_DATE_AND_TIME_AS_ZERO)) {
                deserializer.setInvalidDateAndTimeRepresentation(0L);
            }
            if (compatibilitySet.contains(CompatibilityMode.INVALID_DATE_AND_TIME_AS_NEGATIVE_ONE)) {
                if (!deserializeDateAndTimeAsLong) {
                    throw new IllegalArgumentException("INVALID_DATE_AND_TIME_AS_NEGATIVE_ONE requires " +
                        "DATE_AND_TIME_AS_LONG or DATE_AND_TIME_AS_LONG_MICRO");
                }
                deserializer.setInvalidDateAndTimeRepresentation(-1L);
            }
            if (compatibilitySet.contains(CompatibilityMode.INVALID_DATE_AND_TIME_AS_MIN_VALUE)) {
                if (!deserializeDateAndTimeAsLong) {
                    throw new IllegalArgumentException("INVALID_DATE_AND_TIME_AS_MIN_VALUE requires " +
                        "DATE_AND_TIME_AS_LONG or DATE_AND_TIME_AS_LONG_MICRO");
                }
                deserializer.setInvalidDateAndTimeRepresentation(Long.MIN_VALUE);
            }
            deserializer.setDeserializeCharAndBinaryAsByteArray(
                compatibilitySet.contains(CompatibilityMode.CHAR_AND_BINARY_AS_BYTE_ARRAY)
            );
        }
    }

    /**
     * @return deserialized event or null in case of end-of-stream
     */
    public Event nextEvent(ByteArrayInputStream inputStream) throws IOException {
        if (inputStream.peek() == -1) {
            return null;
        }
        EventHeader eventHeader = eventHeaderDeserializer.deserialize(inputStream);
        EventDataDeserializer eventDataDeserializer = getEventDataDeserializer(eventHeader.getEventType());
        if (eventHeader.getEventType() == EventType.TABLE_MAP && tableMapEventDataDeserializer != null) {
            eventDataDeserializer = tableMapEventDataDeserializer;
        }
        EventData eventData = deserializeEventData(inputStream, eventHeader, eventDataDeserializer);
        if (eventHeader.getEventType() == EventType.TABLE_MAP) {
            TableMapEventData tableMapEvent;
            if (eventData instanceof EventDataWrapper) {
                EventDataWrapper eventDataWrapper = (EventDataWrapper) eventData;
                tableMapEvent = (TableMapEventData) eventDataWrapper.getInternal();
                if (tableMapEventDataDeserializer != null) {
                    eventData = eventDataWrapper.getExternal();
                }
            } else {
                tableMapEvent = (TableMapEventData) eventData;
            }
            tableMapEventByTableId.put(tableMapEvent.getTableId(), tableMapEvent);
        }
        return new Event(eventHeader, eventData);
    }

    private EventData deserializeEventData(ByteArrayInputStream inputStream, EventHeader eventHeader,
            EventDataDeserializer eventDataDeserializer) throws EventDataDeserializationException {
        // todo: use checksum algorithm descriptor from FormatDescriptionEvent
        // (as per http://dev.mysql.com/worklog/task/?id=2540)
        int eventBodyLength = (int) eventHeader.getDataLength() - checksumLength;
        EventData eventData;
        try {
            inputStream.enterBlock(eventBodyLength);
            try {
                eventData = eventDataDeserializer.deserialize(inputStream);
            } finally {
                inputStream.skipToTheEndOfTheBlock();
                inputStream.skip(checksumLength);
            }
        } catch (IOException e) {
            throw new EventDataDeserializationException(eventHeader, e);
        }
        return eventData;
    }

    public EventDataDeserializer getEventDataDeserializer(EventType eventType) {
        EventDataDeserializer eventDataDeserializer = eventDataDeserializers.get(eventType);
        return eventDataDeserializer != null ? eventDataDeserializer : defaultEventDataDeserializer;
    }

    /**
     * @see CompatibilityMode#DATE_AND_TIME_AS_LONG
     * @see CompatibilityMode#DATE_AND_TIME_AS_LONG_MICRO
     * @see CompatibilityMode#INVALID_DATE_AND_TIME_AS_ZERO
     * @see CompatibilityMode#CHAR_AND_BINARY_AS_BYTE_ARRAY
     */
    public enum CompatibilityMode {
        /**
         * Return DATETIME/DATETIME_V2/TIMESTAMP/TIMESTAMP_V2/DATE/TIME/TIME_V2 values as long|s
         * (number of milliseconds since the epoch (00:00:00 Coordinated Universal Time (UTC), Thursday, 1 January 1970,
         * not counting leap seconds)) (instead of java.util.Date/java.sql.Timestamp/java.sql.Date/new java.sql.Time).
         *
         * <p>This option is going to be enabled by default starting from mysql-binlog-connector-java@1.0.0.
         */
        DATE_AND_TIME_AS_LONG,
        /**
         * Same as {@link CompatibilityMode#DATE_AND_TIME_AS_LONG} but values are returned in microseconds.
         */
        DATE_AND_TIME_AS_LONG_MICRO,
        /**
         * Return 0 instead of null if year/month/day is 0.
         * Affects DATETIME/DATETIME_V2/DATE/TIME/TIME_V2.
         */
        INVALID_DATE_AND_TIME_AS_ZERO,
        /**
         * Return -1 instead of null if year/month/day is 0.
         * Affects DATETIME/DATETIME_V2/DATE/TIME/TIME_V2.
         */
        INVALID_DATE_AND_TIME_AS_NEGATIVE_ONE,
        /**
         * Return Long.MIN_VALUE instead of null if year/month/day is 0.
         * Affects DATETIME/DATETIME_V2/DATE/TIME/TIME_V2.
         */
        INVALID_DATE_AND_TIME_AS_MIN_VALUE,
        /**
         * Return CHAR/VARCHAR/BINARY/VARBINARY values as byte[]|s (instead of String|s).
         *
         * <p>This option is going to be enabled by default starting from mysql-binlog-connector-java@1.0.0.
         */
        CHAR_AND_BINARY_AS_BYTE_ARRAY
    }

    /**
     * Enwraps internal {@link EventData} if custom {@link EventDataDeserializer} is provided (for internally used
     * events only).
     */
    public static class EventDataWrapper implements EventData {

        private EventData internal;
        private EventData external;

        public EventDataWrapper(EventData internal, EventData external) {
            this.internal = internal;
            this.external = external;
        }

        public EventData getInternal() {
            return internal;
        }

        public EventData getExternal() {
            return external;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("InternalEventData");
            sb.append("{internal=").append(internal);
            sb.append(", external=").append(external);
            sb.append('}');
            return sb.toString();
        }

        /**
         * {@link com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer.EventDataWrapper} deserializer.
         */
        public static class Deserializer implements EventDataDeserializer {

            private EventDataDeserializer internal;
            private EventDataDeserializer external;

            public Deserializer(EventDataDeserializer internal, EventDataDeserializer external) {
                this.internal = internal;
                this.external = external;
            }

            @Override
            public EventData deserialize(ByteArrayInputStream inputStream) throws IOException {
                byte[] bytes = inputStream.read(inputStream.available());
                EventData internalEventData = internal.deserialize(new ByteArrayInputStream(bytes));
                EventData externalEventData = external.deserialize(new ByteArrayInputStream(bytes));
                return new EventDataWrapper(internalEventData, externalEventData);
            }
        }

    }

}
