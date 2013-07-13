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

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class TCPReverseProxy {

    private final Logger logger = Logger.getLogger(getClass().getSimpleName());

    private final int port;
    private final String targetHost;
    private final int targetPort;

    @SuppressWarnings("unchecked")
    private Set<Closeable> closeable = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap()));
    private volatile Selector selector;
    private volatile CountDownLatch latch;

    public TCPReverseProxy(int port, int targetPort) {
        this(port, "localhost", targetPort);
    }

    public TCPReverseProxy(int port, String targetHost, int targetPort) {
        this.port = port;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        resetInternalState();
    }

    public int getPort() {
        return port;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void bind() throws IOException {
        if (!closeable.isEmpty()) {
            throw new IllegalStateException();
        }
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        closeable.add(serverSocketChannel);
        selector = Selector.open();
        serverSocketChannel.configureBlocking(false).register(selector, SelectionKey.OP_ACCEPT);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("Listening on port " + port);
        }
        latch.countDown();
        selector.select();
        for (; selector.isOpen(); selector.select()) {
            Set<SelectionKey> keys = selector.selectedKeys();
            for (SelectionKey key : keys) {
                if (!key.isValid()) {
                    continue;
                }
                if (key.isAcceptable()) {
                    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                    SocketChannel clientSocketChannel = channel.accept();
                    closeable.add(clientSocketChannel);
                    InetSocketAddress remoteAddress = new InetSocketAddress(targetHost, targetPort);
                    SocketChannel remoteSocketChannel = SocketChannel.open(remoteAddress);
                    closeable.add(remoteSocketChannel);
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest("Established new connection " + System.identityHashCode(remoteSocketChannel) +
                                " to " + targetHost + ":" + targetPort);
                    }
                    clientSocketChannel.configureBlocking(false).
                            register(selector, SelectionKey.OP_READ, remoteSocketChannel);
                    remoteSocketChannel.configureBlocking(false).
                            register(selector, SelectionKey.OP_READ, clientSocketChannel);
                } else if (key.isReadable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    SocketChannel targetChannel = (SocketChannel) key.attachment();
                    try {
                        int x = channel.read(buffer);
                        if (x == -1) {
                            if (logger.isLoggable(Level.FINEST)) {
                                logger.finest("Closed connection " + System.identityHashCode(targetChannel));
                            }
                            closeQuietly(targetChannel, channel);
                            continue;
                        }
                        buffer.flip();
                        targetChannel.write(buffer);
                        buffer.rewind();
                    } catch (IOException e) {
                        closeQuietly(targetChannel, channel);
                    }
                }
            }
            keys.clear();
        }
    }

    public void await(long timeout, TimeUnit unit) throws InterruptedException {
        latch.await(timeout, unit);
    }

    public void unbind() throws IOException {
        try {
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        closeQuietly(closeable.toArray(new Closeable[closeable.size()]));
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("Released port " + port);
        }
        resetInternalState();
    }

    private void closeQuietly(Closeable... arrayOfCloseable) {
        for (Closeable closeable : arrayOfCloseable) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.closeable.remove(closeable);
        }
    }

    private void resetInternalState() {
        latch = new CountDownLatch(1);
    }

}