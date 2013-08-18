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

import com.github.shyiko.mysql.binlog.event.DeleteRowsEventData;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.QueryEventData;
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;
import com.github.shyiko.mysql.binlog.network.AuthenticationException;
import org.mockito.InOrder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.BitSet;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(3);

    private final Logger logger = Logger.getLogger(getClass().getSimpleName());

    {
        logger.setLevel(Level.FINEST);
    }

    private MySQLConnection master, slave;
    private BinaryLogClient client;
    private CountDownEventListener eventListener;

    @BeforeClass
    public void setUp() throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle("jdbc");
        String prefix = "jdbc.mysql.replication.";
        master = new MySQLConnection(bundle.getString(prefix + "master.hostname"),
                Integer.parseInt(bundle.getString(prefix + "master.port")),
                bundle.getString(prefix + "master.username"), bundle.getString(prefix + "master.password"));
        slave = new MySQLConnection(bundle.getString(prefix + "slave.hostname"),
                Integer.parseInt(bundle.getString(prefix + "slave.port")),
                bundle.getString(prefix + "slave.username"), bundle.getString(prefix + "slave.password"));
        client = new BinaryLogClient(slave.hostname, slave.port, slave.username, slave.password);
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
            assertEquals(writtenRows.get(0), new Serializable[]{"SpongeBob"});
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
            assertEquals(updatedRows.get(0).getKey(), new Serializable[]{"SpongeBob"});
            assertEquals(updatedRows.get(0).getValue(), new Serializable[]{"Patrick"});
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
            assertEquals(deletedRows.get(0), new Serializable[]{"Patrick"});
        } finally {
            client.unregisterEventListener(capturingEventListener);
        }
    }

    @Test
    public void testDeserializationOfDifferentColumnTypes() throws Exception {
        // numeric types
        assertEquals(writeAndCaptureRow("bit(3)", "0", "1", "2", "3"),
            new Serializable[]{bitSet(), bitSet(0), bitSet(1), bitSet(0, 1)});
        assertEquals(writeAndCaptureRow("tinyint unsigned", "0", "1", "255"),
            new Serializable[]{0, 1, -1});
        assertEquals(writeAndCaptureRow("tinyint", "-128", "-1", "0", "1", "127"),
            new Serializable[]{-128, -1, 0, 1, 127});
        assertEquals(writeAndCaptureRow("bool", "1"), new Serializable[]{1});
        assertEquals(writeAndCaptureRow("smallint unsigned", "0", "1", "65535"),
            new Serializable[]{0, 1, -1});
        assertEquals(writeAndCaptureRow("smallint", "-32768", "-1", "0", "1", "32767"),
            new Serializable[]{-32768, -1, 0, 1, 32767});
        assertEquals(writeAndCaptureRow("mediumint unsigned", "0", "1", "16777215"),
            new Serializable[]{0, 1, -1});
        assertEquals(writeAndCaptureRow("mediumint", "-8388608", "-1", "0", "1", "8388607"),
            new Serializable[]{-8388608, -1, 0, 1, 8388607});
        assertEquals(writeAndCaptureRow("int unsigned", "0", "1", "4294967295"),
            new Serializable[]{0, 1, -1});
        assertEquals(writeAndCaptureRow("int", "-2147483648", "-1", "0", "1", "2147483647"),
            new Serializable[]{-2147483648, -1, 0, 1, 2147483647});
        assertEquals(writeAndCaptureRow("bigint unsigned", "0", "1", "18446744073709551615"),
            new Serializable[]{0L, 1L, -1L});
        assertEquals(writeAndCaptureRow("bigint", "-9223372036854775808", "-1", "0", "1", "9223372036854775807"),
            new Serializable[]{-9223372036854775808L, -1L, 0L, 1L, 9223372036854775807L});
        MathContext mc = new MathContext(2);
        assertEquals(writeAndCaptureRow("decimal(2,1)", "-2.12", "0", "2.12"),
            new Serializable[]{new BigDecimal(-2.1, mc), new BigDecimal(0).setScale(1), new BigDecimal(2.1, mc)});
        assertEquals(writeAndCaptureRow("float", "-0.3", "0", "0.3"),
            new Serializable[]{-0.3F, 0.0F, 0.3F});
        assertEquals(writeAndCaptureRow("double", "-8.9", "0", "8.9"),
            new Serializable[]{-8.9, 0.0, 8.9});
        // date & time types
        assertEquals(writeAndCaptureRow("date", "'1989-03-21'"), new Serializable[]{
            new java.sql.Date(generateTime(1989, 3, 21, 0, 0, 0, 0))});
        assertEquals(writeAndCaptureRow("datetime", "'1989-03-21 01:02:03.000000'"), new Serializable[]{
            new java.util.Date(generateTime(1989, 3, 21, 1, 2, 3, 0))});
        assertEquals(writeAndCaptureRow("timestamp", "'1989-03-18 01:02:03.000000'"), new Serializable[]{
            new java.sql.Timestamp(generateTime(1989, 3, 18, 1, 2, 3, 0))});
        assertEquals(writeAndCaptureRow("time", "'1:2:3.000000'"), new Serializable[]{
            new java.sql.Time(generateTime(1970, 1, 1, 1, 2, 3, 0))});
        assertEquals(writeAndCaptureRow("year", "'69'"), new Serializable[]{2069});
        // string types
        assertEquals(writeAndCaptureRow("char", "'q'"), new Serializable[]{"q"});
        assertEquals(writeAndCaptureRow("varchar(255)", "'we'"), new Serializable[]{"we"});
        assertEquals(writeAndCaptureRow("binary", "'r'"), new Serializable[]{"r"});
        assertEquals(writeAndCaptureRow("varbinary(255)", "'ty'"), new Serializable[]{"ty"});
        assertEquals(writeAndCaptureRow("tinyblob", "'ui'"), new Serializable[]{"ui".getBytes()});
        assertEquals(writeAndCaptureRow("tinytext", "'op'"), new Serializable[]{"op".getBytes()});
        assertEquals(writeAndCaptureRow("blob", "'as'"), new Serializable[]{"as".getBytes()});
        assertEquals(writeAndCaptureRow("text", "'df'"), new Serializable[]{"df".getBytes()});
        assertEquals(writeAndCaptureRow("mediumblob", "'gh'"), new Serializable[]{"gh".getBytes()});
        assertEquals(writeAndCaptureRow("mediumtext", "'jk'"), new Serializable[]{"jk".getBytes()});
        assertEquals(writeAndCaptureRow("longblob", "'lz'"), new Serializable[]{"lz".getBytes()});
        assertEquals(writeAndCaptureRow("longtext", "'xc'"), new Serializable[]{"xc".getBytes()});
        assertEquals(writeAndCaptureRow("enum('a','b','c')", "'b'"), new Serializable[]{2});
        assertEquals(writeAndCaptureRow("set('a','b','c')", "'a,c'"), new Serializable[]{5L});
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

    private Serializable[] writeAndCaptureRow(final String columnDefinition, final String... values)
            throws Exception {
        CapturingEventListener capturingEventListener = new CapturingEventListener();
        client.registerEventListener(capturingEventListener);
        try {
            master.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("drop table if exists data_type_hell");
                    statement.execute("create table data_type_hell (column_ " + columnDefinition + ")");
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
    public void testUnsupportedColumnTypeDoesNotCauseClientToFail() throws Exception {
        BinaryLogClient.LifecycleListener lifecycleListenerMock = mock(BinaryLogClient.LifecycleListener.class);
        client.registerLifecycleListener(lifecycleListenerMock);
        try {
            master.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("create table geometry_table (location geometry)");
                    statement.execute("insert into geometry_table values(GeomFromText('POINT(40.717957 -73.736501)'))");
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
                    verifyNoMoreInteractions(lifecycleListenerMock);
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

    @AfterMethod
    public void afterEachTest() throws Exception {
        eventListener.reset();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        try {
            if (client != null) {
                client.disconnect();
            }
        } finally {
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

    private static final class MySQLConnection implements Closeable {

        private String hostname;
        private int port;
        private String username;
        private String password;
        private Connection connection;

        private MySQLConnection(String hostname, int port, String username, String password)
                throws ClassNotFoundException, SQLException {
            this.hostname = hostname;
            this.port = port;
            this.username = username;
            this.password = password;
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port,
                    username, password);
            execute(new Callback<Statement>() {

                @Override
                public void execute(Statement statement) throws SQLException {
                    // fixing connection timezone to the "client's one"
                    TimeZone currentTimeZone = TimeZone.getDefault();
                    int offset = currentTimeZone.getRawOffset() + currentTimeZone.getDSTSavings();
                    String timeZoneAsAString = String.format("%s%02d:%02d", offset >= 0 ? "+" : "-", offset / 3600000,
                        (offset / 60000) % 60);
                    statement.execute("SET time_zone = '" + timeZoneAsAString + "'");
                }
            });
        }

        public void execute(Callback<Statement> callback) throws SQLException {
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            try {
                callback.execute(statement);
                connection.commit();
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
    }

    private interface Callback<T> {

        void execute(T obj) throws SQLException;
    }
}

