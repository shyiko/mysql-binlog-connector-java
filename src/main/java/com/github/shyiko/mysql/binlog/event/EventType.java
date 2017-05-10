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
 * @see <a href="https://dev.mysql.com/doc/internals/en/event-meanings.html">Event Meanings</a> for the original
 * documentation.
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public enum EventType {

    /**
     * Events of this event type should never occur. Not written to a binary log.
     */
    UNKNOWN,
    /**
     * A descriptor event that is written to the beginning of the each binary log file. (In MySQL 4.0 and 4.1,
     * this event is written only to the first binary log file that the server creates after startup.) This event is
     * used in MySQL 3.23 through 4.1 and superseded in MySQL 5.0 by {@link #FORMAT_DESCRIPTION}.
     */
    START_V3,
    /**
     * Written when an updating statement is done.
     */
    QUERY,
    /**
     * Written when mysqld stops.
     */
    STOP,
    /**
     * Written when mysqld switches to a new binary log file. This occurs when someone issues a FLUSH LOGS statement or
     * the current binary log file becomes larger than max_binlog_size.
     */
    ROTATE,
    /**
     * Written every time a statement uses an AUTO_INCREMENT column or the LAST_INSERT_ID() function; precedes other
     * events for the statement. This is written only before a {@link #QUERY} and is not used in case of RBR.
     */
    INTVAR,
    /**
     * Used for LOAD DATA INFILE statements in MySQL 3.23.
     */
    LOAD,
    /**
     * Not used.
     */
    SLAVE,
    /**
     * Used for LOAD DATA INFILE statements in MySQL 4.0 and 4.1.
     */
    CREATE_FILE,
    /**
     * Used for LOAD DATA INFILE statements as of MySQL 4.0.
     */
    APPEND_BLOCK,
    /**
     * Used for LOAD DATA INFILE statements in 4.0 and 4.1.
     */
    EXEC_LOAD,
    /**
     * Used for LOAD DATA INFILE statements as of MySQL 4.0.
     */
    DELETE_FILE,
    /**
     * Used for LOAD DATA INFILE statements in MySQL 4.0 and 4.1.
     */
    NEW_LOAD,
    /**
     * Written every time a statement uses the RAND() function; precedes other events for the statement. Indicates the
     * seed values to use for generating a random number with RAND() in the next statement. This is written only
     * before a {@link #QUERY} and is not used in case of RBR.
     */
    RAND,
    /**
     * Written every time a statement uses a user variable; precedes other events for the statement. Indicates the
     * value to use for the user variable in the next statement. This is written only before a {@link #QUERY} and
     * is not used in case of RBR.
     */
    USER_VAR,
    /**
     * A descriptor event that is written to the beginning of the each binary log file.
     * This event is used as of MySQL 5.0; it supersedes {@link #START_V3}.
     */
    FORMAT_DESCRIPTION,
    /**
     * Generated for a commit of a transaction that modifies one or more tables of an XA-capable storage engine.
     * Normal transactions are implemented by sending a {@link #QUERY} containing a BEGIN statement and a {@link #QUERY}
     * containing a COMMIT statement (or a ROLLBACK statement if the transaction is rolled back).
     */
    XID,
    /**
     * Used for LOAD DATA INFILE statements as of MySQL 5.0.
     */
    BEGIN_LOAD_QUERY,
    /**
     * Used for LOAD DATA INFILE statements as of MySQL 5.0.
     */
    EXECUTE_LOAD_QUERY,
    /**
     * This event precedes each row operation event. It maps a table definition to a number, where the table definition
     * consists of database and table names and column definitions. The purpose of this event is to enable replication
     * when a table has different definitions on the master and slave. Row operation events that belong to the same
     * transaction may be grouped into sequences, in which case each such sequence of events begins with a sequence
     * of TABLE_MAP events: one per table used by events in the sequence.
     * Used in case of RBR.
     */
    TABLE_MAP,
    /**
     * Describes inserted rows (within a single table).
     * Used in case of RBR (5.1.0 - 5.1.15).
     */
    PRE_GA_WRITE_ROWS,
    /**
     * Describes updated rows (within a single table).
     * Used in case of RBR (5.1.0 - 5.1.15).
     */
    PRE_GA_UPDATE_ROWS,
    /**
     * Describes deleted rows (within a single table).
     * Used in case of RBR (5.1.0 - 5.1.15).
     */
    PRE_GA_DELETE_ROWS,
    /**
     * Describes inserted rows (within a single table).
     * Used in case of RBR (5.1.16 - mysql-trunk).
     */
    WRITE_ROWS,
    /**
     * Describes updated rows (within a single table).
     * Used in case of RBR (5.1.16 - mysql-trunk).
     */
    UPDATE_ROWS,
    /**
     * Describes deleted rows (within a single table).
     * Used in case of RBR (5.1.16 - mysql-trunk).
     */
    DELETE_ROWS,
    /**
     * Used to log an out of the ordinary event that occurred on the master. It notifies the slave that something
     * happened on the master that might cause data to be in an inconsistent state.
     */
    INCIDENT,
    /**
     * Sent by a master to a slave to let the slave know that the master is still alive. Not written to a binary log.
     */
    HEARTBEAT,
    /**
     * In some situations, it is necessary to send over ignorable data to the slave: data that a slave can handle in
     * case there is code for handling it, but which can be ignored if it is not recognized.
     */
    IGNORABLE,
    /**
     * Introduced to record the original query for rows events in RBR.
     */
    ROWS_QUERY,
    /**
     * Describes inserted rows (within a single table).
     * Used in case of RBR (5.1.18+).
     */
    EXT_WRITE_ROWS,
    /**
     * Describes updated rows (within a single table).
     * Used in case of RBR (5.1.18+).
     */
    EXT_UPDATE_ROWS,
    /**
     * Describes deleted rows (within a single table).
     * Used in case of RBR (5.1.18+).
     */
    EXT_DELETE_ROWS,
    /**
     * Global Transaction Identifier.
     */
    GTID,
    ANONYMOUS_GTID,
    PREVIOUS_GTIDS,
    TRANSACTION_CONTEXT,
    VIEW_CHANGE,
    /**
     * Prepared XA transaction terminal event similar to XID except that it is specific to XA transaction.
     */
    XA_PREPARE;

    public static boolean isRowMutation(EventType eventType) {
        return EventType.isWrite(eventType) ||
               EventType.isUpdate(eventType) ||
               EventType.isDelete(eventType);
    }

    public static boolean isWrite(EventType eventType) {
        return eventType == PRE_GA_WRITE_ROWS ||
               eventType == WRITE_ROWS ||
               eventType == EXT_WRITE_ROWS;
    }

    public static boolean isUpdate(EventType eventType) {
        return eventType == PRE_GA_UPDATE_ROWS ||
               eventType == UPDATE_ROWS ||
               eventType == EXT_UPDATE_ROWS;
    }

    public static boolean isDelete(EventType eventType) {
        return eventType == PRE_GA_DELETE_ROWS ||
               eventType == DELETE_ROWS ||
               eventType == EXT_DELETE_ROWS;
    }

}
