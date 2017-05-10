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
 * Created by cc on 2017/5/10.
 */
public class XAPrepareEventData implements EventData {
    private boolean one_phase;
    private int formatID;
    private int gtrid_length;
    private int bqual_length;
    private byte data[];
    private String gtrid;
    private String bqual;

    public boolean isOne_phase() {
        return one_phase;
    }

    public void setOne_phase(boolean one_phase) {
        this.one_phase = one_phase;
    }

    public int getFormatID() {
        return formatID;
    }

    public void setFormatID(int formatID) {
        this.formatID = formatID;
    }

    public int getGtrid_length() {
        return gtrid_length;
    }

    public void setGtrid_length(int gtrid_length) {
        this.gtrid_length = gtrid_length;
    }

    public int getBqual_length() {
        return bqual_length;
    }

    public void setBqual_length(int bqual_length) {
        this.bqual_length = bqual_length;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        gtrid = new String(data, 0, gtrid_length);
        bqual = new String(data, gtrid_length, bqual_length);
    }

    public String getGtrid() {
        return gtrid;
    }

    public String getBqual() {
        return bqual;
    }

    @Override
    public String toString() {
        return "XAPrepareEventData{" + "one_phase=" + one_phase + ", formatID=" + formatID
            + ", gtrid_length=" + gtrid_length + ", bqual_length=" + bqual_length + ", data="
            + Arrays.toString(data) + ", gtrid='" + gtrid + '\'' + ", bqual='" + bqual + '\'' + '}';
    }
}
