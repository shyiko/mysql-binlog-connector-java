/*
 * Copyright 2013 Patrick Prasse
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

import com.github.shyiko.mysql.binlog.event.GtidEventData;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author <a href="mailto:pprasse@actindo.de">Patrick Prasse</a>
 */
public class GtidEventDataDeserializer implements EventDataDeserializer<GtidEventData> {

    @Override
    public GtidEventData deserialize(ByteArrayInputStream inputStream) throws IOException {
        GtidEventData eventData = new GtidEventData();
        byte flags = (byte) inputStream.readInteger(1);
        byte[] sid = inputStream.read(16);
        long gno = inputStream.readLong(8);
        eventData.setFlags(flags);
        eventData.setGtid(byteArrayToHex(sid, 0, 4) + "-" +
            byteArrayToHex(sid, 4, 2) + "-" +
            byteArrayToHex(sid, 6, 2) + "-" +
            byteArrayToHex(sid, 8, 2) + "-" +
            byteArrayToHex(sid, 10, 6) + ":" +
            String.format("%d", gno)
        );
        return eventData;
    }

    private String byteArrayToHex(byte[] a, int offset, int len) {
        StringBuilder sb = new StringBuilder();
        for (int idx = offset; idx < (offset + len) && idx < a.length; idx++) {
            sb.append(String.format("%02x", a[idx] & 0xff));
        }
        return sb.toString();
    }

}
