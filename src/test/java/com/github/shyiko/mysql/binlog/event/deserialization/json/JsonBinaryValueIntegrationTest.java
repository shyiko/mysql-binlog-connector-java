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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.Serializable;
import java.sql.SQLSyntaxErrorException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.github.shyiko.mysql.binlog.BinaryLogClientIntegrationTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.CapturingEventListener;
import com.github.shyiko.mysql.binlog.CountDownEventListener;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;

/**
 * @author <a href="mailto:rhauch@gmail.com">Randall Hauch</a>
 */
public class JsonBinaryValueIntegrationTest {

    private static final long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(3);

    private BinaryLogClientIntegrationTest.MySQLConnection master;
    private Map<Integer, byte[]> jsonValuesByKey;

    @BeforeClass
    public void setUp() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        ResourceBundle bundle = ResourceBundle.getBundle("jdbc");
        String prefix = "jdbc.mysql.replication.";
        master = new BinaryLogClientIntegrationTest.MySQLConnection(bundle.getString(prefix + "master.hostname"),
                Integer.parseInt(bundle.getString(prefix + "master.port")),
                bundle.getString(prefix + "master.username"), bundle.getString(prefix + "master.password"));
        BinaryLogClient client = new BinaryLogClient(master.hostname(), master.port(), master.username(),
            master.password());
        client.setServerId(client.getServerId() - 1); // avoid clashes between BinaryLogClient instances
        client.setKeepAlive(false);
        // Uncomment the next line for detailed traces of the events ...
        // client.registerEventListener(new TraceEventListener());
        CountDownEventListener eventListener;
        client.registerEventListener(eventListener = new CountDownEventListener());
        // client.registerLifecycleListener(new TraceLifecycleListener());
        client.connect(DEFAULT_TIMEOUT);
        try {
            master.execute("drop database if exists json_test",
                           "create database json_test",
                           "use json_test",
                           "create table t1 (i INT, j JSON)");
            eventListener.waitFor(EventType.QUERY, 3, DEFAULT_TIMEOUT);
            eventListener.reset();
        } catch (SQLSyntaxErrorException e) {
            // Skip the tests altogether since MySQL is pre 5.7
            throw new org.testng.SkipException("JSON data type is not supported by current version of MySQL");
        }

        // Insert values into the t1 table ...
        CapturingEventListener capturingEventListener = new CapturingEventListener();
        client.registerEventListener(capturingEventListener);
        master.execute("INSERT INTO t1 VALUES (0, NULL);",
                       "INSERT INTO t1 VALUES (1, '{\"a\": 2}');",
                       "INSERT INTO t1 VALUES (2, '[1,2]');",
                       // checkstyle, please ignore LineLength for the next line
                       "INSERT INTO t1 VALUES (3, '{\"a\":\"b\", \"c\":\"d\",\"ab\":\"abc\", \"bc\": [\"x\", \"y\"]}');",
                       "INSERT INTO t1 VALUES (4, '[\"here\", [\"I\", \"am\"], \"!!!\"]');",
                       "INSERT INTO t1 VALUES (5, '\"scalar string\"');",
                       "INSERT INTO t1 VALUES (6, 'true');",
                       "INSERT INTO t1 VALUES (7, 'false');",
                       "INSERT INTO t1 VALUES (8, 'null');",
                       "INSERT INTO t1 VALUES (9, '-1');",
                       "INSERT INTO t1 VALUES (10, CAST(CAST(1 AS UNSIGNED) AS JSON));",
                       "INSERT INTO t1 VALUES (11, '32767');",
                       "INSERT INTO t1 VALUES (12, '32768');",
                       "INSERT INTO t1 VALUES (13, '-32768');",
                       "INSERT INTO t1 VALUES (14, '-32769');",
                       "INSERT INTO t1 VALUES (15, '2147483647');",
                       "INSERT INTO t1 VALUES (16, '2147483648');",
                       "INSERT INTO t1 VALUES (17, '-2147483648');",
                       "INSERT INTO t1 VALUES (18, '-2147483649');",
                       "INSERT INTO t1 VALUES (19, '18446744073709551615');",
                       "INSERT INTO t1 VALUES (20, '18446744073709551616');",
                       "INSERT INTO t1 VALUES (21, '3.14');",
                       "INSERT INTO t1 VALUES (22, '{}');",
                       "INSERT INTO t1 VALUES (23, '[]');",
                       "INSERT INTO t1 VALUES (24, CAST(CAST('2015-01-15 23:24:25' AS DATETIME) AS JSON));",
                       "INSERT INTO t1 VALUES (25, CAST(CAST('23:24:25' AS TIME) AS JSON));",
                       "INSERT INTO t1 VALUES (125, CAST(CAST('23:24:25.12' AS TIME(3)) AS JSON));",
                       "INSERT INTO t1 VALUES (225, CAST(CAST('23:24:25.0237' AS TIME(3)) AS JSON));",
                       "INSERT INTO t1 VALUES (26, CAST(CAST('2015-01-15' AS DATE) AS JSON));",
                       "INSERT INTO t1 VALUES (27, CAST(TIMESTAMP'2015-01-15 23:24:25' AS JSON));",
                       "INSERT INTO t1 VALUES (127, CAST(TIMESTAMP'2015-01-15 23:24:25.12' AS JSON));",
                       "INSERT INTO t1 VALUES (227, CAST(TIMESTAMP'2015-01-15 23:24:25.0237' AS JSON));",
                       "INSERT INTO t1 VALUES (327, CAST(UNIX_TIMESTAMP('2015-01-15 23:24:25') AS JSON));",
                       "INSERT INTO t1 VALUES (28, CAST(ST_GeomFromText('POINT(1 1)') AS JSON));",
                       // auto-convert to utf8mb4
                       "INSERT INTO t1 VALUES (29, CAST('[]' AS CHAR CHARACTER SET 'ascii'));",
                       "INSERT INTO t1 VALUES (30, CAST(x'cafe' AS JSON));",
                       "INSERT INTO t1 VALUES (31, CAST(x'cafebabe' AS JSON));",
                       // # Maximum allowed key length is 64k-1
                       "INSERT INTO t1 VALUES (100, CONCAT('{\"', REPEAT('a', 64 * 1024 - 1), '\":123}'));");

        // Wait for the inserts to appear ...
        eventListener.waitFor(WriteRowsEventData.class, 37, DEFAULT_TIMEOUT);

        jsonValuesByKey = new HashMap<Integer, byte[]>();
        List<WriteRowsEventData> events = capturingEventListener.getEvents(WriteRowsEventData.class);
        for (WriteRowsEventData event : events) {
            List<Serializable[]> writtenRows = event.getRows();
            for (Serializable[] row : writtenRows) {
                assertEquals(row.length, 2);
                // Read the values ...
                Integer rowNum = (Integer) row[0];
                byte[] jsonBinary = (byte[]) row[1];
                assertNotNull(rowNum);
                jsonValuesByKey.put(rowNum, jsonBinary);
            }
        }
    }

    @Test
    public void testNullJsonValue() throws Exception {
        assertJson(0, null);
    }

    @Test
    public void testSimpleJsonObject() throws Exception {
        assertJson(1, "{\"a\":2}");
    }

    @Test
    public void testMultiLevelJsonObject() throws Exception {
        assertJson(3, "{\"a\":\"b\",\"c\":\"d\",\"ab\":\"abc\",\"bc\":[\"x\",\"y\"]}");
    }

    @Test
    public void testSimpleJsonArray() throws Exception {
        assertJson(2, "[1,2]");
    }

    @Test
    public void testMultiLevelJsonArray() throws Exception {
        assertJson(4, "[\"here\",[\"I\",\"am\"],\"!!!\"]");
    }

    @Test
    public void testScalarString() throws Exception {
        assertJson(5, "\"scalar string\"");
    }

    @Test
    public void testScalarBooleanTrue() throws Exception {
        assertJson(6, "true");
    }

    @Test
    public void testScalarBooleanFalse() throws Exception {
        assertJson(7, "false");
    }

    @Test
    public void testScalarNull() throws Exception {
        assertJson(8, "null");
    }

    @Test
    public void testScalarNegativeInteger() throws Exception {
        assertJson(9, "-1");
    }

    @Test
    public void testScalarUnsignedInteger() throws Exception {
        assertJson(10, "1");
    }

    @Test
    public void testScalarMaxPositiveInt16() throws Exception {
        assertJson(11, "32767");
    }

    @Test
    public void testScalarInt32() throws Exception {
        assertJson(12, "32768");
    }

    @Test
    public void testScalarNegativeInt16() throws Exception {
        assertJson(13, "-32768");
    }

    @Test
    public void testScalarNegativeInt32() throws Exception {
        assertJson(14, "-32769");
    }

    @Test
    public void testScalarMaxPositiveInt32() throws Exception {
        assertJson(15, "2147483647");
    }

    @Test
    public void testScalarPositiveInt64() throws Exception {
        assertJson(16, "2147483648");
    }

    @Test
    public void testScalarMaxNegativeInt32() throws Exception {
        assertJson(17, "-2147483648");
    }

    @Test
    public void testScalarNegativeInt64() throws Exception {
        assertJson(18, "-2147483649");
    }

    @Test
    public void testScalarUInt64() throws Exception {
        assertJson(19, "18446744073709551615");
    }

    @Test
    public void testScalarBeyondUInt64() throws Exception {
        assertJson(20, "18446744073709551616");
    }

    @Test
    public void testScalarPi() throws Exception {
        assertJson(21, "3.14");
    }

    @Test
    public void testEmptyObject() throws Exception {
        assertJson(22, "{}");
    }

    @Test
    public void testEmptyArray() throws Exception {
        assertJson(23, "[]");
    }

    @Test
    public void testScalarDateTime() throws Exception {
        assertJson(24, "\"2015-01-15 23:24:25\"");
    }

    @Test
    public void testScalarTime() throws Exception {
        assertJson(25, "\"23:24:25\"");
        assertJson(125, "\"23:24:25.12\"");
        assertJson(225, "\"23:24:25.024\"");
    }

    @Test
    public void testScalarDate() throws Exception {
        assertJson(26, "\"2015-01-15\"");
    }

    @Test
    public void testScalarTimestamp() throws Exception {
        // Timestamp literals are interpreted by MySQL as DATETIME values
        assertJson(27, "\"2015-01-15 23:24:25\"");
        assertJson(127, "\"2015-01-15 23:24:25.12\"");
        assertJson(227, "\"2015-01-15 23:24:25.0237\"");
        // The UNIX_TIMESTAMP(ts) function returns the number of seconds past epoch for the given ts
        assertJson(327, "1421364265");
    }

    @Test
    public void testScalarGeometry() throws Exception {
        assertJson(28, "{\"type\":\"Point\",\"coordinates\":[1.0,1.0]}");
    }

    @Test
    public void testScalarStringWithCharsetConversion() throws Exception {
        assertJson(29, "[]");
    }

    @Test
    public void testScalarBinaryAsBase64() throws Exception {
        assertJson(30, "\"yv4=\"");
        assertJson(31, "\"yv66vg==\"");
    }

    protected void assertJson(int i, String expected) throws Exception {
        byte[] b = jsonForId(i);
        String json = b != null ? JsonBinary.parseAsString(b) : null;
        assertEquals(json, expected);
    }

    /**
     * Get the binary representation of the JSON value that corresponds to the specified row number.
     *
     * @param i the row number; should be unique for an insert to work
     * @return the binary representation of the JSON value as read from the {@link WriteRowsEventData} event;
     *         may be null if the JSON value is null
     */
    protected byte[] jsonForId(int i) throws Exception {
        return jsonValuesByKey.get(i);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        if (master != null) {
            master.execute("drop database if exists json_test");
            master.close();
        }
    }
}
