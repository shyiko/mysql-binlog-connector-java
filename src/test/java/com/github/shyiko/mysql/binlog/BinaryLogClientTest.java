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
package com.github.shyiko.mysql.binlog;

import com.github.shyiko.mysql.binlog.jmx.BinaryLogClientStatistics;
import org.testng.annotations.Test;

import java.util.concurrent.TimeoutException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class BinaryLogClientTest {

    @Test
    public void testEventListenersManagement() {
        BinaryLogClient binaryLogClient = new BinaryLogClient("localhost", 3306, "root", "mysql");
        assertTrue(binaryLogClient.getEventListeners().isEmpty());
        TraceEventListener traceEventListener = new TraceEventListener();
        binaryLogClient.registerEventListener(traceEventListener);
        binaryLogClient.registerEventListener(new CountDownEventListener());
        binaryLogClient.registerEventListener(new CapturingEventListener());
        assertEquals(binaryLogClient.getEventListeners().size(), 3);
        binaryLogClient.unregisterEventListener(traceEventListener);
        assertEquals(binaryLogClient.getEventListeners().size(), 2);
        binaryLogClient.unregisterEventListener(CountDownEventListener.class);
        assertEquals(binaryLogClient.getEventListeners().size(), 1);
    }

    @Test
    public void testLifecycleListenersManagement() {
        BinaryLogClient binaryLogClient = new BinaryLogClient("localhost", 3306, "root", "mysql");
        assertTrue(binaryLogClient.getLifecycleListeners().isEmpty());
        TraceLifecycleListener traceLifecycleListener = new TraceLifecycleListener();
        binaryLogClient.registerLifecycleListener(traceLifecycleListener);
        binaryLogClient.registerLifecycleListener(new BinaryLogClientStatistics());
        binaryLogClient.registerLifecycleListener(new BinaryLogClient.AbstractLifecycleListener() {
        });
        assertEquals(binaryLogClient.getLifecycleListeners().size(), 3);
        binaryLogClient.unregisterLifecycleListener(traceLifecycleListener);
        assertEquals(binaryLogClient.getLifecycleListeners().size(), 2);
        binaryLogClient.unregisterLifecycleListener(BinaryLogClientStatistics.class);
        assertEquals(binaryLogClient.getLifecycleListeners().size(), 1);
    }

    @Test(expectedExceptions = TimeoutException.class)
    public void testConnectionTimeout() throws Exception {
        new BinaryLogClient("_localhost_", 3306, "root", "mysql").connect(0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullEventDeserializerIsNotAllowed() throws Exception {
        new BinaryLogClient("localhost", 3306, "root", "mysql").setEventDeserializer(null);
    }

}
