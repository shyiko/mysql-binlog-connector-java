package com.github.shyiko.mysql.binlog.network.protocol.command;

import com.github.shyiko.mysql.binlog.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author <a href="http://github.com/wenerme">wener</a>
 */
public class RegisterSlaveCommand implements Command
{
    private long serverId;
    private String slaveHostname;
    private String slaveUser;
    private String slavePassword;
    private int slavePort;
    private long replicationRank;
    private long masterId;

    public RegisterSlaveCommand(long serverId, String slaveHostname, String slaveUser, String slavePassword, int slavePort, long replicationRank, long masterId)
    {
        this.serverId = serverId;
        this.slaveHostname = slaveHostname;
        this.slaveUser = slaveUser;
        this.slavePassword = slavePassword;
        this.slavePort = slavePort;
        this.replicationRank = replicationRank;
        this.masterId = masterId;
    }

    @Override
    public byte[] toByteArray() throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(CommandType.REGISTER_SLAVE.ordinal());
        buffer.writeLong(serverId, 4);
        buffer.writeInteger(slaveHostname.length(), 1);
        buffer.writeString(slaveHostname);
        buffer.writeInteger(slaveUser.length(), 1);
        buffer.writeString(slaveUser);
        buffer.writeInteger(slavePassword.length(), 1);
        buffer.writeString(slavePassword);
        buffer.writeInteger(slavePort, 2);
        buffer.writeLong(replicationRank, 4);
        buffer.writeLong(masterId, 4);
        return buffer.toByteArray();
    }
}
