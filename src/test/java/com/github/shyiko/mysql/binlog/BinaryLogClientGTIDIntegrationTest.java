/*
 * Copyright 2018 Stanley Shyiko
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

import com.github.shyiko.mysql.binlog.event.QueryEventData;
import com.github.shyiko.mysql.binlog.event.XidEventData;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertNotEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author <a href="https://github.com/osheroff">Ben Osheroff</a>
 */
public class BinaryLogClientGTIDIntegrationTest extends BinaryLogClientIntegrationTest {

    @BeforeClass
    private void enableGTID() throws SQLException {
        MySQLConnection[] servers = {slave, master};
        for (MySQLConnection m : servers) {
            m.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    ResultSet rs = statement.executeQuery("select @@GLOBAL.GTID_MODE as gtid_mode");
                    rs.next();
                    if ("ON".equals(rs.getString("gtid_mode"))) {
                        return;
                    }
                    statement.execute("SET @@GLOBAL.ENFORCE_GTID_CONSISTENCY = ON;");
                    statement.execute("SET @@GLOBAL.GTID_MODE = OFF_PERMISSIVE;");
                    statement.execute("SET @@GLOBAL.GTID_MODE = ON_PERMISSIVE;");
                    statement.execute("SET @@GLOBAL.GTID_MODE = ON;");
                }
            }, true);
        }
    }

    @AfterClass(alwaysRun = true)
    private void disableGTID() throws SQLException {
        MySQLConnection[] servers = {slave, master};
        for (MySQLConnection m : servers) {
            m.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    statement.execute("SET @@GLOBAL.GTID_MODE = ON_PERMISSIVE;");
                    statement.execute("SET @@GLOBAL.GTID_MODE = OFF_PERMISSIVE;");
                    statement.execute("SET @@GLOBAL.GTID_MODE = OFF;");
                    statement.execute("SET @@GLOBAL.ENFORCE_GTID_CONSISTENCY = OFF;");
                }
            }, true);
        }
        slave.execute("STOP SLAVE", "START SLAVE");
    }

    @Test
    public void testGTIDAdvancesStatementBased() throws Exception {
        try {
            master.execute("set global binlog_format=statement");
            slave.execute("set global binlog_format=statement", "stop slave", "start slave");
            master.reconnect();
            master.execute("use test");
            testGTIDAdvances();
        } finally {
            master.execute("set global binlog_format=row");
            slave.execute("set global binlog_format=row", "stop slave", "start slave");
            master.reconnect();
            master.execute("use test");
        }
    }

    @Test
    public void testGTIDAdvances() throws Exception {
        master.execute("CREATE TABLE if not exists foo (i int)");

        final String[] initialGTIDSet = new String[1];
        master.query("show master status", new Callback<ResultSet>() {
            @Override
            public void execute(ResultSet rs) throws SQLException {
                rs.next();
                initialGTIDSet[0] = rs.getString("Executed_Gtid_Set");
            }
        });

        EventDeserializer eventDeserializer = new EventDeserializer();
        try {
            client.disconnect();
            final BinaryLogClient clientWithKeepAlive = new BinaryLogClient(slave.hostname(), slave.port(),
                slave.username(), slave.password());

            clientWithKeepAlive.setGtidSet(initialGTIDSet[0]);
            clientWithKeepAlive.registerEventListener(eventListener);
            clientWithKeepAlive.setEventDeserializer(eventDeserializer);
            try {
                eventListener.reset();
                clientWithKeepAlive.connect(DEFAULT_TIMEOUT);

                master.execute(new Callback<Statement>() {
                    @Override
                    public void execute(Statement statement) throws SQLException {
                        statement.execute("INSERT INTO foo set i = 2");
                        statement.execute("INSERT INTO foo set i = 3");
                    }
                });

                eventListener.waitFor(XidEventData.class, 1, TimeUnit.SECONDS.toMillis(4));
                String gtidSet = clientWithKeepAlive.getGtidSet();
                assertNotNull(gtidSet);

                eventListener.reset();

                master.execute(new Callback<Statement>() {
                    @Override
                    public void execute(Statement statement) throws SQLException {
                        statement.execute("INSERT INTO foo set i = 4");
                        statement.execute("INSERT INTO foo set i = 5");
                    }
                });

                eventListener.waitFor(XidEventData.class, 1, TimeUnit.SECONDS.toMillis(4));
                assertNotEquals(client.getGtidSet(), gtidSet);

                gtidSet = client.getGtidSet();

                eventListener.reset();
                master.execute("DROP TABLE IF EXISTS test.bar");
                eventListener.waitFor(QueryEventData.class, 1, TimeUnit.SECONDS.toMillis(4));
                assertNotEquals(clientWithKeepAlive.getGtidSet(), gtidSet);
            } finally {
                clientWithKeepAlive.disconnect();
            }
        } finally {
            client.connect(DEFAULT_TIMEOUT);
        }
    }
}
