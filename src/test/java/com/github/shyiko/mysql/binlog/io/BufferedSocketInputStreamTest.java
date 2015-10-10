/*
 * Copyright 2015 Stanley Shyiko
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

import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class BufferedSocketInputStreamTest {

    @Test
    public void testCorrectness() throws Exception {
        BufferedSocketInputStream in = new BufferedSocketInputStream(new ByteArrayInputStream(new byte[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}), 5);
        assertEquals(in.read(), 0);
        assertEquals(in.read(), 1);
        byte[] buf = new byte[6];
        assertEquals(in.read(buf, 0, buf.length), 3); // data remaining in BSIS buffer
        assertEquals(Arrays.copyOf(buf, 3), new byte[] {2, 3, 4});
        assertEquals(in.read(buf, 0, buf.length), 6);
        assertEquals(buf, new byte[] {5, 6, 7, 8, 9, 10});
        assertEquals(in.read(buf, 0, 3), 3);
        assertEquals(Arrays.copyOf(buf, 3), new byte[] {11, 12, 13});
        assertEquals(in.read(buf, 0, 3), 2); // data remaining in BSIS buffer
        assertEquals(Arrays.copyOf(buf, 2), new byte[] {14, 15});
        assertEquals(in.read(), 16);
        assertEquals(in.read(), (byte) -1);
    }
}
