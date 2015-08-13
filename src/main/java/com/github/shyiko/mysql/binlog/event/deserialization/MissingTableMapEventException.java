package com.github.shyiko.mysql.binlog.event.deserialization;

import java.io.IOException;

public class MissingTableMapEventException extends IOException {

    public MissingTableMapEventException(String message) {
        super(message);
    }
}
