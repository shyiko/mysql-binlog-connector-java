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

import java.util.Arrays;

/**
 * @author <a href="https://github.com/stevenczp">Steven Cheng</a>
 */
public class XAPrepareEventData implements EventData {
    private boolean onePhase;
    private int formatID;
    private int gtridLength;
    private int bqualLength;
    private byte[] data;
    private String gtrid;
    private String bqual;

    public boolean isOnePhase() {
        return onePhase;
    }

    public void setOnePhase(boolean onePhase) {
        this.onePhase = onePhase;
    }

    public int getFormatID() {
        return formatID;
    }

    public void setFormatID(int formatID) {
        this.formatID = formatID;
    }

    public int getGtridLength() {
        return gtridLength;
    }

    public void setGtridLength(int gtridLength) {
        this.gtridLength = gtridLength;
    }

    public int getBqualLength() {
        return bqualLength;
    }

    public void setBqualLength(int bqualLength) {
        this.bqualLength = bqualLength;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        gtrid = new String(data, 0, gtridLength);
        bqual = new String(data, gtridLength, bqualLength);
    }

    public String getGtrid() {
        return gtrid;
    }

    public String getBqual() {
        return bqual;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("XAPrepareEventData{");
        sb.append("onePhase=").append(onePhase);
        sb.append(", formatID=").append(formatID);
        sb.append(", gtridLength=").append(gtridLength);
        sb.append(", bqualLength=").append(bqualLength);
        sb.append(", data=").append(Arrays.toString(data));
        sb.append(", gtrid='").append(gtrid).append('\'');
        sb.append(", bqual='").append(bqual).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
