package com.github.shyiko.mysql.binlog.event.maria;

/**
 * @author <a href="http://github.com/wenerme">wener</a>
 */
public class Gtid {
    private long domainId;
    private long serverId;
    private long sequenceNumber;//FIXME unsigned long

    public Gtid(long domainId, long serverId, long sequenceNumber) {
        this.domainId = domainId;
        this.serverId = serverId;
        this.sequenceNumber = sequenceNumber;
    }

    public Gtid(String gtid) {
        if (gtid != null && !gtid.isEmpty()) {
            String[] split = gtid.split("-");
            domainId = Long.parseLong(split[0]);
            serverId = Long.parseLong(split[1]);
            sequenceNumber = Long.parseLong(split[2]);
        }
    }

    public long getDomainId() {
        return domainId;
    }

    public Gtid setDomainId(long domainId) {
        this.domainId = domainId;
        return this;
    }

    public long getServerId() {
        return serverId;
    }

    public Gtid setServerId(long serverId) {
        this.serverId = serverId;
        return this;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public Gtid setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
        return this;
    }

    @Override
    public String toString() {
        return String.format("%s-%s-%s", domainId, serverId, sequenceNumber);
    }
}
