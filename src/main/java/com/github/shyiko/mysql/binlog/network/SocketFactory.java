package com.github.shyiko.mysql.binlog.network;

import java.net.Socket;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public interface SocketFactory {

    Socket createSocket();
}
