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

import java.util.BitSet;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public final class ByteArrays {

    public static BitSet toBitSet(byte[] bytes) {
        BitSet result = new BitSet();
        for (int i = 0, end = bytes.length * 8; i < end; i++) {
            if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
                result.set(i);
            }
        }
        return result;
    }

    public static byte[] reverse(byte[] bytes) {
        for (int i = 0, length = bytes.length >> 2; i <= length; i++) {
            int j = bytes.length - 1 - i;
            byte t = bytes[i];
            bytes[i] = bytes[j];
            bytes[j] = t;
        }
        return bytes;
    }

    public static int toInteger(byte[] bytes) {
        return toInteger(bytes, 0, bytes.length);
    }

    public static int toInteger(byte[] bytes, int offset, int length) {
        int result = 0;
        for (int i = offset; i < (offset + length); i++) {
            byte b = bytes[i];
            result = (result << 8) | (b >= 0 ? (int) b : (b + 256));
        }
        return result;
    }

    private ByteArrays() {
    }
}
