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

import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import com.github.shyiko.mysql.binlog.event.TableMapEventMetadata;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;

import java.io.IOException;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class TableMapEventDataDeserializer implements EventDataDeserializer<TableMapEventData> {

    private final TableMapEventMetadataDeserializer metadataDeserializer = new TableMapEventMetadataDeserializer();

    @Override
    public TableMapEventData deserialize(ByteArrayInputStream inputStream) throws IOException {
        TableMapEventData eventData = new TableMapEventData();
        eventData.setTableId(inputStream.readLong(6));
        inputStream.skip(3); // 2 bytes reserved for future use + 1 for the length of database name
        eventData.setDatabase(inputStream.readZeroTerminatedString());
        inputStream.skip(1); // table name
        eventData.setTable(inputStream.readZeroTerminatedString());
        int numberOfColumns = inputStream.readPackedInteger();
        eventData.setColumnTypes(inputStream.read(numberOfColumns));
        inputStream.readPackedInteger(); // metadata length
        eventData.setColumnMetadata(readMetadata(inputStream, eventData.getColumnTypes()));
        eventData.setColumnNullability(inputStream.readBitSet(numberOfColumns, true));
        int metadataLength = inputStream.available();
        TableMapEventMetadata metadata = null;
        if (metadataLength > 0) {
            metadata = metadataDeserializer.deserialize(
                new ByteArrayInputStream(inputStream.read(metadataLength)),
                numericColumnCount(eventData.getColumnTypes())
            );
        }
        eventData.setEventMetadata(metadata);
        return eventData;
    }

    private int numericColumnCount(byte[] types) {
        int count = 0;
        for (int i = 0; i < types.length; i++) {
            switch (ColumnType.byCode(types[i] & 0xff)) {
                case TINY:
                case SHORT:
                case INT24:
                case LONG:
                case LONGLONG:
                case NEWDECIMAL:
                case FLOAT:
                case DOUBLE:
                    count++;
                    break;
                default:
                    break;
            }
        }
        return count;
    }

    private int[] readMetadata(ByteArrayInputStream inputStream, byte[] columnTypes) throws IOException {
        int[] metadata = new int[columnTypes.length];
        for (int i = 0; i < columnTypes.length; i++) {
            switch(ColumnType.byCode(columnTypes[i] & 0xFF)) {
                case FLOAT:
                case DOUBLE:
                case BLOB:
                case JSON:
                case GEOMETRY:
                    metadata[i] = inputStream.readInteger(1);
                    break;
                case BIT:
                case VARCHAR:
                case NEWDECIMAL:
                    metadata[i] = inputStream.readInteger(2);
                    break;
                case SET:
                case ENUM:
                case STRING:
                    metadata[i] = bigEndianInteger(inputStream.read(2), 0, 2);
                    break;
                case TIME_V2:
                case DATETIME_V2:
                case TIMESTAMP_V2:
                    metadata[i] = inputStream.readInteger(1); // fsp (@see {@link ColumnType})
                    break;
                default:
                    metadata[i] = 0;
            }
        }
        return metadata;
    }

    private static int bigEndianInteger(byte[] bytes, int offset, int length) {
        int result = 0;
        for (int i = offset; i < (offset + length); i++) {
            byte b = bytes[i];
            result = (result << 8) | (b >= 0 ? (int) b : (b + 256));
        }
        return result;
    }

}
