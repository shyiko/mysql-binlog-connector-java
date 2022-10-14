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

import com.github.shyiko.mysql.binlog.event.Event;
import com.github.shyiko.mysql.binlog.event.EventData;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class CapturingEventListener implements BinaryLogClientShyiko.EventListener {

    private final List<Event> events = new LinkedList<Event>();

    @Override
    public void onEvent(Event event) {
        synchronized (events) {
            events.add(event);
        }
    }

    @SuppressWarnings("unchecked")
    public <E extends EventData> List<E> getEvents(Class<E> eventDataType) {
        List<E> result = new ArrayList<E>();
        synchronized (events) {
            for (Event event : events) {
                EventData eventData = event.getData();
                if (eventDataType.isInstance(eventData)) {
                    result.add((E) eventData);
                }
            }
        }
        return result;
    }
}
