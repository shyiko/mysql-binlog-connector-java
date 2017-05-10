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

import com.github.shyiko.mysql.binlog.event.XAPrepareEventData;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="https://github.com/stevenczp">Steven Cheng</a>
 */
public class XAPrepareEventDataDeserializerTest {
    private static final byte[] DATA =
        {0, 123, 0, 0, 0, 5, 0, 0, 0, 5, 0, 0, 0, 103, 116, 114, 105, 100, 98, 113, 117, 97, 108};

    private static final boolean ONEPHASE = false;
    private static final int FORMATID = 123;
    private static final String GTRID = "gtrid";
    private static final String BQUAL = "bqual";

    @Test
    public void deserialize() throws IOException {
        XAPrepareEventDataDeserializer deserializer = new XAPrepareEventDataDeserializer();
        XAPrepareEventData xaPrepareEventData =
            deserializer.deserialize(new ByteArrayInputStream(DATA));

        assertEquals(ONEPHASE, xaPrepareEventData.isOnePhase());
        assertEquals(FORMATID, xaPrepareEventData.getFormatID());
        assertEquals(GTRID, xaPrepareEventData.getGtrid());
        assertEquals(BQUAL, xaPrepareEventData.getBqual());
    }
}
