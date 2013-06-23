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
package com.github.shyiko.mysql.binlog.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class ByteArrayOutputStream extends OutputStream {

    private OutputStream outputStream;

    public ByteArrayOutputStream() {
        this(new java.io.ByteArrayOutputStream());
    }

    public ByteArrayOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * Write int in little-endian format.
     */
    public void writeInteger(int value, int length) throws IOException {
        for (int i = 0; i < length; i++) {
            write(0x000000FF & (value >>> (i << 3)));
        }
    }

    /**
     * Write long in little-endian format.
     */
    public void writeLong(long value, int length) throws IOException {
        for (int i = 0; i < length; i++) {
            write((int) (0x00000000000000FF & (value >>> (i << 3))));
        }
    }

    public void writeString(String value) throws IOException {
        write(value.getBytes());
    }

    /**
     * @see ByteArrayInputStream#readLengthEncodedString()
     */
    public void writeLengthEncodedString(String value) throws IOException {
        writePackedLong(value.getBytes().length);
        writeString(value);
    }

    /**
     * @see ByteArrayInputStream#readZeroTerminatedString()
     */
    public void writeZeroTerminatedString(String value) throws IOException {
        write(value.getBytes());
        write(0);
    }

    /**
     * @see ByteArrayInputStream#readPackedNumber()
     */
    public void writePackedLong(long value) throws IOException {
        if (value < 251L) {
            writeLong(value, 1);
        } else if (value < 65536L) {
            writeLong(252, 1);
            writeLong(value, 2);
        } else if (value < 16777216L) {
            writeLong(253, 1);
            writeLong(value, 3);
        } else {
            writeLong(254, 1);
            writeLong(value, 8);
        }
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write(b);
    }

    public byte[] toByteArray() {
        // todo: whole approach feels wrong
        if (outputStream instanceof java.io.ByteArrayOutputStream) {
            return ((java.io.ByteArrayOutputStream) outputStream).toByteArray();
        }
        return new byte[0];
    }
}

