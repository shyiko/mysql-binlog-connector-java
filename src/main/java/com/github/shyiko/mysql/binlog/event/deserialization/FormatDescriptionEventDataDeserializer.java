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

import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.FormatDescriptionEventData;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;

import java.io.IOException;

/**
 * @see <a href="https://dev.mysql.com/doc/internals/en/format-description-event.html">FORMAT_DESCRIPTION_EVENT</a>
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class FormatDescriptionEventDataDeserializer implements EventDataDeserializer<FormatDescriptionEventData> {

    @Override
    public FormatDescriptionEventData deserialize(ByteArrayInputStream inputStream) throws IOException {
        int eventBodyLength = inputStream.available();
        FormatDescriptionEventData eventData = new FormatDescriptionEventData();
        eventData.setBinlogVersion(inputStream.readInteger(2));
        eventData.setServerVersion(inputStream.readString(50).trim());
        inputStream.skip(4); // redundant, present in a header
        eventData.setHeaderLength(inputStream.readInteger(1));
        inputStream.skip(EventType.FORMAT_DESCRIPTION.ordinal() - 1);
        eventData.setDataLength(inputStream.readInteger(1));
        int checksumBlockLength = eventBodyLength - eventData.getDataLength();
        ChecksumType checksumType = ChecksumType.NONE;
        if (checksumBlockLength > 0) {
            inputStream.skip(inputStream.available() - checksumBlockLength);
            checksumType = ChecksumType.byOrdinal(inputStream.read());
        }
        eventData.setChecksumType(checksumType);
        return eventData;
    }
}
