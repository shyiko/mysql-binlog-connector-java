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
import com.github.shyiko.mysql.binlog.event.EventHeader;
import com.github.shyiko.mysql.binlog.event.EventHeaderV4;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.RotateEventData;
import com.github.shyiko.mysql.binlog.event.deserialization.ChecksumType;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;
import com.github.shyiko.mysql.binlog.jmx.BinaryLogClientMXBean;
import com.github.shyiko.mysql.binlog.network.AuthenticationException;
import com.github.shyiko.mysql.binlog.network.SocketFactory;
import com.github.shyiko.mysql.binlog.network.protocol.ErrorPacket;
import com.github.shyiko.mysql.binlog.network.protocol.GreetingPacket;
import com.github.shyiko.mysql.binlog.network.protocol.PacketChannel;
import com.github.shyiko.mysql.binlog.network.protocol.ResultSetRowPacket;
import com.github.shyiko.mysql.binlog.network.protocol.command.AuthenticateCommand;
import com.github.shyiko.mysql.binlog.network.protocol.command.DumpBinaryLogCommand;
import com.github.shyiko.mysql.binlog.network.protocol.command.PingCommand;
import com.github.shyiko.mysql.binlog.network.protocol.command.QueryCommand;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL replication stream client.
 *
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class BinaryLogClient implements BinaryLogClientMXBean {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final String hostname;
    private final int port;
    private final String schema;
    private final String username;
    private final String password;

    private long serverId = 65535;
    private volatile String binlogFilename;
    private volatile long binlogPosition;

    private EventDeserializer eventDeserializer = new EventDeserializer();

    private final List<EventListener> eventListeners = new LinkedList<EventListener>();
    private final List<LifecycleListener> lifecycleListeners = new LinkedList<LifecycleListener>();

    private SocketFactory socketFactory;

    private PacketChannel channel;
    private volatile boolean connected;

    private ThreadFactory threadFactory;

    private boolean keepAlive = true;
    private long keepAliveInterval = TimeUnit.MINUTES.toMillis(1);
    private long keepAliveConnectTimeout = TimeUnit.SECONDS.toMillis(3);

    private volatile ThreadPoolExecutor keepAliveThreadExecutor;
    private long keepAliveThreadShutdownTimeout = TimeUnit.SECONDS.toMillis(6);

    private final Lock shutdownLock = new ReentrantLock();

    /**
     * Alias for BinaryLogClient("localhost", 3306, &lt;no schema&gt; = null, username, password).
     * @see BinaryLogClient#BinaryLogClient(String, int, String, String, String)
     */
    public BinaryLogClient(String username, String password) {
        this("localhost", 3306, null, username, password);
    }

    /**
     * Alias for BinaryLogClient("localhost", 3306, schema, username, password).
     * @see BinaryLogClient#BinaryLogClient(String, int, String, String, String)
     */
    public BinaryLogClient(String schema, String username, String password) {
        this("localhost", 3306, schema, username, password);
    }

    /**
     * Alias for BinaryLogClient(hostname, port, &lt;no schema&gt; = null, username, password).
     * @see BinaryLogClient#BinaryLogClient(String, int, String, String, String)
     */
    public BinaryLogClient(String hostname, int port, String username, String password) {
        this(hostname, port, null, username, password);
    }

    /**
     * @param hostname mysql server hostname
     * @param port mysql server port
     * @param schema database name, nullable. Note that this parameter has nothing to do with event filtering. It's
     * used only during the authentication.
     * @param username login name
     * @param password password
     */
    public BinaryLogClient(String hostname, int port, String schema, String username, String password) {
        this.hostname = hostname;
        this.port = port;
        this.schema = schema;
        this.username = username;
        this.password = password;
    }

    /**
     * @return server id (65535 by default)
     */
    public long getServerId() {
        return serverId;
    }

    /**
     * @param serverId server id (in the range from 1 to 2^32 – 1). This value MUST be unique across whole replication
     * group (that is, different from any other server id being used by any master or slave). Keep in mind that each
     * binary log client (mysql-binlog-connector-java/BinaryLogClient, mysqlbinlog, etc) should be treated as a
     * simplified slave and thus MUST also use a different server id.
     */
    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    /**
     * @return binary log filename, nullable. Note that this value is automatically tracked by the client and thus
     * is subject to change (in response to {@link EventType#ROTATE}, for example).
     */
    public String getBinlogFilename() {
        return binlogFilename;
    }

    /**
     * @param binlogFilename binary log filename (null indicates automatic resolution).
     */
    public void setBinlogFilename(String binlogFilename) {
        this.binlogFilename = binlogFilename;
    }

    /**
     * @return binary log position of the next event. Note that this value changes with each incoming event.
     */
    public long getBinlogPosition() {
        return binlogPosition;
    }

    /**
     * @param binlogPosition binary log position
     */
    public void setBinlogPosition(long binlogPosition) {
        this.binlogPosition = binlogPosition;
    }

    /**
     * @param keepAlive true if "keep alive" thread should be automatically started (recommended and true by default),
     * false otherwise.
     */
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * @param keepAliveInterval "keep alive" interval in milliseconds.
     */
    public void setKeepAliveInterval(long keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    /**
     * @param keepAliveConnectTimeout "keep alive" connect interval in milliseconds.
     */
    public void setKeepAliveConnectTimeout(long keepAliveConnectTimeout) {
        this.keepAliveConnectTimeout = keepAliveConnectTimeout;
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
     * @param socketFactory custom socket factory. If not provided, socket will be created with "new Socket()".
     */
    public void setSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    /**
     * @param threadFactory custom thread factory to use for "connect in separate thread". If not provided, thread
     * will be created using simple "new Thread()".
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    /**
     * Connect to the replication stream. Note that this method blocks until disconnected.
     * @throws AuthenticationException in case of failed authentication
     * @throws IOException if anything goes wrong while trying to connect
     */
    public void connect() throws IOException {
        if (connected) {
            throw new IllegalStateException("BinaryLogClient is already connected");
        }
        try {
            try {
                Socket socket = socketFactory != null ? socketFactory.createSocket() : new Socket();
                socket.connect(new InetSocketAddress(hostname, port));
                channel = new PacketChannel(socket);
                if (channel.getInputStream().peek() == -1) {
                    throw new EOFException();
                }
            } catch (IOException e) {
                throw new IOException("Failed to connect to MySQL on " + hostname + ":" + port +
                        ". Please make sure it's running.", e);
            }
            GreetingPacket greetingPacket = new GreetingPacket(channel.read());
            AuthenticateCommand authenticateCommand = new AuthenticateCommand(schema, username, password,
                    greetingPacket.getScramble());
            authenticateCommand.setCollation(greetingPacket.getServerCollation());
            channel.write(authenticateCommand);
            byte[] authenticationResult = channel.read();
            if (authenticationResult[0] != (byte) 0x00 /* ok */) {
                if (authenticationResult[0] == (byte) 0xFF /* error */) {
                    byte[] bytes = Arrays.copyOfRange(authenticationResult, 1, authenticationResult.length);
                    throw new AuthenticationException(new ErrorPacket(bytes).getErrorMessage());
                }
                throw new AuthenticationException("Unexpected authentication result (" + authenticationResult[0] + ")");
            }
            if (binlogFilename == null) {
                fetchBinlogFilenameAndPosition();
            }
            ChecksumType checksumType = fetchBinlogChecksum();
            if (checksumType != ChecksumType.NONE) {
                confirmSupportOfChecksum(checksumType);
            }
            channel.write(new DumpBinaryLogCommand(serverId, binlogFilename, binlogPosition));
        } catch (IOException e) {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            throw e;
        }
        connected = true;
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Connected to " + hostname + ":" + port + " at " + binlogFilename + "/" + binlogPosition);
        }
        synchronized (lifecycleListeners) {
            for (LifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.onConnect(this);
            }
        }
        if (keepAlive && !isKeepAliveThreadRunning()) {
            spawnKeepAliveThread();
        }
        listenForEventPackets();
    }

    private void spawnKeepAliveThread() {
        keepAliveThreadExecutor = newSingleDaemonThreadExecutor("blc-keepalive-" + hostname + ":" + port);
        keepAliveThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(keepAliveInterval);
                    } catch (InterruptedException e) {
                        // expected in case of disconnect
                    }
                    shutdownLock.lock();
                    try {
                        if (keepAliveThreadExecutor.isShutdown()) {
                            return;
                        }
                        try {
                            channel.write(new PingCommand());
                        } catch (IOException e) {
                            if (logger.isLoggable(Level.INFO)) {
                                logger.info("Trying to restore lost connection to " + hostname + ":" + port);
                            }
                            try {
                                if (isConnected()) {
                                    disconnectChannel();
                                }
                                connect(keepAliveConnectTimeout);
                            } catch (Exception ce) {
                                if (logger.isLoggable(Level.WARNING)) {
                                    logger.warning("Failed to restore connection to " + hostname + ":" + port +
                                        ". Next attempt in " + keepAliveInterval + "ms");
                                }
                            }
                        }
                    } finally {
                        shutdownLock.unlock();
                    }
                }
            }
        });
    }

    private ThreadPoolExecutor newSingleDaemonThreadExecutor(final String threadName) {
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, threadName);
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    protected boolean isKeepAliveThreadRunning() {
        return keepAliveThreadExecutor != null && !keepAliveThreadExecutor.isShutdown();
    }

    /**
     * Connect to the replication stream in a separate thread.
     * @param timeoutInMilliseconds timeout in milliseconds
     * @throws AuthenticationException in case of failed authentication
     * @throws IOException if anything goes wrong while trying to connect
     * @throws TimeoutException if client wasn't able to connect in the requested period of time
     */
    public void connect(long timeoutInMilliseconds) throws IOException, TimeoutException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        AbstractLifecycleListener connectListener = new AbstractLifecycleListener() {
            @Override
            public void onConnect(BinaryLogClient client) {
                countDownLatch.countDown();
            }
        };
        registerLifecycleListener(connectListener);
        final AtomicReference<IOException> exceptionReference = new AtomicReference<IOException>();
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {
                    connect();
                } catch (IOException e) {
                    exceptionReference.set(e);
                    countDownLatch.countDown(); // making sure we don't end up waiting whole "timeout"
                }
            }
        };
        Thread thread = threadFactory == null ? new Thread(runnable) : threadFactory.newThread(runnable);
        thread.start();
        boolean started = false;
        try {
            started = countDownLatch.await(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, e.getMessage());
            }
        }
        unregisterLifecycleListener(connectListener);
        if (exceptionReference.get() != null) {
            throw exceptionReference.get();
        }
        if (!started) {
            throw new TimeoutException("BinaryLogClient was unable to connect in " + timeoutInMilliseconds + "ms");
        }
    }

    /**
     * @return true if client is connected, false otherwise
     */
    public boolean isConnected() {
        return connected;
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

    private ChecksumType fetchBinlogChecksum() throws IOException {
        channel.write(new QueryCommand("show global variables like 'binlog_checksum'"));
        ResultSetRowPacket[] resultSet = readResultSet();
        if (resultSet.length == 0) {
            return ChecksumType.NONE;
        }
        return ChecksumType.valueOf(resultSet[0].getValue(1).toUpperCase());
    }

    private void confirmSupportOfChecksum(ChecksumType checksumType) throws IOException {
        channel.write(new QueryCommand("set @master_binlog_checksum= @@global.binlog_checksum"));
        byte[] statementResult = channel.read();
        if (statementResult[0] == (byte) 0xFF /* error */) {
            byte[] bytes = Arrays.copyOfRange(statementResult, 1, statementResult.length);
            throw new IOException(new ErrorPacket(bytes).getErrorMessage());
        }
        eventDeserializer.setChecksumType(checksumType);
    }

    private void listenForEventPackets() throws IOException {
        ByteArrayInputStream inputStream = channel.getInputStream();
        try {
            while (inputStream.peek() != -1) {
                int packetLength = inputStream.readInteger(3);
                inputStream.skip(1); // 1 byte for sequence
                int marker = inputStream.read();
                if (marker == 0xFF) {
                    ErrorPacket errorPacket = new ErrorPacket(inputStream.read(packetLength - 1));
                    throw new IOException(errorPacket.getErrorCode() + " - " + errorPacket.getErrorMessage());
                }
                Event event;
                try {
                    event = eventDeserializer.nextEvent(inputStream);
                } catch (Exception e) {
                    if (isConnected()) {
                        synchronized (lifecycleListeners) {
                            for (LifecycleListener lifecycleListener : lifecycleListeners) {
                                lifecycleListener.onEventDeserializationFailure(this, e);
                            }
                        }
                    }
                    continue;
                }
                if (isConnected()) {
                    notifyEventListeners(event);
                    updateClientBinlogFilenameAndPosition(event);
                }
            }
        } catch (Exception e) {
            if (isConnected()) {
                synchronized (lifecycleListeners) {
                    for (LifecycleListener lifecycleListener : lifecycleListeners) {
                        lifecycleListener.onCommunicationFailure(this, e);
                    }
                }
            }
        } finally {
            if (isConnected()) {
                disconnectChannel();
            }
        }
    }

    private void updateClientBinlogFilenameAndPosition(Event event) {
        EventHeader eventHeader = event.getHeader();
        if (eventHeader.getEventType() == EventType.ROTATE) {
            RotateEventData eventData = event.getData();
            if (eventData != null) {
                binlogFilename = eventData.getBinlogFilename();
                binlogPosition = eventData.getBinlogPosition();
            }
        } else
        if (eventHeader instanceof EventHeaderV4) {
            EventHeaderV4 trackableEventHeader = (EventHeaderV4) eventHeader;
            long nextBinlogPosition = trackableEventHeader.getNextPosition();
            if (nextBinlogPosition > 0) {
                binlogPosition = nextBinlogPosition;
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
     * @return registered event listeners
     */
    public List<EventListener> getEventListeners() {
        return Collections.unmodifiableList(eventListeners);
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
     * @return registered lifecycle listeners
     */
    public List<LifecycleListener> getLifecycleListeners() {
        return Collections.unmodifiableList(lifecycleListeners);
    }

    /**
     * Register lifecycle listener. Note that multiple lifecycle listeners will be called in order they
     * where registered.
     */
    public void registerLifecycleListener(LifecycleListener lifecycleListener) {
        synchronized (lifecycleListeners) {
            lifecycleListeners.add(lifecycleListener);
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
        shutdownLock.lock();
        try {
            if (isKeepAliveThreadRunning()) {
                keepAliveThreadExecutor.shutdownNow();
            }
            disconnectChannel();
        } finally {
            shutdownLock.unlock();
        }
        if (isKeepAliveThreadRunning()) {
            waitForKeepAliveThreadToBeTerminated();
        }
    }

    private void waitForKeepAliveThreadToBeTerminated() {
        boolean terminated = false;
        try {
            terminated = keepAliveThreadExecutor.awaitTermination(keepAliveThreadShutdownTimeout,
                TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, e.getMessage());
            }
        }
        if (!terminated) {
            throw new IllegalStateException("BinaryLogClient was unable to shut keep alive thread down in " +
                keepAliveThreadShutdownTimeout + "ms");
        }
    }

    private void disconnectChannel() throws IOException {
        try {
            connected = false;
            if (channel != null && channel.isOpen()) {
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
     * {@link BinaryLogClient}'s event listener.
     */
    public interface EventListener {

        void onEvent(Event event);
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

    /**
     * Default (no-op) implementation of {@link LifecycleListener}.
     */
    public static abstract class AbstractLifecycleListener implements LifecycleListener {

        public void onConnect(BinaryLogClient client) {
        }

        public void onCommunicationFailure(BinaryLogClient client, Exception ex) {
        }

        public void onEventDeserializationFailure(BinaryLogClient client, Exception ex) {
        }

        public void onDisconnect(BinaryLogClient client) {
        }

    }

}
