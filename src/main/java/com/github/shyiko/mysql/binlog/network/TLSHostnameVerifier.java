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

import sun.security.util.HostnameChecker;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class TLSHostnameVerifier implements HostnameVerifier {

    public boolean verify(String hostname, SSLSession session) {
        HostnameChecker checker = HostnameChecker.getInstance(HostnameChecker.TYPE_TLS);
        try {
            Certificate[] peerCertificates = session.getPeerCertificates();
            if (peerCertificates.length > 0 && peerCertificates[0] instanceof X509Certificate) {
                X509Certificate peerCertificate = (X509Certificate) peerCertificates[0];
                try {
                    checker.match(hostname, peerCertificate);
                    return true;
                } catch (CertificateException ignored) {
                }
            }
        } catch (SSLPeerUnverifiedException ignored) {
        }
        return false;
    }

}
