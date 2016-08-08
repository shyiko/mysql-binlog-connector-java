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

import com.github.shyiko.mysql.binlog.event.deserialization.AbstractRowsEventDataDeserializer.UnixTime;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class AbstractRowsEventDataDeserializerTest {

    @Test
    public void testFrom() throws Exception {
        assetTimeEquals(UnixTime.from(2260, 8, 19, 0, 0, 0, 0),
            timestamp(2260, 8, 19, 0, 0, 0, 0));
        assetTimeEquals(UnixTime.from(2016, 8, 19, 0, 0, 0, 0),
            timestamp(2016, 8, 19, 0, 0, 0, 0));
        assetTimeEquals(UnixTime.from(1970, 1, 1, 0, 0, 0, 0), 0);
        assetTimeEquals(UnixTime.from(1969, 1, 1, 0, 0, 0, 0),
            -365L * 24 * 60 * 60 * 1000);
        assetTimeEquals(UnixTime.from(1582, 10, 15, 0, 0, 0, 0),
            timestamp(1582, 10, 15, 0, 0, 0, 0));
        assetTimeEquals(UnixTime.from(1582, 10, 14, 0, 0, 0, 0),
            timestamp(1582, 10, 14, 0, 0, 0, 0));
        assetTimeEquals(UnixTime.from(1, 1, 1, 0, 0, 0, 0),
            timestamp(1, 1, 1, 0, 0, 0, 0));
    }

    private void assetTimeEquals(long actual, long expected) {
        assertEquals(actual, expected, actual + " != " + expected +
            ", discrepancy: " + (actual - expected));
    }

    // checkstyle, please ignore ParameterNumber for the next line
    private long timestamp(int year, int month, int dayOfMonth, int hourOfDay,
            int minute, int second, int millis) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        c.set(Calendar.MILLISECOND, millis);
        return c.getTimeInMillis();
    }

}
