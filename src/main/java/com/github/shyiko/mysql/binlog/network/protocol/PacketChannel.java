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
package com.github.shyiko.mysql.binlog.network.protocol;

import com.github.shyiko.mysql.binlog.io.BufferedSocketInputStream;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;
import com.github.shyiko.mysql.binlog.io.ByteArrayOutputStream;
import com.github.shyiko.mysql.binlog.network.IdentityVerificationException;
import com.github.shyiko.mysql.binlog.network.SSLSocketFactory;
import com.github.shyiko.mysql.binlog.network.protocol.command.Command;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.Channel;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class PacketChannel implements Channel {

    private Socket socket;
    private ByteArrayInputStream inputStream;
    private ByteArrayOutputStream outputStream;

    public PacketChannel(String hostname, int port) throws IOException {
        this(new Socket(hostname, port));
    }

    public PacketChannel(Socket socket) throws IOException {
        this.socket = socket;
        this.inputStream = new ByteArrayInputStream(new BufferedSocketInputStream(socket.getInputStream()));
        this.outputStream = new ByteArrayOutputStream(socket.getOutputStream());
    }

    public ByteArrayInputStream getInputStream() {
        return inputStream;
    }

    public ByteArrayOutputStream getOutputStream() {
        return outputStream;
    }

    public byte[] read() throws IOException {
        int length = inputStream.readInteger(3);
        inputStream.skip(1); //sequence
        return inputStream.read(length);
    }

    public void write(Command command, int packetNumber) throws IOException {
        byte[] body = command.toByteArray();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.writeInteger(body.length, 3); // packet length
        buffer.writeInteger(packetNumber, 1);
        buffer.write(body, 0, body.length);
        outputStream.write(buffer.toByteArray());
        // though it has no effect in case of default (underlying) output stream (SocketOutputStream),
        // it may be necessary in case of non-default one
        outputStream.flush();
    }

    /**
     * @deprecated use {@link #write(Command, int)} instead
     */
    @Deprecated
    public void writeBuffered(Command command, int packetNumber) throws IOException {
        write(command, packetNumber);
    }

    public void write(Command command) throws IOException {
        write(command, 0);
    }

    public void upgradeToSSL(SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier) throws IOException {
        SSLSocket sslSocket = sslSocketFactory.createSocket(this.socket);
        sslSocket.startHandshake();
        socket = sslSocket;
        inputStream = new ByteArrayInputStream(sslSocket.getInputStream());
        outputStream = new ByteArrayOutputStream(sslSocket.getOutputStream());
        if (hostnameVerifier != null && !hostnameVerifier.verify(sslSocket.getInetAddress().getHostName(),
            sslSocket.getSession())) {
            throw new IdentityVerificationException("\"" + sslSocket.getInetAddress().getHostName() +
                "\" identity was not confirmed");
        }
    }

    @Override
    public boolean isOpen() {
        return !socket.isClosed();
    }

    @Override
    public void close() throws IOException {
        try {
            socket.shutdownInput(); // for socketInputStream.setEOF(true)
        } catch (Exception e) {
            // ignore
        }
        try {
            socket.shutdownOutput();
        } catch (Exception e) {
            // ignore
        }
        socket.close();
    }
}
