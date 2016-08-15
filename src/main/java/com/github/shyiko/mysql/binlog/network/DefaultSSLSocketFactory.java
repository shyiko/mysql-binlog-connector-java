/*
 * Copyright 2016 Stanley Shyiko
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
package com.github.shyiko.mysql.binlog.network;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.GeneralSecurityException;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class DefaultSSLSocketFactory implements SSLSocketFactory {

    private final String protocol;

    public DefaultSSLSocketFactory() {
        this("TLSv1");
    }

    /**
     * @param protocol TLSv1, TLSv1.1 or TLSv1.2 (the last two require JDK 7+)
     */
    public DefaultSSLSocketFactory(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public SSLSocket createSocket(Socket socket) throws SocketException {
        SSLContext sc;
        try {
            sc = SSLContext.getInstance(this.protocol);
            initSSLContext(sc);
        } catch (GeneralSecurityException e) {
            throw new SocketException(e.getMessage());
        }
        try {
            return (SSLSocket) sc.getSocketFactory()
                .createSocket(socket, socket.getInetAddress().getHostName(), socket.getPort(), true);
        } catch (IOException e) {
            throw new SocketException(e.getMessage());
        }
    }

    protected void initSSLContext(SSLContext sc) throws GeneralSecurityException {
        sc.init(null, null, null);
    }

}
