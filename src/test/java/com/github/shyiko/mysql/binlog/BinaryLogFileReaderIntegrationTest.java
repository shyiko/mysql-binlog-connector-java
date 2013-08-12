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
package com.github.shyiko.mysql.binlog;

import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class BinaryLogFileReaderIntegrationTest {

    @Test
    public void testNextEvent() throws Exception {
        BinaryLogFileReader reader = new BinaryLogFileReader(new GZIPInputStream(
                new FileInputStream("src/test/resources/mysql-bin.sakila.gz")));
        try {
            int numberOfEvents = 0;
            while ((reader.readEvent()) != null) {
                numberOfEvents++;
            }
            assertEquals(numberOfEvents, 1462);
        } finally {
            reader.close();
        }
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "Not a valid binary log")
    public void testMagicHeaderCheck() throws Exception {
        new BinaryLogFileReader(new File("src/test/resources/mysql-bin.sakila.gz"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullEventDeserializerIsNotAllowed() throws Exception {
        new BinaryLogFileReader(new File("src/test/resources/mysql-bin.sakila.gz"), null);
    }

}
