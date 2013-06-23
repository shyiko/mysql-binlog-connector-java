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
import com.github.shyiko.mysql.binlog.network.protocol.ErrorPacket;
import com.github.shyiko.mysql.binlog.network.protocol.GreetingPacket;
import com.github.shyiko.mysql.binlog.network.protocol.PacketChannel;
import com.github.shyiko.mysql.binlog.network.protocol.ResultSetRowPacket;
import com.github.shyiko.mysql.binlog.network.protocol.command.AuthenticateCommand;
import com.github.shyiko.mysql.binlog.network.protocol.command.DumpBinaryLogCommand;
import com.github.shyiko.mysql.binlog.network.protocol.command.QueryCommand;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
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
    private List<EventListener> eventListeners = new LinkedList<EventListener>();

    private PacketChannel channel;

    private CountDownLatch latch = new CountDownLatch(1);

    public BinaryLogClient(String hostname, int port, String username, String password) {
        this(hostname, port, null, username, password);
    }

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

    public void connect() throws IOException {
        channel = new PacketChannel(hostname, port);
        GreetingPacket greetingPacket = new GreetingPacket(channel.read());
        AuthenticateCommand authenticateCommand = new AuthenticateCommand(schema, username, password,
                greetingPacket.getScramble());
        authenticateCommand.setCollation(greetingPacket.getServerCollation());
        channel.write(authenticateCommand);
        byte[] authenticationResult = channel.read();
        if (authenticationResult[0] == (byte) 0xFF /* error */) {
             throw new IOException(new ErrorPacket(authenticationResult).getErrorMessage());
        }
        long serverId = fetchServerId();
        if (binlogFilename == null) {
            fetchBinlogFilenameAndPosition();
        }
        channel.write(new DumpBinaryLogCommand(serverId, binlogFilename, binlogPosition));
        listenForEventPackets();
    }

    public void await(long timeout, TimeUnit timeUnit) throws InterruptedException {
        latch.await(timeout, timeUnit);
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
        latch.countDown();
        ByteArrayInputStream inputStream = channel.getInputStream();
        while (channel.isOpen()) {
            int packetLength = inputStream.readInteger(3);
            inputStream.skip(2); // 1 byte for sequence and 1 for marker
            ByteArrayInputStream eventByteArray = new ByteArrayInputStream(inputStream.read(packetLength - 1));
            Event event = eventDeserializer.nextEvent(eventByteArray);
            notifyEventListeners(event);
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

    public synchronized void registerEventListener(EventListener eventListener) {
        eventListeners.add(eventListener);
    }

    public synchronized void unregisterEventListener(Class<? extends EventListener> listenerClass) {
        Iterator<EventListener> iterator = eventListeners.iterator();
        while (iterator.hasNext()) {
            EventListener replicationEventListener = iterator.next();
            if (listenerClass.isInstance(replicationEventListener)) {
                iterator.remove();
            }
        }
    }

    private synchronized void notifyEventListeners(Event event) {
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

    public void disconnect() throws IOException {
        // todo: execute quit command before closing a socket
        if (channel != null) {
            channel.close();
        }
    }

}
