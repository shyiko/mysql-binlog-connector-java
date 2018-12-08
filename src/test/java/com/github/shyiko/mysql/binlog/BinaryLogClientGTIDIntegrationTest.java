package com.github.shyiko.mysql.binlog;

import com.github.shyiko.mysql.binlog.event.*;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import org.junit.After;
import org.mockito.InOrder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.EOFException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.testng.Assert.assertNotEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class BinaryLogClientGTIDIntegrationTest extends BinaryLogClientIntegrationTest {
    private final Logger logger = Logger.getLogger(getClass().getSimpleName());

    @BeforeClass
    private void enableGTID() throws SQLException {
        MySQLConnection[] servers = {slave, master};
        for (MySQLConnection m : servers) {
            m.execute(new Callback<Statement>() {
                @Override
                public void execute(Statement statement) throws SQLException {
                    ResultSet rs = statement.executeQuery("select @@GLOBAL.GTID_MODE as gtid_mode");
                    rs.next();
                    if ( rs.getString("gtid_mode").equals("ON") ) {
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

    @AfterClass
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
    public void testGTIDAdvances() throws Exception {
        master.execute("CREATE TABLE foo (i int)");

        final String[] initialGTIDSet = new String[1];
        master.query("show master status", new Callback<ResultSet>() {
            @Override
            public void execute(ResultSet rs) throws SQLException {
                rs.next();
                initialGTIDSet[0] = rs.getString("Executed_Gtid_Set");
            }
        });

        client.setGtidSet(initialGTIDSet[0]);

        EventDeserializer eventDeserializer = new EventDeserializer();
        try {
            client.disconnect();
            final BinaryLogClient clientWithKeepAlive = new BinaryLogClient(slave.hostname(), slave.port(),
                slave.username(), slave.password());
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
                        statement.execute("INSERT INTO foo set i = 2");
                        statement.execute("INSERT INTO foo set i = 3");
                    }
                });

                eventListener.waitFor(WriteRowsEventData.class, 2, TimeUnit.SECONDS.toMillis(4));
                eventListener.waitFor(XidEventData.class, 1, TimeUnit.SECONDS.toMillis(4));
                logger.info(eventListener.toString());

                String gtidSet = client.getGtidSet();
                assertNotNull(gtidSet);
                logger.info("hello.  1. " + gtidSet);



                master.execute(new Callback<Statement>() {
                    @Override
                    public void execute(Statement statement) throws SQLException {
                        statement.execute("INSERT INTO foo set i = 4");
                        statement.execute("INSERT INTO foo set i = 5");
                    }
                });

                eventListener.waitFor(WriteRowsEventData.class, 2, TimeUnit.SECONDS.toMillis(4));
                eventListener.waitFor(XidEventData.class, 1, TimeUnit.SECONDS.toMillis(4));
                logger.info("hello. 2." + client.getGtidSet());
                assertNotEquals(client.getGtidSet(), gtidSet);
            } finally {
                clientWithKeepAlive.disconnect();
            }
        } finally {
            client.connect(DEFAULT_TIMEOUT);
        }
    }
}
