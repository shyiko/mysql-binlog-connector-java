package com.github.shyiko.mysql.binlog.event.deserialization.maria;

import com.github.shyiko.mysql.binlog.event.deserialization.EventDataDeserializer;
import com.github.shyiko.mysql.binlog.event.maria.BinlogCheckpointEventData;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author <a href="http://github.com/wenerme">wener</a>
 */
public class BinlogCheckpointDeserializer implements EventDataDeserializer<BinlogCheckpointEventData>
{
    @Override
    public BinlogCheckpointEventData deserialize(ByteArrayInputStream is) throws IOException
    {
        BinlogCheckpointEventData e = new BinlogCheckpointEventData();
        e.setBinlogFilename(is.readString(is.readInteger(4)));
        return e;
    }
}
