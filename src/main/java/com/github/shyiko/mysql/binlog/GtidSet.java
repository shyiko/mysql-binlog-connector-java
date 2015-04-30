/*
 * Copyright 2015 Stanley Shyiko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.shyiko.mysql.binlog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GTID set as described in <a href="https://dev.mysql.com/doc/refman/5.6/en/replication-gtids-concepts.html">GTID
 * Concepts</a> of MySQL 5.6 Reference Manual.
 *
 * <pre>
 * gtid_set: uuid_set[,uuid_set]...
 * uuid_set: uuid:interval[:interval]...
 * uuid: hhhhhhhh-hhhh-hhhh-hhhh-hhhhhhhhhhhh, h: [0-9|A-F]
 * interval: n[-n], (n >= 1)
 * </pre>
 *
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class GtidSet {

    private final Map<String, UUIDSet> map = new LinkedHashMap<String, UUIDSet>();

    public GtidSet(String gtidSet) {
        String[] uuidSets = gtidSet.isEmpty() ? new String[0] : gtidSet.split(",");
        for (String uuidSet : uuidSets) {
            int uuidSeparatorIndex = uuidSet.indexOf(":");
            String sourceId = uuidSet.substring(0, uuidSeparatorIndex);
            List<Interval> intervals = new ArrayList<Interval>();
            String[] rawIntervals = uuidSet.substring(uuidSeparatorIndex + 1).split(":");
            for (String interval : rawIntervals) {
                String[] is = interval.split("-");
                long[] split = new long[is.length];
                for (int i = 0, e = is.length; i < e; i++) {
                    split[i] = Long.parseLong(is[i]);
                }
                if (split.length == 1) {
                    split = new long[] {split[0], split[0] + 1};
                }
                intervals.add(new Interval(split[0], split[1]));
            }
            map.put(sourceId, new UUIDSet(sourceId, intervals));
        }
    }

    public Collection<UUIDSet> getUUIDSets() {
        return map.values();
    }

    /**
     * @param gtid GTID ("source_id:transaction_id")
     * @return whether or not gtid was added to the set (false if it was already there)
     */
    public boolean add(String gtid) {
        String[] split = gtid.split(":");
        String sourceId = split[0];
        long transactionId = Long.parseLong(split[1]);
        UUIDSet uuidSet = map.get(sourceId);
        if (uuidSet == null) {
            map.put(sourceId, uuidSet = new UUIDSet(sourceId, new ArrayList<Interval>()));
        }
        List<Interval> intervals = (List<Interval>) uuidSet.intervals;
        int index = findInterval(intervals, transactionId);
        boolean addedToExisting = false;
        if (index < intervals.size()) {
            Interval interval = intervals.get(index);
            if (interval.getStart() == transactionId + 1) {
                interval.start = transactionId;
                addedToExisting = true;
            } else
            if (interval.getEnd() == transactionId) {
                interval.end = transactionId + 1;
                addedToExisting = true;
            } else
            if (interval.getStart() <= transactionId && transactionId < interval.getEnd()) {
                return false;
            }
        }
        if (!addedToExisting) {
            intervals.add(index, new Interval(transactionId, transactionId + 1));
        }
        if (intervals.size() > 1) {
            joinAdjacentIntervals(intervals, index);
        }
        return true;
    }

    /**
     * Collapses intervals like a-b:b-c into a-c (only in index+-1 range).
     */
    private void joinAdjacentIntervals(List<Interval> intervals, int index) {
        for (int i = Math.min(index + 1, intervals.size() - 1), e = Math.max(index - 1, 0); i > e; i--) {
            Interval a = intervals.get(i - 1), b = intervals.get(i);
            if (a.getEnd() == b.getStart()) {
                a.end = b.end;
                intervals.remove(i);
            }
        }
    }

    @Override
    public String toString() {
        List<String> gtids = new ArrayList<String>();
        for (UUIDSet uuidSet : map.values()) {
            gtids.add(uuidSet.getUUID() + ":" + join(uuidSet.intervals, ":"));
        }
        return join(gtids, ",");
    }

    /**
     * @return index which is either a pointer to the interval containing v or a position at which v can be added
     */
    private static int findInterval(List<Interval> ii, long v) {
        int l = 0, p = 0, r = ii.size();
        while (l < r) {
            p = (l + r) / 2;
            Interval i = ii.get(p);
            if (i.getEnd() < v) {
                l = p + 1;
            } else
            if (v < i.getStart()) {
                r = p;
            } else {
                return p;
            }
        }
        if (!ii.isEmpty() && ii.get(p).getEnd() < v) {
            p++;
        }
        return p;
    }

    private String join(Collection o, String delimiter) {
        if (o.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object o1 : o) {
            sb.append(o1).append(delimiter);
        }
        return sb.substring(0, sb.length() - delimiter.length());
    }

    /**
     * @see GtidSet
     */
    public static class UUIDSet {

        private String uuid;
        private Collection<Interval> intervals;

        public UUIDSet(String uuid, Collection<Interval> intervals) {
            this.uuid = uuid;
            this.intervals = intervals;
        }

        public String getUUID() {
            return uuid;
        }

        public Collection<Interval> getIntervals() {
            return intervals;
        }
    }

    /**
     * @see GtidSet
     */
    public static class Interval implements Comparable<Interval> {

        private long start;
        private long end;

        public Interval(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        @Override
        public String toString() {
            return start + "-" + end;
        }

        @Override
        public int compareTo(Interval o) {
            return saturatedCast(this.start - o.start);
        }

        private static int saturatedCast(long value) {
            if (value > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (value < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            return (int) value;
        }
    }

}
