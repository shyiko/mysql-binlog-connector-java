/*
 * Copyright 2013 Stanley Shyiko
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
package com.github.shyiko.mysql.binlog.jmx;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class BinaryLogClientStatisticsMXBeanTest {

    @Test
    public void testRegistration() throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        BinaryLogClient binaryLogClient = new BinaryLogClient("localhost", 3306, "root", "mysql");
        BinaryLogClientStatistics binaryLogClientStats = new BinaryLogClientStatistics(binaryLogClient);
        ObjectName objectName = new ObjectName("mysql.binlog:type=BinaryLogClientStatistics");
        mBeanServer.registerMBean(binaryLogClientStats, objectName);
        try {
            assertEquals(mBeanServer.getAttribute(objectName, "TotalNumberOfEventsSeen"), 0L);
        } finally {
            mBeanServer.unregisterMBean(objectName);
        }
    }

}
