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
package com.github.shyiko.mysql.binlog.event.deserialization;

import com.github.shyiko.mysql.binlog.event.EventData;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;
import com.github.shyiko.mysql.binlog.io.ByteArrays;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Map;

/**
 * Whole class is basically a mix of <a href="https://code.google.com/p/open-replicator">open-replicator</a>'s
 * AbstractRowEventParser and MySQLUtils.
 *
 * @param <T> event data this deserializer is responsible for
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public abstract class AbstractRowsEventDataDeserializer<T extends EventData> implements EventDataDeserializer<T> {

    private static final int DIG_PER_DEC = 9;
    private static final int[] DIG_TO_BYTES = {0, 1, 1, 2, 2, 3, 3, 4, 4, 4};

    private final Map<Long, TableMapEventData> tableMapEventByTableId;

    public AbstractRowsEventDataDeserializer(Map<Long, TableMapEventData> tableMapEventByTableId) {
        this.tableMapEventByTableId = tableMapEventByTableId;
    }

    protected Object[] deserializeRow(long tableId, BitSet includedColumns, ByteArrayInputStream inputStream)
            throws IOException {
        TableMapEventData tableMapEvent = tableMapEventByTableId.get(tableId);
        byte[] types = tableMapEvent.getColumnTypes();
        int[] metadata = tableMapEvent.getColumnMetadata();
        BitSet nullColumns = inputStream.readBitSet(types.length, true);
        Object[] result = new Object[numberOfBitsSet(includedColumns)];
        for (int i = 0, numberOfSkippedColumns = 0; i < types.length; i++) {
            int typeCode = types[i] & 0xFF, meta = metadata[i], length = 0;
            if (typeCode == ColumnType.STRING.getCode() && meta > 256) {
                int meta0 = meta >> 8, meta1 = meta & 0xFF;
                if ((meta0 & 0x30) != 0x30) { // long CHAR field
                    typeCode = meta0 | 0x30;
                    length = meta1 | (((meta0 & 0x30) ^ 0x30) << 4);
                } else {
                    if (meta0 == ColumnType.SET.getCode() || meta0 == ColumnType.ENUM.getCode() ||
                            meta0 == ColumnType.STRING.getCode()) {
                        typeCode = meta0;
                        length = meta1;
                    } else {
                        throw new IOException("Unexpected meta " + meta + " for column of type " + typeCode);
                    }
                }
            }
            if (!includedColumns.get(i)) {
                numberOfSkippedColumns++;
                continue;
            }
            int index = i - numberOfSkippedColumns;
            if (!nullColumns.get(index)) {
                result[index] = deserializeCell(ColumnType.byCode(typeCode), meta, length, inputStream);
            }
        }
        return result;
    }

    private Object deserializeCell(ColumnType type, int meta, int length, ByteArrayInputStream inputStream)
            throws IOException {
        switch (type) {
            case TINY:
                return inputStream.readInteger(1);
            case SHORT:
                return inputStream.readInteger(2);
            case INT24:
                return inputStream.readInteger(3);
            case LONG:
                return inputStream.readInteger(4);
            case LONGLONG:
                return inputStream.readLong(8);
            case FLOAT:
                return Float.intBitsToFloat(inputStream.readInteger(4));
            case DOUBLE:
                return Double.longBitsToDouble(inputStream.readLong(8));
            case YEAR:
                return 1900 + inputStream.readInteger(1);
            case DATE:
                return toDate(inputStream.readInteger(3));
            case TIME:
                return toTime(inputStream.readInteger(3));
            case TIMESTAMP:
                return toTimestamp(inputStream.readLong(4));
            case DATETIME:
                return toDateTime(inputStream.readLong(8));
            case ENUM:
                return inputStream.readInteger(length);
            case SET:
                return inputStream.readLong(length);
            case STRING:
                int stringLength = length < 256 ? inputStream.readInteger(1) : inputStream.readInteger(2);
                return inputStream.readString(stringLength);
            case BIT:
                int bitSetLength = (meta >> 8) * 8 + (meta & 0xFF);
                return inputStream.readBitSet(bitSetLength, false);
            case NEWDECIMAL:
                int precision = meta & 0xFF, scale = meta >> 8,
                        decimalLength = determineDecimalLength(precision, scale);
                return toDecimal(precision, scale, inputStream.read(decimalLength));
            case BLOB:
                int blobLength = inputStream.readInteger(meta);
                return inputStream.read(blobLength);
            case VARCHAR:
            case VAR_STRING:
                int varcharLength = meta < 256 ? inputStream.readInteger(1) : inputStream.readInteger(2);
                return inputStream.readString(varcharLength);
            default:
                throw new IOException("Unsupported type " + type);
        }
    }

    private static int numberOfBitsSet(BitSet bitSet) {
        int result = 0;
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            result++;
        }
        return result;
    }

    private static java.sql.Date toDate(int value) {
        int day = value % 32;
        value >>>= 5;
        int month = value % 16;
        int year = value >> 4;
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DATE, day);
        return new java.sql.Date(cal.getTimeInMillis());
    }

    private static java.sql.Time toTime(int value) {
        int[] split = split(value, 100, 3);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.HOUR_OF_DAY, split[2]);
        c.set(Calendar.MINUTE, split[1]);
        c.set(Calendar.SECOND, split[0]);
        return new java.sql.Time(c.getTimeInMillis());
    }

    private static java.sql.Timestamp toTimestamp(long value) {
        return new java.sql.Timestamp(value * 1000L);
    }

    private static java.util.Date toDateTime(long value) {
        int[] split = split(value, 100, 6);
        Calendar c = Calendar.getInstance();
        c.set(split[5], split[4] - 1, split[3], split[2], split[1], split[0]);
        return c.getTime();
    }

    private static int[] split(long value, int divider, int length) {
        int[] result = new int[length];
        for (int i = 0; i < length - 1; i++) {
            result[i] = (int) (value % divider);
            value /= divider;
        }
        result[length - 1] = (int) value;
        return result;
    }

    private static int determineDecimalLength(int precision, int scale) {
        int x = precision - scale;
        int ipDigits = x / DIG_PER_DEC;
        int fpDigits = scale / DIG_PER_DEC;
        int ipDigitsX = x - ipDigits * DIG_PER_DEC;
        int fpDigitsX = scale - fpDigits * DIG_PER_DEC;
        return (ipDigits << 2) + DIG_TO_BYTES[ipDigitsX] + (fpDigits << 2) + DIG_TO_BYTES[fpDigitsX];
    }

    /**
     * see mysql/strings/decimal.c
     */
    private static BigDecimal toDecimal(int precision, int scale, byte[] value) {
        boolean positive = (value[0] & 0x80) == 0x80;
        value[0] ^= 0x80;
        if (!positive) {
            for (int i = 0; i < value.length; i++) {
                value[i] ^= 0xFF;
            }
        }
        int x = precision - scale;
        int ipDigits = x / DIG_PER_DEC;
        int ipDigitsX = x - ipDigits * DIG_PER_DEC;
        int ipSize = (ipDigits << 2) + DIG_TO_BYTES[ipDigitsX];
        int offset = DIG_TO_BYTES[ipDigitsX];
        BigDecimal ip = offset > 0 ? BigDecimal.valueOf(ByteArrays.toInteger(value, 0, offset)) : BigDecimal.ZERO;
        for (; offset < ipSize; offset += 4) {
            int i = ByteArrays.toInteger(value, offset, 4);
            ip = ip.movePointRight(DIG_PER_DEC).add(BigDecimal.valueOf(i));
        }
        int shift = 0;
        BigDecimal fp = BigDecimal.ZERO;
        for (; shift + DIG_PER_DEC <= scale; shift += DIG_PER_DEC, offset += 4) {
            int i = ByteArrays.toInteger(value, offset, 4);
            fp = fp.add(BigDecimal.valueOf(i).movePointLeft(shift + DIG_PER_DEC));
        }
        if (shift < scale) {
            int i = ByteArrays.toInteger(value, offset, DIG_TO_BYTES[scale - shift]);
            fp = fp.add(BigDecimal.valueOf(i).movePointLeft(scale));
        }
        BigDecimal result = ip.add(fp);
        return positive ? result : result.negate();
    }

}