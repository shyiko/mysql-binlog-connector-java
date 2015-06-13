package com.github.shyiko.mysql.binlog.event.deserialization.maria;

import com.github.shyiko.mysql.binlog.event.deserialization.EventDataDeserializer;
import com.github.shyiko.mysql.binlog.event.maria.MariaGtidEventData;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author <a href="http://github.com/wenerme">wener</a>
 */
public class GtidDeserializer implements EventDataDeserializer<MariaGtidEventData>
{
    @Override
    public MariaGtidEventData deserialize(ByteArrayInputStream is) throws IOException
    {
        MariaGtidEventData e = new MariaGtidEventData();
        e.setSequenceNumber(is.readLong(8));
        e.setDomainId(is.readLong(4));
        e.setFlags2(is.readInteger(1));

        // reserved
        long n = 6 + ((e.getFlags2() & MariaGtidEventData.FL_GROUP_COMMIT_ID) > 0 ? 2 : 0);
        long skip = is.skip(n);
        assert n == skip;
        return e;
    }
}
