package com.github.shyiko.mysql.binlog.network;

import java.io.IOException;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class AuthenticationException extends IOException {

    public AuthenticationException(String message) {
        super(message);
    }
}
