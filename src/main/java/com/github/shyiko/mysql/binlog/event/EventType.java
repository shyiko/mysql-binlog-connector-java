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

import java.util.HashMap;
import java.util.Map;

/**
 * @see <a href="https://dev.mysql.com/doc/internals/en/event-meanings.html">Event Meanings</a> for the original
 * documentation.
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public enum EventType {

    /**
     * Events of this event type should never occur. Not written to a binary log.
     */
    UNKNOWN(0),
    /**
     * A descriptor event that is written to the beginning of the each binary log file. (In MySQL 4.0 and 4.1,
     * this event is written only to the first binary log file that the server creates after startup.) This event is
     * used in MySQL 3.23 through 4.1 and superseded in MySQL 5.0 by {@link #FORMAT_DESCRIPTION}.
     */
    START_V3(1),
    /**
     * Written when an updating statement is done.
     */
    QUERY(2),
    /**
     * Written when mysqld stops.
     */
    STOP(3),
    /**
     * Written when mysqld switches to a new binary log file. This occurs when someone issues a FLUSH LOGS statement or
     * the current binary log file becomes larger than max_binlog_size.
     *
     * See https://dev.mysql.com/doc/internals/en/rotate-event.html
     */
    ROTATE(4),
    /**
     * Written every time a statement uses an AUTO_INCREMENT column or the LAST_INSERT_ID() function; precedes other
     * events for the statement. This is written only before a {@link #QUERY} and is not used in case of RBR.
     */
    INTVAR(5),
    /**
     * Used for LOAD DATA INFILE statements in MySQL 3.23.
     */
    LOAD(6),
    /**
     * Not used.
     */
    SLAVE(7),
    /**
     * Used for LOAD DATA INFILE statements in MySQL 4.0 and 4.1.
     */
    CREATE_FILE(8),
    /**
     * Used for LOAD DATA INFILE statements as of MySQL 4.0.
     */
    APPEND_BLOCK(9),
    /**
     * Used for LOAD DATA INFILE statements in 4.0 and 4.1.
     */
    EXEC_LOAD(10),
    /**
     * Used for LOAD DATA INFILE statements as of MySQL 4.0.
     */
    DELETE_FILE(11),
    /**
     * Used for LOAD DATA INFILE statements in MySQL 4.0 and 4.1.
     */
    NEW_LOAD(12),
    /**
     * Written every time a statement uses the RAND() function; precedes other events for the statement. Indicates the
     * seed values to use for generating a random number with RAND() in the next statement. This is written only
     * before a {@link #QUERY} and is not used in case of RBR.
     */
    RAND(13),
    /**
     * Written every time a statement uses a user variable; precedes other events for the statement. Indicates the
     * value to use for the user variable in the next statement. This is written only before a {@link #QUERY} and
     * is not used in case of RBR.
     */
    USER_VAR(14),
    /**
     * A descriptor event that is written to the beginning of the each binary log file.
     * This event is used as of MySQL 5.0; it supersedes {@link #START_V3}.
     *
     * See: https://dev.mysql.com/doc/internals/en/format-description-event.html
     */
    FORMAT_DESCRIPTION(15),
    /**
     * Generated for a commit of a transaction that modifies one or more tables of an XA-capable storage engine.
     * Normal transactions are implemented by sending a {@link #QUERY} containing a BEGIN statement and a {@link #QUERY}
     * containing a COMMIT statement (or a ROLLBACK statement if the transaction is rolled back).
     */
    XID(16),
    /**
     * Used for LOAD DATA INFILE statements as of MySQL 5.0.
     */
    BEGIN_LOAD_QUERY(17),
    /**
     * Used for LOAD DATA INFILE statements as of MySQL 5.0.
     */
    EXECUTE_LOAD_QUERY(18),
    /**
     * This event precedes each row operation event. It maps a table definition to a number, where the table definition
     * consists of database and table names and column definitions. The purpose of this event is to enable replication
     * when a table has different definitions on the master and slave. Row operation events that belong to the same
     * transaction may be grouped into sequences, in which case each such sequence of events begins with a sequence
     * of TABLE_MAP events: one per table used by events in the sequence.
     * Used in case of RBR.
     */
    TABLE_MAP(19),
    /**
     * Describes inserted rows (within a single table).
     * Used in case of RBR (5.1.0 - 5.1.15).
     */
    PRE_GA_WRITE_ROWS(20),
    /**
     * Describes updated rows (within a single table).
     * Used in case of RBR (5.1.0 - 5.1.15).
     */
    PRE_GA_UPDATE_ROWS(21),
    /**
     * Describes deleted rows (within a single table).
     * Used in case of RBR (5.1.0 - 5.1.15).
     */
    PRE_GA_DELETE_ROWS(22),
    /**
     * Describes inserted rows (within a single table).
     * Used in case of RBR (5.1.16 - mysql-trunk).
     */
    WRITE_ROWS_V1(23),
    /**
     * Describes updated rows (within a single table).
     * Used in case of RBR (5.1.16 - mysql-trunk).
     */
    UPDATE_ROWS_V1(24),
    /**
     * Describes deleted rows (within a single table).
     * Used in case of RBR (5.1.16 - mysql-trunk).
     */
    DELETE_ROWS_V1(25),
    /**
     * Used to log an out of the ordinary event that occurred on the master. It notifies the slave that something
     * happened on the master that might cause data to be in an inconsistent state.
     */
    INCIDENT(26),
    /**
     * Sent by a master to a slave to let the slave know that the master is still alive. Not written to a binary log.
     */
    HEARTBEAT_LOG(27),
    /**
     * In some situations, it is necessary to send over ignorable data to the slave: data that a slave can handle in
     * case there is code for handling it, but which can be ignored if it is not recognized.
     */
    IGNORABLE_LOG(28),
    /**
     * Introduced to record the original query for rows events in RBR.
     */
    ROWS_QUERY_LOG(29),
    /**
     * Describes inserted rows (within a single table).
     * Used in case of RBR (5.1.18+).
     */
    WRITE_ROWS(30),
    /**
     * Describes updated rows (within a single table).
     * Used in case of RBR (5.1.18+).
     */
    UPDATE_ROWS(31),
    /**
     * Describes deleted rows (within a single table).
     * Used in case of RBR (5.1.18+).
     */
    DELETE_ROWS(32),

    /**
     * Global Transaction Identifier.
     *
     * MySQL 5.6 GTID events
     */
    GTID_LOG(33),
    ANONYMOUS_GTID_LOG(34),
    PREVIOUS_GTIDS_LOG(35),

    /**
     * MySQL 5.7 Events
     */
    TRANSACTION_CONTEXT(36),
    VIEW_CHANGE(37),
    /**
     * Prepared XA transaction terminal event similar to XID except that it is specific to XA transaction.
     */
    XA_PREPARE_LOG(38),

    /**
     *  New Maria event numbers start from here
     */
    MARIA_ANNOTATE_ROWS(160),
    /**
     * Binlog checkpoint event. Used for XA crash recovery on the master, not used
     * in replication.
     * A binlog checkpoint event specifies a binlog file such that XA crash
     * recovery can start from that file - and it is guaranteed to find all XIDs
     * that are prepared in storage engines but not yet committed.
    */
    MARIA_BINLOG_CHECKPOINT(161),
    /**
     * Gtid event. For global transaction ID, used to start a new event group,
     * instead of the old BEGIN query event, and also to mark stand-alone
     * events.
    */
    MARIA_GTID(162),
    /**
     * Gtid list event. Logged at the start of every binlog, to record the
     * current replication state. This consists of the last GTID seen for
     * each replication domain.
    */
    MARIA_GTID_LIST(163),

    MARIA_START_ENCRYPTION(164),

    /**
     * Compressed binlog event.

     * Note that the order between WRITE/UPDATE/DELETE events is significant;
     * this is so that we can convert from the compressed to the uncompressed
     * event type with (type-WRITE_ROWS_COMPRESSED_EVENT + WRITE_ROWS_EVENT)
     * and similar for _V1.
    */
    MARIA_QUERY_COMPRESSED(165),
    MARIA_WRITE_ROWS_COMPRESSED_V1(166),
    MARIA_UPDATE_ROWS_COMPRESSED_V1(167),
    MARIA_DELETE_ROWS_COMPRESSED_V1(168),
    MARIA_WRITE_ROWS_COMPRESSED(169),
    MARIA_UPDATE_ROWS_COMPRESSED(170),
    MARIA_DELETE_ROWS_COMPRESSED(171);

    private int index;

    EventType(int index) {
        this.index = index;
    }

    public static final Map<Integer, EventType> EVENT_TYPES = new HashMap<Integer, EventType>();
    static {
        for (EventType eventType: EventType.values()) {
            EVENT_TYPES.put(eventType.index, eventType);
        }
    }

    public static boolean isRowMutation(EventType eventType) {
        return EventType.isWrite(eventType) ||
               EventType.isUpdate(eventType) ||
               EventType.isDelete(eventType);
    }

    public static boolean isWrite(EventType eventType) {
        return eventType == PRE_GA_WRITE_ROWS ||
               eventType == WRITE_ROWS_V1 ||
               eventType == WRITE_ROWS ||
               eventType == MARIA_WRITE_ROWS_COMPRESSED_V1 ||
               eventType == MARIA_WRITE_ROWS_COMPRESSED;

    }

    public static boolean isUpdate(EventType eventType) {
        return eventType == PRE_GA_UPDATE_ROWS ||
               eventType == UPDATE_ROWS_V1 ||
               eventType == UPDATE_ROWS ||
               eventType == MARIA_UPDATE_ROWS_COMPRESSED_V1 ||
               eventType == MARIA_UPDATE_ROWS_COMPRESSED;
    }

    public static boolean isDelete(EventType eventType) {
        return eventType == PRE_GA_DELETE_ROWS ||
               eventType == DELETE_ROWS_V1 ||
               eventType == DELETE_ROWS ||
               eventType == MARIA_DELETE_ROWS_COMPRESSED_V1 ||
               eventType == MARIA_DELETE_ROWS_COMPRESSED;
    }

}
