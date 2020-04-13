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
package com.github.shyiko.mysql.binlog;

import com.github.shyiko.mysql.binlog.event.ByteArrayEventData;
import com.github.shyiko.mysql.binlog.event.DeleteRowsEventData;
import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventData;
import com.github.shyiko.mysql.binlog.event.EventHeaderV4;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.QueryEventData;
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;
import com.github.shyiko.mysql.binlog.event.deserialization.ByteArrayEventDataDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDataDeserializationException;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.EventHeaderV4Deserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.QueryEventDataDeserializer;
import com.github.shyiko.mysql.binlog.io.BufferedSocketInputStream;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;
import com.github.shyiko.mysql.binlog.network.AuthenticationException;
import com.github.shyiko.mysql.binlog.network.ServerException;
import com.github.shyiko.mysql.binlog.network.SocketFactory;
import org.mockito.InOrder;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.xml.bind.DatatypeConverter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.BitSet;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer.CompatibilityMode;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class BinaryLogClientIntegrationTest {

    protected static final long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(3);

    private final Logger logger = Logger.getLogger(getClass().getSimpleName());

    {
        logger.setLevel(Level.FINEST);
    }

    private final TimeZone timeZoneBeforeTheTest = TimeZone.getDefault();

    protected MySQLConnection master, slave;
    protected BinaryLogClient client;
    protected CountDownEventListener eventListener;

    @BeforeClass
    public void setUp() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        ResourceBundle bundle = ResourceBundle.getBundle("jdbc");
        String prefix = "jdbc.mysql.replication.";
        master = new MySQLConnection(bundle.getString(prefix + "master.hostname"),
                Integer.parseInt(bundle.getString(prefix + "master.port")),
                bundle.getString(prefix + "master.username"), bundle.getString(prefix + "master.password"));
        slave = new MySQLConnection(bundle.getString(prefix + "slave.hostname"),
                Integer.parseInt(bundle.getString(prefix + "slave.port")),
                bundle.getString(prefix + "slave.superUsername"), bundle.getString(prefix + "slave.superPassword"));
        client = new BinaryLogClient(slave.hostname, slave.port, slave.username, slave.password);
        EventDeserializer eventDeserializer = new EventDeserializer();
        eventDeserializer.setCompatibilityMode(CompatibilityMode.CHAR_AND_BINARY_AS_BYTE_ARRAY,
            CompatibilityMode.DATE_AND_TIME_AS_LONG);
        client.setEventDeserializer(eventDeserializer);
        client.setServerId(client.getServerId() - 1); // avoid clashes between BinaryLogClient instances
        client.setKeepAlive(false);
        client.registerEventListener(new TraceEventListener());
        client.registerEventListener(eventListener = new CountDownEventListener());
        client.registerLifecycleListener(new TraceLifecycleListener());
        client.connect(DEFAULT_TIMEOUT);
        master.execute(new Callback<Statement>() {
            @Override
            public void execute(Statement statement) throws SQLException {
                statement.execute("drop database if exists mbcj_test");
                statement.execute("create database mbcj_test");
                statement.execute("use mbcj_test");
            }
        });
        eventListener.waitFor(EventType.QUERY, 2, DEFAULT_TIMEOUT);
    }

    @BeforeMethod
    public void beforeEachTest() throws Exception {
        master.execute(new Callback<Statement>() {
            @Override
            public void execute(Statement statement) throws SQLException {
                statement.execute("drop table if exists bikini_bottom");
                statement.execute("create table bikini_bottom (name varchar(255) primary key)");
            }
        });
        eventListener.waitFor(EventType.QUERY, 2, DEFAULT_TIMEOUT);
        eventListener.reset();
    }

    @Test
    public void testWriteUpdateDeleteEvents() throws Exception {
        CapturingEventListener capturingEventListener = new CapturingEventListener();
        client.registerEventListener(capturingEventListener);
        // ensure "capturingEventListener -> eventListener" order
        client.unregisterEventListener(eventListener);
        client.registerEventListener(eventListener);
        try {
            master.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("insert into bikini_bottom values('SpongeBob')");
                }
            });
            eventListener.waitFor(WriteRowsEventData.class, 1, DEFAULT_TIMEOUT);
            List<Serializable[]> writtenRows =
                capturingEventListener.getEvents(WriteRowsEventData.class).get(0).getRows();
            assertEquals(writtenRows.size(), 1);
            assertEquals(writtenRows.get(0), new Serializable[]{"SpongeBob".getBytes("UTF-8")});
            master.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("update bikini_bottom set name = 'Patrick' where name = 'SpongeBob'");
                }
            });
            eventListener.waitFor(UpdateRowsEventData.class, 1, DEFAULT_TIMEOUT);
            List<Map.Entry<Serializable[], Serializable[]>> updatedRows =
                capturingEventListener.getEvents(UpdateRowsEventData.class).get(0).getRows();
            assertEquals(updatedRows.size(), 1);
            assertEquals(updatedRows.get(0).getKey(), new Serializable[]{"SpongeBob".getBytes("UTF-8")});
            assertEquals(updatedRows.get(0).getValue(), new Serializable[]{"Patrick".getBytes("UTF-8")});
            master.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("delete from bikini_bottom where name = 'Patrick'");
                }
            });
            eventListener.waitFor(DeleteRowsEventData.class, 1, DEFAULT_TIMEOUT);
            List<Serializable[]> deletedRows =
                capturingEventListener.getEvents(DeleteRowsEventData.class).get(0).getRows();
            assertEquals(deletedRows.size(), 1);
            assertEquals(deletedRows.get(0), new Serializable[]{"Patrick".getBytes("UTF-8")});
        } finally {
            client.unregisterEventListener(capturingEventListener);
        }
    }

    @Test
    public void testDeserializationOfBIT() throws Exception {
        assertEquals(writeAndCaptureRow("bit(3)", "0", "1", "2", "3"),
            new Serializable[]{bitSet(), bitSet(0), bitSet(1), bitSet(0, 1)});
    }

    @Test
    public void testDeserializationOfTINY() throws Exception {
        assertEquals(writeAndCaptureRow("tinyint unsigned", "0", "1", "255"),
            new Serializable[]{0, 1, -1});
        assertEquals(writeAndCaptureRow("tinyint", "-128", "-1", "0", "1", "127"),
            new Serializable[]{-128, -1, 0, 1, 127});
        assertEquals(writeAndCaptureRow("bool", "1"), new Serializable[]{1});
    }

    @Test
    public void testDeserializationOfSHORT() throws Exception {
        assertEquals(writeAndCaptureRow("smallint unsigned", "0", "1", "65535"),
            new Serializable[]{0, 1, -1});
        assertEquals(writeAndCaptureRow("smallint", "-32768", "-1", "0", "1", "32767"),
            new Serializable[]{-32768, -1, 0, 1, 32767});
    }

    @Test
    public void testDeserializationOfINT24() throws Exception {
        assertEquals(writeAndCaptureRow("mediumint unsigned", "0", "1", "16777215"),
            new Serializable[]{0, 1, -1});
        assertEquals(writeAndCaptureRow("mediumint", "-8388608", "-1", "0", "1", "8388607"),
            new Serializable[]{-8388608, -1, 0, 1, 8388607});
    }

    @Test
    public void testDeserializationOfLONG() throws Exception {
        assertEquals(writeAndCaptureRow("int unsigned", "0", "1", "4294967295"),
            new Serializable[]{0, 1, -1});
        assertEquals(writeAndCaptureRow("int", "-2147483648", "-1", "0", "1", "2147483647"),
            new Serializable[]{-2147483648, -1, 0, 1, 2147483647});
    }

    @Test
    public void testDeserializationOfLONGLONG() throws Exception {
        assertEquals(writeAndCaptureRow("bigint unsigned", "0", "1", "18446744073709551615"),
            new Serializable[]{0L, 1L, -1L});
        assertEquals(writeAndCaptureRow("bigint", "-9223372036854775808", "-1", "0", "1", "9223372036854775807"),
            new Serializable[]{-9223372036854775808L, -1L, 0L, 1L, 9223372036854775807L});
    }

    @Test
    public void testDeserializationOfFLOAT() throws Exception {
        assertEquals(writeAndCaptureRow("float", "-0.3", "0", "0.3"),
            new Serializable[]{-0.3F, 0.0F, 0.3F});
    }

    @Test
    public void testDeserializationOfDOUBLE() throws Exception {
        assertEquals(writeAndCaptureRow("double", "-8.9", "0", "8.9"),
            new Serializable[]{-8.9, 0.0, 8.9});
    }

    @Test
    public void testDeserializationOfNEWDECIMAL() throws Exception {
        MathContext mc = new MathContext(2);
        assertEquals(writeAndCaptureRow("decimal(2,1)", "-2.12", "0", "2.12"),
            new Serializable[]{new BigDecimal(-2.1, mc), new BigDecimal(0).setScale(1), new BigDecimal(2.1, mc)});
    }

    @Test
    public void testDeserializationOfDATE() throws Exception {
        assertEquals(writeAndCaptureRow("date", "'1989-03-21'"), new Serializable[]{
            generateTime(1989, 3, 21, 0, 0, 0, 0)});
        final boolean[] noZeroInDate = new boolean[1];
        master.query("select @@sql_mode;", new Callback<ResultSet>() {

            @Override
            public void execute(ResultSet rs) throws SQLException {
                // NO_ZERO_IN_DATE is turned on by default in MySQL 5.7
                // https://github.com/shyiko/mysql-binlog-connector-java/pull/119#issuecomment-251870581
                noZeroInDate[0] = rs.next() && rs.getString(1).contains("NO_ZERO_IN_DATE");
            }
        });
        if (!noZeroInDate[0]) {
            assertEquals(writeAndCaptureRow("date", "'0000-00-00'"), new Serializable[]{null});
            assertEquals(writeAndCaptureRow("date", "'0000-03-21'"), new Serializable[]{null});
            assertEquals(writeAndCaptureRow("date", "'1989-00-21'"), new Serializable[]{null});
            assertEquals(writeAndCaptureRow("date", "'1989-03-00'"), new Serializable[]{null});
        }
    }

    @Test
    public void testDeserializationOfTIME() throws Exception {
        assertEquals(writeAndCaptureRow("time", "'1:2:3.000000'"), new Serializable[]{
            generateTime(1970, 1, 1, 1, 2, 3, 0)});
    }

    @Test
    public void testDeserializationOfTIMESTAMP() throws Exception {
        assertEquals(writeAndCaptureRow("timestamp", "'1989-03-18 01:02:03.000000'"), new Serializable[]{
            generateTime(1989, 3, 18, 1, 2, 3, 0)});
    }

    @Test
    public void testDeserializationOfDATETIME() throws Exception {
        assertEquals(writeAndCaptureRow("datetime", "'1989-03-21 01:02:03.000000'"), new Serializable[]{
            generateTime(1989, 3, 21, 1, 2, 3, 0)});
    }

    @Test
    public void testDeserializationOfYEAR() throws Exception {
        assertEquals(writeAndCaptureRow("year", "'69'"), new Serializable[]{2069});
    }

    @Test
    public void testDeserializationOfSTRING() throws Exception {
        assertEquals(writeAndCaptureRow("char", "'q'"), new Serializable[]{"q".getBytes("UTF-8")});
        assertEquals(writeAndCaptureRow("char", "'Â'"), new Serializable[]{"Â".getBytes("UTF-8")});
        assertEquals(writeAndCaptureRow("binary", "x'01'"), new Serializable[]{new byte[] {1}});
        assertEquals(writeAndCaptureRow("binary", "x'FF'"), new Serializable[]{new byte[] {-1}});
        assertEquals(writeAndCaptureRow("binary(16)", "unhex(md5(\"glob\"))"),
            new Serializable[]{DatatypeConverter.parseHexBinary("8684147451a6cc3b92142c6f4b78e61c")});
    }

    @Test
    public void testDeserializationOfVARSTRING() throws Exception {
        assertEquals(writeAndCaptureRow("varchar(255)", "'weÂ'"), new Serializable[]{"weÂ".getBytes("UTF-8")});
        assertEquals(writeAndCaptureRow("varbinary(255)", "x'01FF'"), new Serializable[]{new byte[] {1, -1}});
    }

    @Test
    public void testDeserializationOfBLOB() throws Exception {
        assertEquals(writeAndCaptureRow("tinyblob", "x'01FF'"), new Serializable[]{new byte[] {1, -1}});
        assertEquals(writeAndCaptureRow("tinytext", "'opÂ'"), new Serializable[]{"opÂ".getBytes("UTF-8")});
        assertEquals(writeAndCaptureRow("blob", "x'01FF'"), new Serializable[]{new byte[] {1, -1}});
        assertEquals(writeAndCaptureRow("text", "'dfÂ'"), new Serializable[]{"dfÂ".getBytes("UTF-8")});
        assertEquals(writeAndCaptureRow("mediumblob", "x'01FF'"), new Serializable[]{new byte[] {1, -1}});
        assertEquals(writeAndCaptureRow("mediumtext", "'jkÂ'"), new Serializable[]{"jkÂ".getBytes("UTF-8")});
        assertEquals(writeAndCaptureRow("longblob", "x'01FF'"), new Serializable[]{new byte[] {1, -1}});
        assertEquals(writeAndCaptureRow("longtext", "'xcÂ'"), new Serializable[]{"xcÂ".getBytes("UTF-8")});
    }

    @Test
    public void testDeserializationOfENUM() throws Exception {
        assertEquals(writeAndCaptureRow("enum('a','b','c')", "'b'"), new Serializable[]{2});
    }

    @Test
    public void testDeserializationOfSET() throws Exception {
        assertEquals(writeAndCaptureRow("set('a','b','c')", "'a,c'"), new Serializable[]{5L});
    }

    @Test
    public void testDeserializationOfGEOMETRY() throws Exception {
        assertEquals(writeAndCaptureRow("geometry", "ST_GeomFromText('POINT(40.717957 -73.736501)')"),
            new Serializable[]{new byte[] {0, 0, 0, 0, 1, 1, 0, 0, 0, -106, 119, -43, 3, -26, 91, 68,
                64, 42, 30, 23, -43, 34, 111, 82, -64}});
    }

    @Test
    public void testFSP() throws Exception {
        try {
            master.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("create table fsp_check (column_ datetime(0))");
                }
            });
        } catch (SQLSyntaxErrorException e) {
            throw new SkipException("MySQL < 5.6.4+");
        }
        assertEquals(writeAndCaptureRow("datetime(0)", "'1989-03-21 01:02:03.777777'"), new Serializable[]{
            generateTime(1989, 3, 21, 1, 2, 4, 0)});
        assertEquals(writeAndCaptureRow("datetime(1)", "'1989-03-21 01:02:03.777777'"), new Serializable[]{
            generateTime(1989, 3, 21, 1, 2, 3, 800)});
        assertEquals(writeAndCaptureRow("datetime(2)", "'1989-03-21 01:02:03.777777'"), new Serializable[]{
            generateTime(1989, 3, 21, 1, 2, 3, 780)});
        assertEquals(writeAndCaptureRow("datetime(3)", "'1989-03-21 01:02:03.777777'"), new Serializable[]{
            generateTime(1989, 3, 21, 1, 2, 3, 778)});
        assertEquals(writeAndCaptureRow("datetime(3)", "'1989-03-21 01:02:03.777'"), new Serializable[]{
            generateTime(1989, 3, 21, 1, 2, 3, 777)});
        assertEquals(writeAndCaptureRow("datetime(4)", "'1989-03-21 01:02:03.777777'"), new Serializable[]{
            generateTime(1989, 3, 21, 1, 2, 3, 777)});
        assertEquals(writeAndCaptureRow("datetime(5)", "'1989-03-21 01:02:03.777777'"), new Serializable[]{
            generateTime(1989, 3, 21, 1, 2, 3, 777)});
        assertEquals(writeAndCaptureRow("datetime(6)", "'1989-03-21 01:02:03.777777'"), new Serializable[]{
            generateTime(1989, 3, 21, 1, 2, 3, 777)});
    }

    @Test
    public void testDeserializationOfDateAndTimeAsLong() throws Exception {
        final BinaryLogClient client = new BinaryLogClient(slave.hostname, slave.port,
            slave.username, slave.password);
        EventDeserializer eventDeserializer = new EventDeserializer();
        eventDeserializer.setCompatibilityMode(CompatibilityMode.DATE_AND_TIME_AS_LONG);
        client.setEventDeserializer(eventDeserializer);
        client.connect(DEFAULT_TIMEOUT);
        try {
            assertEquals(writeAndCaptureRow(client, "datetime(6)", "'1989-03-21 01:02:03.123456'"), new Serializable[]{
                generateTime(1989, 3, 21, 1, 2, 3, 123)});
        } finally {
            client.disconnect();
        }
    }

    @Test
    public void testDeserializationOfDateAndTimeAsLongMicrosecondsPrecision() throws Exception {
        final BinaryLogClient client = new BinaryLogClient(slave.hostname, slave.port,
            slave.username, slave.password);
        EventDeserializer eventDeserializer = new EventDeserializer();
        eventDeserializer.setCompatibilityMode(CompatibilityMode.DATE_AND_TIME_AS_LONG_MICRO);
        client.setEventDeserializer(eventDeserializer);
        client.connect(DEFAULT_TIMEOUT);
        try {
            assertEquals(writeAndCaptureRow(client, "datetime(6)", "'1989-03-21 01:02:03.123456'"), new Serializable[]{
                generateTime(1989, 3, 21, 1, 2, 3, 123) * 1000 + 456});
        } finally {
            client.disconnect();
        }
    }

    private BitSet bitSet(int... bitsToSetTrue) {
        BitSet result = new BitSet(bitsToSetTrue.length);
        for (int bit : bitsToSetTrue) {
            result.set(bit);
        }
        return result;
    }

    // checkstyle, please ignore ParameterNumber for the next line
    private long generateTime(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.YEAR, year);
        instance.set(Calendar.MONTH, month - 1);
        instance.set(Calendar.DAY_OF_MONTH, day);
        instance.set(Calendar.HOUR_OF_DAY, hour);
        instance.set(Calendar.MINUTE, minute);
        instance.set(Calendar.SECOND, second);
        instance.set(Calendar.MILLISECOND, millisecond);
        return instance.getTimeInMillis();
    }

    private Serializable[] writeAndCaptureRow(final String columnDefinition, final String... values) throws Exception {
        return writeAndCaptureRow(client, columnDefinition, values);
    }

    private Serializable[] writeAndCaptureRow(BinaryLogClient client, final String columnDefinition,
            final String... values) throws Exception {
        CapturingEventListener capturingEventListener = new CapturingEventListener();
        client.registerEventListener(capturingEventListener);
        CountDownEventListener eventListener = new CountDownEventListener();
        client.registerEventListener(eventListener);
        try {
            master.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("drop table if exists data_type_hell");
                    statement.execute("create table data_type_hell (column_ " + columnDefinition +
                        ") CHARACTER SET utf8");
                    StringBuilder insertQueryBuilder = new StringBuilder("insert into data_type_hell values");
                    for (String value : values) {
                        insertQueryBuilder.append("(").append(value).append("), ");
                    }
                    int insertQueryLength = insertQueryBuilder.length();
                    insertQueryBuilder.replace(insertQueryLength - 2, insertQueryLength, "");
                    statement.execute(insertQueryBuilder.toString());
                }
            });
            eventListener.waitFor(WriteRowsEventData.class, 1, DEFAULT_TIMEOUT);
        } finally {
            client.unregisterEventListener(eventListener);
            client.unregisterEventListener(capturingEventListener);
        }
        List<Serializable[]> writtenRows =
            capturingEventListener.getEvents(WriteRowsEventData.class).get(0).getRows();
        Serializable[] result = new Serializable[writtenRows.size()];
        int index = 0;
        for (Serializable[] writtenRow : writtenRows) {
            result[index++] = writtenRow[0];
        }
        return result;
    }

    @Test
    public void testBinlogPositionPointsToTableMapEventUntilTheEndOfLogicalGroup() throws Exception {
        final AtomicReference<Map.Entry<String, Long>> markHolder = new AtomicReference<Map.Entry<String, Long>>();
        BinaryLogClient.EventListener markEventListener = new BinaryLogClient.EventListener() {

            private int counter;

            @Override
            public void onEvent(Event event) {
                if (EventType.isRowMutation(event.getHeader().getEventType()) && counter++ == 1) {
                    // coordinates of second insert
                    markHolder.set(new AbstractMap.SimpleEntry<String, Long>(client.getBinlogFilename(),
                        client.getBinlogPosition()));
                }
            }
        };
        client.registerEventListener(markEventListener);
        try {
            master.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("insert into bikini_bottom values('SpongeBob')");
                    statement.execute("insert into bikini_bottom values('Patrick')");
                    statement.execute("insert into bikini_bottom values('Squidward')");
                }
            });
            eventListener.waitFor(WriteRowsEventData.class, 3, DEFAULT_TIMEOUT);
            final BinaryLogClient anotherClient = new BinaryLogClient(slave.hostname, slave.port,
                slave.username, slave.password);
            anotherClient.registerLifecycleListener(new TraceLifecycleListener());
            CountDownEventListener anotherClientEventListener = new CountDownEventListener();
            anotherClient.registerEventListener(anotherClientEventListener);
            Map.Entry<String, Long> mark = markHolder.get();
            anotherClient.setBinlogFilename(mark.getKey());
            anotherClient.setBinlogPosition(mark.getValue());
            anotherClient.connect(DEFAULT_TIMEOUT);
            try {
                // expecting Patrick & Squidward
                anotherClientEventListener.waitFor(WriteRowsEventData.class, 2, DEFAULT_TIMEOUT);
            } finally {
                anotherClient.disconnect();
            }
        } finally {
            client.unregisterEventListener(markEventListener);
        }
    }

    @Test(enabled = false)
    public void testUnsupportedColumnTypeDoesNotCauseClientToFail() throws Exception {
        BinaryLogClient.LifecycleListener lifecycleListenerMock = mock(BinaryLogClient.LifecycleListener.class);
        client.registerLifecycleListener(lifecycleListenerMock);
        try {
            master.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("create table geometry_table (location geometry)");
                    statement.execute(
                        "insert into geometry_table values(ST_GeomFromText('POINT(40.717957 -73.736501)'))");
                    statement.execute("drop table geometry_table");
                }
            });
            eventListener.waitFor(QueryEventData.class, 3, DEFAULT_TIMEOUT); // create + BEGIN of insert + drop
            eventListener.waitFor(WriteRowsEventData.class, 0, DEFAULT_TIMEOUT);
            verify(lifecycleListenerMock, only()).onEventDeserializationFailure(eq(client), any(Exception.class));
            master.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("insert into bikini_bottom values('SpongeBob')");
                }
            });
            eventListener.waitFor(WriteRowsEventData.class, 1, DEFAULT_TIMEOUT);
        } finally {
            client.unregisterLifecycleListener(lifecycleListenerMock);
        }
    }

    @Test
    public void testTrackingOfLastKnownBinlogFilenameAndPosition() throws Exception {
        master.execute(new Callback<Statement>() {
            @Override
            public void execute(Statement statement) throws SQLException {
                statement.execute("insert into bikini_bottom values('SpongeBob')");
            }
        });
        eventListener.waitFor(WriteRowsEventData.class, 1, DEFAULT_TIMEOUT);
        String binlogFilename = client.getBinlogFilename();
        long binlogPosition = client.getBinlogPosition();
        slave.execute(new Callback<Statement>() {
            @Override
            public void execute(Statement statement) throws SQLException {
                statement.execute("flush logs");
            }
        });
        master.execute(new Callback<Statement>() {
            @Override
            public void execute(Statement statement) throws SQLException {
                statement.execute("insert into bikini_bottom values('Patrick')");

            }
        });
        eventListener.waitFor(WriteRowsEventData.class, 1, DEFAULT_TIMEOUT);
        String updatedBinlogFilename = client.getBinlogFilename();
        long updatedBinlogPosition = client.getBinlogPosition();
        assertNotEquals(updatedBinlogFilename, binlogFilename);
        assertNotEquals(updatedBinlogPosition, binlogPosition);
        master.execute(new Callback<Statement>() {
            @Override
            public void execute(Statement statement) throws SQLException {
                statement.execute("insert into bikini_bottom values('Rocky')");
            }
        });
        eventListener.waitFor(WriteRowsEventData.class, 1, DEFAULT_TIMEOUT);
        assertEquals(client.getBinlogFilename(), updatedBinlogFilename);
        assertNotEquals(client.getBinlogPosition(), updatedBinlogPosition);
    }

    @Test
    public void testAbilityToBeSuspendedAndResumed() throws Exception {
        master.execute(new Callback<Statement>() {
            @Override
            public void execute(Statement statement) throws SQLException {
                statement.execute("insert into bikini_bottom values('SpongeBob')");
            }
        });
        eventListener.waitFor(WriteRowsEventData.class, 1, DEFAULT_TIMEOUT);
        try {
            client.disconnect();
            master.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("insert into bikini_bottom values('Patrick')");
                    statement.execute("insert into bikini_bottom values('Rocky')");
                }
            });
            try {
                eventListener.waitFor(WriteRowsEventData.class, 2, TimeUnit.SECONDS.toMillis(1));
                fail();
            } catch (TimeoutException e) {
                eventListener.reset();
            }
        } finally {
            client.connect(DEFAULT_TIMEOUT);
        }
        eventListener.waitFor(WriteRowsEventData.class, 2, DEFAULT_TIMEOUT);
    }

    @Test
    public void testAbilityToBeRewind() throws Exception {
        String binlogFilename = client.getBinlogFilename();
        long binlogPosition = client.getBinlogPosition();
        master.execute(new Callback<Statement>() {
            @Override
            public void execute(Statement statement) throws SQLException {
                statement.execute("insert into bikini_bottom values('SpongeBob')");
            }
        });
        eventListener.waitFor(WriteRowsEventData.class, 1, DEFAULT_TIMEOUT);
        client.disconnect();
        client.setBinlogFilename(binlogFilename);
        client.setBinlogPosition(binlogPosition);
        client.connect(DEFAULT_TIMEOUT);
        eventListener.waitFor(WriteRowsEventData.class, 1, DEFAULT_TIMEOUT);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testAutomaticFailover() throws Exception {
        TCPReverseProxy tcpReverseProxy = new TCPReverseProxy(33262, slave.port);
        try {
            bindInSeparateThread(tcpReverseProxy);
            try {
                client.disconnect();
                final BinaryLogClient clientOverProxy = new BinaryLogClient(slave.hostname, tcpReverseProxy.getPort(),
                        slave.username, slave.password);
                clientOverProxy.setKeepAliveInterval(TimeUnit.MILLISECONDS.toMillis(100));
                clientOverProxy.setKeepAliveConnectTimeout(TimeUnit.SECONDS.toMillis(2));
                clientOverProxy.registerEventListener(eventListener);
                try {
                    clientOverProxy.connect(DEFAULT_TIMEOUT);
                    eventListener.waitFor(EventType.FORMAT_DESCRIPTION, 1, DEFAULT_TIMEOUT);
                    assertTrue(clientOverProxy.isKeepAliveThreadRunning());
                    BinaryLogClient.LifecycleListener lifecycleListenerMock =
                        mock(BinaryLogClient.LifecycleListener.class);
                    clientOverProxy.registerLifecycleListener(lifecycleListenerMock);
                    TimeUnit.MILLISECONDS.sleep(300); // giving keep-alive-thread a chance to run few iterations
                    tcpReverseProxy.unbind();
                    TimeUnit.MILLISECONDS.sleep(300);
                    master.execute(new Callback<Statement>() {
                        @Override
                        public void execute(Statement statement) throws SQLException {
                            statement.execute("insert into bikini_bottom values('SpongeBob')");
                        }
                    });
                    bindInSeparateThread(tcpReverseProxy);
                    eventListener.waitFor(WriteRowsEventData.class, 1, TimeUnit.SECONDS.toMillis(4));
                    InOrder inOrder = inOrder(lifecycleListenerMock);
                    inOrder.verify(lifecycleListenerMock).onDisconnect(eq(clientOverProxy));
                    inOrder.verify(lifecycleListenerMock).onConnect(eq(clientOverProxy));
                } finally {
                    clientOverProxy.disconnect();
                }
                assertFalse(clientOverProxy.isKeepAliveThreadRunning());
            } finally {
                client.connect(DEFAULT_TIMEOUT);
            }
        } finally {
            tcpReverseProxy.unbind();
        }
    }

    @Test
    public void testEOFExceptionTriggersReconnectIfKeepAliveIsOn() throws Exception {
        testCommunicationFailureInTheMiddleOfEventHeaderDeserialization(new EOFException());
        testCommunicationFailureInTheMiddleOfEventDataDeserialization(new EventDataDeserializationException(null,
                new EOFException()));
    }

    @Test
    public void testSocketExceptionTriggersReconnectIfKeepAliveIsOn() throws Exception {
        testCommunicationFailureInTheMiddleOfEventHeaderDeserialization(new SocketException());
        testCommunicationFailureInTheMiddleOfEventDataDeserialization(new EventDataDeserializationException(null,
                new SocketException()));
    }

    private void testCommunicationFailureInTheMiddleOfEventHeaderDeserialization(final IOException ex)
            throws Exception {
        testCommunicationFailure(new EventDeserializer(new EventHeaderV4Deserializer() {

            private boolean failureSimulated;

            @Override
            public EventHeaderV4 deserialize(ByteArrayInputStream inputStream) throws IOException {
                EventHeaderV4 eventHeader = super.deserialize(inputStream);
                if (eventHeader.getEventType() == EventType.QUERY && !failureSimulated) {
                    failureSimulated = true;
                    throw ex;
                }
                return eventHeader;
            }
        }));
    }

    private void testCommunicationFailureInTheMiddleOfEventDataDeserialization(final IOException ex) throws Exception {
        EventDeserializer eventDeserializer = new EventDeserializer();
        eventDeserializer.setEventDataDeserializer(EventType.QUERY, new QueryEventFailureSimulator());
        testCommunicationFailure(eventDeserializer);
    }

    @SuppressWarnings("deprecation")
    protected void testCommunicationFailure(EventDeserializer eventDeserializer) throws Exception {
        try {
            client.disconnect();
            final BinaryLogClient clientWithKeepAlive = new BinaryLogClient(slave.hostname, slave.port,
                    slave.username, slave.password);
            clientWithKeepAlive.setKeepAliveInterval(TimeUnit.MILLISECONDS.toMillis(100));
            clientWithKeepAlive.setKeepAliveConnectTimeout(TimeUnit.SECONDS.toMillis(2));
            clientWithKeepAlive.registerEventListener(eventListener);
            clientWithKeepAlive.setEventDeserializer(eventDeserializer);
            try {
                eventListener.reset();
                clientWithKeepAlive.connect(DEFAULT_TIMEOUT);
                eventListener.waitFor(EventType.FORMAT_DESCRIPTION, 1, DEFAULT_TIMEOUT);
                BinaryLogClient.LifecycleListener lifecycleListenerMock =
                        mock(BinaryLogClient.LifecycleListener.class);
                clientWithKeepAlive.registerLifecycleListener(lifecycleListenerMock);
                master.execute(new Callback<Statement>() {
                    @Override
                    public void execute(Statement statement) throws SQLException {
                        statement.execute("drop table if exists not_meant_to_exist");
                    }
                });
                eventListener.waitFor(QueryEventData.class, 1, TimeUnit.SECONDS.toMillis(4));
                InOrder inOrder = inOrder(lifecycleListenerMock);
                inOrder.verify(lifecycleListenerMock).onCommunicationFailure(eq(clientWithKeepAlive),
                        any(EOFException.class));
                inOrder.verify(lifecycleListenerMock).onDisconnect(eq(clientWithKeepAlive));
                inOrder.verify(lifecycleListenerMock).onConnect(eq(clientWithKeepAlive));
                verifyNoMoreInteractions(lifecycleListenerMock);
            } finally {
                clientWithKeepAlive.disconnect();
            }
        } finally {
            client.connect(DEFAULT_TIMEOUT);
        }
    }

    @Test
    public void testCustomEventDataDeserializers() throws Exception {
        try {
            client.disconnect();
            final BinaryLogClient binaryLogClient = new BinaryLogClient(slave.hostname, slave.port,
                    slave.username, slave.password);
            binaryLogClient.registerEventListener(new TraceEventListener());
            binaryLogClient.registerEventListener(eventListener);
            EventDeserializer deserializer = new EventDeserializer();
            deserializer.setEventDataDeserializer(EventType.QUERY, new ByteArrayEventDataDeserializer());
            // TABLE_MAP and ROTATE events are both used internally, but that doesn't mean it shouldn't be possible to
            // specify different EventDataDeserializer|s
            deserializer.setEventDataDeserializer(EventType.TABLE_MAP, new ByteArrayEventDataDeserializer());
            deserializer.setEventDataDeserializer(EventType.ROTATE, new ByteArrayEventDataDeserializer());
            binaryLogClient.setEventDeserializer(deserializer);
            try {
                eventListener.reset();
                binaryLogClient.connect(DEFAULT_TIMEOUT);
                eventListener.waitFor(EventType.FORMAT_DESCRIPTION, 1, DEFAULT_TIMEOUT);
                master.execute(new Callback<Statement>() {
                    @Override
                    public void execute(Statement statement) throws SQLException {
                        statement.execute("insert into bikini_bottom values('SpongeBob')");
                    }
                });
                slave.execute(new Callback<Statement>() {
                    @Override
                    public void execute(Statement statement) throws SQLException {
                        statement.execute("flush logs");
                    }
                });
                eventListener.waitFor(EventType.QUERY, 1, DEFAULT_TIMEOUT);
                eventListener.waitFor(EventType.ROTATE, 3, DEFAULT_TIMEOUT); /* 2 with timestamp 0 */
                eventListener.waitFor(ByteArrayEventData.class, 5, DEFAULT_TIMEOUT);
            } finally {
                binaryLogClient.disconnect();
            }
        } finally {
            client.connect(DEFAULT_TIMEOUT);
        }
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testExceptionIsThrownWhenTryingToConnectAlreadyConnectedClient() throws Exception {
        assertTrue(client.isConnected());
        client.connect();
    }

    @Test
    public void testExceptionIsThrownWhenProvidedWithWrongCredentials() throws Exception {
        BinaryLogClient binaryLogClient =
            new BinaryLogClient(slave.hostname, slave.port, slave.username, slave.password + "^_^");
        try {
            binaryLogClient.connect();
            fail("Wrong password should have resulted in AuthenticationException being thrown");
        } catch (AuthenticationException e) {
            assertFalse(binaryLogClient.isConnected());
        }
    }

    @Test(expectedExceptions = ServerException.class)
    public void testExceptionIsThrownWhenInsufficientPermissionsToDetectPosition() throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle("jdbc");
        String prefix = "jdbc.mysql.replication.";
        String slaveUsername = bundle.getString(prefix + "slave.slaveUsername");
        String slavePassword = bundle.getString(prefix + "slave.slavePassword");
        new BinaryLogClient(slave.hostname, slave.port, slaveUsername, slavePassword).connect();
    }

    private void bindInSeparateThread(final TCPReverseProxy tcpReverseProxy) throws InterruptedException {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    tcpReverseProxy.bind();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        tcpReverseProxy.await(3, TimeUnit.SECONDS);
    }

    @Test(expectedExceptions = AuthenticationException.class)
    public void testAuthenticationFailsWhenNonExistingSchemaProvided() throws Exception {
        new BinaryLogClient(slave.hostname, slave.port, "mbcj_test_non_existing", slave.username, slave.password).
            connect(DEFAULT_TIMEOUT);
    }

    @Test
    public void testSpecifiedSchemaDoesNotResultInEventFiltering() throws Exception {
        master.execute(new Callback<Statement>() {
            @Override
            public void execute(Statement statement) throws SQLException {
                statement.execute("drop database if exists mbcj_test_isolated");
                statement.execute("create database mbcj_test_isolated");
                statement.execute("drop table if exists mbcj_test_isolated.bikini_bottom");
                statement.execute("create table mbcj_test_isolated.bikini_bottom (name varchar(255) primary key)");
            }
        });
        eventListener.waitFor(QueryEventData.class, 4, DEFAULT_TIMEOUT);
        BinaryLogClient isolatedClient =
            new BinaryLogClient(slave.hostname, slave.port, "mbcj_test_isolated", slave.username, slave.password);
        try {
            CountDownEventListener isolatedEventListener = new CountDownEventListener();
            isolatedClient.registerEventListener(isolatedEventListener);
            isolatedClient.connect(DEFAULT_TIMEOUT);
            master.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("insert into mbcj_test_isolated.bikini_bottom values('Patrick')");
                    statement.execute("insert into mbcj_test.bikini_bottom values('Rocky')");
                }
            });
            eventListener.waitFor(WriteRowsEventData.class, 2, DEFAULT_TIMEOUT);
            isolatedEventListener.waitFor(WriteRowsEventData.class, 2, DEFAULT_TIMEOUT);
        } finally {
            isolatedClient.disconnect();
        }
    }

    @Test
    public void testReconnectRaceCondition() throws Exception {
        // this test relies on SO_RCVBUF (sysctl -a | grep rcvbuf)
        // a more reliable way would be to use buffered 2-level concurrent filter input stream
        try {
            client.disconnect();
            final BinaryLogClient binaryLogClient =
                new BinaryLogClient(slave.hostname, slave.port, slave.username, slave.password);
            final Lock inputStreamLock = new ReentrantLock();
            final AtomicBoolean breakOutputStream = new AtomicBoolean();
            binaryLogClient.setSocketFactory(new SocketFactory() {

                @Override
                public Socket createSocket() throws SocketException {
                    return new Socket() {

                        @Override
                        public InputStream getInputStream() throws IOException {
                            return new FilterInputStream(new BufferedSocketInputStream(super.getInputStream())) {

                                @Override
                                public int read(byte[] b, int off, int len) throws IOException {
                                    int read = super.read(b, off, len);
                                    inputStreamLock.lock();
                                    inputStreamLock.unlock();
                                    return read;
                                }
                            };
                        }

                        @Override
                        public OutputStream getOutputStream() throws IOException {
                            return new FilterOutputStream(super.getOutputStream()) {

                                @Override
                                public void write(int b) throws IOException {
                                    if (breakOutputStream.get()) {
                                        binaryLogClient.setSocketFactory(null);
                                        throw new IOException();
                                    }
                                    super.write(b);
                                }
                            };
                        }
                    };
                }
            });
            binaryLogClient.registerEventListener(eventListener);
            binaryLogClient.setKeepAliveInterval(TimeUnit.MILLISECONDS.toMillis(100));
            binaryLogClient.connect(DEFAULT_TIMEOUT);
            try {
                eventListener.waitFor(EventType.FORMAT_DESCRIPTION, 1, DEFAULT_TIMEOUT);
                master.execute(new Callback<Statement>() {
                    @Override
                    public void execute(Statement statement) throws SQLException {
                        statement.execute("insert into bikini_bottom values('SpongeBob')");
                    }
                });
                eventListener.waitFor(WriteRowsEventData.class, 1, DEFAULT_TIMEOUT);
                // lock input stream
                inputStreamLock.lock();
                // fill input stream buffer
                master.execute(new Callback<Statement>() {
                    @Override
                    public void execute(Statement statement) throws SQLException {
                        statement.execute("insert into bikini_bottom values('Patrick')");
                        statement.execute("insert into bikini_bottom values('Rocky')");
                    }
                });
                // trigger reconnect
                final CountDownLatch reconnect = new CountDownLatch(1);
                binaryLogClient.registerLifecycleListener(new BinaryLogClient.AbstractLifecycleListener() {

                    @Override
                    public void onConnect(BinaryLogClient client) {
                        reconnect.countDown();
                    }
                });
                breakOutputStream.set(true);
                // wait for connection to be reestablished
                reconnect.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
                // unlock input stream (from previous connection)
                inputStreamLock.unlock();
                master.execute(new Callback<Statement>() {
                    @Override
                    public void execute(Statement statement) throws SQLException {
                        statement.execute("delete from bikini_bottom where name = 'Patrick'");
                    }
                });
                eventListener.waitFor(DeleteRowsEventData.class, 1, DEFAULT_TIMEOUT);
                // assert that no events were delivered twice
                eventListener.waitFor(WriteRowsEventData.class, 2, DEFAULT_TIMEOUT);
            } finally {
                binaryLogClient.disconnect();
            }
        } finally {
            client.connect(DEFAULT_TIMEOUT);
        }
    }

    @Test
    public void testMySQL8TableMetadata() throws Exception {
        master.execute("drop table if exists test_metameta");
        master.execute("create table test_metameta ( " +
                "a date, b date, c date, d date, e date, f date, g date, " +
                "h date, i date, j int)");
        master.execute("insert into test_metameta set j = 5");
        eventListener.waitFor(WriteRowsEventData.class, 1, DEFAULT_TIMEOUT);
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
        master.execute(new Callback<Statement>() {
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
            if (slave != null) {
                slave.close();
            }
            if (master != null) {
                master.execute(new Callback<Statement>() {
                    @Override
                    public void execute(Statement statement) throws SQLException {
                        statement.execute("drop database mbcj_test");
                    }
                });
                master.close();
            }
        }
    }

    /**
     * Representation of a MySQL connection.
     */
    public static final class MySQLConnection implements Closeable {

        private final String hostname;
        private final int port;
        private final String username;
        private final String password;
        private Connection connection;

        public MySQLConnection(String hostname, int port, String username, String password)
            throws ClassNotFoundException, SQLException {
            this.hostname = hostname;
            this.port = port;
            this.username = username;
            this.password = password;
            Class.forName("com.mysql.jdbc.Driver");
            connect();
        }

        private void connect() throws SQLException {
            this.connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port +
                "?serverTimezone=UTC", username, password);
            execute(new Callback<Statement>() {

                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("SET time_zone = '+00:00'");
                }
            });
        }

        public String hostname() {
            return hostname;
        }

        public int port() {
            return port;
        }

        public String username() {
            return username;
        }

        public String password() {
            return password;
        }

        public void execute(Callback<Statement> callback, boolean autocommit) throws SQLException {
            connection.setAutoCommit(autocommit);
            Statement statement = connection.createStatement();
            try {
                callback.execute(statement);
                if (!autocommit) {
                    connection.commit();
                }
            } finally {
                statement.close();
            }
        }

        public void execute(Callback<Statement> callback) throws SQLException {
            execute(callback, false);
        }

        public void execute(final String...statements) throws SQLException {
            execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    for (String command : statements) {
                        statement.execute(command);
                    }
                }
            });
        }

        public void query(String sql, Callback<ResultSet> callback) throws SQLException {
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            try {
                ResultSet rs = statement.executeQuery(sql);
                try {
                    callback.execute(rs);
                    connection.commit();
                } finally {
                    rs.close();
                }
            } finally {
                statement.close();
            }
        }

        @Override
        public void close() throws IOException {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }

        public void reconnect() throws IOException, SQLException {
            close();
            connect();
        }
    }

    /**
     * Callback used in the {@link MySQLConnection#execute(Callback)} method.
     *
     * @param <T> the type of argument
     */
    public interface Callback<T> {

        void execute(T obj) throws SQLException;
    }

    /**
     * Used to simulate {@link SocketException} inside
     * {@link QueryEventDataDeserializer#deserialize(ByteArrayInputStream)} (once).
     */
    protected class QueryEventFailureSimulator extends QueryEventDataDeserializer {
        private boolean failureSimulated;

        @Override
        public QueryEventData deserialize(ByteArrayInputStream inputStream) throws IOException {
            QueryEventData eventData = super.deserialize(inputStream);
            if (!failureSimulated) {
                failureSimulated = true;
                throw new SocketException();
            }
            return eventData;
        }
    }

}
