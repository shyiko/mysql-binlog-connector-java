package com.github.shyiko.mysql.binlog.event.maria;

/**
 * @author <a href="http://github.com/wenerme">wener</a>
 */
public class Gtid
{
    private long domainId;
    private long serverId;
    private long sequenceNumber;//FIXME unsigned long

    public Gtid(long domainId, long serverId, long sequenceNumber)
    {
        this.domainId = domainId;
        this.serverId = serverId;
        this.sequenceNumber = sequenceNumber;
    }

    public long getDomainId()
    {
        return domainId;
    }

    public long getServerId()
    {
        return serverId;
    }

    public long getSequenceNumber()
    {
        return sequenceNumber;
    }

    @Override
    public String toString()
    {
        return String.format("%s-%s-%s", domainId, serverId, sequenceNumber);
    }
}
