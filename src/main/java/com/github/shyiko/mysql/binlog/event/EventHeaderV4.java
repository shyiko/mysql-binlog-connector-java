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

/**
 * Used in MySQL 5.0+.
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class EventHeaderV4 implements EventHeader {

    // v1 (MySQL 3.23)
    private long timestamp;
    private EventType eventType;
    private long serverId;
    private long eventLength;
    // v3 (MySQL 4.0.2-4.1)
    private long nextPosition;
    private int flags;

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    @Override
    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public long getEventLength() {
        return eventLength;
    }

    public void setEventLength(long eventLength) {
        this.eventLength = eventLength;
    }

    public long getPosition() {
        return nextPosition - eventLength;
    }

    public long getNextPosition() {
        return nextPosition;
    }

    public void setNextPosition(long nextPosition) {
        this.nextPosition = nextPosition;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    @Override
    public long getHeaderLength() {
        return 19;
    }

    @Override
    public long getDataLength() {
        return getEventLength() - getHeaderLength();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("EventHeaderV4");
        sb.append("{timestamp=").append(timestamp);
        sb.append(", eventType=").append(eventType);
        sb.append(", serverId=").append(serverId);
        sb.append(", headerLength=").append(getHeaderLength());
        sb.append(", dataLength=").append(getDataLength());
        sb.append(", nextPosition=").append(nextPosition);
        sb.append(", flags=").append(flags);
        sb.append('}');
        return sb.toString();
    }
}
