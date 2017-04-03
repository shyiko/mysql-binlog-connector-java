/*
 * Copyright 2017 Juan Olivares
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

import static junit.framework.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.github.shyiko.mysql.binlog.event.PreviousGtidSetEventData;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;

/**
 * @author <a href="https://github.com/jolivares">Juan Olivares</a>
 */
public class PreviousGtidSetDeserializerTest {

    private static final byte[] DATA = {2, 0, 0, 0, 0, 0, 0, 0, -75, -51, 22,
            36, 95, 48, 17, -28, -76, -23, 16, 81, 114, 27, -46, 65, 1, 0, 0, 0,
            0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, -15, 15, 108, 0, 0, 0, 0, 0,
            -69, 66, 29, 38, 95, 48, 17, -28, -76, -23, -40, -99, 103, 43, 46,
            -8, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, -47, 97, 119, 0,
            0, 0, 0, 0};

    private static final String GTID_SET = "b5cd1624-5f30-11e4-b4e9-1051721bd241:1-7081968," +
            "bb421d26-5f30-11e4-b4e9-d89d672b2ef8:1-7823824";

    @Test
    public void deserialize() throws IOException {
        PreviousGtidSetDeserializer deserializer = new PreviousGtidSetDeserializer();
        PreviousGtidSetEventData previousGtidSetData = deserializer
                .deserialize(new ByteArrayInputStream(DATA));

        assertEquals(GTID_SET, previousGtidSetData.getGtidSet());
    }

}
