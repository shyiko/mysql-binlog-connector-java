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
import com.github.shyiko.mysql.binlog.event.EventData;
import com.github.shyiko.mysql.binlog.event.EventHeader;
import com.github.shyiko.mysql.binlog.event.EventHeaderV4;
import com.github.shyiko.mysql.binlog.event.EventType;
import com.github.shyiko.mysql.binlog.event.GtidEventData;
import com.github.shyiko.mysql.binlog.event.QueryEventData;
import com.github.shyiko.mysql.binlog.event.RotateEventData;
import com.github.shyiko.mysql.binlog.event.deserialization.ChecksumType;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDataDeserializationException;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDataDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.GtidEventDataDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.QueryEventDataDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.RotateEventDataDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.maria.BinlogCheckpointDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.maria.GtidDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.maria.GtidListDeserializer;
import com.github.shyiko.mysql.binlog.event.maria.Gtid;
import com.github.shyiko.mysql.binlog.event.maria.MariaGtidEventData;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;
import com.github.shyiko.mysql.binlog.jmx.BinaryLogClientMXBean;
import com.github.shyiko.mysql.binlog.network.AuthenticationException;
import com.github.shyiko.mysql.binlog.network.ClientCapabilities;
import com.github.shyiko.mysql.binlog.network.DefaultSSLSocketFactory;
import com.github.shyiko.mysql.binlog.network.DefaultSocketFactory;
import com.github.shyiko.mysql.binlog.network.SSLMode;
import com.github.shyiko.mysql.binlog.network.SSLSocketFactory;
import com.github.shyiko.mysql.binlog.network.ServerException;
import com.github.shyiko.mysql.binlog.network.SocketFactory;
import com.github.shyiko.mysql.binlog.network.TLSHostnameVerifier;
import com.github.shyiko.mysql.binlog.network.protocol.ErrorPacket;
import com.github.shyiko.mysql.binlog.network.protocol.GreetingPacket;
import com.github.shyiko.mysql.binlog.network.protocol.Packet;
import com.github.shyiko.mysql.binlog.network.protocol.PacketChannel;
import com.github.shyiko.mysql.binlog.network.protocol.ResultSetRowPacket;
import com.github.shyiko.mysql.binlog.network.protocol.command.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
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

    private static final SocketFactory DEFAULT_SOCKET_FACTORY = new DefaultSocketFactory();
    private static final SSLSocketFactory DEFAULT_REQUIRED_SSL_MODE_SOCKET_FACTORY = new DefaultSSLSocketFactory() {

        @Override
        protected void initSSLContext(SSLContext sc) throws GeneralSecurityException {
            sc.init(null, new TrustManager[]{
                new X509TrustManager() {

                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                        throws CertificateException { }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                        throws CertificateException { }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            }, null);
        }
    };
    private static final SSLSocketFactory DEFAULT_VERIFY_CA_SSL_MODE_SOCKET_FACTORY = new DefaultSSLSocketFactory();

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final String hostname;
    private final int port;
    private final String schema;
    private final String username;
    private final String password;

    private boolean blocking = true;
    private long serverId = 65535;
    private volatile String binlogFilename;
    private volatile long binlogPosition = 4;
    private volatile long connectionId;
    private SSLMode sslMode = SSLMode.DISABLED;

    private volatile GtidSet gtidSet;
    private final Object gtidSetAccessLock = new Object();

    private EventDeserializer eventDeserializer = new EventDeserializer();

    private final List<EventListener> eventListeners = new LinkedList<EventListener>();
    private final List<LifecycleListener> lifecycleListeners = new LinkedList<LifecycleListener>();

    private SocketFactory socketFactory;
    private SSLSocketFactory sslSocketFactory;

    private PacketChannel channel;
    private volatile boolean connected;

    private ThreadFactory threadFactory;

    private boolean keepAlive = true;
    private long keepAliveInterval = TimeUnit.MINUTES.toMillis(1);
    private long keepAliveConnectTimeout = TimeUnit.SECONDS.toMillis(3);

    private volatile ExecutorService keepAliveThreadExecutor;
    private long keepAliveThreadShutdownTimeout = TimeUnit.SECONDS.toMillis(6);

    private final Lock shutdownLock = new ReentrantLock();

    private Event previousEvent;
    private Event previousGtidEvent;

    // MariaDB
    private Gtid mariaGtid;
    private String gtid;
    private boolean mariaDB;

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

    public boolean isBlocking() {
        return blocking;
    }

    /**
     * @param blocking blocking mode. If set to false - BinaryLogClient will disconnect after the last event.
     */
    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    public SSLMode getSSLMode() {
        return sslMode;
    }

    public void setSSLMode(SSLMode sslMode) {
        if (sslMode == null) {
            throw new IllegalArgumentException("SSL mode cannot be NULL");
        }
        this.sslMode = sslMode;
    }

    /**
     * @return server id (65535 by default)
     * @see #setServerId(long)
     */
    public long getServerId() {
        return serverId;
    }

    /**
     * @param serverId server id (in the range from 1 to 2^32 - 1). This value MUST be unique across whole replication
     * group (that is, different from any other server id being used by any master or slave). Keep in mind that each
     * binary log client (mysql-binlog-connector-java/BinaryLogClient, mysqlbinlog, etc) should be treated as a
     * simplified slave and thus MUST also use a different server id.
     * @see #getServerId()
     */
    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    /**
     * @return binary log filename, nullable (and null be default). Note that this value is automatically tracked by
     * the client and thus is subject to change (in response to {@link EventType#ROTATE}, for example).
     * @see #setBinlogFilename(String)
     */
    public String getBinlogFilename() {
        return binlogFilename;
    }

    /**
     * @param binlogFilename binary log filename.
     * Special values are:
     * <ul>
     *   <li>null, which turns on automatic resolution (resulting in the last known binlog and position). This is what
     * happens by default when you don't specify binary log filename explicitly.</li>
     *   <li>"" (empty string), which instructs server to stream events starting from the oldest known binlog.</li>
     * </ul>
     * @see #getBinlogFilename()
     */
    public void setBinlogFilename(String binlogFilename) {
        this.binlogFilename = binlogFilename;
    }

    /**
     * @return binary log position of the next event, 4 by default (which is a position of first event). Note that this
     * value changes with each incoming event.
     * @see #setBinlogPosition(long)
     */
    public long getBinlogPosition() {
        return binlogPosition;
    }

    /**
     * @param binlogPosition binary log position. Any value less than 4 gets automatically adjusted to 4 on connect.
     * @see #getBinlogPosition()
     */
    public void setBinlogPosition(long binlogPosition) {
        this.binlogPosition = binlogPosition;
    }

    /**
     * @return thread id
     */
    public long getConnectionId() {
        return connectionId;
    }

    /**
     * @return GTID set. Note that this value changes with each received GTID event (provided client is in GTID mode).
     * @see #setGtidSet(String)
     */
    public String getGtidSet() {
        synchronized (gtidSetAccessLock) {
            return gtidSet != null ? gtidSet.toString() : null;
        }
    }

    /**
     * @param gtidSet GTID set (can be an empty string).
     * <p>NOTE #1: Any value but null will switch BinaryLogClient into a GTID mode (in which case GTID set will be
     * updated with each incoming GTID event) as well as set binlogFilename to "" (empty string) (meaning
     * BinaryLogClient will request events "outside of the set" <u>starting from the oldest known binlog</u>).
     * <p>NOTE #2: {@link #setBinlogFilename(String)} and {@link #setBinlogPosition(long)} can be used to specify the
     * exact position from which MySQL server should start streaming events (taking into account GTID set).
     * @see #getGtidSet()
     */
    public void setGtidSet(String gtidSet) {
        if (gtidSet != null && this.binlogFilename == null) {
            this.binlogFilename = "";
        }
        synchronized (gtidSetAccessLock) {
            this.gtidSet = gtidSet != null ? new GtidSet(gtidSet) : null;
        }
    }

    /**
     * @return true if "keep alive" thread should be automatically started (default), false otherwise.
     * @see #setKeepAlive(boolean)
     */
    public boolean isKeepAlive() {
        return keepAlive;
    }

    /**
     * @param keepAlive true if "keep alive" thread should be automatically started (recommended and true by default),
     * false otherwise.
     * @see #isKeepAlive()
     */
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * @return "keep alive" interval in milliseconds, 1 minute by default.
     * @see #setKeepAliveInterval(long)
     */
    public long getKeepAliveInterval() {
        return keepAliveInterval;
    }

    /**
     * @param keepAliveInterval "keep alive" interval in milliseconds.
     * @see #getKeepAliveInterval()
     */
    public void setKeepAliveInterval(long keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    /**
     * @return "keep alive" connect timeout in milliseconds, 3 seconds by default.
     * @see #setKeepAliveConnectTimeout(long)
     */
    public long getKeepAliveConnectTimeout() {
        return keepAliveConnectTimeout;
    }

    /**
     * @param keepAliveConnectTimeout "keep alive" connect timeout in milliseconds.
     * @see #getKeepAliveConnectTimeout()
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
     * @param socketFactory custom socket factory
     */
    public void setSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    /**
     * @param sslSocketFactory custom ssl socket factory
     */
    public void setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    /**
     * @param threadFactory custom thread factory. If not provided, threads will be created using simple "new Thread()".
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    /**
     * Connect to the replication stream. Note that this method blocks until disconnected.
     * @throws AuthenticationException if authentication fails
     * @throws ServerException if MySQL server responds with an error
     * @throws IOException if anything goes wrong while trying to connect
     */
    public void connect() throws IOException {
        if (connected) {
            throw new IllegalStateException("BinaryLogClient is already connected");
        }
        try {
            establishConnection();
            GreetingPacket greetingPacket = receiveGreeting();
            authenticate(greetingPacket);
            connectionId = greetingPacket.getThreadId();
            if (binlogFilename == null && gtidSet == null) {
                autoPosition();
            }
            if (binlogPosition < 4) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning("Binary log position adjusted from " + binlogPosition + " to " + 4);
                }
                binlogPosition = 4;
            }
            ChecksumType checksumType = fetchBinlogChecksum();
            if (checksumType != ChecksumType.NONE) {
                resetBinlogChecksumToNONE();
            }
            requestBinaryLogStream();
        } catch (IOException e) {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            throw e;
        }
        connected = true;
        if (logger.isLoggable(Level.INFO)) {
            String position;
            synchronized (gtidSetAccessLock) {
                position = gtidSet != null ? gtidSet.toString() : binlogFilename + "/" + binlogPosition;
            }
            logger.info("Connected to " + hostname + ":" + port + " at " + position +
                " (" + (blocking ? "sid:" + serverId + ", " : "") + "cid:" + connectionId + ")");
        }
        synchronized (lifecycleListeners) {
            for (LifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.onConnect(this);
            }
        }
        if (keepAlive && !isKeepAliveThreadRunning()) {
            spawnKeepAliveThread();
        }
        ensureEventDataDeserializer(EventType.ROTATE, RotateEventDataDeserializer.class);
        if (gtidSet != null) {
            ensureEventDataDeserializer(EventType.GTID, GtidEventDataDeserializer.class);
            ensureEventDataDeserializer(EventType.QUERY, QueryEventDataDeserializer.class);
        }
        reset();
        listenForEventPackets();
    }

    private void reset() {
        previousEvent = previousGtidEvent = null;
    }

    private void establishConnection() throws IOException {
        try {
            SocketFactory socketFactory = this.socketFactory != null ? this.socketFactory : DEFAULT_SOCKET_FACTORY;
            Socket socket = socketFactory.createSocket();
            socket.connect(new InetSocketAddress(hostname, port));
            channel = new PacketChannel(socket);
            if (channel.getInputStream().peek() == -1) {
                throw new EOFException();
            }
        } catch (IOException e) {
            throw new IOException("Failed to connect to MySQL on " + hostname + ":" + port +
                ". Please make sure it's running.", e);
        }
    }

    private GreetingPacket receiveGreeting() throws IOException {
        byte[] initialHandshakePacket = channel.read();
        if (initialHandshakePacket[0] == ErrorPacket.HEADER) {
            ErrorPacket errorPacket = new ErrorPacket(initialHandshakePacket, 1);
            throw new ServerException(errorPacket.getErrorMessage(), errorPacket.getErrorCode(),
                errorPacket.getSqlState());
        }
        return new GreetingPacket(initialHandshakePacket);
    }

    private void requestBinaryLogStream() throws IOException {
        long serverId = blocking ? this.serverId : 0; // http://bugs.mysql.com/bug.php?id=71178
        Command dumpBinaryLogCommand;
        synchronized (gtidSetAccessLock) {
            if (gtidSet != null) {
                dumpBinaryLogCommand = new DumpBinaryLogGtidCommand(serverId, "", 4, gtidSet);
            } else {
                dumpBinaryLogCommand = new DumpBinaryLogCommand(serverId, binlogFilename, binlogPosition);
            }
        }
        channel.write(dumpBinaryLogCommand);
    }

    private void ensureEventDataDeserializer(EventType eventType,
             Class<? extends EventDataDeserializer> eventDataDeserializerClass) {
        EventDataDeserializer eventDataDeserializer = eventDeserializer.getEventDataDeserializer(eventType);
        if (eventDataDeserializer.getClass() != eventDataDeserializerClass &&
            eventDataDeserializer.getClass() != EventDeserializer.EventDataWrapper.Deserializer.class) {
            EventDataDeserializer internalEventDataDeserializer;
            try {
                internalEventDataDeserializer = eventDataDeserializerClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            eventDeserializer.setEventDataDeserializer(eventType,
                new EventDeserializer.EventDataWrapper.Deserializer(internalEventDataDeserializer,
                    eventDataDeserializer));
        }
    }

    private void authenticate(GreetingPacket greetingPacket) throws IOException {
        int collation = greetingPacket.getServerCollation();
        int packetNumber = 1;
        if (sslMode != SSLMode.DISABLED) {
            boolean serverSupportsSSL = (greetingPacket.getServerCapabilities() & ClientCapabilities.SSL) != 0;
            if (!serverSupportsSSL && (sslMode == SSLMode.REQUIRED || sslMode == SSLMode.VERIFY_CA ||
                    sslMode == SSLMode.VERIFY_IDENTITY)) {
                throw new IOException("MySQL server does not support SSL");
            }
            if (serverSupportsSSL) {
                SSLRequestCommand sslRequestCommand = new SSLRequestCommand();
                sslRequestCommand.setCollation(collation);
                channel.write(sslRequestCommand, packetNumber++);
                SSLSocketFactory sslSocketFactory = this.sslSocketFactory != null ? this.sslSocketFactory :
                    sslMode == SSLMode.REQUIRED ? DEFAULT_REQUIRED_SSL_MODE_SOCKET_FACTORY :
                        DEFAULT_VERIFY_CA_SSL_MODE_SOCKET_FACTORY;
                channel.upgradeToSSL(sslSocketFactory,
                    sslMode == SSLMode.VERIFY_IDENTITY ? new TLSHostnameVerifier() : null);
            }
        }
        AuthenticateCommand authenticateCommand = new AuthenticateCommand(schema, username, password,
            greetingPacket.getScramble());
        authenticateCommand.setCollation(collation);
        channel.write(authenticateCommand, packetNumber);
        byte[] authenticationResult = channel.read();
        if (authenticationResult[0] == ErrorPacket.HEADER) {
            ErrorPacket errorPacket = new ErrorPacket(authenticationResult, 1);
            throw new AuthenticationException(errorPacket.getErrorMessage(), errorPacket.getErrorCode(),
                errorPacket.getSqlState());
        }
    }

    private void spawnKeepAliveThread() {
        keepAliveThreadExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable runnable) {
                return newNamedThread(runnable, "blc-keepalive-" + hostname + ":" + port);
            }
        });
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

    private Thread newNamedThread(Runnable runnable, String threadName) {
        Thread thread = threadFactory == null ? new Thread(runnable) : threadFactory.newThread(runnable);
        thread.setName(threadName);
        return thread;
    }

    boolean isKeepAliveThreadRunning() {
        return keepAliveThreadExecutor != null && !keepAliveThreadExecutor.isShutdown();
    }

    /**
     * Connect to the replication stream in a separate thread.
     * @param timeoutInMilliseconds timeout in milliseconds
     * @throws AuthenticationException if authentication fails
     * @throws ServerException if MySQL server responds with an error
     * @throws IOException if anything goes wrong while trying to connect
     * @throws TimeoutException if client was unable to connect within given time limit
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
        newNamedThread(runnable, "blc-" + hostname + ":" + port).start();
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

    private void autoPosition() throws IOException {
        ResultSetRowPacket[] resultSet;
        channel.write(new QueryCommand("show master status"));
        resultSet = readResultSet();
        if (resultSet.length == 0) {
            throw new IOException("Failed to determine current binlog position");
        }
        ResultSetRowPacket resultSetRow = resultSet[0];
        // File | Position | Binlog_Do_DB | Binlog_Ignore_DB | Executed_Gtid_Set
        // see https://dev.mysql.com/doc/refman/5.6/en/show-master-status.html
        binlogFilename = resultSetRow.getValue(0);
        binlogPosition = Long.parseLong(resultSetRow.getValue(1));
        if (isGtidModeOn()) {
            synchronized (gtidSetAccessLock) {
                gtidSet = new GtidSet(resultSetRow.getValue(4));
            }
        }
    }

    private boolean isGtidModeOn() throws IOException  {
        channel.write(new QueryCommand("show global variables like 'gtid_mode'"));
        ResultSetRowPacket[] resultSet = readResultSet();
        return resultSet.length > 0 && "ON".equalsIgnoreCase(resultSet[0].getValue(1));
    }

    private ChecksumType fetchBinlogChecksum() throws IOException {
        channel.write(new QueryCommand("show global variables like 'binlog_checksum'"));
        ResultSetRowPacket[] resultSet = readResultSet();
        if (resultSet.length == 0) {
            return ChecksumType.NONE;
        }
        return ChecksumType.valueOf(resultSet[0].getValue(1).toUpperCase());
    }

    private void resetBinlogChecksumToNONE() throws IOException {
        // letting MySQL server know that we are aware of binlog_checksum so that it would not terminate connection
        // saying "slave can not handle replication events with the checksum that master is configured to log; ..."
        channel.write(new QueryCommand("set @master_binlog_checksum='NONE'"));
        byte[] statementResult = channel.read();
        if (statementResult[0] == ErrorPacket.HEADER) {
            ErrorPacket errorPacket = new ErrorPacket(statementResult, 1);
            throw new ServerException(errorPacket.getErrorMessage(), errorPacket.getErrorCode(),
                    errorPacket.getSqlState());
        }
        // required because of the synthetic ROTATE event (not present in the actual binlog file) that precedes
        // FORMAT_DESCRIPTION (later carries the checksum)
        eventDeserializer.setChecksumType(ChecksumType.NONE);
    }

    private void listenForEventPackets() throws IOException {
        ByteArrayInputStream inputStream = channel.getInputStream();
        boolean completeShutdown = false;
        try {
            while (inputStream.peek() != -1) {
                int packetLength = inputStream.readInteger(3);
                inputStream.skip(1); // 1 byte for sequence
                byte marker = (byte) inputStream.read();
                if (marker == ErrorPacket.HEADER) {
                    ErrorPacket errorPacket = new ErrorPacket(inputStream.read(packetLength - 1));
                    throw new ServerException(errorPacket.getErrorMessage(), errorPacket.getErrorCode(),
                        errorPacket.getSqlState());
                }
                if (marker == (byte) 0xFE && !blocking) {
                    completeShutdown = true;
                    break;
                }
                Event event;
                try {
                    event = eventDeserializer.nextEvent(packetLength == Packet.MAX_LENGTH ?
                        new ByteArrayInputStream(readPacketSplitInChunks(inputStream, packetLength - 1)) :
                        inputStream);
                } catch (Exception e) {
                    Throwable cause = e instanceof EventDataDeserializationException ? e.getCause() : e;
                    if (cause instanceof EOFException || cause instanceof SocketException) {
                        throw e;
                    }
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
                    // Update the GTID before the event listeners, and the binlog filename/position after the
                    // listeners (since the binlog filename/position point to the *next* event) ...
                    boolean updatedGtid = updateGtid(event);
                    notifyEventListeners(event);
                    updatePosition(event, updatedGtid);
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
                if (completeShutdown) {
                    disconnect(); // initiate complete shutdown sequence (which includes keep alive thread)
                } else {
                    disconnectChannel();
                }
            }
        }
    }

    private byte[] readPacketSplitInChunks(ByteArrayInputStream inputStream, int packetLength) throws IOException {
        byte[] result = inputStream.read(packetLength);
        int chunkLength;
        do {
            chunkLength = inputStream.readInteger(3);
            inputStream.skip(1); // 1 byte for sequence
            result = Arrays.copyOf(result, result.length + chunkLength);
            inputStream.fill(result, result.length - chunkLength, chunkLength);
        } while (chunkLength == Packet.MAX_LENGTH);
        return result;
    }

    private boolean updateGtid(Event event) {
        EventHeader eventHeader = event.getHeader();
        EventType eventType = eventHeader.getEventType();
        if (gtidSet != null) {
            if (eventType == EventType.XID) {
                advanceGTID();
                return true;
            }
            if (eventType == EventType.QUERY) {
                QueryEventData queryEventData = getInternalEventData(event);
                String query = queryEventData.getSql();
                if ("COMMIT".equals(query) || "ROLLBACK".equals(query) ||
                        (previousEvent != null && previousEvent.getHeader().getEventType() == EventType.GTID &&
                                !"BEGIN".equals(query))) {
                    advanceGTID();
                }
                return true;
            }
            if (eventType == EventType.GTID) {
                if (previousGtidEvent != null) {
                    if (advanceGTID()) {
                        if (logger.isLoggable(Level.WARNING)) {
                            logger.log(Level.WARNING, "GtidSet wasn't synchronized before GTID. " +
                                "Please submit a bug report to https://github.com/shyiko/mysql-binlog-connector-java");
                        }
                    }
                }
                previousGtidEvent = event;
                return true;
            }
        }
        return false;
    }

    private void updatePosition(Event event, boolean updatedGtid) {
        EventHeader eventHeader = event.getHeader();
        EventType eventType = eventHeader.getEventType();
        if (updatedGtid) {
            // the GTID was previously updated based upon this event
        } else if (eventType == EventType.ROTATE) {
            RotateEventData rotateEventData = getInternalEventData(event);
            binlogFilename = rotateEventData.getBinlogFilename();
            binlogPosition = rotateEventData.getBinlogPosition();
        } else if (eventType != EventType.TABLE_MAP && eventHeader instanceof EventHeaderV4) {
            // do not update binlogPosition on TABLE_MAP so that in case of reconnect (using a different instance of
            // client) table mapping cache could be reconstructed before hitting row mutation event
            EventHeaderV4 trackableEventHeader = (EventHeaderV4) eventHeader;
            long nextBinlogPosition = trackableEventHeader.getNextPosition();
            if (nextBinlogPosition > 0) {
                binlogPosition = nextBinlogPosition;
            }
        } else
        if (eventType == EventType.MARIA_GTID_EVENT){
            updateMariaGTID(event);
        }
        previousEvent = event;
    }

    private boolean advanceGTID() {
        GtidEventData gtidEventData = getInternalEventData(previousGtidEvent);
        synchronized (gtidSetAccessLock) {
            return gtidSet.add(gtidEventData.getGtid());
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends EventData> T getInternalEventData(Event event) {
        EventData eventData = event.getData();
        if (eventData instanceof EventDeserializer.EventDataWrapper) {
            return (T) ((EventDeserializer.EventDataWrapper) eventData).getInternal();
        } else {
            return (T) eventData;
        }
    }

    private ResultSetRowPacket[] readResultSet() throws IOException {
        List<ResultSetRowPacket> resultSet = new LinkedList<ResultSetRowPacket>();
        byte[] statementResult = channel.read();
        if (statementResult[0] == ErrorPacket.HEADER) {
            ErrorPacket errorPacket = new ErrorPacket(statementResult, 1);
            throw new ServerException(errorPacket.getErrorMessage(), errorPacket.getErrorCode(),
                    errorPacket.getSqlState());
        }
        while ((channel.read())[0] != (byte) 0xFE) { /* skip */ }
        for (byte[] bytes; (bytes = channel.read())[0] != (byte) 0xFE;) {
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
        if (event.getData() instanceof EventDeserializer.EventDataWrapper) {
            event = new Event(event.getHeader(), ((EventDeserializer.EventDataWrapper) event.getData()).getExternal());
        }
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
     * @return Note that this value changes with each received GTID event (provided client is in GTID mode).
     */
    public String getGtid() {
        synchronized (gtidSetAccessLock) {
            if (gtidSet != null) {
                return gtidSet.toString();
            }
            return gtid;
        }
    }

    /**
     * @param gtid For MySQL this is GTID set format, for MariaDB the format is domainId-serverId-sequenceNumber(can be an empty string).
     *             <p>NOTE #1: Any value but null will switch BinaryLogClient into a GTID mode (in which case GTID set will be
     *             updated with each incoming GTID event) as well as set binlogFilename to "" (empty string) (meaning
     *             BinaryLogClient will request events "outside of the set" <u>starting from the oldest known binlog</u>).
     *             <p>NOTE #2: {@link #setBinlogFilename(String)} and {@link #setBinlogPosition(long)} can be used to specify the
     *             exact position from which MySQL server should start streaming events (taking into account GTID set).
     * @see #getGtid()
     */
    public BinaryLogClient setGtid(String gtid) {
        if (gtid != null && this.binlogFilename == null) {
            this.binlogFilename = "";
        }
        synchronized (gtidSetAccessLock) {
            this.gtid = gtid;
        }
        return this;
    }

    // region MariaDB
    public boolean isMariaDB() {
        return mariaDB;
    }

    private void updateMariaGTID(Event event) {
        EventHeader eventHeader = event.getHeader();
        if (eventHeader.getEventType() == EventType.MARIA_GTID_EVENT) {
            synchronized (gtidSetAccessLock) {
                if (mariaGtid != null) {
                    MariaGtidEventData eventData = event.getData();
                    mariaGtid.setDomainId(eventData.getDomainId());
                    mariaGtid.setSequenceNumber(eventData.getSequenceNumber());
                    gtid = mariaGtid.toString();
                }
            }
        }
    }

    private DumpBinaryLogCommand requestMariaBinaryLogStream() throws IOException {
        if ("gtid_current_pos".equals(gtid) || "".equals(gtid)||gtid==null) {
            channel.write(new QueryCommand("select @@gtid_current_pos"));
            ResultSetRowPacket[] rs = readResultSet();
            gtid = rs[0].getValue(0);
            logger.fine("Use server current gtid position "+gtid);
        }

        // update server id
        channel.write(new QueryCommand("SHOW VARIABLES LIKE 'SERVER_ID'"));
        ResultSetRowPacket[] rs = readResultSet();
        long serverId = Long.parseLong(rs[0].getValue(1));
        // If we got multi gtid, chose the gtid for current server
        String[] split = gtid.split(",");
        for (String s : split) {
            Gtid g = new Gtid(s);
            if (g.getServerId() == serverId) {
                mariaGtid = g;
                gtid = mariaGtid.toString();
                logger.fine("Chose gtid "+gtid+" for this server");
            }
        }

        // set up gtid
        channel.write(new QueryCommand("SET @mariadb_slave_capability = 4"));// support GTID
        channel.read();// ignore
        channel.write(new QueryCommand("SET @slave_connect_state = '" + gtid + "'"));
        channel.read();// ignore
        channel.write(new QueryCommand("SET @slave_gtid_strict_mode = 0"));
        channel.read();// ignore
        channel.write(new QueryCommand("SET @slave_gtid_ignore_duplicates = 0"));
        channel.read();// ignore
        // Register First
        Command command = new RegisterSlaveCommand(serverId, "", "", "", 0, 0, 0);
        channel.write(command);
        channel.read();// ignore

        // MariaDB Event
        eventDeserializer.setEventDataDeserializer(EventType.MARIA_GTID_EVENT, new GtidDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.MARIA_GTID_LIST_EVENT, new GtidListDeserializer());
        eventDeserializer.setEventDataDeserializer(EventType.MARIA_BINLOG_CHECKPOINT_EVENT, new BinlogCheckpointDeserializer());

        return new DumpBinaryLogCommand(serverId, binlogFilename, binlogPosition);
    }
    // endregion
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

        public void onConnect(BinaryLogClient client) { }

        public void onCommunicationFailure(BinaryLogClient client, Exception ex) { }

        public void onEventDeserializationFailure(BinaryLogClient client, Exception ex) { }

        public void onDisconnect(BinaryLogClient client) { }

    }

}
