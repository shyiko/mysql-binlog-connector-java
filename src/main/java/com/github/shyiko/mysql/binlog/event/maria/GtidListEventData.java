package com.github.shyiko.mysql.binlog.event.maria;

import com.github.shyiko.mysql.binlog.event.EventData;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDataDeserializer;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://github.com/wenerme">wener</a>
 */
public class GtidListEventData implements EventData
{
    // never null
    private final List<Gtid> list = new ArrayList<Gtid>();
    private int flag;

    public List<Gtid> getList()
    {
        return list;
    }

    public int getFlag()
    {
        return flag;
    }

    public GtidListEventData setFlag(int flag)
    {
        this.flag = flag;
        return this;
    }

    @Override
    public String toString()
    {
        return "GtidListEventData{" +
                "list=" + list +
                ", flag=" + flag +
                '}';
    }
}
