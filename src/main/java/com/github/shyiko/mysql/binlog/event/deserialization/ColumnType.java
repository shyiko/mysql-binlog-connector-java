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

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public enum ColumnType {

    DECIMAL(0),
    TINY(1),
    SHORT(2),
    LONG(3),
    FLOAT(4),
    DOUBLE(5),
    NULL(6),
    TIMESTAMP(7),
    LONGLONG(8),
    INT24(9),
    DATE(10),
    TIME(11),
    DATETIME(12),
    YEAR(13),
    NEWDATE(14),
    VARCHAR(15),
    BIT(16),
    // (TIMESTAMP|DATETIME|TIME)_V2 data types appeared in MySQL 5.6.4
    // @see http://dev.mysql.com/doc/internals/en/date-and-time-data-type-representation.html
    TIMESTAMP_V2(17),
    DATETIME_V2(18),
    TIME_V2(19),
    JSON(245),
    NEWDECIMAL(246),
    ENUM(247),
    SET(248),
    TINY_BLOB(249),
    MEDIUM_BLOB(250),
    LONG_BLOB(251),
    BLOB(252),
    VAR_STRING(253),
    STRING(254),
    GEOMETRY(255);

    private int code;

    private ColumnType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    private static final Map<Integer, ColumnType> INDEX_BY_CODE;

    static {
        INDEX_BY_CODE = new HashMap<Integer, ColumnType>();
        for (ColumnType columnType : values()) {
            INDEX_BY_CODE.put(columnType.code, columnType);
        }
    }

    public static ColumnType byCode(int code) {
        return INDEX_BY_CODE.get(code);
    }

}
