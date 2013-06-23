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
package com.github.shyiko.mysql.binlog.network.protocol.command;

import com.github.shyiko.mysql.binlog.io.ByteArrayOutputStream;

import java.io.IOException;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class DumpBinaryLogCommand implements Command {

    private long serverId;
    private String binlogFilename;
    private long binlogPosition;

    public DumpBinaryLogCommand(long serverId, String binlogFilename, long binlogPosition) {
        this.serverId = serverId;
        this.binlogFilename = binlogFilename;
        this.binlogPosition = binlogPosition;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.writeInteger(CommandType.BINLOG_DUMP.ordinal(), 1);
        buffer.writeLong(this.binlogPosition, 4);
        buffer.writeInteger(0, 2); // flag
        buffer.writeLong(this.serverId, 4);
        buffer.writeString(this.binlogFilename);
        return buffer.toByteArray();
    }

}
