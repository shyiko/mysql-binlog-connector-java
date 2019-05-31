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

import static com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer.CompatibilityMode;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.QueryEventData;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.QueryEventDataDeserializer;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class BinaryLogClientIntegrationTestBase {

    protected static final long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(3);
    protected final TimeZone timeZoneBeforeTheTest = TimeZone.getDefault();

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

        protected final String hostname;
        protected final int port;
        protected final String username;
        protected final String password;
        protected Connection connection;

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
