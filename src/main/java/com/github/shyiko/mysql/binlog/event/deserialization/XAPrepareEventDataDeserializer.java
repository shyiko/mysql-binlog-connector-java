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

import java.io.IOException;

/**
 * https://github.com/mysql/mysql-server/blob/5.7/libbinlogevents/src/control_events.cpp#L590
 * <p>
 * one_phase : boolean, 1byte
 * formatID : int, 4byte
 * gtrid_length : int, 4byte
 * bqual_length : int, 4byte
 * data : String, gtrid + bqual, (gtrid_length + bqual_length)byte
 * <p>
 * Created by cc on 2017/5/10.
 */
public class XAPrepareEventDataDeserializer implements EventDataDeserializer<XAPrepareEventData> {
    @Override
    public XAPrepareEventData deserialize(ByteArrayInputStream inputStream) throws IOException {
        XAPrepareEventData xaPrepareEventData = new XAPrepareEventData();
        xaPrepareEventData.setOne_phase(inputStream.read() == 0x00 ? false : true);
        xaPrepareEventData.setFormatID(inputStream.readInteger(4));
        xaPrepareEventData.setGtrid_length(inputStream.readInteger(4));
        xaPrepareEventData.setBqual_length(inputStream.readInteger(4));
        xaPrepareEventData.setData(inputStream.read(
            xaPrepareEventData.getGtrid_length() + xaPrepareEventData.getBqual_length()));

        return xaPrepareEventData;
    }
}
