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
package com.github.shyiko.mysql.binlog.network;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 * @see <a href="http://dev.mysql.com/doc/internals/en/capability-flags.html#packet-Protocol::CapabilityFlags">
 *      Capability Flags</a>
 */
public final class ClientCapabilities {

    public static final int LONG_PASSWORD = 1; /* new more secure passwords */
    public static final int FOUND_ROWS = 1 << 1; /* found instead of affected rows */
    public static final int LONG_FLAG = 1 << 2; /* get all column flags */
    public static final int CONNECT_WITH_DB = 1 << 3; /* one can specify db on connect */
    public static final int NO_SCHEMA = 1 << 4; /* don't allow database.table.column */
    public static final int COMPRESS = 1 << 5; /* can use compression protocol */
    public static final int ODBC = 1 << 6; /* odbc client */
    public static final int LOCAL_FILES = 1 << 7; /* can use LOAD DATA LOCAL */
    public static final int IGNORE_SPACE = 1 << 8; /* ignore spaces before '' */
    public static final int PROTOCOL_41 = 1 << 9; /* new 4.1 protocol */
    public static final int INTERACTIVE = 1 << 10; /* this is an interactive client */
    public static final int SSL = 1 << 11; /* switch to ssl after handshake */
    public static final int IGNORE_SIGPIPE = 1 << 12; /* IGNORE sigpipes */
    public static final int TRANSACTIONS = 1 << 13; /* client knows about transactions */
    public static final int RESERVED = 1 << 14; /* old flag for 4.1 protocol  */
    public static final int SECURE_CONNECTION = 1 << 15; /* new 4.1 authentication */
    public static final int MULTI_STATEMENTS = 1 << 16; /* enable/disable multi-stmt support */
    public static final int MULTI_RESULTS = 1 << 17; /* enable/disable multi-results */
    public static final int PS_MULTI_RESULTS = 1 << 18; /* multi-results in ps-protocol */
    public static final int PLUGIN_AUTH = 1 << 19; /* client supports plugin authentication */
    public static final int PLUGIN_AUTH_LENENC_CLIENT_DATA = 1 << 21;
    public static final int SSL_VERIFY_SERVER_CERT = 1 << 30;
    public static final int REMEMBER_OPTIONS = 1 << 31;

    private ClientCapabilities() {
    }
}
