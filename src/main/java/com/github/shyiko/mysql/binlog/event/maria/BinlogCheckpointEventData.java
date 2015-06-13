package com.github.shyiko.mysql.binlog.event.maria;

import com.github.shyiko.mysql.binlog.event.EventData;

/**
 * @author <a href="http://github.com/wenerme">wener</a>
 */
public class BinlogCheckpointEventData implements EventData
{
    private String binlogFilename;

    public String getBinlogFilename()
    {
        return binlogFilename;
    }

    public BinlogCheckpointEventData setBinlogFilename(String binlogFilename)
    {
        this.binlogFilename = binlogFilename;
        return this;
    }

    @Override
    public String toString()
    {
        return "BinlogCheckpointEventData{" +
                "binlogFilename='" + binlogFilename + '\'' +
                '}';
    }
}
