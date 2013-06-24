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

import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventListener;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;
import com.github.shyiko.mysql.binlog.network.AuthenticationException;
import com.github.shyiko.mysql.binlog.network.protocol.ErrorPacket;
import com.github.shyiko.mysql.binlog.network.protocol.GreetingPacket;
import com.github.shyiko.mysql.binlog.network.protocol.PacketChannel;
import com.github.shyiko.mysql.binlog.network.protocol.ResultSetRowPacket;
import com.github.shyiko.mysql.binlog.network.protocol.command.AuthenticateCommand;
import com.github.shyiko.mysql.binlog.network.protocol.command.DumpBinaryLogCommand;
import com.github.shyiko.mysql.binlog.network.protocol.command.QueryCommand;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL replication stream client.
 *
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class BinaryLogClient {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final String hostname;
    private final int port;
    private final String schema;
    private final String username;
    private final String password;

    private volatile String binlogFilename;
    private volatile long binlogPosition;

    private EventDeserializer eventDeserializer = new EventDeserializer();

    private final List<EventListener> eventListeners = new LinkedList<EventListener>();
    private final List<LifecycleListener> lifecycleListeners = new LinkedList<LifecycleListener>();

    private PacketChannel channel;
    private volatile boolean connected;

    /**
     * Alias for BinaryLogClient(username, port, &lt;no schema&gt; , username, password).
     * @see BinaryLogClient#BinaryLogClient(String, int, String, String, String)
     */
    public BinaryLogClient(String hostname, int port, String username, String password) {
        this(hostname, port, null, username, password);
    }

    /**
     * @param hostname mysql server hostname
     * @param port mysql server port
     * @param schema database
     * @param username login
     * @param password password
     */
    public BinaryLogClient(String hostname, int port, String schema, String username, String password) {
        this.hostname = hostname;
        this.port = port;
        this.schema = schema;
        this.username = username;
        this.password = password;
    }

    public String getBinlogFilename() {
        return binlogFilename;
    }

    public void setBinlogFilename(String binlogFilename) {
        this.binlogFilename = binlogFilename;
    }

    public long getBinlogPosition() {
        return binlogPosition;
    }

    public void setBinlogPosition(long binlogPosition) {
        this.binlogPosition = binlogPosition;
    }

    /**
     * @param eventDeserializer custom event deserializer
     */
    public void setEventDeserializer(EventDeserializer eventDeserializer) {
        if (eventDeserializer == null) {
            throw new IllegalArgumentException("Event deserializer cannot be NULL");
        }
        this.eventDeserializer = eventDeserializer;
    }

    /**
     * Connect to the replication stream. Note this method blocks until disconnected.
     * @throws AuthenticationException in case of failed authentication
     */
    public void connect() throws IOException {
        try {
            channel = new PacketChannel(hostname, port);
            if (channel.getInputStream().peek() == -1) {
                throw new EOFException();
            }
        } catch (IOException e) {
            throw new IOException("Failed to connect to MySQL on " + hostname + ":" + port +
                    ". Please check whether it's running.", e);
        }
        connected = true;
        GreetingPacket greetingPacket = new GreetingPacket(channel.read());
        AuthenticateCommand authenticateCommand = new AuthenticateCommand(schema, username, password,
                greetingPacket.getScramble());
        authenticateCommand.setCollation(greetingPacket.getServerCollation());
        channel.write(authenticateCommand);
        byte[] authenticationResult = channel.read();
        if (authenticationResult[0] == (byte) 0xFF /* error */) {
            byte[] bytes = Arrays.copyOfRange(authenticationResult, 1, authenticationResult.length);
            throw new AuthenticationException(new ErrorPacket(bytes).getErrorMessage());
        }
        long serverId = fetchServerId();
        if (binlogFilename == null) {
            fetchBinlogFilenameAndPosition();
        }
        synchronized (lifecycleListeners) {
            for (LifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.onConnect(this);
            }
        }
        channel.write(new DumpBinaryLogCommand(serverId, binlogFilename, binlogPosition));
        listenForEventPackets();
    }

    private long fetchServerId() throws IOException {
        channel.write(new QueryCommand("show variables WHERE variable_name = 'server_id'"));
        ResultSetRowPacket[] resultSet = readResultSet();
        if (resultSet.length == 0) {
            throw new IOException("Failed to determine server_id");
        }
        return Long.parseLong(resultSet[0].getValue(1));
    }

    private void fetchBinlogFilenameAndPosition() throws IOException {
        ResultSetRowPacket[] resultSet;
        channel.write(new QueryCommand("show master status"));
        resultSet = readResultSet();
        if (resultSet.length == 0) {
            throw new IOException("Failed to determine binlog filename/position");
        }
        ResultSetRowPacket resultSetRow = resultSet[0];
        binlogFilename = resultSetRow.getValue(0);
        binlogPosition = Long.parseLong(resultSetRow.getValue(1));
    }

    private void listenForEventPackets() throws IOException {
        ByteArrayInputStream inputStream = channel.getInputStream();
        try {
            while (true) {
                try {
                    if (inputStream.peek() == -1) {
                        break;
                    }
                } catch (SocketException e) {
                    if (!connected) {
                        break;
                    }
                    throw e;
                }
                int packetLength = inputStream.readInteger(3);
                inputStream.skip(1); // 1 byte for sequence
                int marker = inputStream.read();
                byte[] bytes = inputStream.read(packetLength - 1);
                if (marker == 0xFF) {
                    ErrorPacket errorPacket = new ErrorPacket(bytes);
                    throw new IOException(errorPacket.getErrorCode() + " - " + errorPacket.getErrorMessage());
                }
                Event event;
                try {
                    event = eventDeserializer.nextEvent(new ByteArrayInputStream(bytes));
                } catch (Exception e) {
                    synchronized (lifecycleListeners) {
                        for (LifecycleListener lifecycleListener : lifecycleListeners) {
                            lifecycleListener.onEventDeserializationFailure(this, e);
                        }
                    }
                    continue;
                }
                notifyEventListeners(event);
            }
        } catch (Exception e) {
            synchronized (lifecycleListeners) {
                for (LifecycleListener lifecycleListener : lifecycleListeners) {
                    lifecycleListener.onCommunicationFailure(this, e);
                }
            }
        } finally {
            if (connected) {
                disconnect();
            } else {
                if (channel.isOpen()) {
                    channel.close();
                }
            }
        }
    }

    private ResultSetRowPacket[] readResultSet() throws IOException {
        List<ResultSetRowPacket> resultSet = new LinkedList<ResultSetRowPacket>();
        while ((channel.read())[0] != (byte) 0xFE /* eof */) { /* skip */ }
        for (byte[] bytes; (bytes = channel.read())[0] != (byte) 0xFE /* eof */; ) {
            resultSet.add(new ResultSetRowPacket(bytes));
        }
        return resultSet.toArray(new ResultSetRowPacket[resultSet.size()]);
    }

    /**
     * Register event listener. Note that multiple event listeners will be called in order they
     * where registered.
     */
    public void registerEventListener(EventListener eventListener) {
        synchronized (eventListeners) {
            eventListeners.add(eventListener);
        }
    }

    /**
     * Unregister all event listener of specific type.
     */
    public void unregisterEventListener(Class<? extends EventListener> listenerClass) {
        synchronized (eventListeners) {
            Iterator<EventListener> iterator = eventListeners.iterator();
            while (iterator.hasNext()) {
                EventListener eventListener = iterator.next();
                if (listenerClass.isInstance(eventListener)) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Unregister single event listener.
     */
    public void unregisterEventListener(EventListener eventListener) {
        synchronized (eventListeners) {
            eventListeners.remove(eventListener);
        }
    }

    private void notifyEventListeners(Event event) {
        synchronized (eventListeners) {
            for (EventListener eventListener : eventListeners) {
                try {
                    eventListener.onEvent(event);
                } catch (Exception e) {
                    if (logger.isLoggable(Level.WARNING)) {
                        logger.log(Level.WARNING, eventListener + " choked on " + event, e);
                    }
                }
            }
        }
    }

    /**
     * Register lifecycle listener. Note that multiple lifecycle listeners will be called in order they
     * where registered.
     */
    public void registerLifecycleListener(LifecycleListener eventListener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.add(eventListener);
        }
    }

    /**
     * Unregister all lifecycle listener of specific type.
     */
    public synchronized void unregisterLifecycleListener(Class<? extends LifecycleListener> listenerClass) {
        synchronized (lifecycleListeners) {
            Iterator<LifecycleListener> iterator = lifecycleListeners.iterator();
            while (iterator.hasNext()) {
                LifecycleListener lifecycleListener = iterator.next();
                if (listenerClass.isInstance(lifecycleListener)) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Unregister single lifecycle listener.
     */
    public synchronized void unregisterLifecycleListener(LifecycleListener eventListener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.remove(eventListener);
        }
    }

    /**
     * Disconnect from the replication stream.
     * Note that this does not cause binlogFilename/binlogPosition to be cleared out.
     * As the result following {@link #connect()} resumes client from where it left off.
     */
    public void disconnect() throws IOException {
        try {
            connected = false;
            if (channel != null) {
                channel.close();
            }
        } finally {
            synchronized (lifecycleListeners) {
                for (LifecycleListener lifecycleListener : lifecycleListeners) {
                    lifecycleListener.onDisconnect(this);
                }
            }
        }
    }

    /**
     * {@link BinaryLogClient}'s lifecycle listener.
     */
    public interface LifecycleListener {

        /**
         * Called once client has successfully logged in but before started to receive binlog events.
         */
        void onConnect(BinaryLogClient client);

        /**
         * It's guarantied to be called before {@link #onDisconnect(BinaryLogClient)}) in case of
         * communication failure.
         */
        void onCommunicationFailure(BinaryLogClient client, Exception ex);

        /**
         * Called in case of failed event deserialization. Note this type of error does NOT cause client to
         * disconnect. If you wish to stop receiving events you'll need to fire client.disconnect() manually.
         */
        void onEventDeserializationFailure(BinaryLogClient client, Exception ex);

        /**
         * Called upon disconnect (regardless of the reason).
         */
        void onDisconnect(BinaryLogClient client);
    }

}
