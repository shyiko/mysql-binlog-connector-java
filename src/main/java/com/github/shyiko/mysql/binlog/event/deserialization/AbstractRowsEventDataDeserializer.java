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
        if (tableMapEvent == null) {
            throw new MissingTableMapEventException("No TableMapEventData has been found for table id:" + tableId +
                ". Usually that means that you have started reading binary log 'within the logical event group'" +
                " (e.g. from WRITE_ROWS and not proceeding TABLE_MAP");
        }
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
                // mysql-5.6.24 sql/log_event.cc log_event_print_value (line 1980)
                int typeCode = types[i] & 0xFF, meta = metadata[i], length = 0;
                if (typeCode == ColumnType.STRING.getCode()) {
                    if (meta >= 256) {
                        int meta0 = meta >> 8, meta1 = meta & 0xFF;
                        if ((meta0 & 0x30) != 0x30) {
                            typeCode = meta0 | 0x30;
                            length = meta1 | (((meta0 & 0x30) ^ 0x30) << 4);
                        } else {
                            // mysql-5.6.24 sql/rpl_utility.h enum_field_types (line 278)
                            if (meta0 == ColumnType.ENUM.getCode() || meta0 == ColumnType.SET.getCode()) {
                                typeCode = meta0;
                            }
                            length = meta1;
                        }
                    } else {
                        length = meta;
                    }
                }
                result[index] = deserializeCell(ColumnType.byCode(typeCode), meta, length, inputStream);
            }
        }
        return result;
    }

    protected Serializable deserializeCell(ColumnType type, int meta, int length, ByteArrayInputStream inputStream)
            throws IOException {
        switch (type) {
            case BIT:
                return deserializeBit(meta, inputStream);
            case TINY:
                return deserializeTiny(inputStream);
            case SHORT:
                return deserializeShort(inputStream);
            case INT24:
                return deserializeInt24(inputStream);
            case LONG:
                return deserializeLong(inputStream);
            case LONGLONG:
                return deserializeLongLong(inputStream);
            case FLOAT:
                return deserializeFloat(inputStream);
            case DOUBLE:
                return deserializeDouble(inputStream);
            case NEWDECIMAL:
                return deserializeNewDecimal(meta, inputStream);
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
                return deserializeYear(inputStream);
            case STRING: // CHAR or BINARY
                return deserializeString(length, inputStream);
            case VARCHAR: case VAR_STRING: // VARCHAR or VARBINARY
                return deserializeVarString(meta, inputStream);
            case BLOB:
                return deserializeBlob(meta, inputStream);
            case ENUM:
                return deserializeEnum(length, inputStream);
            case SET:
                return deserializeSet(length, inputStream);
            default:
                throw new IOException("Unsupported type " + type);
        }
    }

    protected Serializable deserializeBit(int meta, ByteArrayInputStream inputStream) throws IOException {
        int bitSetLength = (meta >> 8) * 8 + (meta & 0xFF);
        return inputStream.readBitSet(bitSetLength, false);
    }

    protected Serializable deserializeTiny(ByteArrayInputStream inputStream) throws IOException {
        return (int) ((byte) inputStream.readInteger(1));
    }

    protected Serializable deserializeShort(ByteArrayInputStream inputStream) throws IOException {
        return (int) ((short) inputStream.readInteger(2));
    }

    protected Serializable deserializeInt24(ByteArrayInputStream inputStream) throws IOException {
        return (inputStream.readInteger(3) << 8) >> 8;
    }

    protected Serializable deserializeLong(ByteArrayInputStream inputStream) throws IOException {
        return inputStream.readInteger(4);
    }

    protected Serializable deserializeLongLong(ByteArrayInputStream inputStream) throws IOException {
        return inputStream.readLong(8);
    }

    protected Serializable deserializeFloat(ByteArrayInputStream inputStream) throws IOException {
        return Float.intBitsToFloat(inputStream.readInteger(4));
    }

    protected Serializable deserializeDouble(ByteArrayInputStream inputStream) throws IOException {
        return Double.longBitsToDouble(inputStream.readLong(8));
    }

    protected Serializable deserializeNewDecimal(int meta, ByteArrayInputStream inputStream) throws IOException {
        int precision = meta & 0xFF, scale = meta >> 8, x = precision - scale;
        int ipd = x / DIG_PER_DEC, fpd = scale / DIG_PER_DEC;
        int decimalLength = (ipd << 2) + DIG_TO_BYTES[x - ipd * DIG_PER_DEC] +
            (fpd << 2) + DIG_TO_BYTES[scale - fpd * DIG_PER_DEC];
        return asBigDecimal(precision, scale, inputStream.read(decimalLength));
    }

    protected Serializable deserializeDate(ByteArrayInputStream inputStream) throws IOException {
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

    protected Serializable deserializeTime(ByteArrayInputStream inputStream) throws IOException {
        int value = inputStream.readInteger(3);
        int[] split = split(value, 100, 3);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.HOUR_OF_DAY, split[2]);
        c.set(Calendar.MINUTE, split[1]);
        c.set(Calendar.SECOND, split[0]);
        return new java.sql.Time(c.getTimeInMillis());
    }

    protected Serializable deserializeTimeV2(int meta, ByteArrayInputStream inputStream) throws IOException {
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
        c.set(Calendar.MILLISECOND, deserializeFractionalSeconds(meta, inputStream));
        return new java.sql.Time(c.getTimeInMillis());
    }

    protected Serializable deserializeTimestamp(ByteArrayInputStream inputStream) throws IOException {
        long value = inputStream.readLong(4);
        return new java.sql.Timestamp(value * 1000L);
    }

    protected Serializable deserializeTimestampV2(int meta, ByteArrayInputStream inputStream) throws IOException {
        // big endian
        long timestamp = bigEndianLong(inputStream.read(4), 0, 4);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp * 1000);
        c.set(Calendar.MILLISECOND, deserializeFractionalSeconds(meta, inputStream));
        return new java.sql.Timestamp(c.getTimeInMillis());
    }

    protected Serializable deserializeDatetime(ByteArrayInputStream inputStream) throws IOException {
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

    protected Serializable deserializeDatetimeV2(int meta, ByteArrayInputStream inputStream) throws IOException {
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
        c.set(Calendar.MILLISECOND, deserializeFractionalSeconds(meta, inputStream));
        return c.getTime();
    }

    protected Serializable deserializeYear(ByteArrayInputStream inputStream) throws IOException {
        return 1900 + inputStream.readInteger(1);
    }

    protected Serializable deserializeString(int length, ByteArrayInputStream inputStream) throws IOException {
        // charset is not present in the binary log (meaning there is no way to distinguish between CHAR / BINARY)
        // as a result - return byte[] instead of an actual String
        int stringLength = length < 256 ? inputStream.readInteger(1) : inputStream.readInteger(2);
        return inputStream.readString(stringLength);
    }

    protected Serializable deserializeVarString(int meta, ByteArrayInputStream inputStream) throws IOException {
        int varcharLength = meta < 256 ? inputStream.readInteger(1) : inputStream.readInteger(2);
        return inputStream.readString(varcharLength);
    }

    protected Serializable deserializeBlob(int meta, ByteArrayInputStream inputStream) throws IOException {
        int blobLength = inputStream.readInteger(meta);
        return inputStream.read(blobLength);
    }

    protected Serializable deserializeEnum(int length, ByteArrayInputStream inputStream) throws IOException {
        return inputStream.readInteger(length);
    }

    protected Serializable deserializeSet(int length, ByteArrayInputStream inputStream) throws IOException {
        return inputStream.readLong(length);
    }

    protected int deserializeFractionalSeconds(int meta, ByteArrayInputStream inputStream) throws IOException {
        int length = (meta + 1) / 2;
        if (length > 0) {
            long fraction = bigEndianLong(inputStream.read(length), 0, length);
            return (int) (fraction / (0.1 * Math.pow(100, length - 1)));
        }
        return 0;
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

    /**
     * see mysql/strings/decimal.c
     */
    private static BigDecimal asBigDecimal(int precision, int scale, byte[] value) {
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
