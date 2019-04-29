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

/**
 * @see
 * <a href="https://dev.mysql.com/doc/refman/5.6/en/replication-options-binary-log.html#option_mysqld_binlog-checksum">
 *     MySQL --binlog-checksum option
 * </a>
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public enum ChecksumType {

    NONE(0), CRC32(4);

    private int length;

    private ChecksumType(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    private static final ChecksumType[] VALUES = values();

    public static ChecksumType byOrdinal(int ordinal) {
        return VALUES[ordinal];
    }

}
