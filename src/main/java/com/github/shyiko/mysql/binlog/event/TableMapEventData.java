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

import java.util.BitSet;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class TableMapEventData implements EventData {

    private long tableId;
    private String database;
    private String table;
    private byte[] columnTypes;
    private int[] columnMetadata;
    private BitSet columnNullability;
    private TableMapEventMetadata eventMetadata;

    public long getTableId() {
        return tableId;
    }

    public void setTableId(long tableId) {
        this.tableId = tableId;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public byte[] getColumnTypes() {
        return columnTypes;
    }

    public void setColumnTypes(byte[] columnTypes) {
        this.columnTypes = columnTypes;
    }

    public int[] getColumnMetadata() {
        return columnMetadata;
    }

    public void setColumnMetadata(int[] columnMetadata) {
        this.columnMetadata = columnMetadata;
    }

    public BitSet getColumnNullability() {
        return columnNullability;
    }

    public void setColumnNullability(BitSet columnNullability) {
        this.columnNullability = columnNullability;
    }

    public TableMapEventMetadata getEventMetadata() { return eventMetadata; }

    public void setEventMetadata(TableMapEventMetadata eventMetadata) { this.eventMetadata = eventMetadata; }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TableMapEventData");
        sb.append("{tableId=").append(tableId);
        sb.append(", database='").append(database).append('\'');
        sb.append(", table='").append(table).append('\'');
        sb.append(", columnTypes=").append(columnTypes == null ? "null" : "");
        for (int i = 0; columnTypes != null && i < columnTypes.length; ++i) {
            sb.append(i == 0 ? "" : ", ").append(columnTypes[i]);
        }
        sb.append(", columnMetadata=").append(columnMetadata == null ? "null" : "");
        for (int i = 0; columnMetadata != null && i < columnMetadata.length; ++i) {
            sb.append(i == 0 ? "" : ", ").append(columnMetadata[i]);
        }
        sb.append(", columnNullability=").append(columnNullability);
        sb.append(", eventMetadata=").append(eventMetadata);
        sb.append('}');
        return sb.toString();
    }
}
