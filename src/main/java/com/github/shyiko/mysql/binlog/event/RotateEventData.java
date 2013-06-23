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
package com.github.shyiko.mysql.binlog.event;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class RotateEventData implements EventData {

    private String binlogFilename;
    private long binlogPosition;

    public String getBinlogFilename() {
        return binlogFilename;
    }

    public void setBinlogFilename(String binlogFilename) {
        this.binlogFilename = binlogFilename;
    }

    public long getBinlogPosition() {
        return binlogPosition;
    }

    public void setBinlogPosition(long binlogPosition) {
        this.binlogPosition = binlogPosition;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RotateEventData");
        sb.append("{binlogFilename='").append(binlogFilename).append('\'');
        sb.append(", binlogPosition=").append(binlogPosition);
        sb.append('}');
        return sb.toString();
    }
}
