package com.github.shyiko.mysql.binlog.event.deserialization.maria;

import com.github.shyiko.mysql.binlog.event.deserialization.EventDataDeserializer;
import com.github.shyiko.mysql.binlog.event.maria.Gtid;
import com.github.shyiko.mysql.binlog.event.maria.GtidListEventData;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author <a href="http://github.com/wenerme">wener</a>
 */
public class GtidListDeserializer implements EventDataDeserializer<GtidListEventData>
{
    @Override
    public GtidListEventData deserialize(ByteArrayInputStream is) throws IOException
    {
        GtidListEventData e = new GtidListEventData();
        e.getList().clear();
        long count = is.readLong(4);
        e.setFlag((int) (count >> 28));// higher 4 bit
        count = count & 0x1fffffff;// lower 28 bit

        for (long i = 0; i < count; i++)
        {
            e.getList().add(new Gtid(is.readLong(4), is.readLong(4), is.readLong(8)));
        }
        return e;
    }
}
