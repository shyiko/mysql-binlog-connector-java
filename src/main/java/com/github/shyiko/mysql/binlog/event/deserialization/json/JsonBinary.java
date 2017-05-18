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

import com.github.shyiko.mysql.binlog.event.deserialization.AbstractRowsEventDataDeserializer;
import com.github.shyiko.mysql.binlog.event.deserialization.ColumnType;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;

/**
 * Utility to parse the binary-encoded value of a MySQL {@code JSON} type, translating the encoded representation into
 * method calls on a supplied {@link JsonFormatter} implementation.
 *
 * <h2>Binary Format</h2>
 *
 * Each JSON value (scalar, object or array) has a one byte type identifier followed by the actual value.
 *
 * <h3>Scalar</h3>
 *
 * The binary value may contain a single scalar that is one of:
 * <ul>
 * <li>null</li>
 * <li>boolean</li>
 * <li>int16</li>
 * <li>int32</li>
 * <li>int64</li>
 * <li>uint16</li>
 * <li>uint32</li>
 * <li>uint64</li>
 * <li>double</li>
 * <li>string</li>
 * <li>{@code DATE} as a string of the form {@code YYYY-MM-DD} where {@code YYYY} can be positive or negative</li>
 * <li>{@code TIME} as a string of the form {@code HH-MM-SS} where {@code HH} can be positive or negative</li>
 * <li>{@code DATETIME} as a string of the form {@code YYYY-MM-DD HH-mm-SS.ssssss} where {@code YYYY} can be positive or
 * negative</li>
 * <li>{@code TIMESTAMP} as the number of microseconds past epoch (January 1, 1970), or if negative the number of
 * microseconds before epoch (January 1, 1970)</li>
 * <li>any other MySQL value encoded as an opaque binary value</li>
 * </ul>
 *
 * <h3>JSON Object</h3>
 *
 * If the value is a JSON object, its binary representation will have a header that contains:
 * <ul>
 * <li>the member count</li>
 * <li>the size of the binary value in bytes</li>
 * <li>a list of pointers to each key</li>
 * <li>a list of pointers to each value</li>
 * </ul>
 *
 * The actual keys and values will come after the header, in the same order as in the header.
 *
 * <h3>JSON Array</h3>
 *
 * If the value is a JSON array, the binary representation will have a header with
 * <ul>
 * <li>the element count</li>
 * <li>the size of the binary value in bytes</li>
 * <li>a list of pointers to each value</li>
 * </ul>
 * followed by the actual values, in the same order as in the header.
 *
 * <h2>Grammar</h2>
 * The grammar of the binary representation of JSON objects are defined in the MySQL codebase in the
 * <a href="https://github.com/mysql/mysql-server/blob/5.7/sql/json_binary.h">json_binary.h</a> file:
 * <p>
 *
 * <pre>
 *   doc ::= type value
 *   type ::=
 *       0x00 |  // small JSON object
 *       0x01 |  // large JSON object
 *       0x02 |  // small JSON array
 *       0x03 |  // large JSON array
 *       0x04 |  // literal (true/false/null)
 *       0x05 |  // int16
 *       0x06 |  // uint16
 *       0x07 |  // int32
 *       0x08 |  // uint32
 *       0x09 |  // int64
 *       0x0a |  // uint64
 *       0x0b |  // double
 *       0x0c |  // utf8mb4 string
 *       0x0f    // custom data (any MySQL data type)
 *   value ::=
 *       object  |
 *       array   |
 *       literal |
 *       number  |
 *       string  |
 *       custom-data
 *   object ::= element-count size key-entry* value-entry* key* value*
 *   array ::= element-count size value-entry* value*
 *   // number of members in object or number of elements in array
 *   element-count ::=
 *       uint16 |  // if used in small JSON object/array
 *       uint32    // if used in large JSON object/array
 *   // number of bytes in the binary representation of the object or array
 *   size ::=
 *       uint16 |  // if used in small JSON object/array
 *       uint32    // if used in large JSON object/array
 *   key-entry ::= key-offset key-length
 *   key-offset ::=
 *       uint16 |  // if used in small JSON object
 *       uint32    // if used in large JSON object
 *   key-length ::= uint16    // key length must be less than 64KB
 *   value-entry ::= type offset-or-inlined-value
 *   // This field holds either the offset to where the value is stored,
 *   // or the value itself if it is small enough to be inlined (that is,
 *   // if it is a JSON literal or a small enough [u]int).
 *   offset-or-inlined-value ::=
 *       uint16 |   // if used in small JSON object/array
 *       uint32     // if used in large JSON object/array
 *   key ::= utf8mb4-data
 *   literal ::=
 *       0x00 |   // JSON null literal
 *       0x01 |   // JSON true literal
 *       0x02 |   // JSON false literal
 *   number ::=  ....  // little-endian format for [u]int(16|32|64), whereas
 *                     // double is stored in a platform-independent, eight-byte
 *                     // format using float8store()
 *   string ::= data-length utf8mb4-data
 *   custom-data ::= custom-type data-length binary-data
 *   custom-type ::= uint8   // type identifier that matches the
 *                           // internal enum_field_types enum
 *   data-length ::= uint8*  // If the high bit of a byte is 1, the length
 *                           // field is continued in the next byte,
 *                           // otherwise it is the last byte of the length
 *                           // field. So we need 1 byte to represent
 *                           // lengths up to 127, 2 bytes to represent
 *                           // lengths up to 16383, and so on...
 * </pre>
 *
 * @author <a href="mailto:rhauch@gmail.com">Randall Hauch</a>
 */
public class JsonBinary {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * Parse the MySQL binary representation of a {@code JSON} value and return the JSON string representation.
     * <p>
     * This method is equivalent to {@link #parse(byte[], JsonFormatter)} using the {@link JsonStringFormatter}.
     *
     * @param bytes the binary representation; may not be null
     * @return the JSON string representation; never null
     * @throws IOException if there is a problem reading or processing the binary representation
     */
    public static String parseAsString(byte[] bytes) throws IOException {
        JsonStringFormatter handler = new JsonStringFormatter();
        parse(bytes, handler);
        return handler.getString();
    }

    /**
     * Parse the MySQL binary representation of a {@code JSON} value and call the supplied {@link JsonFormatter}
     * for the various components of the value.
     *
     * @param bytes the binary representation; may not be null
     * @param formatter the formatter that will be called as the binary representation is parsed; may not be null
     * @throws IOException if there is a problem reading or processing the binary representation
     */
    public static void parse(byte[] bytes, JsonFormatter formatter) throws IOException {
        new JsonBinary(bytes).parse(formatter);
    }

    private final ByteArrayInputStream reader;

    public JsonBinary(byte[] bytes) {
        this(new ByteArrayInputStream(bytes));
    }

    public JsonBinary(ByteArrayInputStream contents) {
        this.reader = contents;
    }

    public String getString() {
        JsonStringFormatter handler = new JsonStringFormatter();
        try {
            parse(handler);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return handler.getString();
    }

    public void parse(JsonFormatter formatter) throws IOException {
        parse(readValueType(), formatter);
    }

    protected void parse(ValueType type, JsonFormatter formatter) throws IOException {
        switch (type) {
            case SMALL_DOCUMENT:
                parseObject(true, formatter);
                break;
            case LARGE_DOCUMENT:
                parseObject(false, formatter);
                break;
            case SMALL_ARRAY:
                parseArray(true, formatter);
                break;
            case LARGE_ARRAY:
                parseArray(false, formatter);
                break;
            case LITERAL:
                parseBoolean(formatter);
                break;
            case INT16:
                parseInt16(formatter);
                break;
            case UINT16:
                parseUInt16(formatter);
                break;
            case INT32:
                parseInt32(formatter);
                break;
            case UINT32:
                parseUInt32(formatter);
                break;
            case INT64:
                parseInt64(formatter);
                break;
            case UINT64:
                parseUInt64(formatter);
                break;
            case DOUBLE:
                parseDouble(formatter);
                break;
            case STRING:
                parseString(formatter);
                break;
            case CUSTOM:
                parseOpaque(formatter);
                break;
            default:
                throw new IOException("Unknown type value '" + asHex(type.getCode()) +
                    "' in first byte of a JSON value");
        }
    }

    /**
     * Parse a JSON object.
     * <p>
     * The grammar of the binary representation of JSON objects are defined in the MySQL code base in the
     * <a href="https://github.com/mysql/mysql-server/blob/5.7/sql/json_binary.h">json_binary.h</a> file:
     * <h3>Grammar</h3>
     *
     * <h3>Grammar</h3>
     *
     * <pre>
     *   value ::=
     *       object  |
     *       array   |
     *       literal |
     *       number  |
     *       string  |
     *       custom-data
     *   object ::= element-count size key-entry* value-entry* key* value*
     *   // number of members in object or number of elements in array
     *   element-count ::=
     *       uint16 |  // if used in small JSON object/array
     *       uint32    // if used in large JSON object/array
     *   // number of bytes in the binary representation of the object or array
     *   size ::=
     *       uint16 |  // if used in small JSON object/array
     *       uint32    // if used in large JSON object/array
     *   key-entry ::= key-offset key-length
     *   key-offset ::=
     *       uint16 |  // if used in small JSON object
     *       uint32    // if used in large JSON object
     *   key-length ::= uint16    // key length must be less than 64KB
     *   value-entry ::= type offset-or-inlined-value
     *   // This field holds either the offset to where the value is stored,
     *   // or the value itself if it is small enough to be inlined (that is,
     *   // if it is a JSON literal or a small enough [u]int).
     *   offset-or-inlined-value ::=
     *       uint16 |   // if used in small JSON object/array
     *       uint32     // if used in large JSON object/array
     *   key ::= utf8mb4-data
     *   literal ::=
     *       0x00 |   // JSON null literal
     *       0x01 |   // JSON true literal
     *       0x02 |   // JSON false literal
     *   number ::=  ....  // little-endian format for [u]int(16|32|64), whereas
     *                     // double is stored in a platform-independent, eight-byte
     *                     // format using float8store()
     *   string ::= data-length utf8mb4-data
     *   custom-data ::= custom-type data-length binary-data
     *   custom-type ::= uint8   // type identifier that matches the
     *                           // internal enum_field_types enum
     *   data-length ::= uint8*  // If the high bit of a byte is 1, the length
     *                           // field is continued in the next byte,
     *                           // otherwise it is the last byte of the length
     *                           // field. So we need 1 byte to represent
     *                           // lengths up to 127, 2 bytes to represent
     *                           // lengths up to 16383, and so on...
     * </pre>
     *
     * @param small {@code true} if the object being read is "small", or {@code false} otherwise
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseObject(boolean small, JsonFormatter formatter)
            throws IOException {
        // Read the header ...
        int numElements = readUnsignedIndex(Integer.MAX_VALUE, small, "number of elements in");
        int numBytes = readUnsignedIndex(Integer.MAX_VALUE, small, "size of");
        int valueSize = small ? 2 : 4;

        // Read each key-entry, consisting of the offset and length of each key ...
        int[] keyLengths = new int[numElements];
        for (int i = 0; i != numElements; ++i) {
            readUnsignedIndex(numBytes, small, "key offset in"); // unused
            keyLengths[i] = readUInt16();
        }

        // Read each key value value-entry
        ValueEntry[] entries = new ValueEntry[numElements];
        for (int i = 0; i != numElements; ++i) {
            // Parse the value ...
            ValueType type = readValueType();
            switch (type) {
                case LITERAL:
                    entries[i] = new ValueEntry(type).setValue(readLiteral());
                    reader.skip(valueSize - 1);
                    break;
                case INT16:
                    entries[i] = new ValueEntry(type).setValue(readInt16());
                    reader.skip(valueSize - 2);
                    break;
                case UINT16:
                    entries[i] = new ValueEntry(type).setValue(readUInt16());
                    reader.skip(valueSize - 2);
                    break;
                case INT32:
                    if (!small) {
                        entries[i] = new ValueEntry(type).setValue(readInt32());
                        break;
                    }
                case UINT32:
                    if (!small) {
                        entries[i] = new ValueEntry(type).setValue(readUInt32());
                        break;
                    }
                default:
                    // It is an offset, not a value ...
                    int offset = readUnsignedIndex(Integer.MAX_VALUE, small, "value offset in");
                    if (offset >= numBytes) {
                        throw new IOException("The offset for the value in the JSON binary document is " +
                                offset +
                                ", which is larger than the binary form of the JSON document (" +
                                numBytes + " bytes)");
                    }
                    entries[i] = new ValueEntry(type, offset);
            }
        }

        // Read each key ...
        String[] keys = new String[numElements];
        for (int i = 0; i != numElements; ++i) {
            keys[i] = reader.readString(keyLengths[i]);
        }

        // Now parse the values ...
        formatter.beginObject(numElements);
        for (int i = 0; i != numElements; ++i) {
            if (i != 0) {
                formatter.nextEntry();
            }
            formatter.name(keys[i]);
            ValueEntry entry = entries[i];
            if (entry.resolved) {
                Object value = entry.value;
                if (value == null) {
                    formatter.valueNull();
                } else if (value instanceof Boolean) {
                    formatter.value((Boolean) value);
                } else if (value instanceof Integer) {
                    formatter.value((Integer) value);
                }
            } else {
                // Parse the value ...
                parse(entry.type, formatter);
            }
        }
        formatter.endObject();
    }

    /**
     * Parse a JSON array.
     * <p>
     * The grammar of the binary representation of JSON objects are defined in the MySQL code base in the
     * <a href="https://github.com/mysql/mysql-server/blob/5.7/sql/json_binary.h">json_binary.h</a> file, and are:
     * <h3>Grammar</h3>
     *
     * <h3>Grammar</h3>
     *
     * <pre>
     *   value ::=
     *       object  |
     *       array   |
     *       literal |
     *       number  |
     *       string  |
     *       custom-data
     *   array ::= element-count size value-entry* value*
     *   // number of members in object or number of elements in array
     *   element-count ::=
     *       uint16 |  // if used in small JSON object/array
     *       uint32    // if used in large JSON object/array
     *   // number of bytes in the binary representation of the object or array
     *   size ::=
     *       uint16 |  // if used in small JSON object/array
     *       uint32    // if used in large JSON object/array
     *   value-entry ::= type offset-or-inlined-value
     *   // This field holds either the offset to where the value is stored,
     *   // or the value itself if it is small enough to be inlined (that is,
     *   // if it is a JSON literal or a small enough [u]int).
     *   offset-or-inlined-value ::=
     *       uint16 |   // if used in small JSON object/array
     *       uint32     // if used in large JSON object/array
     *   key ::= utf8mb4-data
     *   literal ::=
     *       0x00 |   // JSON null literal
     *       0x01 |   // JSON true literal
     *       0x02 |   // JSON false literal
     *   number ::=  ....  // little-endian format for [u]int(16|32|64), whereas
     *                     // double is stored in a platform-independent, eight-byte
     *                     // format using float8store()
     *   string ::= data-length utf8mb4-data
     *   custom-data ::= custom-type data-length binary-data
     *   custom-type ::= uint8   // type identifier that matches the
     *                           // internal enum_field_types enum
     *   data-length ::= uint8*  // If the high bit of a byte is 1, the length
     *                           // field is continued in the next byte,
     *                           // otherwise it is the last byte of the length
     *                           // field. So we need 1 byte to represent
     *                           // lengths up to 127, 2 bytes to represent
     *                           // lengths up to 16383, and so on...
     * </pre>
     *
     * @param small {@code true} if the object being read is "small", or {@code false} otherwise
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    // checkstyle, please ignore MethodLength for the next line
    protected void parseArray(boolean small, JsonFormatter formatter)
            throws IOException {
        // Read the header ...
        int numElements = readUnsignedIndex(Integer.MAX_VALUE, small, "number of elements in");
        int numBytes = readUnsignedIndex(Integer.MAX_VALUE, small, "size of");
        int valueSize = small ? 2 : 4;

        // Read each key value value-entry
        ValueEntry[] entries = new ValueEntry[numElements];
        for (int i = 0; i != numElements; ++i) {
            // Parse the value ...
            ValueType type = readValueType();
            switch (type) {
                case LITERAL:
                    entries[i] = new ValueEntry(type).setValue(readLiteral());
                    reader.skip(valueSize - 1);
                    break;
                case INT16:
                    entries[i] = new ValueEntry(type).setValue(readInt16());
                    reader.skip(valueSize - 2);
                    break;
                case UINT16:
                    entries[i] = new ValueEntry(type).setValue(readUInt16());
                    reader.skip(valueSize - 2);
                    break;
                case INT32:
                    if (!small) {
                        entries[i] = new ValueEntry(type).setValue(readInt32());
                        break;
                    }
                case UINT32:
                    if (!small) {
                        entries[i] = new ValueEntry(type).setValue(readUInt32());
                        break;
                    }
                default:
                    // It is an offset, not a value ...
                    int offset = readUnsignedIndex(Integer.MAX_VALUE, small, "value offset in");
                    if (offset >= numBytes) {
                        throw new IOException("The offset for the value in the JSON binary document is " +
                                offset +
                                ", which is larger than the binary form of the JSON document (" +
                                numBytes + " bytes)");
                    }
                    entries[i] = new ValueEntry(type, offset);
            }
        }

        // Now parse the values ...
        formatter.beginArray(numElements);
        for (int i = 0; i != numElements; ++i) {
            if (i != 0) {
                formatter.nextEntry();
            }
            ValueEntry entry = entries[i];
            if (entry.resolved) {
                Object value = entry.value;
                if (value == null) {
                    formatter.valueNull();
                } else if (value instanceof Boolean) {
                    formatter.value((Boolean) value);
                } else if (value instanceof Integer) {
                    formatter.value((Integer) value);
                }
            } else {
                // Parse the value ...
                parse(entry.type, formatter);
            }
        }
        formatter.endArray();
    }

    /**
     * Parse a literal value that is either null, {@code true}, or {@code false}.
     *
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseBoolean(JsonFormatter formatter) throws IOException {
        Boolean literal = readLiteral();
        if (literal == null) {
            formatter.valueNull();
        } else {
            formatter.value(literal);
        }
    }

    /**
     * Parse a 2 byte integer value.
     *
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseInt16(JsonFormatter formatter) throws IOException {
        int value = readInt16();
        formatter.value(value);
    }

    /**
     * Parse a 2 byte unsigned integer value.
     *
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseUInt16(JsonFormatter formatter) throws IOException {
        int value = readUInt16();
        formatter.value(value);
    }

    /**
     * Parse a 4 byte integer value.
     *
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseInt32(JsonFormatter formatter) throws IOException {
        int value = readInt32();
        formatter.value(value);
    }

    /**
     * Parse a 4 byte unsigned integer value.
     *
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseUInt32(JsonFormatter formatter) throws IOException {
        long value = readUInt32();
        formatter.value(value);
    }

    /**
     * Parse a 8 byte integer value.
     *
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseInt64(JsonFormatter formatter) throws IOException {
        long value = readInt64();
        formatter.value(value);
    }

    /**
     * Parse a 8 byte unsigned integer value.
     *
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseUInt64(JsonFormatter formatter) throws IOException {
        BigInteger value = readUInt64();
        formatter.value(value);
    }

    /**
     * Parse a 8 byte double value.
     *
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseDouble(JsonFormatter formatter) throws IOException {
        long rawValue = readInt64();
        double value = Double.longBitsToDouble(rawValue);
        formatter.value(value);
    }

    /**
     * Parse the length and value of a string stored in MySQL's "utf8mb" character set (which equates to Java's
     * UTF-8 character set. The length is a {@link #readVariableInt() variable length integer} length of the string.
     *
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseString(JsonFormatter formatter) throws IOException {
        int length = readVariableInt();
        String value = new String(reader.read(length), UTF_8);
        formatter.value(value);
    }

    /**
     * Parse an opaque type. Specific types such as {@link #parseDate(JsonFormatter) DATE},
     * {@link #parseTime(JsonFormatter) TIME}, and {@link #parseDatetime(JsonFormatter) DATETIME} values are
     * stored as opaque types, though they are to be unpacked. TIMESTAMPs are also stored as opaque types, but
     * <a href="https://github.com/mysql/mysql-server/blob/5.7/sql/sql_time.cc#L1624">converted</a> by MySQL to
     * {@code DATETIME} prior to storage.
     * Other MySQL types are stored as opaque types and passed on to the formatter as opaque values.
     * <p>
     * See the <a href=
     * "https://github.com/mysql/mysql-server/blob/e0e0ae2ea27c9bb76577664845507ef224d362e4/sql/json_binary.cc#L1034">
     * MySQL source code</a> for the logic used in this method.
     * <p>
     * <h3>Grammar</h3>
     *
     * <pre>
     *   custom-data ::= custom-type data-length binary-data
     *   custom-type ::= uint8   // type identifier that matches the
     *                           // internal enum_field_types enum
     *   data-length ::= uint8*  // If the high bit of a byte is 1, the length
     *                           // field is continued in the next byte,
     *                           // otherwise it is the last byte of the length
     *                           // field. So we need 1 byte to represent
     *                           // lengths up to 127, 2 bytes to represent
     *                           // lengths up to 16383, and so on...
     * </pre>
     *
     *
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseOpaque(JsonFormatter formatter) throws IOException {
        // Read the custom type, which should be a standard ColumnType ...
        int customType = reader.read();
        ColumnType type = ColumnType.byCode(customType);
        if (type == null) {
            throw new IOException("Unknown type '" + asHex(customType) +
                    "' in first byte of a JSON opaque value");
        }
        // Read the data length ...
        int length = readVariableInt();

        switch (type) {
            case DECIMAL:
            case NEWDECIMAL:
                // See 'Json_decimal::convert_from_binary'
                // https://github.com/mysql/mysql-server/blob/5.7/sql/json_dom.cc#L1625
                parseDecimal(length, formatter);
                break;

            // All dates and times are in one of these types
            // See 'Json_datetime::to_packed' for details
            // https://github.com/mysql/mysql-server/blob/5.7/sql/json_dom.cc#L1681
            // which calls 'TIME_to_longlong_packed'
            // https://github.com/mysql/mysql-server/blob/5.7/sql-common/my_time.c#L2005
            //
            // and 'Json_datetime::from_packed'
            // https://github.com/mysql/mysql-server/blob/5.7/sql/json_dom.cc#L1688
            // which calls 'TIME_from_longlong_packed'
            // https://github.com/mysql/mysql-server/blob/5.7/sql/sql_time.cc#L1624
            case DATE:
                parseDate(formatter);
                break;
            case TIME:
            case TIME_V2:
                parseTime(formatter);
                break;
            case DATETIME:
            case DATETIME_V2:
            case TIMESTAMP:
            case TIMESTAMP_V2:
                parseDatetime(formatter);
                break;
            default:
                parseOpaqueValue(type, length, formatter);
        }
    }

    /**
     * Parse a {@code DATE} value, which is stored using the same format as {@code DATETIME}:
     * 5 bytes + fractional-seconds storage. However, the hour, minute, second, and fractional seconds are ignored.
     * <p>
     * The non-fractional part is 40 bits:
     *
     * <pre>
     *  1 bit  sign           (1= non-negative, 0= negative)
     *  17 bits year*13+month  (year 0-9999, month 0-12)
     *   5 bits day            (0-31)
     *   5 bits hour           (0-23)
     *   6 bits minute         (0-59)
     *   6 bits second         (0-59)
     * </pre>
     *
     * The fractional part is typically dependent upon the <i>fsp</i> (i.e., fractional seconds part) defined by
     * a column, but in the case of JSON it is always 3 bytes.
     * <p>
     * The format of all temporal values is outlined in the <a href=
     * "https://dev.mysql.com/doc/internals/en/date-and-time-data-type-representation.html">MySQL documentation</a>,
     * although since the MySQL {@code JSON} type is only available in 5.7, only version 2 of the date-time formats
     * are necessary.
     *
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseDate(JsonFormatter formatter) throws IOException {
        long raw = readInt64();
        long value = raw >> 24;
        int yearMonth = (int) (value >> 22) % (1 << 17); // 17 bits starting at 22nd
        int year = yearMonth / 13;
        int month = yearMonth % 13;
        int day = (int) (value >> 17) % (1 << 5); // 5 bits starting at 17th
        formatter.valueDate(year, month, day);
    }

    /**
     * Parse a {@code TIME} value, which is stored using the same format as {@code DATETIME}:
     * 5 bytes + fractional-seconds storage. However, the year, month, and day values are ignored
     * <p>
     * The non-fractional part is 40 bits:
     *
     * <pre>
     *  1 bit  sign           (1= non-negative, 0= negative)
     *  17 bits year*13+month  (year 0-9999, month 0-12)
     *   5 bits day            (0-31)
     *   5 bits hour           (0-23)
     *   6 bits minute         (0-59)
     *   6 bits second         (0-59)
     * </pre>
     *
     * The fractional part is typically dependent upon the <i>fsp</i> (i.e., fractional seconds part) defined by
     * a column, but in the case of JSON it is always 3 bytes.
     * <p>
     * The format of all temporal values is outlined in the <a href=
     * "https://dev.mysql.com/doc/internals/en/date-and-time-data-type-representation.html">MySQL documentation</a>,
     * although since the MySQL {@code JSON} type is only available in 5.7, only version 2 of the date-time formats
     * are necessary.
     *
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseTime(JsonFormatter formatter) throws IOException {
        long raw = readInt64();
        long value = raw >> 24;
        boolean negative = value < 0L;
        int hour = (int) (value >> 12) % (1 << 10); // 10 bits starting at 12th
        int min = (int) (value >> 6) % (1 << 6); // 6 bits starting at 6th
        int sec = (int) value % (1 << 6); // 6 bits starting at 0th
        if (negative) {
            hour *= -1;
        }
        int microSeconds = (int) (raw % (1 << 24));
        formatter.valueTime(hour, min, sec, microSeconds);
    }

    /**
     * Parse a {@code DATETIME} value, which is stored as 5 bytes + fractional-seconds storage.
     * <p>
     * The non-fractional part is 40 bits:
     *
     * <pre>
     *  1 bit  sign           (1= non-negative, 0= negative)
     *  17 bits year*13+month  (year 0-9999, month 0-12)
     *   5 bits day            (0-31)
     *   5 bits hour           (0-23)
     *   6 bits minute         (0-59)
     *   6 bits second         (0-59)
     * </pre>
     *
     * The sign bit is always 1. A value of 0 (negative) is reserved. The fractional part is typically dependent upon
     * the <i>fsp</i> (i.e., fractional seconds part) defined by a column, but in the case of JSON it is always 3 bytes.
     * Unlike the documentation, however, the 8 byte value is in <i>little-endian</i> form.
     * <p>
     * The format of all temporal values is outlined in the <a href=
     * "https://dev.mysql.com/doc/internals/en/date-and-time-data-type-representation.html">MySQL documentation</a>,
     * although since the MySQL {@code JSON} type is only available in 5.7, only version 2 of the date-time formats
     * are necessary.
     *
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseDatetime(JsonFormatter formatter) throws IOException {
        long raw = readInt64();
        long value = raw >> 24;
        int yearMonth = (int) (value >> 22) % (1 << 17); // 17 bits starting at 22nd
        int year = yearMonth / 13;
        int month = yearMonth % 13;
        int day = (int) (value >> 17) % (1 << 5); // 5 bits starting at 17th
        int hour = (int) (value >> 12) % (1 << 5); // 5 bits starting at 12th
        int min = (int) (value >> 6) % (1 << 6); // 6 bits starting at 6th
        int sec = (int) (value % (1 << 6)); // 6 bits starting at 0th
        int microSeconds = (int) (raw % (1 << 24));
        formatter.valueDatetime(year, month, day, hour, min, sec, microSeconds);
    }

    /**
     * Parse a {@code DECIMAL} value. The first two bytes are the precision and scale, followed by the binary
     * representation of the decimal itself.
     *
     * @param length the length of the complete binary representation
     * @param formatter the formatter to be notified of the parsed value; may not be null
     * @throws IOException if there is a problem reading the JSON value
     */
    protected void parseDecimal(int length, JsonFormatter formatter) throws IOException {
        // First two bytes are the precision and scale ...
        int precision = reader.read();
        int scale = reader.read();

        // Followed by the binary representation (see `my_decimal_get_binary_size`)
        int decimalLength = length - 2;
        BigDecimal dec = AbstractRowsEventDataDeserializer.asBigDecimal(precision, scale, reader.read(decimalLength));
        formatter.value(dec);
    }

    protected void parseOpaqueValue(ColumnType type, int length, JsonFormatter formatter)
            throws IOException {
        formatter.valueOpaque(type, reader.read(length));
    }

    protected int readFractionalSecondsInMicroseconds() throws IOException {
        return (int) readBigEndianLong(3);
    }

    protected long readBigEndianLong(int numBytes) throws IOException {
        byte[] bytes = reader.read(numBytes);
        long result = 0;
        for (int i = 0; i != numBytes; i++) {
            int b = bytes[i] & 0xFF;
            result = (result << 8) | b;
        }
        return result;
    }

    protected int readUnsignedIndex(int maxValue, boolean isSmall, String desc) throws IOException {
        long result = isSmall ? readUInt16() : readUInt32();
        if (result > maxValue) {
            throw new IOException("The " + desc + " the JSON document is " + result +
                    " and is too big for the binary form of the document (" + maxValue + ")");
        }
        if (result > Integer.MAX_VALUE) {
            throw new IOException("The " + desc + " the JSON document is " + result + " and is too big to be used");
        }
        return (int) result;
    }

    protected int readInt16() throws IOException {
        int b1 = reader.read() & 0xFF;
        int b2 = reader.read();
        return (short) (b2 << 8 | b1);
    }

    protected int readUInt16() throws IOException {
        int b1 = reader.read() & 0xFF;
        int b2 = reader.read() & 0xFF;
        return (b2 << 8 | b1) & 0xFFFF;
    }

    protected int readInt24() throws IOException {
        int b1 = reader.read() & 0xFF;
        int b2 = reader.read() & 0xFF;
        int b3 = reader.read();
        return b3 << 16 | b2 << 8 | b1;
    }

    protected int readInt32() throws IOException {
        int b1 = reader.read() & 0xFF;
        int b2 = reader.read() & 0xFF;
        int b3 = reader.read() & 0xFF;
        int b4 = reader.read();
        return b4 << 24 | b3 << 16 | b2 << 8 | b1;
    }

    protected long readUInt32() throws IOException {
        int b1 = reader.read() & 0xFF;
        int b2 = reader.read() & 0xFF;
        int b3 = reader.read() & 0xFF;
        int b4 = reader.read() & 0xFF;
        return (long) ((b4 << 24) | (b3 << 16) | (b2 << 8) | b1) & 0xFFFFFFFF;
    }

    protected long readInt64() throws IOException {
        int b1 = reader.read() & 0xFF;
        int b2 = reader.read() & 0xFF;
        int b3 = reader.read() & 0xFF;
        long b4 = reader.read() & 0xFF;
        long b5 = reader.read() & 0xFF;
        long b6 = reader.read() & 0xFF;
        long b7 = reader.read() & 0xFF;
        long b8 = reader.read();
        return b8 << 56 | (b7 << 48) | (b6 << 40) | (b5 << 32) |
                (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

    protected BigInteger readUInt64() throws IOException {
        byte[] bigEndian = new byte[8];
        for (int i = 8; i != 0; --i) {
            bigEndian[i - 1] = (byte) (reader.read() & 0xFF);
        }
        return new BigInteger(1, bigEndian);
    }

    /**
     * Read a variable-length integer value.
     * <p>
     * If the high bit of a byte is 1, the length field is continued in the next byte, otherwise it is the last
     * byte of the length field. So we need 1 byte to represent lengths up to 127, 2 bytes to represent lengths up
     * to 16383, and so on...
     *
     * @return the integer value
     */
    protected int readVariableInt() throws IOException {
        int length = 0;
        for (int i = 0; i < 5; i++) {
            byte b = (byte) reader.read();
            length |= (b & 0x7F) << (7 * i);
            if ((b & 0x80) == 0) {
                return length;
            }
        }
        throw new IOException("Unexpected byte sequence (" + length + ")");
    }

    protected Boolean readLiteral() throws IOException {
        byte b = (byte) reader.read();
        if (b == 0x00) {
            return null;
        } else if (b == 0x01) {
            return Boolean.TRUE;
        } else if (b == 0x02) {
            return Boolean.FALSE;
        }
        throw new IOException("Unexpected value '" + asHex(b) + "' for literal");
    }

    protected ValueType readValueType() throws IOException {
        byte b = (byte) reader.read();
        ValueType result = ValueType.byCode(b);
        if (result == null) {
            throw new IOException("Unknown value type code '" + String.format("%02X", (int) b) + "'");
        }
        return result;
    }

    protected static String asHex(byte b) {
        return String.format("%02X ", b);
    }

    protected static String asHex(int value) {
        return Integer.toHexString(value);
    }

    /**
     * Class used internally to hold value entry information.
     */
    protected static final class ValueEntry {

        protected final ValueType type;
        protected final int index;
        protected Object value;
        protected boolean resolved;

        public ValueEntry(ValueType type) {
            this.type = type;
            this.index = 0;
        }

        public ValueEntry(ValueType type, int index) {
            this.type = type;
            this.index = index;
        }

        public ValueEntry setValue(Object value) {
            this.value = value;
            this.resolved = true;
            return this;
        }
    }
}
