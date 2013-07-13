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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class TraceEventListener implements BinaryLogClient.EventListener {

    private final Logger logger = Logger.getLogger(getClass().getSimpleName());

    @Override
    public void onEvent(Event event) {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Received " + event);
        }
    }
}
