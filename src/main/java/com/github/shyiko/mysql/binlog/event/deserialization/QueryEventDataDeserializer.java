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

import com.github.shyiko.mysql.binlog.event.QueryEventData;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;

import java.io.IOException;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class QueryEventDataDeserializer implements EventDataDeserializer<QueryEventData> {

    @Override
    public QueryEventData deserialize(ByteArrayInputStream inputStream) throws IOException {
        QueryEventData eventData = new QueryEventData();
        eventData.setThreadId(inputStream.readLong(4));
        eventData.setExecutionTime(inputStream.readLong(4));
        inputStream.skip(1); // length of the name of the database
        eventData.setErrorCode(inputStream.readInteger(2));
        inputStream.skip(inputStream.readInteger(2)); // status variables block
        eventData.setDatabase(inputStream.readZeroTerminatedString());
        eventData.setSql(inputStream.readString(inputStream.available()));
        return eventData;
    }
}
