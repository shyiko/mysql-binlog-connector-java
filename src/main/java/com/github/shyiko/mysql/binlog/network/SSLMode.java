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
package com.github.shyiko.mysql.binlog.network;

/**
 * @see <a href="https://dev.mysql.com/doc/refman/5.7/en/secure-connection-options.html#option_general_ssl-mode>
 * ssl-mode</a> for the original documentation.
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public enum SSLMode {

    /**
     * Establish a secure (encrypted) connection if the server supports secure connections.
     * Fall back to an unencrypted connection otherwise.
     */
    PREFERRED,
    /**
     * Establish an unencrypted connection.
     */
    DISABLED,
    /**
     * Establish a secure connection if the server supports secure connections.
     * The connection attempt fails if a secure connection cannot be established.
     */
    REQUIRED,
    /**
     * Like REQUIRED, but additionally verify the server TLS certificate against the configured Certificate Authority
     * (CA) certificates. The connection attempt fails if no valid matching CA certificates are found.
     */
    VERIFY_CA,
    /**
     * Like VERIFY_CA, but additionally verify that the server certificate matches the host to which the connection is
     * attempted.
     */
    VERIFY_IDENTITY

}
