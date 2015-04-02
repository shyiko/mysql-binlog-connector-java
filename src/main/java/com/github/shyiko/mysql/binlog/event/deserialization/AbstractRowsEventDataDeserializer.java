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

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Map;

/**
 * Whole class is basically a mix of <a href="https://code.google.com/p/open-replicator">open-replicator</a>'s
 * AbstractRowEventParser and MySQLUtils. Main purpose here is to ease rows deserialization.<p>
 *
 * Current {@link ColumnType} to java type mapping is following:
 * <pre>
 * Integer: {@link ColumnType#TINY}, {@link ColumnType#SHORT}, {@link ColumnType#LONG}, {@link ColumnType#INT24},
 * {@link ColumnType#YEAR}, {@link ColumnType#ENUM}, {@link ColumnType#SET},
 * Long: {@link ColumnType#LONGLONG},
 * Float: {@link ColumnType#FLOAT},
 * Double: {@link ColumnType#DOUBLE},
 * String: {@link ColumnType#VARCHAR}, {@link ColumnType#VAR_STRING}, {@link ColumnType#STRING},
 * java.util.BitSet: {@link ColumnType#BIT},
 * java.util.Date: {@link ColumnType#DATETIME}, {@link ColumnType#DATETIME_V2},
 * java.math.BigDecimal: {@link ColumnType#NEWDECIMAL},
 * java.sql.Timestamp: {@link ColumnType#TIMESTAMP}, {@link ColumnType#TIMESTAMP_V2},
 * java.sql.Date: {@link ColumnType#DATE},
 * java.sql.Time: {@link ColumnType#TIME}, {@link ColumnType#TIME_V2},
 * byte[]: {@link ColumnType#BLOB},
 * </pre>
 *
 * At the moment {@link ColumnType#GEOMETRY} is unsupported.
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

    protected Serializable[] deserializeRow(long tableId, BitSet includedColumns, ByteArrayInputStream inputStream)
            throws IOException {
        TableMapEventData tableMapEvent = tableMapEventByTableId.get(tableId);
        byte[] types = tableMapEvent.getColumnTypes();
        int[] metadata = tableMapEvent.getColumnMetadata();
        Serializable[] result = new Serializable[numberOfBitsSet(includedColumns)];
        BitSet nullColumns = inputStream.readBitSet(result.length, true);
        for (int i = 0, numberOfSkippedColumns = 0; i < types.length; i++) {
            if (!includedColumns.get(i)) {
                numberOfSkippedColumns++;
                continue;
            }
            int index = i - numberOfSkippedColumns;
            if (!nullColumns.get(index)) {
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
                result[index] = deserializeCell(ColumnType.byCode(typeCode), meta, length, inputStream);
            }
        }
        return result;
    }

    private Serializable deserializeCell(ColumnType type, int meta, int length, ByteArrayInputStream inputStream)
            throws IOException {
        switch (type) {
            case BIT:
                int bitSetLength = (meta >> 8) * 8 + (meta & 0xFF);
                return inputStream.readBitSet(bitSetLength, false);
            case TINY:
                return (int) ((byte) inputStream.readInteger(1));
            case SHORT:
                return (int) ((short) inputStream.readInteger(2));
            case INT24:
                return (inputStream.readInteger(3) << 8) >> 8;
            case LONG:
                return inputStream.readInteger(4);
            case LONGLONG:
                return inputStream.readLong(8);
            case FLOAT:
                return Float.intBitsToFloat(inputStream.readInteger(4));
            case DOUBLE:
                return Double.longBitsToDouble(inputStream.readLong(8));
            case NEWDECIMAL:
                int precision = meta & 0xFF, scale = meta >> 8,
                    decimalLength = determineDecimalLength(precision, scale);
                return toDecimal(precision, scale, inputStream.read(decimalLength));
            case DATE:
                return deserializeDate(inputStream);
            case TIME:
                return deserializeTime(inputStream);
            case TIME_V2:
                return deserializeTimeV2(meta, inputStream);
            case TIMESTAMP:
                return deserializeTimestamp(inputStream);
            case TIMESTAMP_V2:
                return deserializeTimestampV2(meta, inputStream);
            case DATETIME:
                return deserializeDatetime(inputStream);
            case DATETIME_V2:
                return deserializeDatetimeV2(meta, inputStream);
            case YEAR:
                return 1900 + inputStream.readInteger(1);
            case STRING:
                int stringLength = length < 256 ? inputStream.readInteger(1) : inputStream.readInteger(2);
                return inputStream.readString(stringLength);
            case VARCHAR:
            case VAR_STRING:
                int varcharLength = meta < 256 ? inputStream.readInteger(1) : inputStream.readInteger(2);
                return inputStream.readString(varcharLength);
            case BLOB:
                int blobLength = inputStream.readInteger(meta);
                return inputStream.read(blobLength);
            case ENUM:
                return inputStream.readInteger(length);
            case SET:
                return inputStream.readLong(length);
            default:
                throw new IOException("Unsupported type " + type);
        }
    }

    private java.sql.Date deserializeDate(ByteArrayInputStream inputStream) throws IOException {
        int value = inputStream.readInteger(3);
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

    private static java.sql.Time deserializeTime(ByteArrayInputStream inputStream) throws IOException {
        int value = inputStream.readInteger(3);
        int[] split = split(value, 100, 3);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.HOUR_OF_DAY, split[2]);
        c.set(Calendar.MINUTE, split[1]);
        c.set(Calendar.SECOND, split[0]);
        return new java.sql.Time(c.getTimeInMillis());
    }

    private java.sql.Time deserializeTimeV2(int meta, ByteArrayInputStream inputStream) throws IOException {
        /*
            in big endian:

            1 bit sign (1= non-negative, 0= negative)
            1 bit unused (reserved for future extensions)
            10 bits hour (0-838)
            6 bits minute (0-59)
            6 bits second (0-59)
            = (3 bytes in total)
            +
            fractional-seconds storage (size depends on meta)
        */
        long time = bigEndianLong(inputStream.read(3), 0, 3);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.HOUR_OF_DAY, extractBits(time, 2, 10, 24));
        c.set(Calendar.MINUTE, extractBits(time, 12, 6, 24));
        c.set(Calendar.SECOND, extractBits(time, 18, 6, 24));
        c.set(Calendar.MILLISECOND, getFractionalSeconds(meta, inputStream));
        return new java.sql.Time(c.getTimeInMillis());
    }

    private java.sql.Timestamp deserializeTimestamp(ByteArrayInputStream inputStream) throws IOException {
        long value = inputStream.readLong(4);
        return new java.sql.Timestamp(value * 1000L);
    }

    private java.sql.Timestamp deserializeTimestampV2(int meta, ByteArrayInputStream inputStream) throws IOException {
        // big endian
        long timestamp = bigEndianLong(inputStream.read(4), 0, 4);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp * 1000);
        c.set(Calendar.MILLISECOND, getFractionalSeconds(meta, inputStream));
        return new java.sql.Timestamp(c.getTimeInMillis());
    }

    private java.util.Date deserializeDatetime(ByteArrayInputStream inputStream) throws IOException {
        long value = inputStream.readLong(8);
        int[] split = split(value, 100, 6);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, split[5]);
        c.set(Calendar.MONTH, split[4] - 1);
        c.set(Calendar.DAY_OF_MONTH, split[3]);
        c.set(Calendar.HOUR_OF_DAY, split[2]);
        c.set(Calendar.MINUTE, split[1]);
        c.set(Calendar.SECOND, split[0]);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private java.util.Date deserializeDatetimeV2(int meta, ByteArrayInputStream inputStream) throws IOException {
        /*
            in big endian:

            1 bit sign (1= non-negative, 0= negative)
            17 bits year*13+month (year 0-9999, month 0-12)
            5 bits day (0-31)
            5 bits hour (0-23)
            6 bits minute (0-59)
            6 bits second (0-59)
            = (5 bytes in total)
            +
            fractional-seconds storage (size depends on meta)
        */
        long datetime = bigEndianLong(inputStream.read(5), 0, 5);
        int yearMonth = extractBits(datetime, 1, 17, 40);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, yearMonth / 13);
        c.set(Calendar.MONTH, yearMonth % 13 - 1);
        c.set(Calendar.DAY_OF_MONTH, extractBits(datetime, 18, 5, 40));
        c.set(Calendar.HOUR_OF_DAY, extractBits(datetime, 23, 5, 40));
        c.set(Calendar.MINUTE, extractBits(datetime, 28, 6, 40));
        c.set(Calendar.SECOND, extractBits(datetime, 34, 6, 40));
        c.set(Calendar.MILLISECOND, getFractionalSeconds(meta, inputStream));
        return c.getTime();
    }

    private int getFractionalSeconds(int meta, ByteArrayInputStream inputStream) throws IOException {
        int fractionalSecondsStorageSize = getFractionalSecondsStorageSize(meta);
        if (fractionalSecondsStorageSize > 0) {
            long fractionalSeconds = bigEndianLong(inputStream.read(meta), 0, meta);
            if (meta % 2 == 1) {
                fractionalSeconds /= 10;
            }
            return (int) (fractionalSeconds / 1000);
        }
        return 0;
    }

    private static int getFractionalSecondsStorageSize(int fsp) {
        switch (fsp) {
            case 1:
            case 2:
                return 1;
            case 3:
            case 4:
                return 2;
            case 5:
            case 6:
                return 3;
            default:
                return 0;
        }
    }

    private static int extractBits(long value, int bitOffset, int numberOfBits, int payloadSize) {
        long result = value >> payloadSize - (bitOffset + numberOfBits);
        return (int) (result & ((1 << numberOfBits) - 1));
    }

    private static int numberOfBitsSet(BitSet bitSet) {
        int result = 0;
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            result++;
        }
        return result;
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
        BigDecimal ip = offset > 0 ? BigDecimal.valueOf(bigEndianInteger(value, 0, offset)) : BigDecimal.ZERO;
        for (; offset < ipSize; offset += 4) {
            int i = bigEndianInteger(value, offset, 4);
            ip = ip.movePointRight(DIG_PER_DEC).add(BigDecimal.valueOf(i));
        }
        int shift = 0;
        BigDecimal fp = BigDecimal.ZERO;
        for (; shift + DIG_PER_DEC <= scale; shift += DIG_PER_DEC, offset += 4) {
            int i = bigEndianInteger(value, offset, 4);
            fp = fp.add(BigDecimal.valueOf(i).movePointLeft(shift + DIG_PER_DEC));
        }
        if (shift < scale) {
            int i = bigEndianInteger(value, offset, DIG_TO_BYTES[scale - shift]);
            fp = fp.add(BigDecimal.valueOf(i).movePointLeft(scale));
        }
        BigDecimal result = ip.add(fp);
        return positive ? result : result.negate();
    }

    private static int bigEndianInteger(byte[] bytes, int offset, int length) {
        int result = 0;
        for (int i = offset; i < (offset + length); i++) {
            byte b = bytes[i];
            result = (result << 8) | (b >= 0 ? (int) b : (b + 256));
        }
        return result;
    }

    private static long bigEndianLong(byte[] bytes, int offset, int length) {
        long result = 0;
        for (int i = offset; i < (offset + length); i++) {
            byte b = bytes[i];
            result = (result << 8) | (b >= 0 ? (int) b : (b + 256));
        }
        return result;
    }

}
