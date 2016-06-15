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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import java.util.LinkedList;

import org.testng.annotations.Test;

import com.github.shyiko.mysql.binlog.GtidSet.Interval;
import com.github.shyiko.mysql.binlog.GtidSet.UUIDSet;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class GtidSetTest {

    private static final String UUID1 = "24bc7850-2c16-11e6-a073-0242ac110002";

    private GtidSet gtids;

    @Test
    public void testAdd() throws Exception {
        GtidSet gtidSet = new GtidSet("00000000-0000-0000-0000-000000000000:3-5");
        gtidSet.add("00000000-0000-0000-0000-000000000000:2");
        gtidSet.add("00000000-0000-0000-0000-000000000000:4");
        gtidSet.add("00000000-0000-0000-0000-000000000000:5");
        gtidSet.add("00000000-0000-0000-0000-000000000000:7");
        gtidSet.add("00000000-0000-0000-0000-000000000001:9");
        gtidSet.add("00000000-0000-0000-0000-000000000000:0");
        assertEquals(gtidSet.toString(),
                     "00000000-0000-0000-0000-000000000000:0-0:2-5:7-7,00000000-0000-0000-0000-000000000001:9-9");
    }

    @Test
    public void testJoin() throws Exception {
        GtidSet gtidSet = new GtidSet("00000000-0000-0000-0000-000000000000:3-4:6-7");
        gtidSet.add("00000000-0000-0000-0000-000000000000:5");
        assertEquals(gtidSet.getUUIDSets().iterator().next().getIntervals().iterator().next().getEnd(), 7);
        assertEquals(gtidSet.toString(), "00000000-0000-0000-0000-000000000000:3-7");
    }

    @Test
    public void testEmptySet() throws Exception {
        assertEquals(new GtidSet("").toString(), "");
    }

    @Test
    public void testEquals() {
        assertEquals(new GtidSet(""), new GtidSet(null));
        assertEquals(new GtidSet(""), new GtidSet(""));
        assertEquals(new GtidSet(UUID1 + ":1-191"), new GtidSet(UUID1 + ":1-191"));
        assertEquals(new GtidSet(UUID1 + ":1-191:192-199"), new GtidSet(UUID1 + ":1-191:192-199"));
        assertEquals(new GtidSet(UUID1 + ":1-191:192-199"), new GtidSet(UUID1 + ":1-199"));
        assertEquals(new GtidSet(UUID1 + ":1-191:193-199"), new GtidSet(UUID1 + ":1-191:193-199"));
        assertNotEquals(new GtidSet(UUID1 + ":1-191:193-199"), new GtidSet(UUID1 + ":1-199"));
    }

    @Test
    public void testSubsetOf() {
        GtidSet empty = new GtidSet("");
        GtidSet range1 = new GtidSet(UUID1 + ":1-191");
        GtidSet range2 = new GtidSet(UUID1 + ":192-199");
        GtidSet range1and2 = new GtidSet(UUID1 + ":1-191:192-199");
        GtidSet range3 = new GtidSet(UUID1 + ":1-191:193-199");
        GtidSet range4 = new GtidSet(UUID1 + ":2-199");
        GtidSet range5 = new GtidSet(UUID1 + ":1-200");

        assertEquals(empty.isContainedWithin(range1), true);
        assertEquals(empty.isContainedWithin(range2), true);
        assertEquals(empty.isContainedWithin(range1and2), true);
        assertEquals(empty.isContainedWithin(range3), true);
        assertEquals(empty.isContainedWithin(range4), true);
        assertEquals(empty.isContainedWithin(range5), true);
        assertEquals(range1.isContainedWithin(empty), false);
        assertEquals(range1.isContainedWithin(range1), true);
        assertEquals(range1.isContainedWithin(range2), false);
        assertEquals(range1.isContainedWithin(range1and2), true);
        assertEquals(range1.isContainedWithin(range3), true);
        assertEquals(range1.isContainedWithin(range4), false);
        assertEquals(range1.isContainedWithin(range5), true);
        assertEquals(range2.isContainedWithin(empty), false);
        assertEquals(range2.isContainedWithin(range1), false);
        assertEquals(range2.isContainedWithin(range2), true);
        assertEquals(range2.isContainedWithin(range1and2), true);
        assertEquals(range2.isContainedWithin(range3), false);
        assertEquals(range2.isContainedWithin(range4), true);
        assertEquals(range2.isContainedWithin(range5), true);
        assertEquals(range1and2.isContainedWithin(empty), false);
        assertEquals(range1and2.isContainedWithin(range1), false);
        assertEquals(range1and2.isContainedWithin(range2), false);
        assertEquals(range1and2.isContainedWithin(range1and2), true);
        assertEquals(range1and2.isContainedWithin(range3), false);
        assertEquals(range1and2.isContainedWithin(range4), false);
        assertEquals(range1and2.isContainedWithin(range5), true);
        assertEquals(range3.isContainedWithin(empty), false);
        assertEquals(range3.isContainedWithin(range1), false);
        assertEquals(range3.isContainedWithin(range2), false);
        assertEquals(range3.isContainedWithin(range1and2), true);
        assertEquals(range3.isContainedWithin(range3), true);
        assertEquals(range3.isContainedWithin(range4), false);
        assertEquals(range3.isContainedWithin(range5), true);
        assertEquals(range4.isContainedWithin(empty), false);
        assertEquals(range4.isContainedWithin(range1), false);
        assertEquals(range4.isContainedWithin(range2), false);
        assertEquals(range4.isContainedWithin(range1and2), true);
        assertEquals(range4.isContainedWithin(range3), false);
        assertEquals(range4.isContainedWithin(range4), true);
        assertEquals(range4.isContainedWithin(range5), true);
        assertEquals(range5.isContainedWithin(empty), false);
        assertEquals(range5.isContainedWithin(range1), false);
        assertEquals(range5.isContainedWithin(range2), false);
        assertEquals(range5.isContainedWithin(range1and2), false);
        assertEquals(range5.isContainedWithin(range3), false);
        assertEquals(range5.isContainedWithin(range4), false);
        assertEquals(range5.isContainedWithin(range5), true);
    }

    @Test
    public void testCreateSetWithSingleInterval() {
        gtids = new GtidSet(UUID1 + ":1-191");
        asertIntervalCount(UUID1, 1);
        asertIntervalExists(UUID1, 1, 191);
        asertFirstInterval(UUID1, 1, 191);
        asertLastInterval(UUID1, 1, 191);
        assertEquals(gtids.toString(), UUID1 + ":1-191");
    }

    @Test
    public void testCollapseAdjacentIntervals() {
        gtids = new GtidSet(UUID1 + ":1-191:192-199");
        asertIntervalCount(UUID1, 1);
        asertIntervalExists(UUID1, 1, 199);
        asertFirstInterval(UUID1, 1, 199);
        asertLastInterval(UUID1, 1, 199);
        assertEquals(gtids.toString(), UUID1 + ":1-199");
    }

    @Test
    public void testNotCollapseNonAdjacentIntervals() {
        gtids = new GtidSet(UUID1 + ":1-191:193-199");
        asertIntervalCount(UUID1, 2);
        asertFirstInterval(UUID1, 1, 191);
        asertLastInterval(UUID1, 193, 199);
        assertEquals(gtids.toString(), UUID1 + ":1-191:193-199");
    }

    @Test
    public void testCreateWithMultipleIntervals() {
        gtids = new GtidSet(UUID1 + ":1-191:193-199:1000-1033");
        asertIntervalCount(UUID1, 3);
        asertFirstInterval(UUID1, 1, 191);
        asertIntervalExists(UUID1, 193, 199);
        asertLastInterval(UUID1, 1000, 1033);
        assertEquals(gtids.toString(), UUID1 + ":1-191:193-199:1000-1033");
    }

    @Test
    public void testCreateWithMultipleIntervalsThatMayBeAdjacent() {
        gtids = new GtidSet(UUID1 + ":1-191:192-199:1000-1033:1035-1036:1038-1039");
        asertIntervalCount(UUID1, 4);
        asertFirstInterval(UUID1, 1, 199);
        asertIntervalExists(UUID1, 1000, 1033);
        asertIntervalExists(UUID1, 1035, 1036);
        asertLastInterval(UUID1, 1038, 1039);
        assertEquals(gtids.toString(), UUID1 + ":1-199:1000-1033:1035-1036:1038-1039"); // ??
    }

    protected void asertIntervalCount(String uuid, int count) {
        UUIDSet set = gtids.forServerWithId(uuid);
        assertEquals(set.getIntervals().size(), count);
    }

    protected void asertIntervalExists(String uuid, int start, int end) {
        assertEquals(hasInterval(uuid, start, end), true);
    }

    protected void asertFirstInterval(String uuid, int start, int end) {
        UUIDSet set = gtids.forServerWithId(uuid);
        Interval interval = set.getIntervals().iterator().next();
        assertEquals(interval.getStart(), start);
        assertEquals(interval.getEnd(), end);
    }

    protected void asertLastInterval(String uuid, int start, int end) {
        UUIDSet set = gtids.forServerWithId(uuid);
        Interval interval = new LinkedList<Interval>(set.getIntervals()).getLast();
        assertEquals(interval.getStart(), start);
        assertEquals(interval.getEnd(), end);
    }

    protected boolean hasInterval(String uuid, int start, int end) {
        UUIDSet set = gtids.forServerWithId(uuid);
        for (Interval interval : set.getIntervals()) {
            if (interval.getStart() == start && interval.getEnd() == end) {
                return true;
            }
        }
        return false;
    }
}
