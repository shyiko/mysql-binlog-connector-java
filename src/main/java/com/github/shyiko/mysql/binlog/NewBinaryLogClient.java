package com.github.shyiko.mysql.binlog;

import com.github.shyiko.mysql.binlog.network.protocol.PacketChannel;

import java.io.IOException;

public class NewBinaryLogClient extends BinaryLogClient {

    public NewBinaryLogClient(String username, String password) {
        super(username, password);
    }

    public NewBinaryLogClient(String schema, String username, String password) {
        super(schema, username, password);
    }

    public NewBinaryLogClient(String hostname, int port, String username, String password) {
        super(hostname, port, username, password);
    }

    public NewBinaryLogClient(String hostname, int port, String schema, String username, String password) {
        super(hostname, port, schema, username, password);
    }

    @Override
    protected void listenForEventPackets(PacketChannel channel) throws IOException {
        abortRequest = false;
        super.listenForEventPackets(channel);
    }
}
