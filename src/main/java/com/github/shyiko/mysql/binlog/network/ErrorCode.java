/*
 * Copyright 2015 Stanley Shyiko
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
package com.github.shyiko.mysql.binlog.network;

/**
 * MySQL error codes. Auto-generated from sql/share/errmsg-utf8.txt.
 *
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public final class ErrorCode {

    /**
     * Hashchk
     */
    public static final int ER_HASHCHK = 1000;

    /**
     * Isamchk
     */
    public static final int ER_NISAMCHK = 1001;

    /**
     * NO
     */
    public static final int ER_NO = 1002;

    /**
     * YES
     */
    public static final int ER_YES = 1003;

    /**
     * Can't create file '%-.200s' (errno: %d - %s)
     */
    public static final int ER_CANT_CREATE_FILE = 1004;

    /**
     * Can't create table '%-.200s' (errno: %d)
     */
    public static final int ER_CANT_CREATE_TABLE = 1005;

    /**
     * Can't create database '%-.192s' (errno: %d)
     */
    public static final int ER_CANT_CREATE_DB = 1006;

    /**
     * Can't create database '%-.192s'; database exists
     */
    public static final int ER_DB_CREATE_EXISTS = 1007;

    /**
     * Can't drop database '%-.192s'; database doesn't exist
     */
    public static final int ER_DB_DROP_EXISTS = 1008;

    /**
     * Error dropping database (can't delete '%-.192s', errno: %d)
     */
    public static final int ER_DB_DROP_DELETE = 1009;

    /**
     * Error dropping database (can't rmdir '%-.192s', errno: %d)
     */
    public static final int ER_DB_DROP_RMDIR = 1010;

    /**
     * Error on delete of '%-.192s' (errno: %d - %s)
     */
    public static final int ER_CANT_DELETE_FILE = 1011;

    /**
     * Can't read record in system table
     */
    public static final int ER_CANT_FIND_SYSTEM_REC = 1012;

    /**
     * Can't get status of '%-.200s' (errno: %d - %s)
     */
    public static final int ER_CANT_GET_STAT = 1013;

    /**
     * Can't get working directory (errno: %d - %s)
     */
    public static final int ER_CANT_GET_WD = 1014;

    /**
     * Can't lock file (errno: %d - %s)
     */
    public static final int ER_CANT_LOCK = 1015;

    /**
     * Can't open file: '%-.200s' (errno: %d - %s)
     */
    public static final int ER_CANT_OPEN_FILE = 1016;

    /**
     * Can't find file: '%-.200s' (errno: %d - %s)
     */
    public static final int ER_FILE_NOT_FOUND = 1017;

    /**
     * Can't read dir of '%-.192s' (errno: %d - %s)
     */
    public static final int ER_CANT_READ_DIR = 1018;

    /**
     * Can't change dir to '%-.192s' (errno: %d - %s)
     */
    public static final int ER_CANT_SET_WD = 1019;

    /**
     * Record has changed since last read in table '%-.192s'
     */
    public static final int ER_CHECKREAD = 1020;

    /**
     * Disk full (%s); waiting for someone to free some space... (errno: %d - %s)
     */
    public static final int ER_DISK_FULL = 1021;

    /**
     * Can't write; duplicate key in table '%-.192s'
     */
    public static final int ER_DUP_KEY = 1022;

    /**
     * Error on close of '%-.192s' (errno: %d - %s)
     */
    public static final int ER_ERROR_ON_CLOSE = 1023;

    /**
     * Error reading file '%-.200s' (errno: %d - %s)
     */
    public static final int ER_ERROR_ON_READ = 1024;

    /**
     * Error on rename of '%-.210s' to '%-.210s' (errno: %d - %s)
     */
    public static final int ER_ERROR_ON_RENAME = 1025;

    /**
     * Error writing file '%-.200s' (errno: %d - %s)
     */
    public static final int ER_ERROR_ON_WRITE = 1026;

    /**
     * '%-.192s' is locked against change
     */
    public static final int ER_FILE_USED = 1027;

    /**
     * Sort aborted
     */
    public static final int ER_FILSORT_ABORT = 1028;

    /**
     * View '%-.192s' doesn't exist for '%-.192s'
     */
    public static final int ER_FORM_NOT_FOUND = 1029;

    /**
     * Got error %d from storage engine
     */
    public static final int ER_GET_ERRNO = 1030;

    /**
     * Table storage engine for '%-.192s' doesn't have this option
     */
    public static final int ER_ILLEGAL_HA = 1031;

    /**
     * Can't find record in '%-.192s'
     */
    public static final int ER_KEY_NOT_FOUND = 1032;

    /**
     * Incorrect information in file: '%-.200s'
     */
    public static final int ER_NOT_FORM_FILE = 1033;

    /**
     * Incorrect key file for table '%-.200s'; try to repair it
     */
    public static final int ER_NOT_KEYFILE = 1034;

    /**
     * Old key file for table '%-.192s'; repair it!
     */
    public static final int ER_OLD_KEYFILE = 1035;

    /**
     * Table '%-.192s' is read only
     */
    public static final int ER_OPEN_AS_READONLY = 1036;

    /**
     * Out of memory; restart server and try again (needed %d bytes)
     */
    public static final int ER_OUTOFMEMORY = 1037;

    /**
     * Out of sort memory, consider increasing server sort buffer size
     */
    public static final int ER_OUT_OF_SORTMEMORY = 1038;

    /**
     * Unexpected EOF found when reading file '%-.192s' (errno: %d - %s)
     */
    public static final int ER_UNEXPECTED_EOF = 1039;

    /**
     * Too many connections
     */
    public static final int ER_CON_COUNT_ERROR = 1040;

    /**
     * Out of memory; check if mysqld or some other process uses all available memory; if not, you may have to use
     * 'ulimit' to allow mysqld to use more memory or you can add more swap space
     */
    public static final int ER_OUT_OF_RESOURCES = 1041;

    /**
     * Can't get hostname for your address
     */
    public static final int ER_BAD_HOST_ERROR = 1042;

    /**
     * Bad handshake
     */
    public static final int ER_HANDSHAKE_ERROR = 1043;

    /**
     * Access denied for user '%-.48s'@'%-.64s' to database '%-.192s'
     */
    public static final int ER_DBACCESS_DENIED_ERROR = 1044;

    /**
     * Access denied for user '%-.48s'@'%-.64s' (using password: %s)
     */
    public static final int ER_ACCESS_DENIED_ERROR = 1045;

    /**
     * No database selected
     */
    public static final int ER_NO_DB_ERROR = 1046;

    /**
     * Unknown command
     */
    public static final int ER_UNKNOWN_COM_ERROR = 1047;

    /**
     * Column '%-.192s' cannot be null
     */
    public static final int ER_BAD_NULL_ERROR = 1048;

    /**
     * Unknown database '%-.192s'
     */
    public static final int ER_BAD_DB_ERROR = 1049;

    /**
     * Table '%-.192s' already exists
     */
    public static final int ER_TABLE_EXISTS_ERROR = 1050;

    /**
     * Unknown table '%-.100s'
     */
    public static final int ER_BAD_TABLE_ERROR = 1051;

    /**
     * Column '%-.192s' in %-.192s is ambiguous
     */
    public static final int ER_NON_UNIQ_ERROR = 1052;

    /**
     * Server shutdown in progress
     */
    public static final int ER_SERVER_SHUTDOWN = 1053;

    /**
     * Unknown column '%-.192s' in '%-.192s'
     */
    public static final int ER_BAD_FIELD_ERROR = 1054;

    /**
     * '%-.192s' isn't in GROUP BY
     */
    public static final int ER_WRONG_FIELD_WITH_GROUP = 1055;

    /**
     * Can't group on '%-.192s'
     */
    public static final int ER_WRONG_GROUP_FIELD = 1056;

    /**
     * Statement has sum functions and columns in same statement
     */
    public static final int ER_WRONG_SUM_SELECT = 1057;

    /**
     * Column count doesn't match value count
     */
    public static final int ER_WRONG_VALUE_COUNT = 1058;

    /**
     * Identifier name '%-.100s' is too long
     */
    public static final int ER_TOO_LONG_IDENT = 1059;

    /**
     * Duplicate column name '%-.192s'
     */
    public static final int ER_DUP_FIELDNAME = 1060;

    /**
     * Duplicate key name '%-.192s'
     */
    public static final int ER_DUP_KEYNAME = 1061;

    /**
     * Duplicate entry '%-.192s' for key %d
     */
    public static final int ER_DUP_ENTRY = 1062;

    /**
     * Incorrect column specifier for column '%-.192s'
     */
    public static final int ER_WRONG_FIELD_SPEC = 1063;

    /**
     * %s near '%-.80s' at line %d
     */
    public static final int ER_PARSE_ERROR = 1064;

    /**
     * Query was empty
     */
    public static final int ER_EMPTY_QUERY = 1065;

    /**
     * Not unique table/alias: '%-.192s'
     */
    public static final int ER_NONUNIQ_TABLE = 1066;

    /**
     * Invalid default value for '%-.192s'
     */
    public static final int ER_INVALID_DEFAULT = 1067;

    /**
     * Multiple primary key defined
     */
    public static final int ER_MULTIPLE_PRI_KEY = 1068;

    /**
     * Too many keys specified; max %d keys allowed
     */
    public static final int ER_TOO_MANY_KEYS = 1069;

    /**
     * Too many key parts specified; max %d parts allowed
     */
    public static final int ER_TOO_MANY_KEY_PARTS = 1070;

    /**
     * Specified key was too long; max key length is %d bytes
     */
    public static final int ER_TOO_LONG_KEY = 1071;

    /**
     * Key column '%-.192s' doesn't exist in table
     */
    public static final int ER_KEY_COLUMN_DOES_NOT_EXITS = 1072;

    /**
     * BLOB column '%-.192s' can't be used in key specification with the used table type
     */
    public static final int ER_BLOB_USED_AS_KEY = 1073;

    /**
     * Column length too big for column '%-.192s' (max = %lu); use BLOB or TEXT instead
     */
    public static final int ER_TOO_BIG_FIELDLENGTH = 1074;

    /**
     * Incorrect table definition; there can be only one auto column and it must be defined as a key
     */
    public static final int ER_WRONG_AUTO_KEY = 1075;

    /**
     * %s: ready for connections.\nVersion: '%s'  socket: '%s'  port: %d
     */
    public static final int ER_READY = 1076;

    /**
     * %s: Normal shutdown\n
     */
    public static final int ER_NORMAL_SHUTDOWN = 1077;

    /**
     * %s: Got signal %d. Aborting!\n
     */
    public static final int ER_GOT_SIGNAL = 1078;

    /**
     * %s: Shutdown complete\n
     */
    public static final int ER_SHUTDOWN_COMPLETE = 1079;

    /**
     * %s: Forcing close of thread %ld  user: '%-.48s'\n
     */
    public static final int ER_FORCING_CLOSE = 1080;

    /**
     * Can't create IP socket
     */
    public static final int ER_IPSOCK_ERROR = 1081;

    /**
     * Table '%-.192s' has no index like the one used in CREATE INDEX; recreate the table
     */
    public static final int ER_NO_SUCH_INDEX = 1082;

    /**
     * Field separator argument is not what is expected; check the manual
     */
    public static final int ER_WRONG_FIELD_TERMINATORS = 1083;

    /**
     * You can't use fixed rowlength with BLOBs; please use 'fields terminated by'
     */
    public static final int ER_BLOBS_AND_NO_TERMINATED = 1084;

    /**
     * The file '%-.128s' must be in the database directory or be readable by all
     */
    public static final int ER_TEXTFILE_NOT_READABLE = 1085;

    /**
     * File '%-.200s' already exists
     */
    public static final int ER_FILE_EXISTS_ERROR = 1086;

    /**
     * Records: %ld  Deleted: %ld  Skipped: %ld  Warnings: %ld
     */
    public static final int ER_LOAD_INFO = 1087;

    /**
     * Records: %ld  Duplicates: %ld
     */
    public static final int ER_ALTER_INFO = 1088;

    /**
     * Incorrect prefix key; the used key part isn't a string, the used length is longer than the key part, or the
     * storage engine doesn't support unique prefix keys
     */
    public static final int ER_WRONG_SUB_KEY = 1089;

    /**
     * You can't delete all columns with ALTER TABLE; use DROP TABLE instead
     */
    public static final int ER_CANT_REMOVE_ALL_FIELDS = 1090;

    /**
     * Can't DROP '%-.192s'; check that column/key exists
     */
    public static final int ER_CANT_DROP_FIELD_OR_KEY = 1091;

    /**
     * Records: %ld  Duplicates: %ld  Warnings: %ld
     */
    public static final int ER_INSERT_INFO = 1092;

    /**
     * You can't specify target table '%-.192s' for update in FROM clause
     */
    public static final int ER_UPDATE_TABLE_USED = 1093;

    /**
     * Unknown thread id: %lu
     */
    public static final int ER_NO_SUCH_THREAD = 1094;

    /**
     * You are not owner of thread %lu
     */
    public static final int ER_KILL_DENIED_ERROR = 1095;

    /**
     * No tables used
     */
    public static final int ER_NO_TABLES_USED = 1096;

    /**
     * Too many strings for column %-.192s and SET
     */
    public static final int ER_TOO_BIG_SET = 1097;

    /**
     * Can't generate a unique log-filename %-.200s.(1-999)\n
     */
    public static final int ER_NO_UNIQUE_LOGFILE = 1098;

    /**
     * Table '%-.192s' was locked with a READ lock and can't be updated
     */
    public static final int ER_TABLE_NOT_LOCKED_FOR_WRITE = 1099;

    /**
     * Table '%-.192s' was not locked with LOCK TABLES
     */
    public static final int ER_TABLE_NOT_LOCKED = 1100;

    /**
     * BLOB/TEXT column '%-.192s' can't have a default value
     */
    public static final int ER_BLOB_CANT_HAVE_DEFAULT = 1101;

    /**
     * Incorrect database name '%-.100s'
     */
    public static final int ER_WRONG_DB_NAME = 1102;

    /**
     * Incorrect table name '%-.100s'
     */
    public static final int ER_WRONG_TABLE_NAME = 1103;

    /**
     * The SELECT would examine more than MAX_JOIN_SIZE rows; check your WHERE and use SET SQL_BIG_SELECTS=1 or SET
     * MAX_JOIN_SIZE=# if the SELECT is okay
     */
    public static final int ER_TOO_BIG_SELECT = 1104;

    /**
     * Unknown error
     */
    public static final int ER_UNKNOWN_ERROR = 1105;

    /**
     * Unknown procedure '%-.192s'
     */
    public static final int ER_UNKNOWN_PROCEDURE = 1106;

    /**
     * Incorrect parameter count to procedure '%-.192s'
     */
    public static final int ER_WRONG_PARAMCOUNT_TO_PROCEDURE = 1107;

    /**
     * Incorrect parameters to procedure '%-.192s'
     */
    public static final int ER_WRONG_PARAMETERS_TO_PROCEDURE = 1108;

    /**
     * Unknown table '%-.192s' in %-.32s
     */
    public static final int ER_UNKNOWN_TABLE = 1109;

    /**
     * Column '%-.192s' specified twice
     */
    public static final int ER_FIELD_SPECIFIED_TWICE = 1110;

    /**
     * Invalid use of group function
     */
    public static final int ER_INVALID_GROUP_FUNC_USE = 1111;

    /**
     * Table '%-.192s' uses an extension that doesn't exist in this MySQL version
     */
    public static final int ER_UNSUPPORTED_EXTENSION = 1112;

    /**
     * A table must have at least 1 column
     */
    public static final int ER_TABLE_MUST_HAVE_COLUMNS = 1113;

    /**
     * The table '%-.192s' is full
     */
    public static final int ER_RECORD_FILE_FULL = 1114;

    /**
     * Unknown character set: '%-.64s'
     */
    public static final int ER_UNKNOWN_CHARACTER_SET = 1115;

    /**
     * Too many tables; MySQL can only use %d tables in a join
     */
    public static final int ER_TOO_MANY_TABLES = 1116;

    /**
     * Too many columns
     */
    public static final int ER_TOO_MANY_FIELDS = 1117;

    /**
     * Row size too large. The maximum row size for the used table type, not counting BLOBs, is %ld. This includes
     * storage overhead, check the manual. You have to change some columns to TEXT or BLOBs
     */
    public static final int ER_TOO_BIG_ROWSIZE = 1118;

    /**
     * Thread stack overrun:  Used: %ld of a %ld stack.  Use 'mysqld --thread_stack=#' to specify a bigger stack if
     * needed
     */
    public static final int ER_STACK_OVERRUN = 1119;

    /**
     * Cross dependency found in OUTER JOIN; examine your ON conditions
     */
    public static final int ER_WRONG_OUTER_JOIN = 1120;

    /**
     * Table handler doesn't support NULL in given index. Please change column '%-.192s' to be NOT NULL or use
     * another handler
     */
    public static final int ER_NULL_COLUMN_IN_INDEX = 1121;

    /**
     * Can't load function '%-.192s'
     */
    public static final int ER_CANT_FIND_UDF = 1122;

    /**
     * Can't initialize function '%-.192s'; %-.80s
     */
    public static final int ER_CANT_INITIALIZE_UDF = 1123;

    /**
     * No paths allowed for shared library
     */
    public static final int ER_UDF_NO_PATHS = 1124;

    /**
     * Function '%-.192s' already exists
     */
    public static final int ER_UDF_EXISTS = 1125;

    /**
     * Can't open shared library '%-.192s' (errno: %d %-.128s)
     */
    public static final int ER_CANT_OPEN_LIBRARY = 1126;

    /**
     * Can't find symbol '%-.128s' in library
     */
    public static final int ER_CANT_FIND_DL_ENTRY = 1127;

    /**
     * Function '%-.192s' is not defined
     */
    public static final int ER_FUNCTION_NOT_DEFINED = 1128;

    /**
     * Host '%-.64s' is blocked because of many connection errors; unblock with 'mysqladmin flush-hosts'
     */
    public static final int ER_HOST_IS_BLOCKED = 1129;

    /**
     * Host '%-.64s' is not allowed to connect to this MySQL server
     */
    public static final int ER_HOST_NOT_PRIVILEGED = 1130;

    /**
     * You are using MySQL as an anonymous user and anonymous users are not allowed to change passwords
     */
    public static final int ER_PASSWORD_ANONYMOUS_USER = 1131;

    /**
     * You must have privileges to update tables in the mysql database to be able to change passwords for others
     */
    public static final int ER_PASSWORD_NOT_ALLOWED = 1132;

    /**
     * Can't find any matching row in the user table
     */
    public static final int ER_PASSWORD_NO_MATCH = 1133;

    /**
     * Rows matched: %ld  Changed: %ld  Warnings: %ld
     */
    public static final int ER_UPDATE_INFO = 1134;

    /**
     * Can't create a new thread (errno %d); if you are not out of available memory, you can consult the manual for a
     * possible OS-dependent bug
     */
    public static final int ER_CANT_CREATE_THREAD = 1135;

    /**
     * Column count doesn't match value count at row %ld
     */
    public static final int ER_WRONG_VALUE_COUNT_ON_ROW = 1136;

    /**
     * Can't reopen table: '%-.192s'
     */
    public static final int ER_CANT_REOPEN_TABLE = 1137;

    /**
     * Invalid use of NULL value
     */
    public static final int ER_INVALID_USE_OF_NULL = 1138;

    /**
     * Got error '%-.64s' from regexp
     */
    public static final int ER_REGEXP_ERROR = 1139;

    /**
     * Mixing of GROUP columns (MIN(),MAX(),COUNT(),...) with no GROUP columns is illegal if there is no GROUP BY
     * clause
     */
    public static final int ER_MIX_OF_GROUP_FUNC_AND_FIELDS = 1140;

    /**
     * There is no such grant defined for user '%-.48s' on host '%-.64s'
     */
    public static final int ER_NONEXISTING_GRANT = 1141;

    /**
     * %-.128s command denied to user '%-.48s'@'%-.64s' for table '%-.64s'
     */
    public static final int ER_TABLEACCESS_DENIED_ERROR = 1142;

    /**
     * %-.16s command denied to user '%-.48s'@'%-.64s' for column '%-.192s' in table '%-.192s'
     */
    public static final int ER_COLUMNACCESS_DENIED_ERROR = 1143;

    /**
     * Illegal GRANT/REVOKE command; please consult the manual to see which privileges can be used
     */
    public static final int ER_ILLEGAL_GRANT_FOR_TABLE = 1144;

    /**
     * The host or user argument to GRANT is too long
     */
    public static final int ER_GRANT_WRONG_HOST_OR_USER = 1145;

    /**
     * Table '%-.192s.%-.192s' doesn't exist
     */
    public static final int ER_NO_SUCH_TABLE = 1146;

    /**
     * There is no such grant defined for user '%-.48s' on host '%-.64s' on table '%-.192s'
     */
    public static final int ER_NONEXISTING_TABLE_GRANT = 1147;

    /**
     * The used command is not allowed with this MySQL version
     */
    public static final int ER_NOT_ALLOWED_COMMAND = 1148;

    /**
     * You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the
     * right syntax to use
     */
    public static final int ER_SYNTAX_ERROR = 1149;

    /**
     * Delayed insert thread couldn't get requested lock for table %-.192s
     */
    public static final int ER_DELAYED_CANT_CHANGE_LOCK = 1150;

    /**
     * Too many delayed threads in use
     */
    public static final int ER_TOO_MANY_DELAYED_THREADS = 1151;

    /**
     * Aborted connection %ld to db: '%-.192s' user: '%-.48s' (%-.64s)
     */
    public static final int ER_ABORTING_CONNECTION = 1152;

    /**
     * Got a packet bigger than 'max_allowed_packet' bytes
     */
    public static final int ER_NET_PACKET_TOO_LARGE = 1153;

    /**
     * Got a read error from the connection pipe
     */
    public static final int ER_NET_READ_ERROR_FROM_PIPE = 1154;

    /**
     * Got an error from fcntl()
     */
    public static final int ER_NET_FCNTL_ERROR = 1155;

    /**
     * Got packets out of order
     */
    public static final int ER_NET_PACKETS_OUT_OF_ORDER = 1156;

    /**
     * Couldn't uncompress communication packet
     */
    public static final int ER_NET_UNCOMPRESS_ERROR = 1157;

    /**
     * Got an error reading communication packets
     */
    public static final int ER_NET_READ_ERROR = 1158;

    /**
     * Got timeout reading communication packets
     */
    public static final int ER_NET_READ_INTERRUPTED = 1159;

    /**
     * Got an error writing communication packets
     */
    public static final int ER_NET_ERROR_ON_WRITE = 1160;

    /**
     * Got timeout writing communication packets
     */
    public static final int ER_NET_WRITE_INTERRUPTED = 1161;

    /**
     * Result string is longer than 'max_allowed_packet' bytes
     */
    public static final int ER_TOO_LONG_STRING = 1162;

    /**
     * The used table type doesn't support BLOB/TEXT columns
     */
    public static final int ER_TABLE_CANT_HANDLE_BLOB = 1163;

    /**
     * The used table type doesn't support AUTO_INCREMENT columns
     */
    public static final int ER_TABLE_CANT_HANDLE_AUTO_INCREMENT = 1164;

    /**
     * INSERT DELAYED can't be used with table '%-.192s' because it is locked with LOCK TABLES
     */
    public static final int ER_DELAYED_INSERT_TABLE_LOCKED = 1165;

    /**
     * Incorrect column name '%-.100s'
     */
    public static final int ER_WRONG_COLUMN_NAME = 1166;

    /**
     * The used storage engine can't index column '%-.192s'
     */
    public static final int ER_WRONG_KEY_COLUMN = 1167;

    /**
     * Unable to open underlying table which is differently defined or of non-MyISAM type or doesn't exist
     */
    public static final int ER_WRONG_MRG_TABLE = 1168;

    /**
     * Can't write, because of unique constraint, to table '%-.192s'
     */
    public static final int ER_DUP_UNIQUE = 1169;

    /**
     * BLOB/TEXT column '%-.192s' used in key specification without a key length
     */
    public static final int ER_BLOB_KEY_WITHOUT_LENGTH = 1170;

    /**
     * All parts of a PRIMARY KEY must be NOT NULL; if you need NULL in a key, use UNIQUE instead
     */
    public static final int ER_PRIMARY_CANT_HAVE_NULL = 1171;

    /**
     * Result consisted of more than one row
     */
    public static final int ER_TOO_MANY_ROWS = 1172;

    /**
     * This table type requires a primary key
     */
    public static final int ER_REQUIRES_PRIMARY_KEY = 1173;

    /**
     * This version of MySQL is not compiled with RAID support
     */
    public static final int ER_NO_RAID_COMPILED = 1174;

    /**
     * You are using safe update mode and you tried to update a table without a WHERE that uses a KEY column
     */
    public static final int ER_UPDATE_WITHOUT_KEY_IN_SAFE_MODE = 1175;

    /**
     * Key '%-.192s' doesn't exist in table '%-.192s'
     */
    public static final int ER_KEY_DOES_NOT_EXITS = 1176;

    /**
     * Can't open table
     */
    public static final int ER_CHECK_NO_SUCH_TABLE = 1177;

    /**
     * The storage engine for the table doesn't support %s
     */
    public static final int ER_CHECK_NOT_IMPLEMENTED = 1178;

    /**
     * You are not allowed to execute this command in a transaction
     */
    public static final int ER_CANT_DO_THIS_DURING_AN_TRANSACTION = 1179;

    /**
     * Got error %d during COMMIT
     */
    public static final int ER_ERROR_DURING_COMMIT = 1180;

    /**
     * Got error %d during ROLLBACK
     */
    public static final int ER_ERROR_DURING_ROLLBACK = 1181;

    /**
     * Got error %d during FLUSH_LOGS
     */
    public static final int ER_ERROR_DURING_FLUSH_LOGS = 1182;

    /**
     * Got error %d during CHECKPOINT
     */
    public static final int ER_ERROR_DURING_CHECKPOINT = 1183;

    /**
     * Aborted connection %ld to db: '%-.192s' user: '%-.48s' host: '%-.64s' (%-.64s)
     */
    public static final int ER_NEW_ABORTING_CONNECTION = 1184;

    /**
     * The storage engine for the table does not support binary table dump
     */
    public static final int ER_DUMP_NOT_IMPLEMENTED = 1185;

    /**
     * Binlog closed, cannot RESET MASTER
     */
    public static final int ER_FLUSH_MASTER_BINLOG_CLOSED = 1186;

    /**
     * Failed rebuilding the index of  dumped table '%-.192s'
     */
    public static final int ER_INDEX_REBUILD = 1187;

    /**
     * Error from master: '%-.64s'
     */
    public static final int ER_MASTER = 1188;

    /**
     * Net error reading from master
     */
    public static final int ER_MASTER_NET_READ = 1189;

    /**
     * Net error writing to master
     */
    public static final int ER_MASTER_NET_WRITE = 1190;

    /**
     * Can't find FULLTEXT index matching the column list
     */
    public static final int ER_FT_MATCHING_KEY_NOT_FOUND = 1191;

    /**
     * Can't execute the given command because you have active locked tables or an active transaction
     */
    public static final int ER_LOCK_OR_ACTIVE_TRANSACTION = 1192;

    /**
     * Unknown system variable '%-.64s'
     */
    public static final int ER_UNKNOWN_SYSTEM_VARIABLE = 1193;

    /**
     * Table '%-.192s' is marked as crashed and should be repaired
     */
    public static final int ER_CRASHED_ON_USAGE = 1194;

    /**
     * Table '%-.192s' is marked as crashed and last (automatic?) repair failed
     */
    public static final int ER_CRASHED_ON_REPAIR = 1195;

    /**
     * Some non-transactional changed tables couldn't be rolled back
     */
    public static final int ER_WARNING_NOT_COMPLETE_ROLLBACK = 1196;

    /**
     * Multi-statement transaction required more than 'max_binlog_cache_size' bytes of storage; increase this mysqld
     * variable and try again
     */
    public static final int ER_TRANS_CACHE_FULL = 1197;

    /**
     * This operation cannot be performed with a running slave; run STOP SLAVE first
     */
    public static final int ER_SLAVE_MUST_STOP = 1198;

    /**
     * This operation requires a running slave; configure slave and do START SLAVE
     */
    public static final int ER_SLAVE_NOT_RUNNING = 1199;

    /**
     * The server is not configured as slave; fix in config file or with CHANGE MASTER TO
     */
    public static final int ER_BAD_SLAVE = 1200;

    /**
     * Could not initialize master info structure; more error messages can be found in the MySQL error log
     */
    public static final int ER_MASTER_INFO = 1201;

    /**
     * Could not create slave thread; check system resources
     */
    public static final int ER_SLAVE_THREAD = 1202;

    /**
     * User %-.64s already has more than 'max_user_connections' active connections
     */
    public static final int ER_TOO_MANY_USER_CONNECTIONS = 1203;

    /**
     * You may only use constant expressions with SET
     */
    public static final int ER_SET_CONSTANTS_ONLY = 1204;

    /**
     * Lock wait timeout exceeded; try restarting transaction
     */
    public static final int ER_LOCK_WAIT_TIMEOUT = 1205;

    /**
     * The total number of locks exceeds the lock table size
     */
    public static final int ER_LOCK_TABLE_FULL = 1206;

    /**
     * Update locks cannot be acquired during a READ UNCOMMITTED transaction
     */
    public static final int ER_READ_ONLY_TRANSACTION = 1207;

    /**
     * DROP DATABASE not allowed while thread is holding global read lock
     */
    public static final int ER_DROP_DB_WITH_READ_LOCK = 1208;

    /**
     * CREATE DATABASE not allowed while thread is holding global read lock
     */
    public static final int ER_CREATE_DB_WITH_READ_LOCK = 1209;

    /**
     * Incorrect arguments to %s
     */
    public static final int ER_WRONG_ARGUMENTS = 1210;

    /**
     * '%-.48s'@'%-.64s' is not allowed to create new users
     */
    public static final int ER_NO_PERMISSION_TO_CREATE_USER = 1211;

    /**
     * Incorrect table definition; all MERGE tables must be in the same database
     */
    public static final int ER_UNION_TABLES_IN_DIFFERENT_DIR = 1212;

    /**
     * Deadlock found when trying to get lock; try restarting transaction
     */
    public static final int ER_LOCK_DEADLOCK = 1213;

    /**
     * The used table type doesn't support FULLTEXT indexes
     */
    public static final int ER_TABLE_CANT_HANDLE_FT = 1214;

    /**
     * Cannot add foreign key constraint
     */
    public static final int ER_CANNOT_ADD_FOREIGN = 1215;

    /**
     * Cannot add or update a child row: a foreign key constraint fails
     */
    public static final int ER_NO_REFERENCED_ROW = 1216;

    /**
     * Cannot delete or update a parent row: a foreign key constraint fails
     */
    public static final int ER_ROW_IS_REFERENCED = 1217;

    /**
     * Error connecting to master: %-.128s
     */
    public static final int ER_CONNECT_TO_MASTER = 1218;

    /**
     * Error running query on master: %-.128s
     */
    public static final int ER_QUERY_ON_MASTER = 1219;

    /**
     * Error when executing command %s: %-.128s
     */
    public static final int ER_ERROR_WHEN_EXECUTING_COMMAND = 1220;

    /**
     * Incorrect usage of %s and %s
     */
    public static final int ER_WRONG_USAGE = 1221;

    /**
     * The used SELECT statements have a different number of columns
     */
    public static final int ER_WRONG_NUMBER_OF_COLUMNS_IN_SELECT = 1222;

    /**
     * Can't execute the query because you have a conflicting read lock
     */
    public static final int ER_CANT_UPDATE_WITH_READLOCK = 1223;

    /**
     * Mixing of transactional and non-transactional tables is disabled
     */
    public static final int ER_MIXING_NOT_ALLOWED = 1224;

    /**
     * Option '%s' used twice in statement
     */
    public static final int ER_DUP_ARGUMENT = 1225;

    /**
     * User '%-.64s' has exceeded the '%s' resource (current value: %ld)
     */
    public static final int ER_USER_LIMIT_REACHED = 1226;

    /**
     * Access denied; you need (at least one of) the %-.128s privilege(s) for this operation
     */
    public static final int ER_SPECIFIC_ACCESS_DENIED_ERROR = 1227;

    /**
     * Variable '%-.64s' is a SESSION variable and can't be used with SET GLOBAL
     */
    public static final int ER_LOCAL_VARIABLE = 1228;

    /**
     * Variable '%-.64s' is a GLOBAL variable and should be set with SET GLOBAL
     */
    public static final int ER_GLOBAL_VARIABLE = 1229;

    /**
     * Variable '%-.64s' doesn't have a default value
     */
    public static final int ER_NO_DEFAULT = 1230;

    /**
     * Variable '%-.64s' can't be set to the value of '%-.200s'
     */
    public static final int ER_WRONG_VALUE_FOR_VAR = 1231;

    /**
     * Incorrect argument type to variable '%-.64s'
     */
    public static final int ER_WRONG_TYPE_FOR_VAR = 1232;

    /**
     * Variable '%-.64s' can only be set, not read
     */
    public static final int ER_VAR_CANT_BE_READ = 1233;

    /**
     * Incorrect usage/placement of '%s'
     */
    public static final int ER_CANT_USE_OPTION_HERE = 1234;

    /**
     * This version of MySQL doesn't yet support '%s'
     */
    public static final int ER_NOT_SUPPORTED_YET = 1235;

    /**
     * Got fatal error %d from master when reading data from binary log: '%-.320s'
     */
    public static final int ER_MASTER_FATAL_ERROR_READING_BINLOG = 1236;

    /**
     * Slave SQL thread ignored the query because of replicate-*-table rules
     */
    public static final int ER_SLAVE_IGNORED_TABLE = 1237;

    /**
     * Variable '%-.192s' is a %s variable
     */
    public static final int ER_INCORRECT_GLOBAL_LOCAL_VAR = 1238;

    /**
     * Incorrect foreign key definition for '%-.192s': %s
     */
    public static final int ER_WRONG_FK_DEF = 1239;

    /**
     * Key reference and table reference don't match
     */
    public static final int ER_KEY_REF_DO_NOT_MATCH_TABLE_REF = 1240;

    /**
     * Operand should contain %d column(s)
     */
    public static final int ER_OPERAND_COLUMNS = 1241;

    /**
     * Subquery returns more than 1 row
     */
    public static final int ER_SUBQUERY_NO_1_ROW = 1242;

    /**
     * Unknown prepared statement handler (%.*s) given to %s
     */
    public static final int ER_UNKNOWN_STMT_HANDLER = 1243;

    /**
     * Help database is corrupt or does not exist
     */
    public static final int ER_CORRUPT_HELP_DB = 1244;

    /**
     * Cyclic reference on subqueries
     */
    public static final int ER_CYCLIC_REFERENCE = 1245;

    /**
     * Converting column '%s' from %s to %s
     */
    public static final int ER_AUTO_CONVERT = 1246;

    /**
     * Reference '%-.64s' not supported (%s)
     */
    public static final int ER_ILLEGAL_REFERENCE = 1247;

    /**
     * Every derived table must have its own alias
     */
    public static final int ER_DERIVED_MUST_HAVE_ALIAS = 1248;

    /**
     * Select %u was reduced during optimization
     */
    public static final int ER_SELECT_REDUCED = 1249;

    /**
     * Table '%-.192s' from one of the SELECTs cannot be used in %-.32s
     */
    public static final int ER_TABLENAME_NOT_ALLOWED_HERE = 1250;

    /**
     * Client does not support authentication protocol requested by server; consider upgrading MySQL client
     */
    public static final int ER_NOT_SUPPORTED_AUTH_MODE = 1251;

    /**
     * All parts of a SPATIAL index must be NOT NULL
     */
    public static final int ER_SPATIAL_CANT_HAVE_NULL = 1252;

    /**
     * COLLATION '%s' is not valid for CHARACTER SET '%s'
     */
    public static final int ER_COLLATION_CHARSET_MISMATCH = 1253;

    /**
     * Slave is already running
     */
    public static final int ER_SLAVE_WAS_RUNNING = 1254;

    /**
     * Slave already has been stopped
     */
    public static final int ER_SLAVE_WAS_NOT_RUNNING = 1255;

    /**
     * Uncompressed data size too large; the maximum size is %d (probably, length of uncompressed data was corrupted)
     */
    public static final int ER_TOO_BIG_FOR_UNCOMPRESS = 1256;

    /**
     * ZLIB: Not enough memory
     */
    public static final int ER_ZLIB_Z_MEM_ERROR = 1257;

    /**
     * ZLIB: Not enough room in the output buffer (probably, length of uncompressed data was corrupted)
     */
    public static final int ER_ZLIB_Z_BUF_ERROR = 1258;

    /**
     * ZLIB: Input data corrupted
     */
    public static final int ER_ZLIB_Z_DATA_ERROR = 1259;

    /**
     * Row %u was cut by GROUP_CONCAT()
     */
    public static final int ER_CUT_VALUE_GROUP_CONCAT = 1260;

    /**
     * Row %ld doesn't contain data for all columns
     */
    public static final int ER_WARN_TOO_FEW_RECORDS = 1261;

    /**
     * Row %ld was truncated; it contained more data than there were input columns
     */
    public static final int ER_WARN_TOO_MANY_RECORDS = 1262;

    /**
     * Column set to default value; NULL supplied to NOT NULL column '%s' at row %ld
     */
    public static final int ER_WARN_NULL_TO_NOTNULL = 1263;

    /**
     * Out of range value for column '%s' at row %ld
     */
    public static final int ER_WARN_DATA_OUT_OF_RANGE = 1264;

    /**
     * Data truncated for column '%s' at row %ld
     */
    public static final int WARN_DATA_TRUNCATED = 1265;

    /**
     * Using storage engine %s for table '%s'
     */
    public static final int ER_WARN_USING_OTHER_HANDLER = 1266;

    /**
     * Illegal mix of collations (%s,%s) and (%s,%s) for operation '%s'
     */
    public static final int ER_CANT_AGGREGATE_2COLLATIONS = 1267;

    /**
     * Cannot drop one or more of the requested users
     */
    public static final int ER_DROP_USER = 1268;

    /**
     * Can't revoke all privileges for one or more of the requested users
     */
    public static final int ER_REVOKE_GRANTS = 1269;

    /**
     * Illegal mix of collations (%s,%s), (%s,%s), (%s,%s) for operation '%s'
     */
    public static final int ER_CANT_AGGREGATE_3COLLATIONS = 1270;

    /**
     * Illegal mix of collations for operation '%s'
     */
    public static final int ER_CANT_AGGREGATE_NCOLLATIONS = 1271;

    /**
     * Variable '%-.64s' is not a variable component (can't be used as XXXX.variable_name)
     */
    public static final int ER_VARIABLE_IS_NOT_STRUCT = 1272;

    /**
     * Unknown collation: '%-.64s'
     */
    public static final int ER_UNKNOWN_COLLATION = 1273;

    /**
     * SSL parameters in CHANGE MASTER are ignored because this MySQL slave was compiled without SSL support; they
     * can be used later if MySQL slave with SSL is started
     */
    public static final int ER_SLAVE_IGNORED_SSL_PARAMS = 1274;

    /**
     * Server is running in --secure-auth mode, but '%s'@'%s' has a password in the old format; please change the
     * password to the new format
     */
    public static final int ER_SERVER_IS_IN_SECURE_AUTH_MODE = 1275;

    /**
     * Field or reference '%-.192s%s%-.192s%s%-.192s' of SELECT #%d was resolved in SELECT #%d
     */
    public static final int ER_WARN_FIELD_RESOLVED = 1276;

    /**
     * Incorrect parameter or combination of parameters for START SLAVE UNTIL
     */
    public static final int ER_BAD_SLAVE_UNTIL_COND = 1277;

    /**
     * It is recommended to use --skip-slave-start when doing step-by-step replication with START SLAVE UNTIL;
     * otherwise, you will get problems if you get an unexpected slave's mysqld restart
     */
    public static final int ER_MISSING_SKIP_SLAVE = 1278;

    /**
     * SQL thread is not to be started so UNTIL options are ignored
     */
    public static final int ER_UNTIL_COND_IGNORED = 1279;

    /**
     * Incorrect index name '%-.100s'
     */
    public static final int ER_WRONG_NAME_FOR_INDEX = 1280;

    /**
     * Incorrect catalog name '%-.100s'
     */
    public static final int ER_WRONG_NAME_FOR_CATALOG = 1281;

    /**
     * Query cache failed to set size %lu; new query cache size is %lu
     */
    public static final int ER_WARN_QC_RESIZE = 1282;

    /**
     * Column '%-.192s' cannot be part of FULLTEXT index
     */
    public static final int ER_BAD_FT_COLUMN = 1283;

    /**
     * Unknown key cache '%-.100s'
     */
    public static final int ER_UNKNOWN_KEY_CACHE = 1284;

    /**
     * MySQL is started in --skip-name-resolve mode; you must restart it without this switch for this grant to work
     */
    public static final int ER_WARN_HOSTNAME_WONT_WORK = 1285;

    /**
     * Unknown storage engine '%s'
     */
    public static final int ER_UNKNOWN_STORAGE_ENGINE = 1286;

    /**
     * '%s' is deprecated and will be removed in a future release. Please use %s instead
     */
    public static final int ER_WARN_DEPRECATED_SYNTAX = 1287;

    /**
     * The target table %-.100s of the %s is not updatable
     */
    public static final int ER_NON_UPDATABLE_TABLE = 1288;

    /**
     * The '%s' feature is disabled; you need MySQL built with '%s' to have it working
     */
    public static final int ER_FEATURE_DISABLED = 1289;

    /**
     * The MySQL server is running with the %s option so it cannot execute this statement
     */
    public static final int ER_OPTION_PREVENTS_STATEMENT = 1290;

    /**
     * Column '%-.100s' has duplicated value '%-.64s' in %s
     */
    public static final int ER_DUPLICATED_VALUE_IN_TYPE = 1291;

    /**
     * Truncated incorrect %-.32s value: '%-.128s'
     */
    public static final int ER_TRUNCATED_WRONG_VALUE = 1292;

    /**
     * Incorrect table definition; there can be only one TIMESTAMP column with CURRENT_TIMESTAMP in DEFAULT or ON
     * UPDATE clause
     */
    public static final int ER_TOO_MUCH_AUTO_TIMESTAMP_COLS = 1293;

    /**
     * Invalid ON UPDATE clause for '%-.192s' column
     */
    public static final int ER_INVALID_ON_UPDATE = 1294;

    /**
     * This command is not supported in the prepared statement protocol yet
     */
    public static final int ER_UNSUPPORTED_PS = 1295;

    /**
     * Got error %d '%-.100s' from %s
     */
    public static final int ER_GET_ERRMSG = 1296;

    /**
     * Got temporary error %d '%-.100s' from %s
     */
    public static final int ER_GET_TEMPORARY_ERRMSG = 1297;

    /**
     * Unknown or incorrect time zone: '%-.64s'
     */
    public static final int ER_UNKNOWN_TIME_ZONE = 1298;

    /**
     * Invalid TIMESTAMP value in column '%s' at row %ld
     */
    public static final int ER_WARN_INVALID_TIMESTAMP = 1299;

    /**
     * Invalid %s character string: '%.64s'
     */
    public static final int ER_INVALID_CHARACTER_STRING = 1300;

    /**
     * Result of %s() was larger than max_allowed_packet (%ld) - truncated
     */
    public static final int ER_WARN_ALLOWED_PACKET_OVERFLOWED = 1301;

    /**
     * Conflicting declarations: '%s%s' and '%s%s'
     */
    public static final int ER_CONFLICTING_DECLARATIONS = 1302;

    /**
     * Can't create a %s from within another stored routine
     */
    public static final int ER_SP_NO_RECURSIVE_CREATE = 1303;

    /**
     * %s %s already exists
     */
    public static final int ER_SP_ALREADY_EXISTS = 1304;

    /**
     * %s %s does not exist
     */
    public static final int ER_SP_DOES_NOT_EXIST = 1305;

    /**
     * Failed to DROP %s %s
     */
    public static final int ER_SP_DROP_FAILED = 1306;

    /**
     * Failed to CREATE %s %s
     */
    public static final int ER_SP_STORE_FAILED = 1307;

    /**
     * %s with no matching label: %s
     */
    public static final int ER_SP_LILABEL_MISMATCH = 1308;

    /**
     * Redefining label %s
     */
    public static final int ER_SP_LABEL_REDEFINE = 1309;

    /**
     * End-label %s without match
     */
    public static final int ER_SP_LABEL_MISMATCH = 1310;

    /**
     * Referring to uninitialized variable %s
     */
    public static final int ER_SP_UNINIT_VAR = 1311;

    /**
     * PROCEDURE %s can't return a result set in the given context
     */
    public static final int ER_SP_BADSELECT = 1312;

    /**
     * RETURN is only allowed in a FUNCTION
     */
    public static final int ER_SP_BADRETURN = 1313;

    /**
     * %s is not allowed in stored procedures
     */
    public static final int ER_SP_BADSTATEMENT = 1314;

    /**
     * The update log is deprecated and replaced by the binary log; SET SQL_LOG_UPDATE has been ignored.
     */
    public static final int ER_UPDATE_LOG_DEPRECATED_IGNORED = 1315;

    /**
     * The update log is deprecated and replaced by the binary log; SET SQL_LOG_UPDATE has been translated to SET
     * SQL_LOG_BIN.
     */
    public static final int ER_UPDATE_LOG_DEPRECATED_TRANSLATED = 1316;

    /**
     * Query execution was interrupted
     */
    public static final int ER_QUERY_INTERRUPTED = 1317;

    /**
     * Incorrect number of arguments for %s %s; expected %u, got %u
     */
    public static final int ER_SP_WRONG_NO_OF_ARGS = 1318;

    /**
     * Undefined CONDITION: %s
     */
    public static final int ER_SP_COND_MISMATCH = 1319;

    /**
     * No RETURN found in FUNCTION %s
     */
    public static final int ER_SP_NORETURN = 1320;

    /**
     * FUNCTION %s ended without RETURN
     */
    public static final int ER_SP_NORETURNEND = 1321;

    /**
     * Cursor statement must be a SELECT
     */
    public static final int ER_SP_BAD_CURSOR_QUERY = 1322;

    /**
     * Cursor SELECT must not have INTO
     */
    public static final int ER_SP_BAD_CURSOR_SELECT = 1323;

    /**
     * Undefined CURSOR: %s
     */
    public static final int ER_SP_CURSOR_MISMATCH = 1324;

    /**
     * Cursor is already open
     */
    public static final int ER_SP_CURSOR_ALREADY_OPEN = 1325;

    /**
     * Cursor is not open
     */
    public static final int ER_SP_CURSOR_NOT_OPEN = 1326;

    /**
     * Undeclared variable: %s
     */
    public static final int ER_SP_UNDECLARED_VAR = 1327;

    /**
     * Incorrect number of FETCH variables
     */
    public static final int ER_SP_WRONG_NO_OF_FETCH_ARGS = 1328;

    /**
     * No data - zero rows fetched, selected, or processed
     */
    public static final int ER_SP_FETCH_NO_DATA = 1329;

    /**
     * Duplicate parameter: %s
     */
    public static final int ER_SP_DUP_PARAM = 1330;

    /**
     * Duplicate variable: %s
     */
    public static final int ER_SP_DUP_VAR = 1331;

    /**
     * Duplicate condition: %s
     */
    public static final int ER_SP_DUP_COND = 1332;

    /**
     * Duplicate cursor: %s
     */
    public static final int ER_SP_DUP_CURS = 1333;

    /**
     * Failed to ALTER %s %s
     */
    public static final int ER_SP_CANT_ALTER = 1334;

    /**
     * Subquery value not supported
     */
    public static final int ER_SP_SUBSELECT_NYI = 1335;

    /**
     * %s is not allowed in stored function or trigger
     */
    public static final int ER_STMT_NOT_ALLOWED_IN_SF_OR_TRG = 1336;

    /**
     * Variable or condition declaration after cursor or handler declaration
     */
    public static final int ER_SP_VARCOND_AFTER_CURSHNDLR = 1337;

    /**
     * Cursor declaration after handler declaration
     */
    public static final int ER_SP_CURSOR_AFTER_HANDLER = 1338;

    /**
     * Case not found for CASE statement
     */
    public static final int ER_SP_CASE_NOT_FOUND = 1339;

    /**
     * Configuration file '%-.192s' is too big
     */
    public static final int ER_FPARSER_TOO_BIG_FILE = 1340;

    /**
     * Malformed file type header in file '%-.192s'
     */
    public static final int ER_FPARSER_BAD_HEADER = 1341;

    /**
     * Unexpected end of file while parsing comment '%-.200s'
     */
    public static final int ER_FPARSER_EOF_IN_COMMENT = 1342;

    /**
     * Error while parsing parameter '%-.192s' (line: '%-.192s')
     */
    public static final int ER_FPARSER_ERROR_IN_PARAMETER = 1343;

    /**
     * Unexpected end of file while skipping unknown parameter '%-.192s'
     */
    public static final int ER_FPARSER_EOF_IN_UNKNOWN_PARAMETER = 1344;

    /**
     * EXPLAIN/SHOW can not be issued; lacking privileges for underlying table
     */
    public static final int ER_VIEW_NO_EXPLAIN = 1345;

    /**
     * File '%-.192s' has unknown type '%-.64s' in its header
     */
    public static final int ER_FRM_UNKNOWN_TYPE = 1346;

    /**
     * '%-.192s.%-.192s' is not %s
     */
    public static final int ER_WRONG_OBJECT = 1347;

    /**
     * Column '%-.192s' is not updatable
     */
    public static final int ER_NONUPDATEABLE_COLUMN = 1348;

    /**
     * View's SELECT contains a subquery in the FROM clause
     */
    public static final int ER_VIEW_SELECT_DERIVED = 1349;

    /**
     * View's SELECT contains a '%s' clause
     */
    public static final int ER_VIEW_SELECT_CLAUSE = 1350;

    /**
     * View's SELECT contains a variable or parameter
     */
    public static final int ER_VIEW_SELECT_VARIABLE = 1351;

    /**
     * View's SELECT refers to a temporary table '%-.192s'
     */
    public static final int ER_VIEW_SELECT_TMPTABLE = 1352;

    /**
     * View's SELECT and view's field list have different column counts
     */
    public static final int ER_VIEW_WRONG_LIST = 1353;

    /**
     * View merge algorithm can't be used here for now (assumed undefined algorithm)
     */
    public static final int ER_WARN_VIEW_MERGE = 1354;

    /**
     * View being updated does not have complete key of underlying table in it
     */
    public static final int ER_WARN_VIEW_WITHOUT_KEY = 1355;

    /**
     * View '%-.192s.%-.192s' references invalid table(s) or column(s) or function(s) or definer/invoker of view lack
     * rights to use them
     */
    public static final int ER_VIEW_INVALID = 1356;

    /**
     * Can't drop or alter a %s from within another stored routine
     */
    public static final int ER_SP_NO_DROP_SP = 1357;

    /**
     * GOTO is not allowed in a stored procedure handler
     */
    public static final int ER_SP_GOTO_IN_HNDLR = 1358;

    /**
     * Trigger already exists
     */
    public static final int ER_TRG_ALREADY_EXISTS = 1359;

    /**
     * Trigger does not exist
     */
    public static final int ER_TRG_DOES_NOT_EXIST = 1360;

    /**
     * Trigger's '%-.192s' is view or temporary table
     */
    public static final int ER_TRG_ON_VIEW_OR_TEMP_TABLE = 1361;

    /**
     * Updating of %s row is not allowed in %strigger
     */
    public static final int ER_TRG_CANT_CHANGE_ROW = 1362;

    /**
     * There is no %s row in %s trigger
     */
    public static final int ER_TRG_NO_SUCH_ROW_IN_TRG = 1363;

    /**
     * Field '%-.192s' doesn't have a default value
     */
    public static final int ER_NO_DEFAULT_FOR_FIELD = 1364;

    /**
     * Division by 0
     */
    public static final int ER_DIVISION_BY_ZERO = 1365;

    /**
     * Incorrect %-.32s value: '%-.128s' for column '%.192s' at row %ld
     */
    public static final int ER_TRUNCATED_WRONG_VALUE_FOR_FIELD = 1366;

    /**
     * Illegal %s '%-.192s' value found during parsing
     */
    public static final int ER_ILLEGAL_VALUE_FOR_TYPE = 1367;

    /**
     * CHECK OPTION on non-updatable view '%-.192s.%-.192s'
     */
    public static final int ER_VIEW_NONUPD_CHECK = 1368;

    /**
     * CHECK OPTION failed '%-.192s.%-.192s'
     */
    public static final int ER_VIEW_CHECK_FAILED = 1369;

    /**
     * %-.16s command denied to user '%-.48s'@'%-.64s' for routine '%-.192s'
     */
    public static final int ER_PROCACCESS_DENIED_ERROR = 1370;

    /**
     * Failed purging old relay logs: %s
     */
    public static final int ER_RELAY_LOG_FAIL = 1371;

    /**
     * Password hash should be a %d-digit hexadecimal number
     */
    public static final int ER_PASSWD_LENGTH = 1372;

    /**
     * Target log not found in binlog index
     */
    public static final int ER_UNKNOWN_TARGET_BINLOG = 1373;

    /**
     * I/O error reading log index file
     */
    public static final int ER_IO_ERR_LOG_INDEX_READ = 1374;

    /**
     * Server configuration does not permit binlog purge
     */
    public static final int ER_BINLOG_PURGE_PROHIBITED = 1375;

    /**
     * Failed on fseek()
     */
    public static final int ER_FSEEK_FAIL = 1376;

    /**
     * Fatal error during log purge
     */
    public static final int ER_BINLOG_PURGE_FATAL_ERR = 1377;

    /**
     * A purgeable log is in use, will not purge
     */
    public static final int ER_LOG_IN_USE = 1378;

    /**
     * Unknown error during log purge
     */
    public static final int ER_LOG_PURGE_UNKNOWN_ERR = 1379;

    /**
     * Failed initializing relay log position: %s
     */
    public static final int ER_RELAY_LOG_INIT = 1380;

    /**
     * You are not using binary logging
     */
    public static final int ER_NO_BINARY_LOGGING = 1381;

    /**
     * The '%-.64s' syntax is reserved for purposes internal to the MySQL server
     */
    public static final int ER_RESERVED_SYNTAX = 1382;

    /**
     * WSAStartup Failed
     */
    public static final int ER_WSAS_FAILED = 1383;

    /**
     * Can't handle procedures with different groups yet
     */
    public static final int ER_DIFF_GROUPS_PROC = 1384;

    /**
     * Select must have a group with this procedure
     */
    public static final int ER_NO_GROUP_FOR_PROC = 1385;

    /**
     * Can't use ORDER clause with this procedure
     */
    public static final int ER_ORDER_WITH_PROC = 1386;

    /**
     * Binary logging and replication forbid changing the global server %s
     */
    public static final int ER_LOGGING_PROHIBIT_CHANGING_OF = 1387;

    /**
     * Can't map file: %-.200s, errno: %d
     */
    public static final int ER_NO_FILE_MAPPING = 1388;

    /**
     * Wrong magic in %-.64s
     */
    public static final int ER_WRONG_MAGIC = 1389;

    /**
     * Prepared statement contains too many placeholders
     */
    public static final int ER_PS_MANY_PARAM = 1390;

    /**
     * Key part '%-.192s' length cannot be 0
     */
    public static final int ER_KEY_PART_0 = 1391;

    /**
     * View text checksum failed
     */
    public static final int ER_VIEW_CHECKSUM = 1392;

    /**
     * Can not modify more than one base table through a join view '%-.192s.%-.192s'
     */
    public static final int ER_VIEW_MULTIUPDATE = 1393;

    /**
     * Can not insert into join view '%-.192s.%-.192s' without fields list
     */
    public static final int ER_VIEW_NO_INSERT_FIELD_LIST = 1394;

    /**
     * Can not delete from join view '%-.192s.%-.192s'
     */
    public static final int ER_VIEW_DELETE_MERGE_VIEW = 1395;

    /**
     * Operation %s failed for %.256s
     */
    public static final int ER_CANNOT_USER = 1396;

    /**
     * XAER_NOTA: Unknown XID
     */
    public static final int ER_XAER_NOTA = 1397;

    /**
     * XAER_INVAL: Invalid arguments (or unsupported command)
     */
    public static final int ER_XAER_INVAL = 1398;

    /**
     * XAER_RMFAIL: The command cannot be executed when global transaction is in the  %.64s state
     */
    public static final int ER_XAER_RMFAIL = 1399;

    /**
     * XAER_OUTSIDE: Some work is done outside global transaction
     */
    public static final int ER_XAER_OUTSIDE = 1400;

    /**
     * XAER_RMERR: Fatal error occurred in the transaction branch - check your data for consistency
     */
    public static final int ER_XAER_RMERR = 1401;

    /**
     * XA_RBROLLBACK: Transaction branch was rolled back
     */
    public static final int ER_XA_RBROLLBACK = 1402;

    /**
     * There is no such grant defined for user '%-.48s' on host '%-.64s' on routine '%-.192s'
     */
    public static final int ER_NONEXISTING_PROC_GRANT = 1403;

    /**
     * Failed to grant EXECUTE and ALTER ROUTINE privileges
     */
    public static final int ER_PROC_AUTO_GRANT_FAIL = 1404;

    /**
     * Failed to revoke all privileges to dropped routine
     */
    public static final int ER_PROC_AUTO_REVOKE_FAIL = 1405;

    /**
     * Data too long for column '%s' at row %ld
     */
    public static final int ER_DATA_TOO_LONG = 1406;

    /**
     * Bad SQLSTATE: '%s'
     */
    public static final int ER_SP_BAD_SQLSTATE = 1407;

    /**
     * %s: ready for connections.\nVersion: '%s'  socket: '%s'  port: %d  %s
     */
    public static final int ER_STARTUP = 1408;

    /**
     * Can't load value from file with fixed size rows to variable
     */
    public static final int ER_LOAD_FROM_FIXED_SIZE_ROWS_TO_VAR = 1409;

    /**
     * You are not allowed to create a user with GRANT
     */
    public static final int ER_CANT_CREATE_USER_WITH_GRANT = 1410;

    /**
     * Incorrect %-.32s value: '%-.128s' for function %-.32s
     */
    public static final int ER_WRONG_VALUE_FOR_TYPE = 1411;

    /**
     * Table definition has changed, please retry transaction
     */
    public static final int ER_TABLE_DEF_CHANGED = 1412;

    /**
     * Duplicate handler declared in the same block
     */
    public static final int ER_SP_DUP_HANDLER = 1413;

    /**
     * OUT or INOUT argument %d for routine %s is not a variable or NEW pseudo-variable in BEFORE trigger
     */
    public static final int ER_SP_NOT_VAR_ARG = 1414;

    /**
     * Not allowed to return a result set from a %s
     */
    public static final int ER_SP_NO_RETSET = 1415;

    /**
     * Cannot get geometry object from data you send to the GEOMETRY field
     */
    public static final int ER_CANT_CREATE_GEOMETRY_OBJECT = 1416;

    /**
     * A routine failed and has neither NO SQL nor READS SQL DATA in its declaration and binary logging is enabled;
     * if non-transactional tables were updated, the binary log will miss their changes
     */
    public static final int ER_FAILED_ROUTINE_BREAK_BINLOG = 1417;

    /**
     * This function has none of DETERMINISTIC, NO SQL, or READS SQL DATA in its declaration and binary logging is
     * enabled (you *might* want to use the less safe log_bin_trust_function_creators variable)
     */
    public static final int ER_BINLOG_UNSAFE_ROUTINE = 1418;

    /**
     * You do not have the SUPER privilege and binary logging is enabled (you *might* want to use the less safe
     * log_bin_trust_function_creators variable)
     */
    public static final int ER_BINLOG_CREATE_ROUTINE_NEED_SUPER = 1419;

    /**
     * You can't execute a prepared statement which has an open cursor associated with it. Reset the statement to
     * re-execute it.
     */
    public static final int ER_EXEC_STMT_WITH_OPEN_CURSOR = 1420;

    /**
     * The statement (%lu) has no open cursor.
     */
    public static final int ER_STMT_HAS_NO_OPEN_CURSOR = 1421;

    /**
     * Explicit or implicit commit is not allowed in stored function or trigger.
     */
    public static final int ER_COMMIT_NOT_ALLOWED_IN_SF_OR_TRG = 1422;

    /**
     * Field of view '%-.192s.%-.192s' underlying table doesn't have a default value
     */
    public static final int ER_NO_DEFAULT_FOR_VIEW_FIELD = 1423;

    /**
     * Recursive stored functions and triggers are not allowed.
     */
    public static final int ER_SP_NO_RECURSION = 1424;

    /**
     * Too big scale %d specified for column '%-.192s'. Maximum is %lu.
     */
    public static final int ER_TOO_BIG_SCALE = 1425;

    /**
     * Too big precision %d specified for column '%-.192s'. Maximum is %lu.
     */
    public static final int ER_TOO_BIG_PRECISION = 1426;

    /**
     * For float(M,D), double(M,D) or decimal(M,D), M must be >= D (column '%-.192s').
     */
    public static final int ER_M_BIGGER_THAN_D = 1427;

    /**
     * You can't combine write-locking of system tables with other tables or lock types
     */
    public static final int ER_WRONG_LOCK_OF_SYSTEM_TABLE = 1428;

    /**
     * Unable to connect to foreign data source: %.64s
     */
    public static final int ER_CONNECT_TO_FOREIGN_DATA_SOURCE = 1429;

    /**
     * There was a problem processing the query on the foreign data source. Data source error: %-.64s
     */
    public static final int ER_QUERY_ON_FOREIGN_DATA_SOURCE = 1430;

    /**
     * The foreign data source you are trying to reference does not exist. Data source error:  %-.64s
     */
    public static final int ER_FOREIGN_DATA_SOURCE_DOESNT_EXIST = 1431;

    /**
     * Can't create federated table. The data source connection string '%-.64s' is not in the correct format
     */
    public static final int ER_FOREIGN_DATA_STRING_INVALID_CANT_CREATE = 1432;

    /**
     * The data source connection string '%-.64s' is not in the correct format
     */
    public static final int ER_FOREIGN_DATA_STRING_INVALID = 1433;

    /**
     * Can't create federated table. Foreign data src error:  %-.64s
     */
    public static final int ER_CANT_CREATE_FEDERATED_TABLE = 1434;

    /**
     * Trigger in wrong schema
     */
    public static final int ER_TRG_IN_WRONG_SCHEMA = 1435;

    /**
     * Thread stack overrun:  %ld bytes used of a %ld byte stack, and %ld bytes needed.  Use 'mysqld
     * --thread_stack=#' to specify a bigger stack.
     */
    public static final int ER_STACK_OVERRUN_NEED_MORE = 1436;

    /**
     * Routine body for '%-.100s' is too long
     */
    public static final int ER_TOO_LONG_BODY = 1437;

    /**
     * Cannot drop default keycache
     */
    public static final int ER_WARN_CANT_DROP_DEFAULT_KEYCACHE = 1438;

    /**
     * Display width out of range for column '%-.192s' (max = %lu)
     */
    public static final int ER_TOO_BIG_DISPLAYWIDTH = 1439;

    /**
     * XAER_DUPID: The XID already exists
     */
    public static final int ER_XAER_DUPID = 1440;

    /**
     * Datetime function: %-.32s field overflow
     */
    public static final int ER_DATETIME_FUNCTION_OVERFLOW = 1441;

    /**
     * Can't update table '%-.192s' in stored function/trigger because it is already used by statement which invoked
     * this stored function/trigger.
     */
    public static final int ER_CANT_UPDATE_USED_TABLE_IN_SF_OR_TRG = 1442;

    /**
     * The definition of table '%-.192s' prevents operation %.192s on table '%-.192s'.
     */
    public static final int ER_VIEW_PREVENT_UPDATE = 1443;

    /**
     * The prepared statement contains a stored routine call that refers to that same statement. It's not allowed to
     * execute a prepared statement in such a recursive manner
     */
    public static final int ER_PS_NO_RECURSION = 1444;

    /**
     * Not allowed to set autocommit from a stored function or trigger
     */
    public static final int ER_SP_CANT_SET_AUTOCOMMIT = 1445;

    /**
     * Definer is not fully qualified
     */
    public static final int ER_MALFORMED_DEFINER = 1446;

    /**
     * View '%-.192s'.'%-.192s' has no definer information (old table format). Current user is used as definer.
     * Please recreate the view!
     */
    public static final int ER_VIEW_FRM_NO_USER = 1447;

    /**
     * You need the SUPER privilege for creation view with '%-.192s'@'%-.192s' definer
     */
    public static final int ER_VIEW_OTHER_USER = 1448;

    /**
     * The user specified as a definer ('%-.64s'@'%-.64s') does not exist
     */
    public static final int ER_NO_SUCH_USER = 1449;

    /**
     * Changing schema from '%-.192s' to '%-.192s' is not allowed.
     */
    public static final int ER_FORBID_SCHEMA_CHANGE = 1450;

    /**
     * Cannot delete or update a parent row: a foreign key constraint fails (%.192s)
     */
    public static final int ER_ROW_IS_REFERENCED_2 = 1451;

    /**
     * Cannot add or update a child row: a foreign key constraint fails (%.192s)
     */
    public static final int ER_NO_REFERENCED_ROW_2 = 1452;

    /**
     * Variable '%-.64s' must be quoted with `...`, or renamed
     */
    public static final int ER_SP_BAD_VAR_SHADOW = 1453;

    /**
     * No definer attribute for trigger '%-.192s'.'%-.192s'. The trigger will be activated under the authorization of
     * the invoker, which may have insufficient privileges. Please recreate the trigger.
     */
    public static final int ER_TRG_NO_DEFINER = 1454;

    /**
     * '%-.192s' has an old format, you should re-create the '%s' object(s)
     */
    public static final int ER_OLD_FILE_FORMAT = 1455;

    /**
     * Recursive limit %d (as set by the max_sp_recursion_depth variable) was exceeded for routine %.192s
     */
    public static final int ER_SP_RECURSION_LIMIT = 1456;

    /**
     * Failed to load routine %-.192s. The table mysql.proc is missing, corrupt, or contains bad data (internal code
     * %d)
     */
    public static final int ER_SP_PROC_TABLE_CORRUPT = 1457;

    /**
     * Incorrect routine name '%-.192s'
     */
    public static final int ER_SP_WRONG_NAME = 1458;

    /**
     * Table upgrade required. Please do \"REPAIR TABLE `%-.32s`\" or dump/reload to fix it!
     */
    public static final int ER_TABLE_NEEDS_UPGRADE = 1459;

    /**
     * AGGREGATE is not supported for stored functions
     */
    public static final int ER_SP_NO_AGGREGATE = 1460;

    /**
     * Can't create more than max_prepared_stmt_count statements (current value: %lu)
     */
    public static final int ER_MAX_PREPARED_STMT_COUNT_REACHED = 1461;

    /**
     * `%-.192s`.`%-.192s` contains view recursion
     */
    public static final int ER_VIEW_RECURSIVE = 1462;

    /**
     * Non-grouping field '%-.192s' is used in %-.64s clause
     */
    public static final int ER_NON_GROUPING_FIELD_USED = 1463;

    /**
     * The used table type doesn't support SPATIAL indexes
     */
    public static final int ER_TABLE_CANT_HANDLE_SPKEYS = 1464;

    /**
     * Triggers can not be created on system tables
     */
    public static final int ER_NO_TRIGGERS_ON_SYSTEM_SCHEMA = 1465;

    /**
     * Leading spaces are removed from name '%s'
     */
    public static final int ER_REMOVED_SPACES = 1466;

    /**
     * Failed to read auto-increment value from storage engine
     */
    public static final int ER_AUTOINC_READ_FAILED = 1467;

    /**
     * User name
     */
    public static final int ER_USERNAME = 1468;

    /**
     * Host name
     */
    public static final int ER_HOSTNAME = 1469;

    /**
     * String '%-.70s' is too long for %s (should be no longer than %d)
     */
    public static final int ER_WRONG_STRING_LENGTH = 1470;

    /**
     * The target table %-.100s of the %s is not insertable-into
     */
    public static final int ER_NON_INSERTABLE_TABLE = 1471;

    /**
     * Table '%-.64s' is differently defined or of non-MyISAM type or doesn't exist
     */
    public static final int ER_ADMIN_WRONG_MRG_TABLE = 1472;

    /**
     * Too high level of nesting for select
     */
    public static final int ER_TOO_HIGH_LEVEL_OF_NESTING_FOR_SELECT = 1473;

    /**
     * Name '%-.64s' has become ''
     */
    public static final int ER_NAME_BECOMES_EMPTY = 1474;

    /**
     * First character of the FIELDS TERMINATED string is ambiguous; please use non-optional and non-empty FIELDS
     * ENCLOSED BY
     */
    public static final int ER_AMBIGUOUS_FIELD_TERM = 1475;

    /**
     * The foreign server, %s, you are trying to create already exists.
     */
    public static final int ER_FOREIGN_SERVER_EXISTS = 1476;

    /**
     * The foreign server name you are trying to reference does not exist. Data source error:  %-.64s
     */
    public static final int ER_FOREIGN_SERVER_DOESNT_EXIST = 1477;

    /**
     * Table storage engine '%-.64s' does not support the create option '%.64s'
     */
    public static final int ER_ILLEGAL_HA_CREATE_OPTION = 1478;

    /**
     * Syntax error: %-.64s PARTITIONING requires definition of VALUES %-.64s for each partition
     */
    public static final int ER_PARTITION_REQUIRES_VALUES_ERROR = 1479;

    /**
     * Only %-.64s PARTITIONING can use VALUES %-.64s in partition definition
     */
    public static final int ER_PARTITION_WRONG_VALUES_ERROR = 1480;

    /**
     * MAXVALUE can only be used in last partition definition
     */
    public static final int ER_PARTITION_MAXVALUE_ERROR = 1481;

    /**
     * Subpartitions can only be hash partitions and by key
     */
    public static final int ER_PARTITION_SUBPARTITION_ERROR = 1482;

    /**
     * Must define subpartitions on all partitions if on one partition
     */
    public static final int ER_PARTITION_SUBPART_MIX_ERROR = 1483;

    /**
     * Wrong number of partitions defined, mismatch with previous setting
     */
    public static final int ER_PARTITION_WRONG_NO_PART_ERROR = 1484;

    /**
     * Wrong number of subpartitions defined, mismatch with previous setting
     */
    public static final int ER_PARTITION_WRONG_NO_SUBPART_ERROR = 1485;

    /**
     * Constant, random or timezone-dependent expressions in (sub)partitioning function are not allowed
     */
    public static final int ER_WRONG_EXPR_IN_PARTITION_FUNC_ERROR = 1486;

    /**
     * Expression in RANGE/LIST VALUES must be constant
     */
    public static final int ER_NO_CONST_EXPR_IN_RANGE_OR_LIST_ERROR = 1487;

    /**
     * Field in list of fields for partition function not found in table
     */
    public static final int ER_FIELD_NOT_FOUND_PART_ERROR = 1488;

    /**
     * List of fields is only allowed in KEY partitions
     */
    public static final int ER_LIST_OF_FIELDS_ONLY_IN_HASH_ERROR = 1489;

    /**
     * The partition info in the frm file is not consistent with what can be written into the frm file
     */
    public static final int ER_INCONSISTENT_PARTITION_INFO_ERROR = 1490;

    /**
     * The %-.192s function returns the wrong type
     */
    public static final int ER_PARTITION_FUNC_NOT_ALLOWED_ERROR = 1491;

    /**
     * For %-.64s partitions each partition must be defined
     */
    public static final int ER_PARTITIONS_MUST_BE_DEFINED_ERROR = 1492;

    /**
     * VALUES LESS THAN value must be strictly increasing for each partition
     */
    public static final int ER_RANGE_NOT_INCREASING_ERROR = 1493;

    /**
     * VALUES value must be of same type as partition function
     */
    public static final int ER_INCONSISTENT_TYPE_OF_FUNCTIONS_ERROR = 1494;

    /**
     * Multiple definition of same constant in list partitioning
     */
    public static final int ER_MULTIPLE_DEF_CONST_IN_LIST_PART_ERROR = 1495;

    /**
     * Partitioning can not be used stand-alone in query
     */
    public static final int ER_PARTITION_ENTRY_ERROR = 1496;

    /**
     * The mix of handlers in the partitions is not allowed in this version of MySQL
     */
    public static final int ER_MIX_HANDLER_ERROR = 1497;

    /**
     * For the partitioned engine it is necessary to define all %-.64s
     */
    public static final int ER_PARTITION_NOT_DEFINED_ERROR = 1498;

    /**
     * Too many partitions (including subpartitions) were defined
     */
    public static final int ER_TOO_MANY_PARTITIONS_ERROR = 1499;

    /**
     * It is only possible to mix RANGE/LIST partitioning with HASH/KEY partitioning for subpartitioning
     */
    public static final int ER_SUBPARTITION_ERROR = 1500;

    /**
     * Failed to create specific handler file
     */
    public static final int ER_CANT_CREATE_HANDLER_FILE = 1501;

    /**
     * A BLOB field is not allowed in partition function
     */
    public static final int ER_BLOB_FIELD_IN_PART_FUNC_ERROR = 1502;

    /**
     * A %-.192s must include all columns in the table's partitioning function
     */
    public static final int ER_UNIQUE_KEY_NEED_ALL_FIELDS_IN_PF = 1503;

    /**
     * Number of %-.64s = 0 is not an allowed value
     */
    public static final int ER_NO_PARTS_ERROR = 1504;

    /**
     * Partition management on a not partitioned table is not possible
     */
    public static final int ER_PARTITION_MGMT_ON_NONPARTITIONED = 1505;

    /**
     * Foreign key clause is not yet supported in conjunction with partitioning
     */
    public static final int ER_FOREIGN_KEY_ON_PARTITIONED = 1506;

    /**
     * Error in list of partitions to %-.64s
     */
    public static final int ER_DROP_PARTITION_NON_EXISTENT = 1507;

    /**
     * Cannot remove all partitions, use DROP TABLE instead
     */
    public static final int ER_DROP_LAST_PARTITION = 1508;

    /**
     * COALESCE PARTITION can only be used on HASH/KEY partitions
     */
    public static final int ER_COALESCE_ONLY_ON_HASH_PARTITION = 1509;

    /**
     * REORGANIZE PARTITION can only be used to reorganize partitions not to change their numbers
     */
    public static final int ER_REORG_HASH_ONLY_ON_SAME_NO = 1510;

    /**
     * REORGANIZE PARTITION without parameters can only be used on auto-partitioned tables using HASH PARTITIONs
     */
    public static final int ER_REORG_NO_PARAM_ERROR = 1511;

    /**
     * %-.64s PARTITION can only be used on RANGE/LIST partitions
     */
    public static final int ER_ONLY_ON_RANGE_LIST_PARTITION = 1512;

    /**
     * Trying to Add partition(s) with wrong number of subpartitions
     */
    public static final int ER_ADD_PARTITION_SUBPART_ERROR = 1513;

    /**
     * At least one partition must be added
     */
    public static final int ER_ADD_PARTITION_NO_NEW_PARTITION = 1514;

    /**
     * At least one partition must be coalesced
     */
    public static final int ER_COALESCE_PARTITION_NO_PARTITION = 1515;

    /**
     * More partitions to reorganize than there are partitions
     */
    public static final int ER_REORG_PARTITION_NOT_EXIST = 1516;

    /**
     * Duplicate partition name %-.192s
     */
    public static final int ER_SAME_NAME_PARTITION = 1517;

    /**
     * It is not allowed to shut off binlog on this command
     */
    public static final int ER_NO_BINLOG_ERROR = 1518;

    /**
     * When reorganizing a set of partitions they must be in consecutive order
     */
    public static final int ER_CONSECUTIVE_REORG_PARTITIONS = 1519;

    /**
     * Reorganize of range partitions cannot change total ranges except for last partition where it can extend the
     * range
     */
    public static final int ER_REORG_OUTSIDE_RANGE = 1520;

    /**
     * Partition function not supported in this version for this handler
     */
    public static final int ER_PARTITION_FUNCTION_FAILURE = 1521;

    /**
     * Partition state cannot be defined from CREATE/ALTER TABLE
     */
    public static final int ER_PART_STATE_ERROR = 1522;

    /**
     * The %-.64s handler only supports 32 bit integers in VALUES
     */
    public static final int ER_LIMITED_PART_RANGE = 1523;

    /**
     * Plugin '%-.192s' is not loaded
     */
    public static final int ER_PLUGIN_IS_NOT_LOADED = 1524;

    /**
     * Incorrect %-.32s value: '%-.128s'
     */
    public static final int ER_WRONG_VALUE = 1525;

    /**
     * Table has no partition for value %-.64s
     */
    public static final int ER_NO_PARTITION_FOR_GIVEN_VALUE = 1526;

    /**
     * It is not allowed to specify %s more than once
     */
    public static final int ER_FILEGROUP_OPTION_ONLY_ONCE = 1527;

    /**
     * Failed to create %s
     */
    public static final int ER_CREATE_FILEGROUP_FAILED = 1528;

    /**
     * Failed to drop %s
     */
    public static final int ER_DROP_FILEGROUP_FAILED = 1529;

    /**
     * The handler doesn't support autoextend of tablespaces
     */
    public static final int ER_TABLESPACE_AUTO_EXTEND_ERROR = 1530;

    /**
     * A size parameter was incorrectly specified, either number or on the form 10M
     */
    public static final int ER_WRONG_SIZE_NUMBER = 1531;

    /**
     * The size number was correct but we don't allow the digit part to be more than 2 billion
     */
    public static final int ER_SIZE_OVERFLOW_ERROR = 1532;

    /**
     * Failed to alter: %s
     */
    public static final int ER_ALTER_FILEGROUP_FAILED = 1533;

    /**
     * Writing one row to the row-based binary log failed
     */
    public static final int ER_BINLOG_ROW_LOGGING_FAILED = 1534;

    /**
     * Table definition on master and slave does not match: %s
     */
    public static final int ER_BINLOG_ROW_WRONG_TABLE_DEF = 1535;

    /**
     * Slave running with --log-slave-updates must use row-based binary logging to be able to replicate row-based
     * binary log events
     */
    public static final int ER_BINLOG_ROW_RBR_TO_SBR = 1536;

    /**
     * Event '%-.192s' already exists
     */
    public static final int ER_EVENT_ALREADY_EXISTS = 1537;

    /**
     * Failed to store event %s. Error code %d from storage engine.
     */
    public static final int ER_EVENT_STORE_FAILED = 1538;

    /**
     * Unknown event '%-.192s'
     */
    public static final int ER_EVENT_DOES_NOT_EXIST = 1539;

    /**
     * Failed to alter event '%-.192s'
     */
    public static final int ER_EVENT_CANT_ALTER = 1540;

    /**
     * Failed to drop %s
     */
    public static final int ER_EVENT_DROP_FAILED = 1541;

    /**
     * INTERVAL is either not positive or too big
     */
    public static final int ER_EVENT_INTERVAL_NOT_POSITIVE_OR_TOO_BIG = 1542;

    /**
     * ENDS is either invalid or before STARTS
     */
    public static final int ER_EVENT_ENDS_BEFORE_STARTS = 1543;

    /**
     * Event execution time is in the past. Event has been disabled
     */
    public static final int ER_EVENT_EXEC_TIME_IN_THE_PAST = 1544;

    /**
     * Failed to open mysql.event
     */
    public static final int ER_EVENT_OPEN_TABLE_FAILED = 1545;

    /**
     * No datetime expression provided
     */
    public static final int ER_EVENT_NEITHER_M_EXPR_NOR_M_AT = 1546;

    /**
     * Column count of mysql.%s is wrong. Expected %d, found %d. The table is probably corrupted
     */
    public static final int ER_OBSOLETE_COL_COUNT_DOESNT_MATCH_CORRUPTED = 1547;

    /**
     * Cannot load from mysql.%s. The table is probably corrupted
     */
    public static final int ER_OBSOLETE_CANNOT_LOAD_FROM_TABLE = 1548;

    /**
     * Failed to delete the event from mysql.event
     */
    public static final int ER_EVENT_CANNOT_DELETE = 1549;

    /**
     * Error during compilation of event's body
     */
    public static final int ER_EVENT_COMPILE_ERROR = 1550;

    /**
     * Same old and new event name
     */
    public static final int ER_EVENT_SAME_NAME = 1551;

    /**
     * Data for column '%s' too long
     */
    public static final int ER_EVENT_DATA_TOO_LONG = 1552;

    /**
     * Cannot drop index '%-.192s': needed in a foreign key constraint
     */
    public static final int ER_DROP_INDEX_FK = 1553;

    /**
     * The syntax '%s' is deprecated and will be removed in MySQL %s. Please use %s instead
     */
    public static final int ER_WARN_DEPRECATED_SYNTAX_WITH_VER = 1554;

    /**
     * You can't write-lock a log table. Only read access is possible
     */
    public static final int ER_CANT_WRITE_LOCK_LOG_TABLE = 1555;

    /**
     * You can't use locks with log tables.
     */
    public static final int ER_CANT_LOCK_LOG_TABLE = 1556;

    /**
     * Upholding foreign key constraints for table '%.192s', entry '%-.192s', key %d would lead to a duplicate entry
     */
    public static final int ER_FOREIGN_DUPLICATE_KEY_OLD_UNUSED = 1557;

    /**
     * Column count of mysql.%s is wrong. Expected %d, found %d. Created with MySQL %d, now running %d. Please use
     * mysql_upgrade to fix this error.
     */
    public static final int ER_COL_COUNT_DOESNT_MATCH_PLEASE_UPDATE = 1558;

    /**
     * Cannot switch out of the row-based binary log format when the session has open temporary tables
     */
    public static final int ER_TEMP_TABLE_PREVENTS_SWITCH_OUT_OF_RBR = 1559;

    /**
     * Cannot change the binary logging format inside a stored function or trigger
     */
    public static final int ER_STORED_FUNCTION_PREVENTS_SWITCH_BINLOG_FORMAT = 1560;

    /**
     * The NDB cluster engine does not support changing the binlog format on the fly yet
     */
    public static final int ER_NDB_CANT_SWITCH_BINLOG_FORMAT = 1561;

    /**
     * Cannot create temporary table with partitions
     */
    public static final int ER_PARTITION_NO_TEMPORARY = 1562;

    /**
     * Partition constant is out of partition function domain
     */
    public static final int ER_PARTITION_CONST_DOMAIN_ERROR = 1563;

    /**
     * This partition function is not allowed
     */
    public static final int ER_PARTITION_FUNCTION_IS_NOT_ALLOWED = 1564;

    /**
     * Error in DDL log
     */
    public static final int ER_DDL_LOG_ERROR = 1565;

    /**
     * Not allowed to use NULL value in VALUES LESS THAN
     */
    public static final int ER_NULL_IN_VALUES_LESS_THAN = 1566;

    /**
     * Incorrect partition name
     */
    public static final int ER_WRONG_PARTITION_NAME = 1567;

    /**
     * Transaction characteristics can't be changed while a transaction is in progress
     */
    public static final int ER_CANT_CHANGE_TX_CHARACTERISTICS = 1568;

    /**
     * ALTER TABLE causes auto_increment resequencing, resulting in duplicate entry '%-.192s' for key '%-.192s'
     */
    public static final int ER_DUP_ENTRY_AUTOINCREMENT_CASE = 1569;

    /**
     * Internal scheduler error %d
     */
    public static final int ER_EVENT_MODIFY_QUEUE_ERROR = 1570;

    /**
     * Error during starting/stopping of the scheduler. Error code %u
     */
    public static final int ER_EVENT_SET_VAR_ERROR = 1571;

    /**
     * Engine cannot be used in partitioned tables
     */
    public static final int ER_PARTITION_MERGE_ERROR = 1572;

    /**
     * Cannot activate '%-.64s' log
     */
    public static final int ER_CANT_ACTIVATE_LOG = 1573;

    /**
     * The server was not built with row-based replication
     */
    public static final int ER_RBR_NOT_AVAILABLE = 1574;

    /**
     * Decoding of base64 string failed
     */
    public static final int ER_BASE64_DECODE_ERROR = 1575;

    /**
     * Recursion of EVENT DDL statements is forbidden when body is present
     */
    public static final int ER_EVENT_RECURSION_FORBIDDEN = 1576;

    /**
     * Cannot proceed because system tables used by Event Scheduler were found damaged at server start
     */
    public static final int ER_EVENTS_DB_ERROR = 1577;

    /**
     * Only integers allowed as number here
     */
    public static final int ER_ONLY_INTEGERS_ALLOWED = 1578;

    /**
     * This storage engine cannot be used for log tables"
     */
    public static final int ER_UNSUPORTED_LOG_ENGINE = 1579;

    /**
     * You cannot '%s' a log table if logging is enabled
     */
    public static final int ER_BAD_LOG_STATEMENT = 1580;

    /**
     * Cannot rename '%s'. When logging enabled, rename to/from log table must rename two tables: the log table to an
     * archive table and another table back to '%s'
     */
    public static final int ER_CANT_RENAME_LOG_TABLE = 1581;

    /**
     * Incorrect parameter count in the call to native function '%-.192s'
     */
    public static final int ER_WRONG_PARAMCOUNT_TO_NATIVE_FCT = 1582;

    /**
     * Incorrect parameters in the call to native function '%-.192s'
     */
    public static final int ER_WRONG_PARAMETERS_TO_NATIVE_FCT = 1583;

    /**
     * Incorrect parameters in the call to stored function '%-.192s'
     */
    public static final int ER_WRONG_PARAMETERS_TO_STORED_FCT = 1584;

    /**
     * This function '%-.192s' has the same name as a native function
     */
    public static final int ER_NATIVE_FCT_NAME_COLLISION = 1585;

    /**
     * Duplicate entry '%-.64s' for key '%-.192s'
     */
    public static final int ER_DUP_ENTRY_WITH_KEY_NAME = 1586;

    /**
     * Too many files opened, please execute the command again
     */
    public static final int ER_BINLOG_PURGE_EMFILE = 1587;

    /**
     * Event execution time is in the past and ON COMPLETION NOT PRESERVE is set. The event was dropped immediately
     * after creation.
     */
    public static final int ER_EVENT_CANNOT_CREATE_IN_THE_PAST = 1588;

    /**
     * Event execution time is in the past and ON COMPLETION NOT PRESERVE is set. The event was not changed. Specify
     * a time in the future.
     */
    public static final int ER_EVENT_CANNOT_ALTER_IN_THE_PAST = 1589;

    /**
     * The incident %s occured on the master. Message: %-.64s
     */
    public static final int ER_SLAVE_INCIDENT = 1590;

    /**
     * Table has no partition for some existing values
     */
    public static final int ER_NO_PARTITION_FOR_GIVEN_VALUE_SILENT = 1591;

    /**
     * Unsafe statement written to the binary log using statement format since BINLOG_FORMAT = STATEMENT. %s
     */
    public static final int ER_BINLOG_UNSAFE_STATEMENT = 1592;

    /**
     * Fatal error: %s
     */
    public static final int ER_SLAVE_FATAL_ERROR = 1593;

    /**
     * Relay log read failure: %s
     */
    public static final int ER_SLAVE_RELAY_LOG_READ_FAILURE = 1594;

    /**
     * Relay log write failure: %s
     */
    public static final int ER_SLAVE_RELAY_LOG_WRITE_FAILURE = 1595;

    /**
     * Failed to create %s
     */
    public static final int ER_SLAVE_CREATE_EVENT_FAILURE = 1596;

    /**
     * Master command %s failed: %s
     */
    public static final int ER_SLAVE_MASTER_COM_FAILURE = 1597;

    /**
     * Binary logging not possible. Message: %s
     */
    public static final int ER_BINLOG_LOGGING_IMPOSSIBLE = 1598;

    /**
     * View `%-.64s`.`%-.64s` has no creation context
     */
    public static final int ER_VIEW_NO_CREATION_CTX = 1599;

    /**
     * Creation context of view `%-.64s`.`%-.64s' is invalid
     */
    public static final int ER_VIEW_INVALID_CREATION_CTX = 1600;

    /**
     * Creation context of stored routine `%-.64s`.`%-.64s` is invalid
     */
    public static final int ER_SR_INVALID_CREATION_CTX = 1601;

    /**
     * Corrupted TRG file for table `%-.64s`.`%-.64s`
     */
    public static final int ER_TRG_CORRUPTED_FILE = 1602;

    /**
     * Triggers for table `%-.64s`.`%-.64s` have no creation context
     */
    public static final int ER_TRG_NO_CREATION_CTX = 1603;

    /**
     * Trigger creation context of table `%-.64s`.`%-.64s` is invalid
     */
    public static final int ER_TRG_INVALID_CREATION_CTX = 1604;

    /**
     * Creation context of event `%-.64s`.`%-.64s` is invalid
     */
    public static final int ER_EVENT_INVALID_CREATION_CTX = 1605;

    /**
     * Cannot open table for trigger `%-.64s`.`%-.64s`
     */
    public static final int ER_TRG_CANT_OPEN_TABLE = 1606;

    /**
     * Cannot create stored routine `%-.64s`. Check warnings
     */
    public static final int ER_CANT_CREATE_SROUTINE = 1607;

    /**
     * Ambiguous slave modes combination. %s
     */
    public static final int ER_NEVER_USED = 1608;

    /**
     * The BINLOG statement of type `%s` was not preceded by a format description BINLOG statement.
     */
    public static final int ER_NO_FORMAT_DESCRIPTION_EVENT_BEFORE_BINLOG_STATEMENT = 1609;

    /**
     * Corrupted replication event was detected
     */
    public static final int ER_SLAVE_CORRUPT_EVENT = 1610;

    /**
     * Invalid column reference (%-.64s) in LOAD DATA
     */
    public static final int ER_LOAD_DATA_INVALID_COLUMN = 1611;

    /**
     * Being purged log %s was not found
     */
    public static final int ER_LOG_PURGE_NO_FILE = 1612;

    /**
     * XA_RBTIMEOUT: Transaction branch was rolled back: took too long
     */
    public static final int ER_XA_RBTIMEOUT = 1613;

    /**
     * XA_RBDEADLOCK: Transaction branch was rolled back: deadlock was detected
     */
    public static final int ER_XA_RBDEADLOCK = 1614;

    /**
     * Prepared statement needs to be re-prepared
     */
    public static final int ER_NEED_REPREPARE = 1615;

    /**
     * DELAYED option not supported for table '%-.192s'
     */
    public static final int ER_DELAYED_NOT_SUPPORTED = 1616;

    /**
     * The master info structure does not exist
     */
    public static final int WARN_NO_MASTER_INFO = 1617;

    /**
     * <%-.64s> option ignored
     */
    public static final int WARN_OPTION_IGNORED = 1618;

    /**
     * Built-in plugins cannot be deleted
     */
    public static final int WARN_PLUGIN_DELETE_BUILTIN = 1619;

    /**
     * Plugin is busy and will be uninstalled on shutdown
     */
    public static final int WARN_PLUGIN_BUSY = 1620;

    /**
     * %s variable '%s' is read-only. Use SET %s to assign the value
     */
    public static final int ER_VARIABLE_IS_READONLY = 1621;

    /**
     * Storage engine %s does not support rollback for this statement. Transaction rolled back and must be restarted
     */
    public static final int ER_WARN_ENGINE_TRANSACTION_ROLLBACK = 1622;

    /**
     * Unexpected master's heartbeat data: %s
     */
    public static final int ER_SLAVE_HEARTBEAT_FAILURE = 1623;

    /**
     * The requested value for the heartbeat period is either negative or exceeds the maximum allowed (%s seconds).
     */
    public static final int ER_SLAVE_HEARTBEAT_VALUE_OUT_OF_RANGE = 1624;

    /**
     * Bad schema for mysql.ndb_replication table. Message: %-.64s
     */
    public static final int ER_NDB_REPLICATION_SCHEMA_ERROR = 1625;

    /**
     * Error in parsing conflict function. Message: %-.64s
     */
    public static final int ER_CONFLICT_FN_PARSE_ERROR = 1626;

    /**
     * Write to exceptions table failed. Message: %-.128s"
     */
    public static final int ER_EXCEPTIONS_WRITE_ERROR = 1627;

    /**
     * Comment for table '%-.64s' is too long (max = %lu)
     */
    public static final int ER_TOO_LONG_TABLE_COMMENT = 1628;

    /**
     * Comment for field '%-.64s' is too long (max = %lu)
     */
    public static final int ER_TOO_LONG_FIELD_COMMENT = 1629;

    /**
     * FUNCTION %s does not exist. Check the 'Function Name Parsing and Resolution' section in the Reference Manual
     */
    public static final int ER_FUNC_INEXISTENT_NAME_COLLISION = 1630;

    /**
     * Database
     */
    public static final int ER_DATABASE_NAME = 1631;

    /**
     * Table
     */
    public static final int ER_TABLE_NAME = 1632;

    /**
     * Partition
     */
    public static final int ER_PARTITION_NAME = 1633;

    /**
     * Subpartition
     */
    public static final int ER_SUBPARTITION_NAME = 1634;

    /**
     * Temporary
     */
    public static final int ER_TEMPORARY_NAME = 1635;

    /**
     * Renamed
     */
    public static final int ER_RENAMED_NAME = 1636;

    /**
     * Too many active concurrent transactions
     */
    public static final int ER_TOO_MANY_CONCURRENT_TRXS = 1637;

    /**
     * Non-ASCII separator arguments are not fully supported
     */
    public static final int WARN_NON_ASCII_SEPARATOR_NOT_IMPLEMENTED = 1638;

    /**
     * Debug sync point wait timed out
     */
    public static final int ER_DEBUG_SYNC_TIMEOUT = 1639;

    /**
     * Debug sync point hit limit reached
     */
    public static final int ER_DEBUG_SYNC_HIT_LIMIT = 1640;

    /**
     * Duplicate condition information item '%s'
     */
    public static final int ER_DUP_SIGNAL_SET = 1641;

    /**
     * Unhandled user-defined warning condition
     */
    public static final int ER_SIGNAL_WARN = 1642;

    /**
     * Unhandled user-defined not found condition
     */
    public static final int ER_SIGNAL_NOT_FOUND = 1643;

    /**
     * Unhandled user-defined exception condition
     */
    public static final int ER_SIGNAL_EXCEPTION = 1644;

    /**
     * RESIGNAL when handler not active
     */
    public static final int ER_RESIGNAL_WITHOUT_ACTIVE_HANDLER = 1645;

    /**
     * SIGNAL/RESIGNAL can only use a CONDITION defined with SQLSTATE
     */
    public static final int ER_SIGNAL_BAD_CONDITION_TYPE = 1646;

    /**
     * Data truncated for condition item '%s'
     */
    public static final int WARN_COND_ITEM_TRUNCATED = 1647;

    /**
     * Data too long for condition item '%s'
     */
    public static final int ER_COND_ITEM_TOO_LONG = 1648;

    /**
     * Unknown locale: '%-.64s'
     */
    public static final int ER_UNKNOWN_LOCALE = 1649;

    /**
     * The requested server id %d clashes with the slave startup option --replicate-same-server-id
     */
    public static final int ER_SLAVE_IGNORE_SERVER_IDS = 1650;

    /**
     * Query cache is disabled; restart the server with query_cache_type=1 to enable it
     */
    public static final int ER_QUERY_CACHE_DISABLED = 1651;

    /**
     * Duplicate partition field name '%-.192s'
     */
    public static final int ER_SAME_NAME_PARTITION_FIELD = 1652;

    /**
     * Inconsistency in usage of column lists for partitioning
     */
    public static final int ER_PARTITION_COLUMN_LIST_ERROR = 1653;

    /**
     * Partition column values of incorrect type
     */
    public static final int ER_WRONG_TYPE_COLUMN_VALUE_ERROR = 1654;

    /**
     * Too many fields in '%-.192s'
     */
    public static final int ER_TOO_MANY_PARTITION_FUNC_FIELDS_ERROR = 1655;

    /**
     * Cannot use MAXVALUE as value in VALUES IN
     */
    public static final int ER_MAXVALUE_IN_VALUES_IN = 1656;

    /**
     * Cannot have more than one value for this type of %-.64s partitioning
     */
    public static final int ER_TOO_MANY_VALUES_ERROR = 1657;

    /**
     * Row expressions in VALUES IN only allowed for multi-field column partitioning
     */
    public static final int ER_ROW_SINGLE_PARTITION_FIELD_ERROR = 1658;

    /**
     * Field '%-.192s' is of a not allowed type for this type of partitioning
     */
    public static final int ER_FIELD_TYPE_NOT_ALLOWED_AS_PARTITION_FIELD = 1659;

    /**
     * The total length of the partitioning fields is too large
     */
    public static final int ER_PARTITION_FIELDS_TOO_LONG = 1660;

    /**
     * Cannot execute statement: impossible to write to binary log since both row-incapable engines and
     * statement-incapable engines are involved.
     */
    public static final int ER_BINLOG_ROW_ENGINE_AND_STMT_ENGINE = 1661;

    /**
     * Cannot execute statement: impossible to write to binary log since BINLOG_FORMAT = ROW and at least one table
     * uses a storage engine limited to statement-based logging.
     */
    public static final int ER_BINLOG_ROW_MODE_AND_STMT_ENGINE = 1662;

    /**
     * Cannot execute statement: impossible to write to binary log since statement is unsafe, storage engine is
     * limited to statement-based logging, and BINLOG_FORMAT = MIXED. %s
     */
    public static final int ER_BINLOG_UNSAFE_AND_STMT_ENGINE = 1663;

    /**
     * Cannot execute statement: impossible to write to binary log since statement is in row format and at least one
     * table uses a storage engine limited to statement-based logging.
     */
    public static final int ER_BINLOG_ROW_INJECTION_AND_STMT_ENGINE = 1664;

    /**
     * Cannot execute statement: impossible to write to binary log since BINLOG_FORMAT = STATEMENT and at least one
     * table uses a storage engine limited to row-based logging.%s
     */
    public static final int ER_BINLOG_STMT_MODE_AND_ROW_ENGINE = 1665;

    /**
     * Cannot execute statement: impossible to write to binary log since statement is in row format and BINLOG_FORMAT
     * = STATEMENT.
     */
    public static final int ER_BINLOG_ROW_INJECTION_AND_STMT_MODE = 1666;

    /**
     * Cannot execute statement: impossible to write to binary log since more than one engine is involved and at
     * least one engine is self-logging.
     */
    public static final int ER_BINLOG_MULTIPLE_ENGINES_AND_SELF_LOGGING_ENGINE = 1667;

    /**
     * The statement is unsafe because it uses a LIMIT clause. This is unsafe because the set of rows included cannot
     * be predicted.
     */
    public static final int ER_BINLOG_UNSAFE_LIMIT = 1668;

    /**
     * The statement is unsafe because it uses INSERT DELAYED. This is unsafe because the times when rows are
     * inserted cannot be predicted.
     */
    public static final int ER_BINLOG_UNSAFE_INSERT_DELAYED = 1669;

    /**
     * The statement is unsafe because it uses the general log, slow query log, or performance_schema table(s). This
     * is unsafe because system tables may differ on slaves.
     */
    public static final int ER_BINLOG_UNSAFE_SYSTEM_TABLE = 1670;

    /**
     * Statement is unsafe because it invokes a trigger or a stored function that inserts into an AUTO_INCREMENT
     * column. Inserted values cannot be logged correctly.
     */
    public static final int ER_BINLOG_UNSAFE_AUTOINC_COLUMNS = 1671;

    /**
     * Statement is unsafe because it uses a UDF which may not return the same value on the slave.
     */
    public static final int ER_BINLOG_UNSAFE_UDF = 1672;

    /**
     * Statement is unsafe because it uses a system variable that may have a different value on the slave.
     */
    public static final int ER_BINLOG_UNSAFE_SYSTEM_VARIABLE = 1673;

    /**
     * Statement is unsafe because it uses a system function that may return a different value on the slave.
     */
    public static final int ER_BINLOG_UNSAFE_SYSTEM_FUNCTION = 1674;

    /**
     * Statement is unsafe because it accesses a non-transactional table after accessing a transactional table within
     * the same transaction.
     */
    public static final int ER_BINLOG_UNSAFE_NONTRANS_AFTER_TRANS = 1675;

    /**
     * %s Statement: %s
     */
    public static final int ER_MESSAGE_AND_STATEMENT = 1676;

    /**
     * Column %d of table '%-.192s.%-.192s' cannot be converted from type '%-.32s' to type '%-.32s'
     */
    public static final int ER_SLAVE_CONVERSION_FAILED = 1677;

    /**
     * Can't create conversion table for table '%-.192s.%-.192s'
     */
    public static final int ER_SLAVE_CANT_CREATE_CONVERSION = 1678;

    /**
     * Cannot modify @@session.binlog_format inside a transaction
     */
    public static final int ER_INSIDE_TRANSACTION_PREVENTS_SWITCH_BINLOG_FORMAT = 1679;

    /**
     * The path specified for %.64s is too long.
     */
    public static final int ER_PATH_LENGTH = 1680;

    /**
     * '%s' is deprecated and will be removed in a future release.
     */
    public static final int ER_WARN_DEPRECATED_SYNTAX_NO_REPLACEMENT = 1681;

    /**
     * Native table '%-.64s'.'%-.64s' has the wrong structure
     */
    public static final int ER_WRONG_NATIVE_TABLE_STRUCTURE = 1682;

    /**
     * Invalid performance_schema usage.
     */
    public static final int ER_WRONG_PERFSCHEMA_USAGE = 1683;

    /**
     * Table '%s'.'%s' was skipped since its definition is being modified by concurrent DDL statement
     */
    public static final int ER_WARN_I_S_SKIPPED_TABLE = 1684;

    /**
     * Cannot modify @@session.binlog_direct_non_transactional_updates inside a transaction
     */
    public static final int ER_INSIDE_TRANSACTION_PREVENTS_SWITCH_BINLOG_DIRECT = 1685;

    /**
     * Cannot change the binlog direct flag inside a stored function or trigger
     */
    public static final int ER_STORED_FUNCTION_PREVENTS_SWITCH_BINLOG_DIRECT = 1686;

    /**
     * A SPATIAL index may only contain a geometrical type column
     */
    public static final int ER_SPATIAL_MUST_HAVE_GEOM_COL = 1687;

    /**
     * Comment for index '%-.64s' is too long (max = %lu)
     */
    public static final int ER_TOO_LONG_INDEX_COMMENT = 1688;

    /**
     * Wait on a lock was aborted due to a pending exclusive lock
     */
    public static final int ER_LOCK_ABORTED = 1689;

    /**
     * %s value is out of range in '%s'
     */
    public static final int ER_DATA_OUT_OF_RANGE = 1690;

    /**
     * A variable of a non-integer based type in LIMIT clause
     */
    public static final int ER_WRONG_SPVAR_TYPE_IN_LIMIT = 1691;

    /**
     * Mixing self-logging and non-self-logging engines in a statement is unsafe.
     */
    public static final int ER_BINLOG_UNSAFE_MULTIPLE_ENGINES_AND_SELF_LOGGING_ENGINE = 1692;

    /**
     * Statement accesses nontransactional table as well as transactional or temporary table, and writes to any of
     * them.
     */
    public static final int ER_BINLOG_UNSAFE_MIXED_STATEMENT = 1693;

    /**
     * Cannot modify @@session.sql_log_bin inside a transaction
     */
    public static final int ER_INSIDE_TRANSACTION_PREVENTS_SWITCH_SQL_LOG_BIN = 1694;

    /**
     * Cannot change the sql_log_bin inside a stored function or trigger
     */
    public static final int ER_STORED_FUNCTION_PREVENTS_SWITCH_SQL_LOG_BIN = 1695;

    /**
     * Failed to read from the .par file
     */
    public static final int ER_FAILED_READ_FROM_PAR_FILE = 1696;

    /**
     * VALUES value for partition '%-.64s' must have type INT
     */
    public static final int ER_VALUES_IS_NOT_INT_TYPE_ERROR = 1697;

    /**
     * Access denied for user '%-.48s'@'%-.64s'
     */
    public static final int ER_ACCESS_DENIED_NO_PASSWORD_ERROR = 1698;

    /**
     * SET PASSWORD has no significance for users authenticating via plugins
     */
    public static final int ER_SET_PASSWORD_AUTH_PLUGIN = 1699;

    /**
     * GRANT with IDENTIFIED WITH is illegal because the user %-.*s already exists
     */
    public static final int ER_GRANT_PLUGIN_USER_EXISTS = 1700;

    /**
     * Cannot truncate a table referenced in a foreign key constraint (%.192s)
     */
    public static final int ER_TRUNCATE_ILLEGAL_FK = 1701;

    /**
     * Plugin '%s' is force_plus_permanent and can not be unloaded
     */
    public static final int ER_PLUGIN_IS_PERMANENT = 1702;

    /**
     * The requested value for the heartbeat period is less than 1 millisecond. The value is reset to 0, meaning that
     * heartbeating will effectively be disabled.
     */
    public static final int ER_SLAVE_HEARTBEAT_VALUE_OUT_OF_RANGE_MIN = 1703;

    /**
     * The requested value for the heartbeat period exceeds the value of `slave_net_timeout' seconds. A sensible
     * value for the period should be less than the timeout.
     */
    public static final int ER_SLAVE_HEARTBEAT_VALUE_OUT_OF_RANGE_MAX = 1704;

    /**
     * Multi-row statements required more than 'max_binlog_stmt_cache_size' bytes of storage; increase this mysqld
     * variable and try again
     */
    public static final int ER_STMT_CACHE_FULL = 1705;

    /**
     * Primary key/partition key update is not allowed since the table is updated both as '%-.192s' and '%-.192s'.
     */
    public static final int ER_MULTI_UPDATE_KEY_CONFLICT = 1706;

    /**
     * Table rebuild required. Please do \"ALTER TABLE `%-.32s` FORCE\" or dump/reload to fix it!
     */
    public static final int ER_TABLE_NEEDS_REBUILD = 1707;

    /**
     * The value of '%s' should be no less than the value of '%s'
     */
    public static final int WARN_OPTION_BELOW_LIMIT = 1708;

    /**
     * Index column size too large. The maximum column size is %lu bytes.
     */
    public static final int ER_INDEX_COLUMN_TOO_LONG = 1709;

    /**
     * Trigger '%-.64s' has an error in its body: '%-.256s'
     */
    public static final int ER_ERROR_IN_TRIGGER_BODY = 1710;

    /**
     * Unknown trigger has an error in its body: '%-.256s'
     */
    public static final int ER_ERROR_IN_UNKNOWN_TRIGGER_BODY = 1711;

    /**
     * Index %s is corrupted
     */
    public static final int ER_INDEX_CORRUPT = 1712;

    /**
     * Undo log record is too big.
     */
    public static final int ER_UNDO_RECORD_TOO_BIG = 1713;

    /**
     * INSERT IGNORE... SELECT is unsafe because the order in which rows are retrieved by the SELECT determines which
     * (if any) rows are ignored. This order cannot be predicted and may differ on master and the slave.
     */
    public static final int ER_BINLOG_UNSAFE_INSERT_IGNORE_SELECT = 1714;

    /**
     * INSERT... SELECT... ON DUPLICATE KEY UPDATE is unsafe because the order in which rows are retrieved by the
     * SELECT determines which (if any) rows are updated. This order cannot be predicted and may differ on master and
     * the slave.
     */
    public static final int ER_BINLOG_UNSAFE_INSERT_SELECT_UPDATE = 1715;

    /**
     * REPLACE... SELECT is unsafe because the order in which rows are retrieved by the SELECT determines which (if
     * any) rows are replaced. This order cannot be predicted and may differ on master and the slave.
     */
    public static final int ER_BINLOG_UNSAFE_REPLACE_SELECT = 1716;

    /**
     * CREATE... IGNORE SELECT is unsafe because the order in which rows are retrieved by the SELECT determines which
     * (if any) rows are ignored. This order cannot be predicted and may differ on master and the slave.
     */
    public static final int ER_BINLOG_UNSAFE_CREATE_IGNORE_SELECT = 1717;

    /**
     * CREATE... REPLACE SELECT is unsafe because the order in which rows are retrieved by the SELECT determines
     * which (if any) rows are replaced. This order cannot be predicted and may differ on master and the slave.
     */
    public static final int ER_BINLOG_UNSAFE_CREATE_REPLACE_SELECT = 1718;

    /**
     * UPDATE IGNORE is unsafe because the order in which rows are updated determines which (if any) rows are
     * ignored. This order cannot be predicted and may differ on master and the slave.
     */
    public static final int ER_BINLOG_UNSAFE_UPDATE_IGNORE = 1719;

    /**
     * Plugin '%s' is marked as not dynamically uninstallable. You have to stop the server to uninstall it.
     */
    public static final int ER_PLUGIN_NO_UNINSTALL = 1720;

    /**
     * Plugin '%s' is marked as not dynamically installable. You have to stop the server to install it.
     */
    public static final int ER_PLUGIN_NO_INSTALL = 1721;

    /**
     * Statements writing to a table with an auto-increment column after selecting from another table are unsafe
     * because the order in which rows are retrieved determines what (if any) rows will be written. This order cannot
     * be predicted and may differ on master and the slave.
     */
    public static final int ER_BINLOG_UNSAFE_WRITE_AUTOINC_SELECT = 1722;

    /**
     * CREATE TABLE... SELECT...  on a table with an auto-increment column is unsafe because the order in which rows
     * are retrieved by the SELECT determines which (if any) rows are inserted. This order cannot be predicted and
     * may differ on master and the slave.
     */
    public static final int ER_BINLOG_UNSAFE_CREATE_SELECT_AUTOINC = 1723;

    /**
     * INSERT... ON DUPLICATE KEY UPDATE  on a table with more than one UNIQUE KEY is unsafe
     */
    public static final int ER_BINLOG_UNSAFE_INSERT_TWO_KEYS = 1724;

    /**
     * Table is being used in foreign key check.
     */
    public static final int ER_TABLE_IN_FK_CHECK = 1725;

    /**
     * Storage engine '%s' does not support system tables. [%s.%s]
     */
    public static final int ER_UNSUPPORTED_ENGINE = 1726;

    /**
     * INSERT into autoincrement field which is not the first part in the composed primary key is unsafe.
     */
    public static final int ER_BINLOG_UNSAFE_AUTOINC_NOT_FIRST = 1727;

    /**
     * Cannot load from %s.%s. The table is probably corrupted
     */
    public static final int ER_CANNOT_LOAD_FROM_TABLE_V2 = 1728;

    /**
     * The requested value %s for the master delay exceeds the maximum %u
     */
    public static final int ER_MASTER_DELAY_VALUE_OUT_OF_RANGE = 1729;

    /**
     * Only Format_description_log_event and row events are allowed in BINLOG statements (but %s was provided)
     */
    public static final int ER_ONLY_FD_AND_RBR_EVENTS_ALLOWED_IN_BINLOG_STATEMENT = 1730;

    /**
     * Non matching attribute '%-.64s' between partition and table
     */
    public static final int ER_PARTITION_EXCHANGE_DIFFERENT_OPTION = 1731;

    /**
     * Table to exchange with partition is partitioned: '%-.64s'
     */
    public static final int ER_PARTITION_EXCHANGE_PART_TABLE = 1732;

    /**
     * Table to exchange with partition is temporary: '%-.64s'
     */
    public static final int ER_PARTITION_EXCHANGE_TEMP_TABLE = 1733;

    /**
     * Subpartitioned table, use subpartition instead of partition
     */
    public static final int ER_PARTITION_INSTEAD_OF_SUBPARTITION = 1734;

    /**
     * Unknown partition '%-.64s' in table '%-.64s'
     */
    public static final int ER_UNKNOWN_PARTITION = 1735;

    /**
     * Tables have different definitions
     */
    public static final int ER_TABLES_DIFFERENT_METADATA = 1736;

    /**
     * Found a row that does not match the partition
     */
    public static final int ER_ROW_DOES_NOT_MATCH_PARTITION = 1737;

    /**
     * Option binlog_cache_size (%lu) is greater than max_binlog_cache_size (%lu); setting binlog_cache_size equal to
     * max_binlog_cache_size.
     */
    public static final int ER_BINLOG_CACHE_SIZE_GREATER_THAN_MAX = 1738;

    /**
     * Cannot use %-.64s access on index '%-.64s' due to type or collation conversion on field '%-.64s'
     */
    public static final int ER_WARN_INDEX_NOT_APPLICABLE = 1739;

    /**
     * Table to exchange with partition has foreign key references: '%-.64s'
     */
    public static final int ER_PARTITION_EXCHANGE_FOREIGN_KEY = 1740;

    /**
     * Key value '%-.192s' was not found in table '%-.192s.%-.192s'
     */
    public static final int ER_NO_SUCH_KEY_VALUE = 1741;

    /**
     * Data for column '%s' too long
     */
    public static final int ER_RPL_INFO_DATA_TOO_LONG = 1742;

    /**
     * Replication event checksum verification failed while reading from network.
     */
    public static final int ER_NETWORK_READ_EVENT_CHECKSUM_FAILURE = 1743;

    /**
     * Replication event checksum verification failed while reading from a log file.
     */
    public static final int ER_BINLOG_READ_EVENT_CHECKSUM_FAILURE = 1744;

    /**
     * Option binlog_stmt_cache_size (%lu) is greater than max_binlog_stmt_cache_size (%lu); setting
     * binlog_stmt_cache_size equal to max_binlog_stmt_cache_size.
     */
    public static final int ER_BINLOG_STMT_CACHE_SIZE_GREATER_THAN_MAX = 1745;

    /**
     * Can't update table '%-.192s' while '%-.192s' is being created.
     */
    public static final int ER_CANT_UPDATE_TABLE_IN_CREATE_TABLE_SELECT = 1746;

    /**
     * PARTITION () clause on non partitioned table
     */
    public static final int ER_PARTITION_CLAUSE_ON_NONPARTITIONED = 1747;

    /**
     * Found a row not matching the given partition set
     */
    public static final int ER_ROW_DOES_NOT_MATCH_GIVEN_PARTITION_SET = 1748;

    /**
     * Partition '%-.64s' doesn't exist
     */
    // checkstyle, please ignore ConstantName for the next line
    public static final int ER_NO_SUCH_PARTITION__UNUSED = 1749;

    /**
     * Failure while changing the type of replication repository: %s.
     */
    public static final int ER_CHANGE_RPL_INFO_REPOSITORY_FAILURE = 1750;

    /**
     * The creation of some temporary tables could not be rolled back.
     */
    public static final int ER_WARNING_NOT_COMPLETE_ROLLBACK_WITH_CREATED_TEMP_TABLE = 1751;

    /**
     * Some temporary tables were dropped, but these operations could not be rolled back.
     */
    public static final int ER_WARNING_NOT_COMPLETE_ROLLBACK_WITH_DROPPED_TEMP_TABLE = 1752;

    /**
     * %s is not supported in multi-threaded slave mode. %s
     */
    public static final int ER_MTS_FEATURE_IS_NOT_SUPPORTED = 1753;

    /**
     * The number of modified databases exceeds the maximum %d; the database names will not be included in the
     * replication event metadata.
     */
    public static final int ER_MTS_UPDATED_DBS_GREATER_MAX = 1754;

    /**
     * Cannot execute the current event group in the parallel mode. Encountered event %s, relay-log name %s, position
     * %s which prevents execution of this event group in parallel mode. Reason: %s.
     */
    public static final int ER_MTS_CANT_PARALLEL = 1755;

    /**
     * %s
     */
    public static final int ER_MTS_INCONSISTENT_DATA = 1756;

    /**
     * FULLTEXT index is not supported for partitioned tables.
     */
    public static final int ER_FULLTEXT_NOT_SUPPORTED_WITH_PARTITIONING = 1757;

    /**
     * Invalid condition number
     */
    public static final int ER_DA_INVALID_CONDITION_NUMBER = 1758;

    /**
     * Sending passwords in plain text without SSL/TLS is extremely insecure.
     */
    public static final int ER_INSECURE_PLAIN_TEXT = 1759;

    /**
     * Storing MySQL user name or password information in the master info repository is not secure and is therefore
     * not recommended. Please consider using the USER and PASSWORD connection options for START SLAVE; see the
     * 'START SLAVE Syntax' in the MySQL Manual for more information.
     */
    public static final int ER_INSECURE_CHANGE_MASTER = 1760;

    /**
     * Foreign key constraint for table '%.192s', record '%-.192s' would lead to a duplicate entry in table '%.192s',
     * key '%.192s'
     */
    public static final int ER_FOREIGN_DUPLICATE_KEY_WITH_CHILD_INFO = 1761;

    /**
     * Foreign key constraint for table '%.192s', record '%-.192s' would lead to a duplicate entry in a child table
     */
    public static final int ER_FOREIGN_DUPLICATE_KEY_WITHOUT_CHILD_INFO = 1762;

    /**
     * Setting authentication options is not possible when only the Slave SQL Thread is being started.
     */
    public static final int ER_SQLTHREAD_WITH_SECURE_SLAVE = 1763;

    /**
     * The table does not have FULLTEXT index to support this query
     */
    public static final int ER_TABLE_HAS_NO_FT = 1764;

    /**
     * The system variable %.200s cannot be set in stored functions or triggers.
     */
    public static final int ER_VARIABLE_NOT_SETTABLE_IN_SF_OR_TRIGGER = 1765;

    /**
     * The system variable %.200s cannot be set when there is an ongoing transaction.
     */
    public static final int ER_VARIABLE_NOT_SETTABLE_IN_TRANSACTION = 1766;

    /**
     * The system variable @@SESSION.GTID_NEXT has the value %.200s, which is not listed in @@SESSION.GTID_NEXT_LIST.
     */
    public static final int ER_GTID_NEXT_IS_NOT_IN_GTID_NEXT_LIST = 1767;

    /**
     * The system variable @@SESSION.GTID_NEXT cannot change inside a transaction.
     */
    public static final int ER_CANT_CHANGE_GTID_NEXT_IN_TRANSACTION_WHEN_GTID_NEXT_LIST_IS_NULL = 1768;

    /**
     * The statement 'SET %.200s' cannot invoke a stored function.
     */
    public static final int ER_SET_STATEMENT_CANNOT_INVOKE_FUNCTION = 1769;

    /**
     * The system variable @@SESSION.GTID_NEXT cannot be 'AUTOMATIC' when @@SESSION.GTID_NEXT_LIST is non-NULL.
     */
    public static final int ER_GTID_NEXT_CANT_BE_AUTOMATIC_IF_GTID_NEXT_LIST_IS_NON_NULL = 1770;

    /**
     * Skipping transaction %.200s because it has already been executed and logged.
     */
    public static final int ER_SKIPPING_LOGGED_TRANSACTION = 1771;

    /**
     * Malformed GTID set specification '%.200s'.
     */
    public static final int ER_MALFORMED_GTID_SET_SPECIFICATION = 1772;

    /**
     * Malformed GTID set encoding.
     */
    public static final int ER_MALFORMED_GTID_SET_ENCODING = 1773;

    /**
     * Malformed GTID specification '%.200s'.
     */
    public static final int ER_MALFORMED_GTID_SPECIFICATION = 1774;

    /**
     * Impossible to generate Global Transaction Identifier: the integer component reached the maximal value. Restart
     * the server with a new server_uuid.
     */
    public static final int ER_GNO_EXHAUSTED = 1775;

    /**
     * Parameters MASTER_LOG_FILE, MASTER_LOG_POS, RELAY_LOG_FILE and RELAY_LOG_POS cannot be set when
     * MASTER_AUTO_POSITION is active.
     */
    public static final int ER_BAD_SLAVE_AUTO_POSITION = 1776;

    /**
     * CHANGE MASTER TO MASTER_AUTO_POSITION = 1 can only be executed when @@GLOBAL.GTID_MODE = ON.
     */
    public static final int ER_AUTO_POSITION_REQUIRES_GTID_MODE_ON = 1777;

    /**
     * Cannot execute statements with implicit commit inside a transaction when @@SESSION.GTID_NEXT != AUTOMATIC.
     */
    public static final int ER_CANT_DO_IMPLICIT_COMMIT_IN_TRX_WHEN_GTID_NEXT_IS_SET = 1778;

    /**
     * @@GLOBAL.GTID_MODE = ON or UPGRADE_STEP_2 requires @@GLOBAL.ENFORCE_GTID_CONSISTENCY = 1.
     */
    public static final int ER_GTID_MODE_2_OR_3_REQUIRES_ENFORCE_GTID_CONSISTENCY_ON = 1779;

    /**
     * @@GLOBAL.GTID_MODE = ON or UPGRADE_STEP_1 or UPGRADE_STEP_2 requires --log-bin and --log-slave-updates.
     */
    public static final int ER_GTID_MODE_REQUIRES_BINLOG = 1780;

    /**
     * @@SESSION.GTID_NEXT cannot be set to UUID:NUMBER when @@GLOBAL.GTID_MODE = OFF.
     */
    public static final int ER_CANT_SET_GTID_NEXT_TO_GTID_WHEN_GTID_MODE_IS_OFF = 1781;

    /**
     * @@SESSION.GTID_NEXT cannot be set to ANONYMOUS when @@GLOBAL.GTID_MODE = ON.
     */
    public static final int ER_CANT_SET_GTID_NEXT_TO_ANONYMOUS_WHEN_GTID_MODE_IS_ON = 1782;

    /**
     * @@SESSION.GTID_NEXT_LIST cannot be set to a non-NULL value when @@GLOBAL.GTID_MODE = OFF.
     */
    public static final int ER_CANT_SET_GTID_NEXT_LIST_TO_NON_NULL_WHEN_GTID_MODE_IS_OFF = 1783;

    /**
     * Found a Gtid_log_event or Previous_gtids_log_event when @@GLOBAL.GTID_MODE = OFF.
     */
    public static final int ER_FOUND_GTID_EVENT_WHEN_GTID_MODE_IS_OFF = 1784;

    /**
     * When @@GLOBAL.ENFORCE_GTID_CONSISTENCY = 1, updates to non-transactional tables can only be done in either
     * autocommitted statements or single-statement transactions, and never in the same statement as updates to
     * transactional tables.
     */
    public static final int ER_GTID_UNSAFE_NON_TRANSACTIONAL_TABLE = 1785;

    /**
     * CREATE TABLE ... SELECT is forbidden when @@GLOBAL.ENFORCE_GTID_CONSISTENCY = 1.
     */
    public static final int ER_GTID_UNSAFE_CREATE_SELECT = 1786;

    /**
     * When @@GLOBAL.ENFORCE_GTID_CONSISTENCY = 1, the statements CREATE TEMPORARY TABLE and DROP TEMPORARY TABLE can
     * be executed in a non-transactional context only, and require that AUTOCOMMIT = 1.
     */
    public static final int ER_GTID_UNSAFE_CREATE_DROP_TEMPORARY_TABLE_IN_TRANSACTION = 1787;

    /**
     * The value of @@GLOBAL.GTID_MODE can only change one step at a time: OFF <-> UPGRADE_STEP_1 <-> UPGRADE_STEP_2
     * <-> ON. Also note that this value must be stepped up or down simultaneously on all servers; see the Manual for
     * instructions.
     */
    public static final int ER_GTID_MODE_CAN_ONLY_CHANGE_ONE_STEP_AT_A_TIME = 1788;

    /**
     * The slave is connecting using CHANGE MASTER TO MASTER_AUTO_POSITION = 1, but the master has purged binary logs
     * containing GTIDs that the slave requires.
     */
    public static final int ER_MASTER_HAS_PURGED_REQUIRED_GTIDS = 1789;

    /**
     * @@SESSION.GTID_NEXT cannot be changed by a client that owns a GTID. The client owns %s. Ownership is released
     * on COMMIT or ROLLBACK.
     */
    public static final int ER_CANT_SET_GTID_NEXT_WHEN_OWNING_GTID = 1790;

    /**
     * Unknown EXPLAIN format name: '%s'
     */
    public static final int ER_UNKNOWN_EXPLAIN_FORMAT = 1791;

    /**
     * Cannot execute statement in a READ ONLY transaction.
     */
    public static final int ER_CANT_EXECUTE_IN_READ_ONLY_TRANSACTION = 1792;

    /**
     * Comment for table partition '%-.64s' is too long (max = %lu)
     */
    public static final int ER_TOO_LONG_TABLE_PARTITION_COMMENT = 1793;

    /**
     * Slave is not configured or failed to initialize properly. You must at least set --server-id to enable either a
     * master or a slave. Additional error messages can be found in the MySQL error log.
     */
    public static final int ER_SLAVE_CONFIGURATION = 1794;

    /**
     * InnoDB presently supports one FULLTEXT index creation at a time
     */
    public static final int ER_INNODB_FT_LIMIT = 1795;

    /**
     * Cannot create FULLTEXT index on temporary InnoDB table
     */
    public static final int ER_INNODB_NO_FT_TEMP_TABLE = 1796;

    /**
     * Column '%-.192s' is of wrong type for an InnoDB FULLTEXT index
     */
    public static final int ER_INNODB_FT_WRONG_DOCID_COLUMN = 1797;

    /**
     * Index '%-.192s' is of wrong type for an InnoDB FULLTEXT index
     */
    public static final int ER_INNODB_FT_WRONG_DOCID_INDEX = 1798;

    /**
     * Creating index '%-.192s' required more than 'innodb_online_alter_log_max_size' bytes of modification log.
     * Please try again.
     */
    public static final int ER_INNODB_ONLINE_LOG_TOO_BIG = 1799;

    /**
     * Unknown ALGORITHM '%s'
     */
    public static final int ER_UNKNOWN_ALTER_ALGORITHM = 1800;

    /**
     * Unknown LOCK type '%s'
     */
    public static final int ER_UNKNOWN_ALTER_LOCK = 1801;

    /**
     * CHANGE MASTER cannot be executed when the slave was stopped with an error or killed in MTS mode. Consider
     * using RESET SLAVE or START SLAVE UNTIL.
     */
    public static final int ER_MTS_CHANGE_MASTER_CANT_RUN_WITH_GAPS = 1802;

    /**
     * Cannot recover after SLAVE errored out in parallel execution mode. Additional error messages can be found in
     * the MySQL error log.
     */
    public static final int ER_MTS_RECOVERY_FAILURE = 1803;

    /**
     * Cannot clean up worker info tables. Additional error messages can be found in the MySQL error log.
     */
    public static final int ER_MTS_RESET_WORKERS = 1804;

    /**
     * Column count of %s.%s is wrong. Expected %d, found %d. The table is probably corrupted
     */
    public static final int ER_COL_COUNT_DOESNT_MATCH_CORRUPTED_V2 = 1805;

    /**
     * Slave must silently retry current transaction
     */
    public static final int ER_SLAVE_SILENT_RETRY_TRANSACTION = 1806;

    /**
     * There is a foreign key check running on table '%-.192s'. Cannot discard the table.
     */
    public static final int ER_DISCARD_FK_CHECKS_RUNNING = 1807;

    /**
     * Schema mismatch (%s)
     */
    public static final int ER_TABLE_SCHEMA_MISMATCH = 1808;

    /**
     * Table '%-.192s' in system tablespace
     */
    public static final int ER_TABLE_IN_SYSTEM_TABLESPACE = 1809;

    /**
     * IO Read error: (%lu, %s) %s
     */
    public static final int ER_IO_READ_ERROR = 1810;

    /**
     * IO Write error: (%lu, %s) %s
     */
    public static final int ER_IO_WRITE_ERROR = 1811;

    /**
     * Tablespace is missing for table '%-.192s'
     */
    public static final int ER_TABLESPACE_MISSING = 1812;

    /**
     * Tablespace for table '%-.192s' exists. Please DISCARD the tablespace before IMPORT.
     */
    public static final int ER_TABLESPACE_EXISTS = 1813;

    /**
     * Tablespace has been discarded for table '%-.192s'
     */
    public static final int ER_TABLESPACE_DISCARDED = 1814;

    /**
     * Internal error: %s
     */
    public static final int ER_INTERNAL_ERROR = 1815;

    /**
     * ALTER TABLE '%-.192s' IMPORT TABLESPACE failed with error %lu : '%s'
     */
    public static final int ER_INNODB_IMPORT_ERROR = 1816;

    /**
     * Index corrupt: %s
     */
    public static final int ER_INNODB_INDEX_CORRUPT = 1817;

    /**
     * YEAR(%lu) column type is deprecated. Creating YEAR(4) column instead.
     */
    public static final int ER_INVALID_YEAR_COLUMN_LENGTH = 1818;

    /**
     * Your password does not satisfy the current policy requirements
     */
    public static final int ER_NOT_VALID_PASSWORD = 1819;

    /**
     * You must SET PASSWORD before executing this statement
     */
    public static final int ER_MUST_CHANGE_PASSWORD = 1820;

    /**
     * Failed to add the foreign key constaint. Missing index for constraint '%s' in the foreign table '%s'
     */
    public static final int ER_FK_NO_INDEX_CHILD = 1821;

    /**
     * Failed to add the foreign key constaint. Missing index for constraint '%s' in the referenced table '%s'
     */
    public static final int ER_FK_NO_INDEX_PARENT = 1822;

    /**
     * Failed to add the foreign key constraint '%s' to system tables
     */
    public static final int ER_FK_FAIL_ADD_SYSTEM = 1823;

    /**
     * Failed to open the referenced table '%s'
     */
    public static final int ER_FK_CANNOT_OPEN_PARENT = 1824;

    /**
     * Failed to add the foreign key constraint on table '%s'. Incorrect options in FOREIGN KEY constraint '%s'
     */
    public static final int ER_FK_INCORRECT_OPTION = 1825;

    /**
     * Duplicate foreign key constraint name '%s'
     */
    public static final int ER_FK_DUP_NAME = 1826;

    /**
     * The password hash doesn't have the expected format. Check if the correct password algorithm is being used with
     * the PASSWORD() function.
     */
    public static final int ER_PASSWORD_FORMAT = 1827;

    /**
     * Cannot drop column '%-.192s': needed in a foreign key constraint '%-.192s'
     */
    public static final int ER_FK_COLUMN_CANNOT_DROP = 1828;

    /**
     * Cannot drop column '%-.192s': needed in a foreign key constraint '%-.192s' of table '%-.192s'
     */
    public static final int ER_FK_COLUMN_CANNOT_DROP_CHILD = 1829;

    /**
     * Column '%-.192s' cannot be NOT NULL: needed in a foreign key constraint '%-.192s' SET NULL
     */
    public static final int ER_FK_COLUMN_NOT_NULL = 1830;

    /**
     * Duplicate index '%-.64s' defined on the table '%-.64s.%-.64s'. This is deprecated and will be disallowed in a
     * future release.
     */
    public static final int ER_DUP_INDEX = 1831;

    /**
     * Cannot change column '%-.192s': used in a foreign key constraint '%-.192s'
     */
    public static final int ER_FK_COLUMN_CANNOT_CHANGE = 1832;

    /**
     * Cannot change column '%-.192s': used in a foreign key constraint '%-.192s' of table '%-.192s'
     */
    public static final int ER_FK_COLUMN_CANNOT_CHANGE_CHILD = 1833;

    /**
     * Cannot delete rows from table which is parent in a foreign key constraint '%-.192s' of table '%-.192s'
     */
    public static final int ER_FK_CANNOT_DELETE_PARENT = 1834;

    /**
     * Malformed communication packet.
     */
    public static final int ER_MALFORMED_PACKET = 1835;

    /**
     * Running in read-only mode
     */
    public static final int ER_READ_ONLY_MODE = 1836;

    /**
     * When @@SESSION.GTID_NEXT is set to a GTID, you must explicitly set it to a different value after a COMMIT or
     * ROLLBACK. Please check GTID_NEXT variable manual page for detailed explanation. Current @@SESSION.GTID_NEXT is
     * '%s'.
     */
    public static final int ER_GTID_NEXT_TYPE_UNDEFINED_GROUP = 1837;

    /**
     * The system variable %.200s cannot be set in stored procedures.
     */
    public static final int ER_VARIABLE_NOT_SETTABLE_IN_SP = 1838;

    /**
     * @@GLOBAL.GTID_PURGED can only be set when @@GLOBAL.GTID_MODE = ON.
     */
    public static final int ER_CANT_SET_GTID_PURGED_WHEN_GTID_MODE_IS_OFF = 1839;

    /**
     * @@GLOBAL.GTID_PURGED can only be set when @@GLOBAL.GTID_EXECUTED is empty.
     */
    public static final int ER_CANT_SET_GTID_PURGED_WHEN_GTID_EXECUTED_IS_NOT_EMPTY = 1840;

    /**
     * @@GLOBAL.GTID_PURGED can only be set when there are no ongoing transactions (not even in other clients).
     */
    public static final int ER_CANT_SET_GTID_PURGED_WHEN_OWNED_GTIDS_IS_NOT_EMPTY = 1841;

    /**
     * @@GLOBAL.GTID_PURGED was changed from '%s' to '%s'.
     */
    public static final int ER_GTID_PURGED_WAS_CHANGED = 1842;

    /**
     * @@GLOBAL.GTID_EXECUTED was changed from '%s' to '%s'.
     */
    public static final int ER_GTID_EXECUTED_WAS_CHANGED = 1843;

    /**
     * Cannot execute statement: impossible to write to binary log since BINLOG_FORMAT = STATEMENT, and both
     * replicated and non replicated tables are written to.
     */
    public static final int ER_BINLOG_STMT_MODE_AND_NO_REPL_TABLES = 1844;

    /**
     * %s is not supported for this operation. Try %s.
     */
    public static final int ER_ALTER_OPERATION_NOT_SUPPORTED = 1845;

    /**
     * %s is not supported. Reason: %s. Try %s.
     */
    public static final int ER_ALTER_OPERATION_NOT_SUPPORTED_REASON = 1846;

    /**
     * COPY algorithm requires a lock
     */
    public static final int ER_ALTER_OPERATION_NOT_SUPPORTED_REASON_COPY = 1847;

    /**
     * Partition specific operations do not yet support LOCK/ALGORITHM
     */
    public static final int ER_ALTER_OPERATION_NOT_SUPPORTED_REASON_PARTITION = 1848;

    /**
     * Columns participating in a foreign key are renamed
     */
    public static final int ER_ALTER_OPERATION_NOT_SUPPORTED_REASON_FK_RENAME = 1849;

    /**
     * Cannot change column type INPLACE
     */
    public static final int ER_ALTER_OPERATION_NOT_SUPPORTED_REASON_COLUMN_TYPE = 1850;

    /**
     * Adding foreign keys needs foreign_key_checks=OFF
     */
    public static final int ER_ALTER_OPERATION_NOT_SUPPORTED_REASON_FK_CHECK = 1851;

    /**
     * Creating unique indexes with IGNORE requires COPY algorithm to remove duplicate rows
     */
    public static final int ER_ALTER_OPERATION_NOT_SUPPORTED_REASON_IGNORE = 1852;

    /**
     * Dropping a primary key is not allowed without also adding a new primary key
     */
    public static final int ER_ALTER_OPERATION_NOT_SUPPORTED_REASON_NOPK = 1853;

    /**
     * Adding an auto-increment column requires a lock
     */
    public static final int ER_ALTER_OPERATION_NOT_SUPPORTED_REASON_AUTOINC = 1854;

    /**
     * Cannot replace hidden FTS_DOC_ID with a user-visible one
     */
    public static final int ER_ALTER_OPERATION_NOT_SUPPORTED_REASON_HIDDEN_FTS = 1855;

    /**
     * Cannot drop or rename FTS_DOC_ID
     */
    public static final int ER_ALTER_OPERATION_NOT_SUPPORTED_REASON_CHANGE_FTS = 1856;

    /**
     * Fulltext index creation requires a lock
     */
    public static final int ER_ALTER_OPERATION_NOT_SUPPORTED_REASON_FTS = 1857;

    /**
     * Sql_slave_skip_counter can not be set when the server is running with @@GLOBAL.GTID_MODE = ON. Instead, for
     * each transaction that you want to skip, generate an empty transaction with the same GTID as the transaction
     */
    public static final int ER_SQL_SLAVE_SKIP_COUNTER_NOT_SETTABLE_IN_GTID_MODE = 1858;

    /**
     * Duplicate entry for key '%-.192s'
     */
    public static final int ER_DUP_UNKNOWN_IN_INDEX = 1859;

    /**
     * Long database name and identifier for object resulted in path length exceeding %d characters. Path: '%s'.
     */
    public static final int ER_IDENT_CAUSES_TOO_LONG_PATH = 1860;

    /**
     * Cannot silently convert NULL values, as required in this SQL_MODE
     */
    public static final int ER_ALTER_OPERATION_NOT_SUPPORTED_REASON_NOT_NULL = 1861;

    /**
     * Your password has expired. To log in you must change it using a client that supports expired passwords.
     */
    public static final int ER_MUST_CHANGE_PASSWORD_LOGIN = 1862;

    /**
     * Found a row in wrong partition %s
     */
    public static final int ER_ROW_IN_WRONG_PARTITION = 1863;

    /**
     * Cannot schedule event %s, relay-log name %s, position %s to Worker thread because its size %lu exceeds %lu of
     * slave_pending_jobs_size_max.
     */
    public static final int ER_MTS_EVENT_BIGGER_PENDING_JOBS_SIZE_MAX = 1864;

    /**
     * Cannot CREATE FULLTEXT INDEX WITH PARSER on InnoDB table
     */
    public static final int ER_INNODB_NO_FT_USES_PARSER = 1865;

    /**
     * The binary log file '%s' is logically corrupted: %s
     */
    public static final int ER_BINLOG_LOGICAL_CORRUPTION = 1866;

    /**
     * File %s was not purged because it was being read by %d thread(s), purged only %d out of %d files.
     */
    public static final int ER_WARN_PURGE_LOG_IN_USE = 1867;

    /**
     * File %s was not purged because it is the active log file.
     */
    public static final int ER_WARN_PURGE_LOG_IS_ACTIVE = 1868;

    /**
     * Auto-increment value in UPDATE conflicts with internally generated values
     */
    public static final int ER_AUTO_INCREMENT_CONFLICT = 1869;

    /**
     * Row events are not logged for %s statements that modify BLACKHOLE tables in row format. Table(s): '%-.192s'
     */
    public static final int WARN_ON_BLOCKHOLE_IN_RBR = 1870;

    /**
     * Slave failed to initialize master info structure from the repository
     */
    public static final int ER_SLAVE_MI_INIT_REPOSITORY = 1871;

    /**
     * Slave failed to initialize relay log info structure from the repository
     */
    public static final int ER_SLAVE_RLI_INIT_REPOSITORY = 1872;

    /**
     * Access denied trying to change to user '%-.48s'@'%-.64s' (using password: %s). Disconnecting.
     */
    public static final int ER_ACCESS_DENIED_CHANGE_USER_ERROR = 1873;

    /**
     * InnoDB is in read only mode.
     */
    public static final int ER_INNODB_READ_ONLY = 1874;

    /**
     * STOP SLAVE command execution is incomplete: Slave SQL thread got the stop signal, thread is busy, SQL thread
     * will stop once the current task is complete.
     */
    public static final int ER_STOP_SLAVE_SQL_THREAD_TIMEOUT = 1875;

    /**
     * STOP SLAVE command execution is incomplete: Slave IO thread got the stop signal, thread is busy, IO thread
     * will stop once the current task is complete.
     */
    public static final int ER_STOP_SLAVE_IO_THREAD_TIMEOUT = 1876;

    /**
     * Operation cannot be performed. The table '%-.64s.%-.64s' is missing, corrupt or contains bad data.
     */
    public static final int ER_TABLE_CORRUPT = 1877;

    /**
     * Temporary file write failure.
     */
    public static final int ER_TEMP_FILE_WRITE_FAILURE = 1878;

    /**
     * Upgrade index name failed, please use create index(alter table) algorithm copy to rebuild index.
     */
    public static final int ER_INNODB_FT_AUX_NOT_HEX_ID = 1879;

    /**
     * TIME/TIMESTAMP/DATETIME columns of old format have been upgraded to the new format.
     */
    public static final int ER_OLD_TEMPORALS_UPGRADED = 1880;

    /**
     * Operation not allowed when innodb_forced_recovery > 0.
     */
    public static final int ER_INNODB_FORCED_RECOVERY = 1881;

    /**
     * The initialization vector supplied to %s is too short. Must be at least %d bytes long
     */
    public static final int ER_AES_INVALID_IV = 1882;

    /**
     * Plugin '%s' cannot be uninstalled now. %s
     */
    public static final int ER_PLUGIN_CANNOT_BE_UNINSTALLED = 1883;

    /**
     * Cannot execute statement because it needs to be written to the binary log as multiple statements, and this is
     * not allowed when @@SESSION.GTID_NEXT == 'UUID:NUMBER'.
     */
    public static final int ER_GTID_UNSAFE_BINLOG_SPLITTABLE_STATEMENT_AND_GTID_GROUP = 1884;

    /**
     * Slave has more GTIDs than the master has, using the master's SERVER_UUID. This may indicate that the end of
     * the binary log was truncated or that the last binary log file was lost, e.g., after a power or disk failure
     * when sync_binlog != 1. The master may or may not have rolled back transactions that were already replicated to
     * the slave. Suggest to replicate any transactions that master has rolled back from slave to master, and/or
     * commit empty transactions on master to account for transactions that have been committed on master but are not
     * included in GTID_EXECUTED.
     */
    public static final int ER_SLAVE_HAS_MORE_GTIDS_THAN_MASTER = 1885;

    private ErrorCode() {
    }
}

