package com.github.shyiko.mysql.binlog.event.maria;

import com.github.shyiko.mysql.binlog.event.EventData;

public class MariaGtidEventData implements EventData
{
    private long sequenceNumber;//8
    private long domainId;// 4
    private int flags2;// 1

    /* Flags2. */

    /**
     * FL_STANDALONE is set when there is no terminating COMMIT event.
     */
    public static final int FL_STANDALONE = 1;
    /**
     * FL_GROUP_COMMIT_ID is set when event group is part of a group commit on the
     * master. Groups with same commit_id are part of the same group commit.
     */
    public static final int FL_GROUP_COMMIT_ID = 2;
    /**
     * FL_TRANSACTIONAL is set for an event group that can be safely rolled back
     * (no MyISAM, eg.).
     */
    public static final int FL_TRANSACTIONAL = 4;
    /**
     * FL_ALLOW_PARALLEL reflects the (negation of the) value of @@SESSION.skip_parallel_replication at the time of commit.
     */
    public static final int FL_ALLOW_PARALLEL = 8;
    /**
     * FL_WAITED is set if a row lock wait (or other wait) is detected during the
     * execution of the transaction.
     */
    public static final int FL_WAITED = 16;
    /**
     * FL_DDL is set for event group containing DDL.
     */
    public static final int FL_DDL = 32;

    public long getSequenceNumber()
    {
        return sequenceNumber;
    }

    public MariaGtidEventData setSequenceNumber(long sequenceNumber)
    {
        this.sequenceNumber = sequenceNumber;
        return this;
    }

    public long getDomainId()
    {
        return domainId;
    }

    public MariaGtidEventData setDomainId(long domainId)
    {
        this.domainId = domainId;
        return this;
    }

    public int getFlags2()
    {
        return flags2;
    }

    public MariaGtidEventData setFlags2(int flags2)
    {
        this.flags2 = flags2;
        return this;
    }

    @Override
    public String toString()
    {
        return "GtidEventData{" +
                "sequenceNumber=" + sequenceNumber +
                ", domainId=" + domainId +
                ", flags2=" + flags2 +
                '}';
    }
}
