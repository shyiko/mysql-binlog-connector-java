package com.github.shyiko.mysql.binlog.jmx;

import java.io.IOException;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public interface BinaryLogClientMXBean {

    String getBinlogFilename();
    void setBinlogFilename(String binlogFilename);
    long getBinlogPosition();
    void setBinlogPosition(long binlogPosition);
    void connect() throws IOException;
    boolean isConnected();
    void disconnect() throws IOException;

}
