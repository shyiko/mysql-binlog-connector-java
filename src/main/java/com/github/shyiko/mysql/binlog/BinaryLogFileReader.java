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
package com.github.shyiko.mysql.binlog;

import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * MySQL binary log file reader.
 *
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class BinaryLogFileReader implements Closeable {

    public static final byte[] MAGIC_HEADER = new byte[]{(byte) 0xfe, (byte) 0x62, (byte) 0x69, (byte) 0x6e};

    private final ByteArrayInputStream inputStream;
    private final EventDeserializer eventDeserializer;

    public BinaryLogFileReader(File file) throws IOException {
        this(file, new EventDeserializer());
    }

    public BinaryLogFileReader(File file, EventDeserializer eventDeserializer) throws IOException {
        this(file != null ? new BufferedInputStream(new FileInputStream(file)) : null, eventDeserializer);
    }

    public BinaryLogFileReader(InputStream inputStream) throws IOException {
        this(inputStream, new EventDeserializer());
    }

    public BinaryLogFileReader(InputStream inputStream, EventDeserializer eventDeserializer) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be NULL");
        }
        if (eventDeserializer == null) {
            throw new IllegalArgumentException("Event deserializer cannot be NULL");
        }
        this.inputStream = new ByteArrayInputStream(inputStream);
        try {
            byte[] magicHeader = this.inputStream.read(MAGIC_HEADER.length);
            if (!Arrays.equals(magicHeader, MAGIC_HEADER)) {
                throw new IOException("Not a valid binary log");
            }
        } catch (IOException e) {
            try {
                this.inputStream.close();
            } catch (IOException ex) {
                // ignore
            }
            throw e;
        }
        this.eventDeserializer = eventDeserializer;
    }

    /**
     * @return deserialized event or null in case of end-of-stream
     */
    public Event readEvent() throws IOException {
        return eventDeserializer.nextEvent(inputStream);
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

}
