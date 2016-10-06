/*
 * Copyright 2016 Stanley Shyiko
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
package com.github.shyiko.mysql.binlog.event.deserialization.json;

import com.github.shyiko.mysql.binlog.event.deserialization.ColumnType;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Handle the various actions involved when {@link JsonBinary#parse(byte[], JsonFormatter)} a JSON binary
 * value.
 *
 * @author <a href="mailto:rhauch@gmail.com">Randall Hauch</a>
 */
public interface JsonFormatter {

    /**
     * Prepare to receive the name-value pairs in a JSON object.
     *
     * @param numElements the number of name-value pairs (or elements)
     */
    void beginObject(int numElements);

    /**
     * Prepare to receive the value pairs that in a JSON array.
     *
     * @param numElements the number of array elements
     */
    void beginArray(int numElements);

    /**
     * Complete the previously-started JSON object.
     */
    void endObject();

    /**
     * Complete the previously-started JSON array.
     */
    void endArray();

    /**
     * Receive the name of an element in a JSON object.
     *
     * @param name the element's name; never null
     */
    void name(String name);

    /**
     * Receive the string value of an element in a JSON object.
     *
     * @param value the element's value; never null
     */
    void value(String value);

    /**
     * Receive the integer value of an element in a JSON object.
     *
     * @param value the element's value
     */
    void value(int value);

    /**
     * Receive the long value of an element in a JSON object.
     *
     * @param value the element's value
     */
    void value(long value);

    /**
     * Receive the double value of an element in a JSON object.
     *
     * @param value the element's value
     */
    void value(double value);

    /**
     * Receive the {@link BigInteger} value of an element in a JSON object.
     *
     * @param value the element's value; never null
     */
    void value(BigInteger value);

    /**
     * Receive the {@link BigDecimal} value of an element in a JSON object.
     *
     * @param value the element's value; never null
     */
    void value(BigDecimal value);

    /**
     * Receive the boolean value of an element in a JSON object.
     *
     * @param value the element's value
     */
    void value(boolean value);

    /**
     * Receive a null value of an element in a JSON object.
     */
    void valueNull();

    /**
     * Receive the year value of an element in a JSON object.
     *
     * @param year the year number that makes up the element's value
     */
    void valueYear(int year);

    /**
     * Receive the date value of an element in a JSON object.
     *
     * @param year the positive or negative year in the element's date value
     * @param month the month (0-12) in the element's date value
     * @param day the day of the month (0-31) in the element's date value
     */
    void valueDate(int year, int month, int day);

    /**
     * Receive the date and time value of an element in a JSON object.
     *
     * @param year the positive or negative year in the element's date value
     * @param month the month (0-12) in the element's date value
     * @param day the day of the month (0-31) in the element's date value
     * @param hour the hour of the day (0-24) in the element's time value
     * @param min the minutes of the hour (0-60) in the element's time value
     * @param sec the seconds of the minute (0-60) in the element's time value
     * @param microSeconds the number of microseconds in the element's time value
     */
    // checkstyle, please ignore ParameterNumber for the next line
    void valueDatetime(int year, int month, int day, int hour, int min, int sec, int microSeconds);

    /**
     * Receive the time value of an element in a JSON object.
     *
     * @param hour the hour of the day (0-24) in the element's time value
     * @param min the minutes of the hour (0-60) in the element's time value
     * @param sec the seconds of the minute (0-60) in the element's time value
     * @param microSeconds the number of microseconds in the element's time value
     */
    void valueTime(int hour, int min, int sec, int microSeconds);

    /**
     * Receive the timestamp value of an element in a JSON object.
     *
     * @param secondsPastEpoch the number of seconds past epoch (January 1, 1970) in the element's timestamp value
     * @param microSeconds the number of microseconds in the element's time value
     */
    void valueTimestamp(long secondsPastEpoch, int microSeconds);

    /**
     * Receive an opaque value of an element in a JSON object.
     *
     * @param type the column type for the value; may not be null
     * @param value the binary representation for the element's value
     */
    void valueOpaque(ColumnType type, byte[] value);

    /**
     * Called after an entry signaling that another entry will be signaled.
     */
    void nextEntry();

}
