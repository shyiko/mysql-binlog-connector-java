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

import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
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
        client.registerEventListener(new TraceEventListener());
        client.registerEventListener(eventListener = new CountDownEventListener());
        client.registerLifecycleListener(new TraceLifecycleListener());
        client.connect(3, TimeUnit.SECONDS);
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
        eventListener.reset();
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
            System.out.println(eventListener);
            try {
                eventListener.waitFor(WriteRowsEventData.class, 2, TimeUnit.SECONDS.toMillis(1));
                fail();
            } catch (TimeoutException e) {
                eventListener.reset();
            }
        } finally {
            client.connect(3, TimeUnit.SECONDS);
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
        client.connect(3, TimeUnit.SECONDS);
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
                clientOverProxy.setKeepAliveInterval(TimeUnit.SECONDS.toMillis(1));
                clientOverProxy.registerEventListener(eventListener);
                try {
                    clientOverProxy.connect(3, TimeUnit.SECONDS);
                    eventListener.waitFor(EventType.FORMAT_DESCRIPTION, 1, DEFAULT_TIMEOUT);
                    tcpReverseProxy.unbind();
                    master.execute(new Callback<Statement>() {
                        @Override
                        public void execute(Statement statement) throws SQLException {
                            statement.execute("insert into bikini_bottom values('SpongeBob')");
                        }
                    });
                    bindInSeparateThread(tcpReverseProxy);
                    eventListener.waitFor(WriteRowsEventData.class, 1, TimeUnit.SECONDS.toMillis(3));
                } finally {
                    clientOverProxy.disconnect();
                }
            } finally {
                client.connect(3, TimeUnit.SECONDS);
            }
        } finally {
            tcpReverseProxy.unbind();
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

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        try {
            client.disconnect();
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

