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
import com.github.shyiko.mysql.binlog.event.GtidEventData;
import com.github.shyiko.mysql.binlog.event.QueryEventData;
import com.github.shyiko.mysql.binlog.event.RotateEventData;
import com.github.shyiko.mysql.binlog.event.deserialization.ChecksumType;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDataDeserializationException;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDataDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer.EventDataWrapper;
import com.github.shyiko.mysql.binlog.event.deserialization.GtidEventDataDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.QueryEventDataDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.RotateEventDataDeserializer;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;
import com.github.shyiko.mysql.binlog.jmx.BinaryLogClientMXBean;
import com.github.shyiko.mysql.binlog.network.AuthenticationException;
import com.github.shyiko.mysql.binlog.network.ClientCapabilities;
import com.github.shyiko.mysql.binlog.network.DefaultSSLSocketFactory;
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
import com.github.shyiko.mysql.binlog.network.protocol.command.AuthenticateCommand;
import com.github.shyiko.mysql.binlog.network.protocol.command.AuthenticateNativePasswordCommand;
import com.github.shyiko.mysql.binlog.network.protocol.command.Command;
import com.github.shyiko.mysql.binlog.network.protocol.command.DumpBinaryLogCommand;
import com.github.shyiko.mysql.binlog.network.protocol.command.DumpBinaryLogGtidCommand;
import com.github.shyiko.mysql.binlog.network.protocol.command.PingCommand;
import com.github.shyiko.mysql.binlog.network.protocol.command.QueryCommand;
import com.github.shyiko.mysql.binlog.network.protocol.command.SSLRequestCommand;

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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
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

    // https://dev.mysql.com/doc/internals/en/sending-more-than-16mbyte.html
    private static final int MAX_PACKET_LENGTH = 16777215;

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

    private GtidSet gtidSet;
    private final Object gtidSetAccessLock = new Object();
    private boolean gtidSetFallbackToPurged;
    private boolean useBinlogFilenamePositionInGtidMode;
    private String gtid;
    private boolean tx;

    private EventDeserializer eventDeserializer = new EventDeserializer();

    private final List<EventListener> eventListeners = new CopyOnWriteArrayList<EventListener>();
    private final List<LifecycleListener> lifecycleListeners = new CopyOnWriteArrayList<LifecycleListener>();

    private SocketFactory socketFactory;
    private SSLSocketFactory sslSocketFactory;

    private volatile PacketChannel channel;
    private volatile boolean connected;

    private ThreadFactory threadFactory;

    private boolean keepAlive = true;
    private long keepAliveInterval = TimeUnit.MINUTES.toMillis(1);

    private long heartbeatInterval;
    private volatile long eventLastSeen;

    private long connectTimeout = TimeUnit.SECONDS.toMillis(3);

    private volatile ExecutorService keepAliveThreadExecutor;

    private final Lock connectLock = new ReentrantLock();
    private final Lock keepAliveThreadExecutorLock = new ReentrantLock();

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
     * <p>NOTE #1: Any value but null will switch BinaryLogClient into a GTID mode (this will also set binlogFilename
     * to "" (provided it's null) forcing MySQL to send events starting from the oldest known binlog (keep in mind
     * that connection will fail if gtid_purged is anything but empty (unless
     * {@link #setGtidSetFallbackToPurged(boolean)} is set to true))).
     * <p>NOTE #2: GTID set is automatically updated with each incoming GTID event (provided GTID mode is on).
     * @see #getGtidSet()
     * @see #setGtidSetFallbackToPurged(boolean)
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
     * @see #setGtidSetFallbackToPurged(boolean)
     */
    public boolean isGtidSetFallbackToPurged() {
        return gtidSetFallbackToPurged;
    }

    /**
     * @param gtidSetFallbackToPurged true if gtid_purged should be used as a fallback when gtidSet is set to "" and
     * MySQL server has purged some of the binary logs, false otherwise (default).
     */
    public void setGtidSetFallbackToPurged(boolean gtidSetFallbackToPurged) {
        this.gtidSetFallbackToPurged = gtidSetFallbackToPurged;
    }

    /**
     * @see #setUseBinlogFilenamePositionInGtidMode(boolean)
     */
    public boolean isUseBinlogFilenamePositionInGtidMode() {
        return useBinlogFilenamePositionInGtidMode;
    }

    /**
     * @param useBinlogFilenamePositionInGtidMode true if MySQL server should start streaming events from a given
     * {@link #getBinlogFilename()} and {@link #getBinlogPosition()} instead of "the oldest known binlog" when
     * {@link #getGtidSet()} is set, false otherwise (default).
     */
    public void setUseBinlogFilenamePositionInGtidMode(boolean useBinlogFilenamePositionInGtidMode) {
        this.useBinlogFilenamePositionInGtidMode = useBinlogFilenamePositionInGtidMode;
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
     * @see #setKeepAliveInterval(long)
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
     * @see #setHeartbeatInterval(long)
     */
    public void setKeepAliveInterval(long keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    /**
     * @return "keep alive" connect timeout in milliseconds.
     * @see #setKeepAliveConnectTimeout(long)
     *
     * @deprecated in favour of {@link #getConnectTimeout()}
     */
    public long getKeepAliveConnectTimeout() {
        return connectTimeout;
    }

    /**
     * @param connectTimeout "keep alive" connect timeout in milliseconds.
     * @see #getKeepAliveConnectTimeout()
    *
     * @deprecated in favour of {@link #setConnectTimeout(long)}
     */
    public void setKeepAliveConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * @return heartbeat period in milliseconds (0 if not set (default)).
     * @see #setHeartbeatInterval(long)
     */
    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    /**
     * @param heartbeatInterval heartbeat period in milliseconds.
     * <p>
     * If set (recommended)
     * <ul>
     * <li> HEARTBEAT event will be emitted every "heartbeatInterval".
     * <li> if {@link #setKeepAlive(boolean)} is on then keepAlive thread will attempt to reconnect if no
     *   HEARTBEAT events were received within {@link #setKeepAliveInterval(long)} (instead of trying to send
     *   PING every {@link #setKeepAliveInterval(long)}, which is fundamentally flawed -
     *   https://github.com/shyiko/mysql-binlog-connector-java/issues/118).
     * </ul>
     * Note that when used together with keepAlive heartbeatInterval MUST be set less than keepAliveInterval.
     *
     * @see #getHeartbeatInterval()
     */
    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    /**
     * @return connect timeout in milliseconds, 3 seconds by default.
     * @see #setConnectTimeout(long)
     */
    public long getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * @param connectTimeout connect timeout in milliseconds.
     * @see #getConnectTimeout()
     */
    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
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
        if (!connectLock.tryLock()) {
            throw new IllegalStateException("BinaryLogClient is already connected");
        }
        boolean notifyWhenDisconnected = false;
        try {
            Callable cancelDisconnect = null;
            try {
                try {
                    long start = System.currentTimeMillis();
                    channel = openChannel();
                    if (connectTimeout > 0 && !isKeepAliveThreadRunning()) {
                        cancelDisconnect = scheduleDisconnectIn(connectTimeout -
                            (System.currentTimeMillis() - start));
                    }
                    if (channel.getInputStream().peek() == -1) {
                        throw new EOFException();
                    }
                } catch (IOException e) {
                    throw new IOException("Failed to connect to MySQL on " + hostname + ":" + port +
                        ". Please make sure it's running.", e);
                }
                GreetingPacket greetingPacket = receiveGreeting();
                authenticate(greetingPacket);
                connectionId = greetingPacket.getThreadId();
                if ("".equals(binlogFilename)) {
                    synchronized (gtidSetAccessLock) {
                        if (gtidSet != null && "".equals(gtidSet.toString()) && gtidSetFallbackToPurged) {
                            gtidSet = new GtidSet(fetchGtidPurged());
                        }
                    }
                }
                if (binlogFilename == null) {
                    fetchBinlogFilenameAndPosition();
                }
                if (binlogPosition < 4) {
                    if (logger.isLoggable(Level.WARNING)) {
                        logger.warning("Binary log position adjusted from " + binlogPosition + " to " + 4);
                    }
                    binlogPosition = 4;
                }
                ChecksumType checksumType = fetchBinlogChecksum();
                if (checksumType != ChecksumType.NONE) {
                    confirmSupportOfChecksum(checksumType);
                }
                if (heartbeatInterval > 0) {
                    enableHeartbeat();
                }
                gtid = null;
                tx = false;
                requestBinaryLogStream();
            } catch (IOException e) {
                disconnectChannel();
                throw e;
            } finally {
                if (cancelDisconnect != null) {
                    try {
                        cancelDisconnect.call();
                    } catch (Exception e) {
                        if (logger.isLoggable(Level.WARNING)) {
                            logger.warning("\"" + e.getMessage() +
                                "\" was thrown while canceling scheduled disconnect call");
                        }
                    }
                }
            }
            connected = true;
            notifyWhenDisconnected = true;
            if (logger.isLoggable(Level.INFO)) {
                String position;
                synchronized (gtidSetAccessLock) {
                    position = gtidSet != null ? gtidSet.toString() : binlogFilename + "/" + binlogPosition;
                }
                logger.info("Connected to " + hostname + ":" + port + " at " + position +
                    " (" + (blocking ? "sid:" + serverId + ", " : "") + "cid:" + connectionId + ")");
            }
            for (LifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.onConnect(this);
            }
            if (keepAlive && !isKeepAliveThreadRunning()) {
                spawnKeepAliveThread();
            }
            ensureEventDataDeserializer(EventType.ROTATE, RotateEventDataDeserializer.class);
            synchronized (gtidSetAccessLock) {
                if (gtidSet != null) {
                    ensureEventDataDeserializer(EventType.GTID, GtidEventDataDeserializer.class);
                    ensureEventDataDeserializer(EventType.QUERY, QueryEventDataDeserializer.class);
                }
            }
            listenForEventPackets();
        } finally {
            connectLock.unlock();
            if (notifyWhenDisconnected) {
                for (LifecycleListener lifecycleListener : lifecycleListeners) {
                    lifecycleListener.onDisconnect(this);
                }
            }
        }
    }

    private PacketChannel openChannel() throws IOException {
        Socket socket = socketFactory != null ? socketFactory.createSocket() : new Socket();
        socket.connect(new InetSocketAddress(hostname, port), (int) connectTimeout);
        return new PacketChannel(socket);
    }

    private Callable scheduleDisconnectIn(final long timeout) {
        final BinaryLogClient self = this;
        final CountDownLatch connectLatch = new CountDownLatch(1);
        final Thread thread = newNamedThread(new Runnable() {
            @Override
            public void run() {
                try {
                    connectLatch.await(timeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    if (logger.isLoggable(Level.WARNING)) {
                        logger.log(Level.WARNING, e.getMessage());
                    }
                }
                if (connectLatch.getCount() != 0) {
                    if (logger.isLoggable(Level.WARNING)) {
                        logger.warning("Failed to establish connection in " + timeout + "ms. " +
                            "Forcing disconnect.");
                    }
                    try {
                        self.disconnectChannel();
                    } catch (IOException e) {
                        if (logger.isLoggable(Level.WARNING)) {
                            logger.log(Level.WARNING, e.getMessage());
                        }
                    }
                }
            }
        }, "blc-disconnect-" + hostname + ":" + port);
        thread.start();
        return new Callable() {

            public Object call() throws Exception {
                connectLatch.countDown();
                thread.join();
                return null;
            }
        };
    }

    private GreetingPacket receiveGreeting() throws IOException {
        byte[] initialHandshakePacket = channel.read();
        if (initialHandshakePacket[0] == (byte) 0xFF /* error */) {
            byte[] bytes = Arrays.copyOfRange(initialHandshakePacket, 1, initialHandshakePacket.length);
            ErrorPacket errorPacket = new ErrorPacket(bytes);
            throw new ServerException(errorPacket.getErrorMessage(), errorPacket.getErrorCode(),
                    errorPacket.getSqlState());
        }
        return new GreetingPacket(initialHandshakePacket);
    }

    private void enableHeartbeat() throws IOException {
        channel.write(new QueryCommand("set @master_heartbeat_period=" + heartbeatInterval * 1000000));
        byte[] statementResult = channel.read();
        if (statementResult[0] == (byte) 0xFF /* error */) {
            byte[] bytes = Arrays.copyOfRange(statementResult, 1, statementResult.length);
            ErrorPacket errorPacket = new ErrorPacket(bytes);
            throw new ServerException(errorPacket.getErrorMessage(), errorPacket.getErrorCode(),
                errorPacket.getSqlState());
        }
    }

    private void requestBinaryLogStream() throws IOException {
        long serverId = blocking ? this.serverId : 0; // http://bugs.mysql.com/bug.php?id=71178
        Command dumpBinaryLogCommand;
        synchronized (gtidSetAccessLock) {
            if (gtidSet != null) {
                dumpBinaryLogCommand = new DumpBinaryLogGtidCommand(serverId,
                    useBinlogFilenamePositionInGtidMode ? binlogFilename : "",
                    useBinlogFilenamePositionInGtidMode ? binlogPosition : 4,
                    gtidSet);
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
            eventDataDeserializer.getClass() != EventDataWrapper.Deserializer.class) {
            EventDataDeserializer internalEventDataDeserializer;
            try {
                internalEventDataDeserializer = eventDataDeserializerClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            eventDeserializer.setEventDataDeserializer(eventType,
                new EventDataWrapper.Deserializer(internalEventDataDeserializer,
                    eventDataDeserializer));
        }
    }

    private void authenticate(GreetingPacket greetingPacket) throws IOException {
        int collation = greetingPacket.getServerCollation();
        int packetNumber = 1;

        boolean usingSSLSocket = false;
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
                SSLSocketFactory sslSocketFactory =
                    this.sslSocketFactory != null ?
                        this.sslSocketFactory :
                        sslMode == SSLMode.REQUIRED || sslMode == SSLMode.PREFERRED ?
                            DEFAULT_REQUIRED_SSL_MODE_SOCKET_FACTORY :
                            DEFAULT_VERIFY_CA_SSL_MODE_SOCKET_FACTORY;
                channel.upgradeToSSL(sslSocketFactory,
                    sslMode == SSLMode.VERIFY_IDENTITY ? new TLSHostnameVerifier() : null);
                usingSSLSocket = true;
            }
        }
        AuthenticateCommand authenticateCommand = new AuthenticateCommand(schema, username, password,
            greetingPacket.getScramble());
        authenticateCommand.setCollation(collation);
        channel.write(authenticateCommand, packetNumber);
        byte[] authenticationResult = channel.read();
        if (authenticationResult[0] != (byte) 0x00 /* ok */) {
            if (authenticationResult[0] == (byte) 0xFF /* error */) {
                byte[] bytes = Arrays.copyOfRange(authenticationResult, 1, authenticationResult.length);
                ErrorPacket errorPacket = new ErrorPacket(bytes);
                throw new AuthenticationException(errorPacket.getErrorMessage(), errorPacket.getErrorCode(),
                    errorPacket.getSqlState());
            } else if (authenticationResult[0] == (byte) 0xFE) {
                switchAuthentication(authenticationResult, usingSSLSocket);
            } else {
                throw new AuthenticationException("Unexpected authentication result (" + authenticationResult[0] + ")");
            }
        }
    }

    private void switchAuthentication(byte[] authenticationResult, boolean usingSSLSocket) throws IOException {
        /*
            Azure-MySQL likes to tell us to switch authentication methods, even though
            we haven't advertised that we support any.  It uses this for some-odd
            reason to send the real password scramble.
        */
        ByteArrayInputStream buffer = new ByteArrayInputStream(authenticationResult);
        buffer.read(1);

        String authName = buffer.readZeroTerminatedString();
        if ("mysql_native_password".equals(authName)) {
            String scramble = buffer.readZeroTerminatedString();

            Command switchCommand = new AuthenticateNativePasswordCommand(scramble, password);
            channel.write(switchCommand, (usingSSLSocket ? 4 : 3));
            byte[] authResult = channel.read();

            if (authResult[0] != (byte) 0x00) {
                byte[] bytes = Arrays.copyOfRange(authResult, 1, authResult.length);
                ErrorPacket errorPacket = new ErrorPacket(bytes);
                throw new AuthenticationException(errorPacket.getErrorMessage(), errorPacket.getErrorCode(),
                    errorPacket.getSqlState());
            }
        } else {
            throw new AuthenticationException("Unsupported authentication type: " + authName);
        }
    }

    private void spawnKeepAliveThread() {
        final ExecutorService threadExecutor =
            Executors.newSingleThreadExecutor(new ThreadFactory() {

                @Override
                public Thread newThread(Runnable runnable) {
                    return newNamedThread(runnable, "blc-keepalive-" + hostname + ":" + port);
                }
            });
        try {
            keepAliveThreadExecutorLock.lock();
            threadExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    while (!threadExecutor.isShutdown()) {
                        try {
                            Thread.sleep(keepAliveInterval);
                        } catch (InterruptedException e) {
                            // expected in case of disconnect
                        }
                        if (threadExecutor.isShutdown()) {
                            return;
                        }
                        boolean connectionLost = false;
                        if (heartbeatInterval > 0) {
                            connectionLost = System.currentTimeMillis() - eventLastSeen > keepAliveInterval;
                        } else {
                            try {
                                channel.write(new PingCommand());
                            } catch (IOException e) {
                                connectionLost = true;
                            }
                        }
                        if (connectionLost) {
                            if (logger.isLoggable(Level.INFO)) {
                                logger.info("Trying to restore lost connection to " + hostname + ":" + port);
                            }
                            try {
                                terminateConnect();
                                connect(connectTimeout);
                            } catch (Exception ce) {
                                if (logger.isLoggable(Level.WARNING)) {
                                    logger.warning("Failed to restore connection to " + hostname + ":" + port +
                                        ". Next attempt in " + keepAliveInterval + "ms");
                                }
                            }
                        }
                    }
                }
            });
            keepAliveThreadExecutor = threadExecutor;
        } finally {
            keepAliveThreadExecutorLock.unlock();
        }
    }

    private Thread newNamedThread(Runnable runnable, String threadName) {
        Thread thread = threadFactory == null ? new Thread(runnable) : threadFactory.newThread(runnable);
        thread.setName(threadName);
        return thread;
    }

    boolean isKeepAliveThreadRunning() {
        try {
            keepAliveThreadExecutorLock.lock();
            return keepAliveThreadExecutor != null && !keepAliveThreadExecutor.isShutdown();
        } finally {
            keepAliveThreadExecutorLock.unlock();
        }
    }

    /**
     * Connect to the replication stream in a separate thread.
     * @param timeout timeout in milliseconds
     * @throws AuthenticationException if authentication fails
     * @throws ServerException if MySQL server responds with an error
     * @throws IOException if anything goes wrong while trying to connect
     * @throws TimeoutException if client was unable to connect within given time limit
     */
    public void connect(final long timeout) throws IOException, TimeoutException {
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
                    setConnectTimeout(timeout);
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
            started = countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
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
            try {
                terminateConnect();
            } finally {
                throw new TimeoutException("BinaryLogClient was unable to connect in " + timeout + "ms");
            }
        }
    }

    /**
     * @return true if client is connected, false otherwise
     */
    public boolean isConnected() {
        return connected;
    }

    private String fetchGtidPurged() throws IOException {
        channel.write(new QueryCommand("show global variables like 'gtid_purged'"));
        ResultSetRowPacket[] resultSet = readResultSet();
        if (resultSet.length != 0) {
            return resultSet[0].getValue(1).toUpperCase();
        }
        return "";
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
            ErrorPacket errorPacket = new ErrorPacket(bytes);
            throw new ServerException(errorPacket.getErrorMessage(), errorPacket.getErrorCode(),
                errorPacket.getSqlState());
        }
        eventDeserializer.setChecksumType(checksumType);
    }

    private void listenForEventPackets() throws IOException {
        ByteArrayInputStream inputStream = channel.getInputStream();
        boolean completeShutdown = false;
        try {
            while (inputStream.peek() != -1) {
                int packetLength = inputStream.readInteger(3);
                inputStream.skip(1); // 1 byte for sequence
                int marker = inputStream.read();
                if (marker == 0xFF) {
                    ErrorPacket errorPacket = new ErrorPacket(inputStream.read(packetLength - 1));
                    throw new ServerException(errorPacket.getErrorMessage(), errorPacket.getErrorCode(),
                        errorPacket.getSqlState());
                }
                if (marker == 0xFE && !blocking) {
                    completeShutdown = true;
                    break;
                }
                Event event;
                try {
                    event = eventDeserializer.nextEvent(packetLength == MAX_PACKET_LENGTH ?
                        new ByteArrayInputStream(readPacketSplitInChunks(inputStream, packetLength - 1)) :
                        inputStream);
                    if (event == null) {
                        throw new EOFException();
                    }
                } catch (Exception e) {
                    Throwable cause = e instanceof EventDataDeserializationException ? e.getCause() : e;
                    if (cause instanceof EOFException || cause instanceof SocketException) {
                        throw e;
                    }
                    if (isConnected()) {
                        for (LifecycleListener lifecycleListener : lifecycleListeners) {
                            lifecycleListener.onEventDeserializationFailure(this, e);
                        }
                    }
                    continue;
                }
                if (isConnected()) {
                    eventLastSeen = System.currentTimeMillis();
                    updateGtidSet(event);
                    notifyEventListeners(event);
                    updateClientBinlogFilenameAndPosition(event);
                }
            }
        } catch (Exception e) {
            if (isConnected()) {
                for (LifecycleListener lifecycleListener : lifecycleListeners) {
                    lifecycleListener.onCommunicationFailure(this, e);
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

    private void updateClientBinlogFilenameAndPosition(Event event) {
        EventHeader eventHeader = event.getHeader();
        EventType eventType = eventHeader.getEventType();
        if (eventType == EventType.ROTATE) {
            RotateEventData rotateEventData = (RotateEventData) EventDataWrapper.internal(event.getData());
            binlogFilename = rotateEventData.getBinlogFilename();
            binlogPosition = rotateEventData.getBinlogPosition();
        } else
        // do not update binlogPosition on TABLE_MAP so that in case of reconnect (using a different instance of
        // client) table mapping cache could be reconstructed before hitting row mutation event
        if (eventType != EventType.TABLE_MAP && eventHeader instanceof EventHeaderV4) {
            EventHeaderV4 trackableEventHeader = (EventHeaderV4) eventHeader;
            long nextBinlogPosition = trackableEventHeader.getNextPosition();
            if (nextBinlogPosition > 0) {
                binlogPosition = nextBinlogPosition;
            }
        }
    }

    private void updateGtidSet(Event event) {
        synchronized (gtidSetAccessLock) {
            if (gtidSet == null) {
                return;
            }
        }
        EventHeader eventHeader = event.getHeader();
        switch(eventHeader.getEventType()) {
            case GTID:
                GtidEventData gtidEventData = (GtidEventData) EventDataWrapper.internal(event.getData());
                gtid = gtidEventData.getGtid();
                break;
            case XID:
                commitGtid();
                tx = false;
                break;
            case QUERY:
                QueryEventData queryEventData = (QueryEventData) EventDataWrapper.internal(event.getData());
                String sql = queryEventData.getSql();
                if (sql == null) {
                    break;
                }
                if ("BEGIN".equals(sql)) {
                    tx = true;
                } else
                if ("COMMIT".equals(sql) || "ROLLBACK".equals(sql)) {
                    commitGtid();
                    tx = false;
                } else
                if (!tx) {
                    // auto-commit query, likely DDL
                    commitGtid();
                }
            default:
        }
    }

    private void commitGtid() {
        if (gtid != null) {
            synchronized (gtidSetAccessLock) {
                gtidSet.add(gtid);
            }
        }
    }

    private ResultSetRowPacket[] readResultSet() throws IOException {
        List<ResultSetRowPacket> resultSet = new LinkedList<ResultSetRowPacket>();
        byte[] statementResult = channel.read();
        if (statementResult[0] == (byte) 0xFF /* error */) {
            byte[] bytes = Arrays.copyOfRange(statementResult, 1, statementResult.length);
            ErrorPacket errorPacket = new ErrorPacket(bytes);
            throw new ServerException(errorPacket.getErrorMessage(), errorPacket.getErrorCode(),
                    errorPacket.getSqlState());
        }
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
        eventListeners.add(eventListener);
    }

    /**
     * Unregister all event listener of specific type.
     */
    public void unregisterEventListener(Class<? extends EventListener> listenerClass) {
        for (EventListener eventListener: eventListeners) {
            if (listenerClass.isInstance(eventListener)) {
                eventListeners.remove(eventListener);
            }
        }
    }

    /**
     * Unregister single event listener.
     */
    public void unregisterEventListener(EventListener eventListener) {
        eventListeners.remove(eventListener);
    }

    private void notifyEventListeners(Event event) {
        if (event.getData() instanceof EventDataWrapper) {
            event = new Event(event.getHeader(), ((EventDataWrapper) event.getData()).getExternal());
        }
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
        lifecycleListeners.add(lifecycleListener);
    }

    /**
     * Unregister all lifecycle listener of specific type.
     */
    public void unregisterLifecycleListener(Class<? extends LifecycleListener> listenerClass) {
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            if (listenerClass.isInstance(lifecycleListener)) {
                lifecycleListeners.remove(lifecycleListener);
            }
        }
    }

    /**
     * Unregister single lifecycle listener.
     */
    public void unregisterLifecycleListener(LifecycleListener eventListener) {
        lifecycleListeners.remove(eventListener);
    }

    /**
     * Disconnect from the replication stream.
     * Note that this does not cause binlogFilename/binlogPosition to be cleared out.
     * As the result following {@link #connect()} resumes client from where it left off.
     */
    public void disconnect() throws IOException {
        terminateKeepAliveThread();
        terminateConnect();
    }

    private void terminateKeepAliveThread() {
        try {
            keepAliveThreadExecutorLock.lock();
            ExecutorService keepAliveThreadExecutor = this.keepAliveThreadExecutor;
            if (keepAliveThreadExecutor == null) {
                return;
            }
            keepAliveThreadExecutor.shutdownNow();
            while (!awaitTerminationInterruptibly(keepAliveThreadExecutor,
                Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                // ignore
            }
        } finally {
            keepAliveThreadExecutorLock.unlock();
        }
    }

    private static boolean awaitTerminationInterruptibly(ExecutorService executorService, long timeout, TimeUnit unit) {
        try {
            return executorService.awaitTermination(timeout, unit);
        } catch (InterruptedException e) {
            return false;
        }
    }

    private void terminateConnect() throws IOException {
        do {
            disconnectChannel();
        } while (!tryLockInterruptibly(connectLock, 1000, TimeUnit.MILLISECONDS));
        connectLock.unlock();
    }

    private static boolean tryLockInterruptibly(Lock lock, long time, TimeUnit unit) {
        try {
            return lock.tryLock(time, unit);
        } catch (InterruptedException e) {
            return false;
        }
    }

    private void disconnectChannel() throws IOException {
        connected = false;
        if (channel != null && channel.isOpen()) {
            channel.close();
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

        public void onConnect(BinaryLogClient client) { }

        public void onCommunicationFailure(BinaryLogClient client, Exception ex) { }

        public void onEventDeserializationFailure(BinaryLogClient client, Exception ex) { }

        public void onDisconnect(BinaryLogClient client) { }

    }

}
