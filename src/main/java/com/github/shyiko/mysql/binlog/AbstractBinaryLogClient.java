package com.github.shyiko.mysql.binlog;

import com.github.shyiko.mysql.binlog.event.*;
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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Luis Casillas
 */
public abstract class AbstractBinaryLogClient implements BinaryLogClientMXBean {
    protected final String hostname;
    protected final int port;
    protected final String schema;
    protected final String username;
    protected final String password;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Lock shutdownLock = new ReentrantLock();
    private long serverId = 65535;
    private volatile String binlogFilename;
    private volatile long binlogPosition;
    private EventDeserializer eventDeserializer = new EventDeserializer();
    private SocketFactory socketFactory;
    private PacketChannel channel;
    private volatile boolean connected;
    private boolean keepAlive = true;
    private long keepAliveInterval = TimeUnit.MINUTES.toMillis(1);
    private long keepAliveConnectTimeout = TimeUnit.SECONDS.toMillis(3);
    private volatile ThreadPoolExecutor keepAliveThreadExecutor;
    private long keepAliveThreadShutdownTimeout = TimeUnit.SECONDS.toMillis(6);


    public AbstractBinaryLogClient(String username, int port, String hostname, String password, String schema) {
        this.username = username;
        this.port = port;
        this.hostname = hostname;
        this.password = password;
        this.schema = schema;
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
     * is subject to change (in response to {@link com.github.shyiko.mysql.binlog.event.EventType#ROTATE}, for example).
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
     * Connect to the replication stream. Note that this method blocks until disconnected.
     * @throws com.github.shyiko.mysql.binlog.network.AuthenticationException in case of failed authentication
     * @throws java.io.IOException if anything goes wrong while trying to connect
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
        onConnect();
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
                        onEventDeserializationFailure(e);
                    }
                    continue;
                }
                if (isConnected()) {
                    notifyEventListener(event);
                    updateClientBinlogFilenameAndPosition(event);
                }
            }
        } catch (Exception e) {
            if (isConnected()) {
                onCommunicationFailure(e);
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

    private void notifyEventListener(Event event) {
        onEvent(event);
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
            onDisconnect();
        }
    }

    /**
     * Invoked once for each {@link Event}, in the order they are processed.
     */
    protected abstract void onEvent(Event event);

    /**
     * Invoked when a connection is established.
     */
    protected abstract void onConnect();

    /**
     * It's guarantied to be called before {@link #onDisconnect()}) in case of communication failure.
     */
    protected abstract void onCommunicationFailure(Exception ex);

    /**
     * Called in case of failed event deserialization. Note this type of error does NOT cause client to
     * disconnect. If you wish to stop receiving events you'll need to fire client.disconnect() manually.
     */
    protected abstract void onEventDeserializationFailure(Exception ex);

    /**
     * Called upon disconnect (regardless of the reason).
     */
    protected abstract void onDisconnect();

}
