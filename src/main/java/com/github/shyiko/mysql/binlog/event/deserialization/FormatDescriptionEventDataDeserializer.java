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

import com.github.shyiko.mysql.binlog.event.FormatDescriptionEventData;
import com.github.shyiko.mysql.binlog.io.ByteArrayInputStream;

import java.io.IOException;
import java.util.Comparator;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class FormatDescriptionEventDataDeserializer implements EventDataDeserializer<FormatDescriptionEventData> {

    private Comparator<String> versionComparator = new NaturalOrderComparator();

    @Override
    public FormatDescriptionEventData deserialize(ByteArrayInputStream inputStream) throws IOException {
        FormatDescriptionEventData eventData = new FormatDescriptionEventData();
        eventData.setBinlogVersion(inputStream.readInteger(2));
        eventData.setServerVersion(inputStream.readString(50).trim());
        inputStream.skip(4); // redundant, present in a header
        eventData.setHeaderLength(inputStream.readInteger(1));
        ChecksumType checksumType = ChecksumType.NONE;
        String serverVersion = eventData.getServerVersion().toLowerCase();
        if (versionComparator.compare("5.6.1", serverVersion) <= 0 ||
            (serverVersion.contains("mariadb") && versionComparator.compare("5.3", serverVersion) <= 0)) {
            inputStream.skip(inputStream.available() - 5); // event types
            if (inputStream.read() == 1) {
                checksumType = ChecksumType.CRC32;
            }
            // 4 remaining bytes - checksum
        }
        eventData.setChecksumType(checksumType);
        return eventData;
    }

    /**
     * Generic natural order comparator which yields: 5.6 < 5.6.2 < 5.6.2-a < 5.6.2-b < 5.6.12.
     */
    private static class NaturalOrderComparator implements Comparator<String> {

        /**
         * General idea is to find first-from-the-left character that is different and (if both characters are numbers)
         * compare the "remaining numeric part" of both strings.
         *
         * Complexity: time - O(n), space - O(1).
         */
        @Override
        public int compare(String l, String r) {
            int ll = l.length(), rl = r.length(), di, dc;
            if (ll > rl) {
                return -1 * compare(r, l);
            }
            for (di = 0, dc = 0; di < ll && (dc = l.charAt(di) - r.charAt(di)) == 0; di++) { }
            if (di == ll) {
                return ll - rl;
            }
            if (Character.isDigit(l.charAt(di))) {
                if (Character.isDigit(r.charAt(di))) {
                    int lnl = 0, rnl = 0;
                    for (int j = di + 1; j < ll && Character.isDigit(l.charAt(j)); j++, lnl++) { }
                    for (int j = di + 1, e = Math.min(j + lnl + 1, rl);
                         j < e && Character.isDigit(r.charAt(j)); j++, rnl++) { }
                    return lnl == rnl ? dc : lnl - rnl;
                }
                return 1;
            } else
            if (Character.isDigit(r.charAt(di))) {
                return -1;
            }
            return dc;
        }
    }
}
