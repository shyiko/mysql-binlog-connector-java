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

import com.github.shyiko.mysql.binlog.GtidSet.Interval;
import com.github.shyiko.mysql.binlog.GtidSet.UUIDSet;
import org.testng.annotations.Test;

import java.util.LinkedList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class GtidSetTest {

    private static final String UUID = "24bc7850-2c16-11e6-a073-0242ac110002";

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
        assertEquals(new GtidSet(UUID + ":1-191"), new GtidSet(UUID + ":1-191"));
        assertEquals(new GtidSet(UUID + ":1-191:192-199"), new GtidSet(UUID + ":1-191:192-199"));
        assertEquals(new GtidSet(UUID + ":1-191:192-199"), new GtidSet(UUID + ":1-199"));
        assertEquals(new GtidSet(UUID + ":1-191:193-199"), new GtidSet(UUID + ":1-191:193-199"));
        assertNotEquals(new GtidSet(UUID + ":1-191:193-199"), new GtidSet(UUID + ":1-199"));
    }

    @Test
    public void testSubsetOf() {
        GtidSet[] set = {
            new GtidSet(""),
            new GtidSet(UUID + ":1-191"),
            new GtidSet(UUID + ":192-199"),
            new GtidSet(UUID + ":1-191:192-199"),
            new GtidSet(UUID + ":1-191:193-199"),
            new GtidSet(UUID + ":2-199"),
            new GtidSet(UUID + ":1-200")
        };
        byte[][] subsetMatrix = {
            {1, 1, 1, 1, 1, 1, 1},
            {0, 1, 0, 1, 1, 0, 1},
            {0, 0, 1, 1, 0, 1, 1},
            {0, 0, 0, 1, 0, 0, 1},
            {0, 0, 0, 1, 1, 0, 1},
            {0, 0, 0, 1, 0, 1, 1},
            {0, 0, 0, 0, 0, 0, 1},
        };
        for (int i = 0; i < subsetMatrix.length; i++) {
            byte[] subset = subsetMatrix[i];
            for (int j = 0; j < subset.length; j++) {
                assertEquals(set[i].isContainedWithin(set[j]), subset[j] == 1,
                    "\"" + set[i] + "\" was expected to be a subset of \"" + set[j] +  "\"" +
                        " (" + i + "," + j + ")");
            }
        }
    }

    @Test
    public void testSingleInterval() {
        GtidSet gtidSet = new GtidSet(UUID + ":1-191");
        UUIDSet uuidSet = gtidSet.getUUIDSet(UUID);
        assertEquals(uuidSet.getIntervals().size(), 1);
        assertTrue(uuidSet.getIntervals().contains(new Interval(1, 191)));
        assertEquals(uuidSet.getIntervals().iterator().next(), new Interval(1, 191));
        assertEquals(new LinkedList<Interval>(uuidSet.getIntervals()).getLast(), new Interval(1, 191));
        assertEquals(gtidSet.toString(), UUID + ":1-191");
    }

    @Test
    public void testCollapseAdjacentIntervals() {
        GtidSet gtidSet = new GtidSet(UUID + ":1-191:192-199");
        UUIDSet uuidSet = gtidSet.getUUIDSet(UUID);
        assertEquals(uuidSet.getIntervals().size(), 1);
        assertTrue(uuidSet.getIntervals().contains(new Interval(1, 199)));
        assertEquals(uuidSet.getIntervals().iterator().next(), new Interval(1, 199));
        assertEquals(new LinkedList<Interval>(uuidSet.getIntervals()).getLast(), new Interval(1, 199));
        assertEquals(gtidSet.toString(), UUID + ":1-199");
    }

    @Test
    public void testNotCollapseNonAdjacentIntervals() {
        GtidSet gtidSet = new GtidSet(UUID + ":1-191:193-199");
        UUIDSet uuidSet = gtidSet.getUUIDSet(UUID);
        assertEquals(uuidSet.getIntervals().size(), 2);
        assertEquals(uuidSet.getIntervals().iterator().next(), new Interval(1, 191));
        assertEquals(new LinkedList<Interval>(uuidSet.getIntervals()).getLast(), new Interval(193, 199));
        assertEquals(gtidSet.toString(), UUID + ":1-191:193-199");
    }

    @Test
    public void testMultipleIntervals() {
        GtidSet set = new GtidSet(UUID + ":1-191:193-199:1000-1033");
        UUIDSet uuidSet = set.getUUIDSet(UUID);
        assertEquals(uuidSet.getIntervals().size(), 3);
        assertTrue(uuidSet.getIntervals().contains(new Interval(193, 199)));
        assertEquals(uuidSet.getIntervals().iterator().next(), new Interval(1, 191));
        assertEquals(new LinkedList<Interval>(uuidSet.getIntervals()).getLast(), new Interval(1000, 1033));
        assertEquals(set.toString(), UUID + ":1-191:193-199:1000-1033");
    }

    @Test
    public void testMultipleIntervalsThatMayBeAdjacent() {
        GtidSet gtidSet = new GtidSet(UUID + ":1-191:192-199:1000-1033:1035-1036:1038-1039");
        UUIDSet uuidSet = gtidSet.getUUIDSet(UUID);
        assertEquals(uuidSet.getIntervals().size(), 4);
        assertTrue(uuidSet.getIntervals().contains(new Interval(1000, 1033)));
        assertTrue(uuidSet.getIntervals().contains(new Interval(1035, 1036)));
        assertEquals(uuidSet.getIntervals().iterator().next(), new Interval(1, 199));
        assertEquals(new LinkedList<Interval>(uuidSet.getIntervals()).getLast(), new Interval(1038, 1039));
        assertEquals(gtidSet.toString(), UUID + ":1-199:1000-1033:1035-1036:1038-1039");
    }

    @Test
    public void testPutUUIDSet() {
        GtidSet gtidSet = new GtidSet(UUID + ":1-191");
        UUIDSet uuidSet = gtidSet.getUUIDSet(UUID);
        GtidSet gtidSet2 = new GtidSet(UUID + ":1-190");
        UUIDSet uuidSet2 = gtidSet2.getUUIDSet(UUID);
        gtidSet.putUUIDSet(uuidSet2);
        assertEquals(gtidSet, gtidSet2);
    }

}
