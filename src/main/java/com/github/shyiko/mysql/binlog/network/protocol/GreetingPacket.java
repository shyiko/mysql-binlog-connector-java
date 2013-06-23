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
package com.github.shyiko.mysql.binlog.network.protocol;

import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;

import java.io.IOException;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class GreetingPacket implements Packet {

    private int protocolVersion;
    private String serverVersion;
    private long threadId;
    private String scramble;
    private int serverCapabilities;
    private int serverCollation;
    private int serverStatus;
    private String pluginProvidedData;

    public GreetingPacket(byte[] bytes) throws IOException {
        ByteArrayInputStream buffer = new ByteArrayInputStream(bytes);
        this.protocolVersion = buffer.readInteger(1);
        this.serverVersion = buffer.readZeroTerminatedString();
        this.threadId = buffer.readLong(4);
        String scramblePrefix = buffer.readZeroTerminatedString();
        this.serverCapabilities = buffer.readInteger(2);
        this.serverCollation = buffer.readInteger(1);
        this.serverStatus = buffer.readInteger(2);
        buffer.skip(13); // reserved
        this.scramble = scramblePrefix + buffer.readZeroTerminatedString();
        if (buffer.available() > 0) {
            this.pluginProvidedData = buffer.readZeroTerminatedString();
        }
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public String getScramble() {
        return scramble;
    }

    public long getThreadId() {
        return threadId;
    }

    public int getServerStatus() {
        return serverStatus;
    }

    public int getServerCapabilities() {
        return serverCapabilities;
    }

    public String getPluginProvidedData() {
        return pluginProvidedData;
    }

    public int getServerCollation() {
        return serverCollation;
    }

}
