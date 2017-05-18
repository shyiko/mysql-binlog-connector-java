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
package com.github.shyiko.mysql.binlog.event.deserialization.json;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.BinaryLogClientIntegrationTest;
import com.github.shyiko.mysql.binlog.CapturingEventListener;
import com.github.shyiko.mysql.binlog.CountDownEventListener;
import com.github.shyiko.mysql.binlog.TraceEventListener;
import com.github.shyiko.mysql.binlog.TraceLifecycleListener;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventData;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.QueryEventData;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author <a href="mailto:rhauch@gmail.com">Randall Hauch</a>
 */
public class JsonBinaryValueIntegrationTest {

    private static final long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(3);

    private final Logger logger = Logger.getLogger(getClass().getSimpleName());

    {
        logger.setLevel(Level.FINEST);
    }

    private final TimeZone timeZoneBeforeTheTest = TimeZone.getDefault();

    private BinaryLogClientIntegrationTest.MySQLConnection master;
    private BinaryLogClient client;
    private CountDownEventListener eventListener;

    @BeforeClass
    public void setUp() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        ResourceBundle bundle = ResourceBundle.getBundle("jdbc");
        String prefix = "jdbc.mysql.replication.";
        master = new BinaryLogClientIntegrationTest.MySQLConnection(bundle.getString(prefix + "master.hostname"),
                Integer.parseInt(bundle.getString(prefix + "master.port")),
                bundle.getString(prefix + "master.username"), bundle.getString(prefix + "master.password"));
        client = new BinaryLogClient(master.hostname(), master.port(), master.username(), master.password());
        client.setServerId(client.getServerId() - 1); // avoid clashes between BinaryLogClient instances
        client.setKeepAlive(false);
        client.registerEventListener(new TraceEventListener());
        client.registerEventListener(eventListener = new CountDownEventListener());
        client.registerLifecycleListener(new TraceLifecycleListener());
        client.connect(DEFAULT_TIMEOUT);
        master.execute(new BinaryLogClientIntegrationTest.Callback<Statement>() {
            @Override
            public void execute(Statement statement) throws SQLException {
                statement.execute("drop database if exists json_test");
                statement.execute("create database json_test");
                statement.execute("use json_test");
            }
        });
        try {
            master.execute(new BinaryLogClientIntegrationTest.Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("create table json_support_validation (i INT, j JSON)");
                }
            });
        } catch (SQLSyntaxErrorException e) {
            // Skip the tests altogether since MySQL is pre 5.7
            throw new org.testng.SkipException("JSON data type is not supported by current version of MySQL");
        }
        eventListener.waitFor(EventType.QUERY, 3, DEFAULT_TIMEOUT);
        eventListener.reset();
    }

    @Test
    public void testValueBoundariesAreHonored() throws Exception {
        CountDownEventListener eventListener = new CountDownEventListener();
        client.registerEventListener(eventListener);
        CapturingEventListener capturingEventListener = new CapturingEventListener();
        client.registerEventListener(capturingEventListener);
        master.execute("create table json_b (h varchar(255), j JSON, k varchar(255))",
            "INSERT INTO json_b VALUES ('sponge', '{}', 'bob');");
        eventListener.waitFor(WriteRowsEventData.class, 1, DEFAULT_TIMEOUT);
        List<WriteRowsEventData> events = capturingEventListener.getEvents(WriteRowsEventData.class);
        Serializable[] data = events.iterator().next().getRows().get(0);
        assertEquals(data[0], "sponge");
        assertEquals(JsonBinary.parseAsString((byte[]) data[1]), "{}");
        assertEquals(data[2], "bob");
    }

    @Test
    public void testNull() throws Exception {
        assertEquals(writeAndCaptureJSON(null), null);
    }

    @Test
    public void testUnicodeSupport() throws Exception {
        assertJSONMatchOriginal("{\"key\":\"éééàààà\"}");
    }

    @Test
    public void testJsonObject() throws Exception {
        assertJSONMatchOriginal(
            "{" +
            "\"k.1\":1," +
            "\"k.0\":0," +
            "\"k.-1\":-1," +
            "\"k.true\":true," +
            "\"k.false\":false," +
            "\"k.null\":null," +
            "\"k.string\":\"string\"," +
            "\"k.true_false\":[true,false]," +
            "\"k.32767\":32767," +
            "\"k.32768\":32768," +
            "\"k.-32768\":-32768," +
            "\"k.-32769\":-32769," +
            "\"k.2147483647\":2147483647," +
            "\"k.2147483648\":2147483648," +
            "\"k.-2147483648\":-2147483648," +
            "\"k.-2147483649\":-2147483649," +
            "\"k.18446744073709551615\":18446744073709551615," +
            "\"k.18446744073709551616\":18446744073709551616," +
            "\"k.3.14\":3.14," +
            "\"k.{}\":{}," +
            "\"k.[]\":[]" +
            "}"
        );
    }

    @Test
    public void testJsonObjectLargerThan64k() throws Exception {
        // https://dev.mysql.com/worklog/task/?id=8132

        // To compensate for the extra space needed by the redundant length
        // information, we will make the format allow offset and length fields to
        // come in two different variants: 2 bytes for documents smaller than
        // 64KB, and 4 bytes to support larger documents.

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 64 * 1024; i++) {
            sb.append("\"g.").append(i).append("\":").append(i).append(',');
        }
        assertJSONMatchOriginal(
            "{" +
                sb +
                "\"k.1\":1," +
                "\"k.0\":0," +
                "\"k.-1\":-1," +
                "\"k.true\":true," +
                "\"k.false\":false," +
                "\"k.null\":null," +
                "\"k.string\":\"string\"," +
                "\"k.true_false\":[true,false]," +
                "\"k.32767\":32767," +
                "\"k.32768\":32768," +
                "\"k.-32768\":-32768," +
                "\"k.-32769\":-32769," +
                "\"k.2147483647\":2147483647," +
                "\"k.2147483648\":2147483648," +
                "\"k.-2147483648\":-2147483648," +
                "\"k.-2147483649\":-2147483649," +
                "\"k.18446744073709551615\":18446744073709551615," +
                "\"k.18446744073709551616\":18446744073709551616," +
                "\"k.3.14\":3.14," +
                "\"k.{}\":{}," +
                "\"k.[]\":[]" +
                "}"
        );
    }

    @Test
    public void testJsonObjectNested() throws Exception {
        assertJSONMatchOriginal("{\"a\":{\"b\":{\"c\":\"d\",\"e\":[\"f\",\"g\"]}}}");
    }

    @Test
    public void testJsonWithEmptyKey() throws Exception {
        assertJSONMatchOriginal("{\"bitrate\":{\"\":0}}");
    }

    @Test
    public void testJsonArray() throws Exception {
        assertJSONMatchOriginal(
            "[" +
            "-1," +
            "0," +
            "1," +
            "true," +
            "false," +
            "null," +
            "\"string\"," +
            "[true,false]," +
            "32767," +
            "32768," +
            "-32768," +
            "-32769," +
            "2147483647," +
            "2147483648," +
            "-2147483648," +
            "-2147483649," +
            "18446744073709551615," +
            "18446744073709551616," +
            "3.14," +
            "{}," +
            "[]" +
            "]");
    }

    @Test
    public void testJsonArrayNested() throws Exception {
        assertJSONMatchOriginal("[-1,[\"b\",[\"c\"]],1]");
    }

    @Test
    public void testScalarString() throws Exception {
        assertJSONMatchOriginal("\"scalar string\"");
        assertJSONMatchOriginal("\"" + new String(new char[65]).replace("\0", "LONG") + "\"");
    }

    @Test
    public void testScalarBooleanTrue() throws Exception {
        assertJSONMatchOriginal("true");
    }

    @Test
    public void testScalarBooleanFalse() throws Exception {
        assertJSONMatchOriginal("false");
    }

    @Test
    public void testScalarNull() throws Exception {
        assertJSONMatchOriginal("null");
    }

    @Test
    public void testScalarNegativeInteger() throws Exception {
        assertJSONMatchOriginal("-1");
    }

    @Test
    public void testScalarPositiveInteger() throws Exception {
        assertJSONMatchOriginal("1");
    }

    @Test
    public void testScalarMaxPositiveInt16() throws Exception {
        assertJSONMatchOriginal("32767");
    }

    @Test
    public void testScalarInt32() throws Exception {
        assertJSONMatchOriginal("32768");
    }

    @Test
    public void testScalarMinNegativeInt16() throws Exception {
        assertJSONMatchOriginal("-32768");
    }

    @Test
    public void testScalarNegativeInt32() throws Exception {
        assertJSONMatchOriginal("-32769");
    }

    @Test
    public void testScalarMaxPositiveInt32() throws Exception {
        assertJSONMatchOriginal("2147483647");
    }

    @Test
    public void testScalarPositiveInt64() throws Exception {
        assertJSONMatchOriginal("2147483648");
    }

    @Test
    public void testScalarMinNegativeInt32() throws Exception {
        assertJSONMatchOriginal("-2147483648");
    }

    @Test
    public void testScalarNegativeInt64() throws Exception {
        assertJSONMatchOriginal("-2147483649");
    }

    @Test
    public void testScalarUInt64() throws Exception {
        assertJSONMatchOriginal("18446744073709551615");
    }

    @Test
    public void testScalarUInt64Overflow() throws Exception {
        assertJSONMatchOriginal("18446744073709551616");
    }

    @Test
    public void testScalarFloat() throws Exception {
        assertJSONMatchOriginal("3.14");
    }

    @Test
    public void testEmptyObject() throws Exception {
        assertJSONMatchOriginal("{}");
    }

    @Test
    public void testEmptyArray() throws Exception {
        assertJSONMatchOriginal("[]");
    }

    @Test
    public void testScalarDateTime() throws Exception {
        assertEquals(writeAndCaptureJSON("CAST(CAST('2015-01-15 23:24:25' AS DATETIME) AS JSON)"),
            "\"2015-01-15 23:24:25\"");
    }

    @Test
    public void testScalarTime() throws Exception {
        assertEquals(writeAndCaptureJSON("CAST(CAST('23:24:25' AS TIME) AS JSON)"),
            "\"23:24:25\"");
        assertEquals(writeAndCaptureJSON("CAST(CAST('23:24:25.12' AS TIME(3)) AS JSON)"),
            "\"23:24:25.12\"");
        assertEquals(writeAndCaptureJSON("CAST(CAST('23:24:25.0237' AS TIME(3)) AS JSON)"),
            "\"23:24:25.024\"");
    }

    @Test
    public void testScalarDate() throws Exception {
        assertEquals(writeAndCaptureJSON("CAST(CAST('2015-01-15' AS DATE) AS JSON)"),
            "\"2015-01-15\"");
    }

    @Test
    public void testScalarTimestamp() throws Exception {
        // timestamp literals are interpreted by MySQL as DATETIME values
        assertEquals(writeAndCaptureJSON("CAST(TIMESTAMP'2015-01-15 23:24:25' AS JSON)"),
            "\"2015-01-15 23:24:25\"");
        assertEquals(writeAndCaptureJSON("CAST(TIMESTAMP'2015-01-15 23:24:25.12' AS JSON)"),
            "\"2015-01-15 23:24:25.12\"");
        assertEquals(writeAndCaptureJSON("CAST(TIMESTAMP'2015-01-15 23:24:25.0237' AS JSON)"),
            "\"2015-01-15 23:24:25.0237\"");
        // UNIX_TIMESTAMP(ts) function returns the number of seconds past epoch for the given ts
        assertEquals(writeAndCaptureJSON("CAST(UNIX_TIMESTAMP('2015-01-15 23:24:25') AS JSON)"),
            "1421364265");
    }

    @Test
    public void testScalarGeometry() throws Exception {
        assertEquals(writeAndCaptureJSON("CAST(ST_GeomFromText('POINT(1 1)') AS JSON)"),
            "{\"type\":\"Point\",\"coordinates\":[1.0,1.0]}");
    }

    @Test
    public void testScalarStringWithCharsetConversion() throws Exception {
        assertEquals(writeAndCaptureJSON("CAST('[]' AS CHAR CHARACTER SET 'ascii')"), "[]");
    }

    @Test
    public void testScalarBinaryAsBase64() throws Exception {
        assertEquals(writeAndCaptureJSON("CAST(x'cafe' AS JSON)"), "\"yv4=\"");
        assertEquals(writeAndCaptureJSON("CAST(x'cafebabe' AS JSON)"), "\"yv66vg==\"");
    }

    private void assertJSONMatchOriginal(String value) throws Exception {
        assetJSONEquals(value, writeAndCaptureJSON("'" + value + "'"));
    }

    private void assetJSONEquals(String actual, String expected) throws Exception {
        if (expected != null && (expected.startsWith("{") || expected.startsWith("["))) {
            JSONAssert.assertEquals(expected, actual, true);
        } else {
            assertEquals(actual, expected);
        }
    }

    private String writeAndCaptureJSON(final String value) throws Exception {
        CapturingEventListener capturingEventListener = new CapturingEventListener();
        client.registerEventListener(capturingEventListener);
        try {
            master.execute(new BinaryLogClientIntegrationTest.Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("drop table if exists data_type_hell");
                    statement.execute("create table data_type_hell (column_ " + "JSON" + ")");
                    statement.execute("insert into data_type_hell values (" + value + ")");
                }
            });
            eventListener.waitFor(WriteRowsEventData.class, 1, DEFAULT_TIMEOUT);
        } finally {
            client.unregisterEventListener(capturingEventListener);
        }
        byte[] b = (byte[]) capturingEventListener.getEvents(WriteRowsEventData.class).get(0).getRows().get(0)[0];
        return b == null ? null : JsonBinary.parseAsString(b);
    }

    @AfterMethod
    public void afterEachTest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final String markerQuery = "drop table if exists _EOS_marker";
        BinaryLogClient.EventListener markerInterceptor = new BinaryLogClient.EventListener() {
            @Override
            public void onEvent(Event event) {
                if (event.getHeader().getEventType() == EventType.QUERY) {
                    EventData data = event.getData();
                    if (data != null && ((QueryEventData) data).getSql().contains("_EOS_marker")) {
                        latch.countDown();
                    }
                }
            }
        };
        client.registerEventListener(markerInterceptor);
        master.execute(new BinaryLogClientIntegrationTest.Callback<Statement>() {
            @Override
            public void execute(Statement statement) throws SQLException {
                statement.execute(markerQuery);
            }
        });
        assertTrue(latch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS));
        client.unregisterEventListener(markerInterceptor);
        eventListener.reset();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        TimeZone.setDefault(timeZoneBeforeTheTest);
        try {
            if (client != null) {
                client.disconnect();
            }
        } finally {
            if (master != null) {
                master.execute(new BinaryLogClientIntegrationTest.Callback<Statement>() {
                    @Override
                    public void execute(Statement statement) throws SQLException {
                        statement.execute("drop database json_test");
                    }
                });
                master.close();
            }
        }
    }

}
