package com.github.shyiko.mysql.binlog.event.deserialization.maria;

import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.deserialization.ChecksumType;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDataDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author <a href="http://github.com/wenerme">wener</a>
 */
public class MariaEventDeserializer extends EventDeserializer
{
    private final EventDeserializer internal;

    public MariaEventDeserializer(EventDeserializer internal)
    {
        this.internal = internal;
        registerMariaDeserializers();
    }

    @Override
    public void setEventDataDeserializer(EventType eventType, EventDataDeserializer eventDataDeserializer)
    {
        internal.setEventDataDeserializer(eventType, eventDataDeserializer);
    }

    private void registerMariaDeserializers()
    {
        setEventDataDeserializer(EventType.MARIA_GTID_EVENT, new GtidDeserializer());
        setEventDataDeserializer(EventType.MARIA_GTID_LIST_EVENT, new GtidListDeserializer());
        setEventDataDeserializer(EventType.MARIA_BINLOG_CHECKPOINT_EVENT, new BinlogCheckpointDeserializer());
    }

    @Override
    public void setChecksumType(ChecksumType checksumType) {internal.setChecksumType(checksumType);}

    @Override
    public Event nextEvent(ByteArrayInputStream is) throws IOException
    {
//        int b = is.read();
//        assert b == 0;
        return internal.nextEvent(is);
    }

    @Override
    public EventDataDeserializer getEventDataDeserializer(EventType eventType)
    {
        // can not delegate, used in super.<init>
        EventDataDeserializer eventDataDeserializer = eventDataDeserializers.get(eventType);
        return eventDataDeserializer != null ? eventDataDeserializer : defaultEventDataDeserializer;
    }
}
