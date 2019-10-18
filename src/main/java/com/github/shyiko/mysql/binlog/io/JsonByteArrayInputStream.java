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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * this byte array input stream is easy to parse json binary.
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class JsonByteArrayInputStream extends ByteArrayInputStream {

    public JsonByteArrayInputStream(byte[] bytes) {
        super(bytes);
    }

    /**
     * Read fixed length string.
     */
    public String readString(int length) throws IOException {
        return new String(read(length));
    }


    public byte[] read(int length) throws IOException {
        byte[] bytes = new byte[length];
        fill(bytes, 0, length);
        return bytes;
    }

    public void fill(byte[] bytes, int offset, int length) throws IOException {
        int remaining = length;
        while (remaining != 0) {
            int read = read(bytes, offset + length - remaining, remaining);
            if (read == -1) {
                throw new EOFException();
            }
            remaining -= read;
        }
    }

    /**
     * return current cursor position
     * @return current cursor
     */
    public int getPosition() {
        return super.pos;
    }
}
