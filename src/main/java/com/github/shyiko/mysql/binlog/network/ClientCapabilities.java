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
 */
public final class ClientCapabilities {

    public static final int LONG_PASSWORD = 1; /* new more secure passwords */
    public static final int FOUND_ROWS = 2; /* found instead of affected rows */
    public static final int LONG_FLAG = 4; /* get all column flags */
    public static final int CONNECT_WITH_DB = 8; /* one can specify db on connect */
    public static final int NO_SCHEMA = 16; /* don't allow database.table.column */
    public static final int COMPRESS = 32; /* can use compression protocol */
    public static final int ODBC = 64; /* odbc client */
    public static final int LOCAL_FILES = 128; /* can use LOAD DATA LOCAL */
    public static final int IGNORE_SPACE = 256; /* ignore spaces before '' */
    public static final int PROTOCOL_41 = 512; /* new 4.1 protocol */
    public static final int INTERACTIVE = 1024; /* this is an interactive client */
    public static final int SSL = 2048; /* switch to ssl after handshake */
    public static final int IGNORE_SIGPIPE = 4096; /* IGNORE sigpipes */
    public static final int TRANSACTIONS = 8192; /* client knows about transactions */
    public static final int RESERVED = 16384; /* old flag for 4.1 protocol  */
    public static final int SECURE_CONNECTION = 32768; /* new 4.1 authentication */
    public static final int MULTI_STATEMENTS = 1 << 16; /* enable/disable multi-stmt support */
    public static final int MULTI_RESULTS = 1 << 17; /* enable/disable multi-results */
    public static final int PS_MULTI_RESULTS = 1 << 18; /* multi-results in ps-protocol */
    public static final int PLUGIN_AUTH = 1 << 19; /* client supports plugin authentication */
    public static final int SSL_VERIFY_SERVER_CERT = 1 << 30;
    public static final int REMEMBER_OPTIONS = 1 << 31;

    private ClientCapabilities() {
    }
}
