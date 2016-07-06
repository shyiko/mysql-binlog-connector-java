# mysql-binlog-connector-java [![Build Status](https://travis-ci.org/shyiko/mysql-binlog-connector-java.svg?branch=master)](https://travis-ci.org/shyiko/mysql-binlog-connector-java) [![Coverage Status](https://coveralls.io/repos/shyiko/mysql-binlog-connector-java/badge.svg?branch=master)](https://coveralls.io/r/shyiko/mysql-binlog-connector-java?branch=master) [![Maven Central](http://img.shields.io/badge/maven_central-0.3.1-blue.svg?style=flat)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.shyiko%22%20AND%20a%3A%22mysql-binlog-connector-java%22)

MySQL Binary Log connector.

Initially project was started as a fork of [open-replicator](https://code.google.com/p/open-replicator), 
but ended up as a complete rewrite. Key differences/features:

- automatic binlog filename/position | GTID resolution
- resumable disconnects
- plugable failover strategies
- binlog_checksum=CRC32 support (for MySQL 5.6.2+ users)
- secure communication over the TLS
- JMX-friendly
- real-time stats
- availability in Maven Central
- no third-party dependencies
- test suite over different versions of MySQL releases

> If you are looking for something similar in other languages - check out 
[siddontang/go-mysql](https://github.com/siddontang/go-mysql) (Go), 
[noplay/python-mysql-replication](https://github.com/noplay/python-mysql-replication) (Python).

## Usage

Get the latest JAR(s) from [here](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.shyiko%22%20AND%20a%3A%22mysql-binlog-connector-java%22). Alternatively you can include following Maven dependency (available through Maven Central):

```xml
<dependency>
    <groupId>com.github.shyiko</groupId>
    <artifactId>mysql-binlog-connector-java</artifactId>
    <version>0.3.1</version>
</dependency>
```

#### Reading binary log file

```java
File binlogFile = ...
BinaryLogFileReader reader = new BinaryLogFileReader(binlogFile);
try {
    for (Event event; (event = reader.readEvent()) != null; ) {
        ...
    }
} finally {
    reader.close();
}
```

#### Tapping into MySQL replication stream

> PREREQUISITES: Whichever user you plan to use for the BinaryLogClient, he MUST have [REPLICATION SLAVE](http://dev.mysql.com/doc/refman/5.5/en/privileges-provided.html#priv_replication-slave) privilege. Unless you specify binlogFilename/binlogPosition yourself (in which case automatic resolution won't kick in), you'll need [REPLICATION CLIENT](http://dev.mysql.com/doc/refman/5.5/en/privileges-provided.html#priv_replication-client) granted as well.

```java
BinaryLogClient client = new BinaryLogClient("hostname", 3306, "username", "password");
client.registerEventListener(new EventListener() {

    @Override
    public void onEvent(Event event) {
        ...
    }
});
client.connect();
```

> By default, BinaryLogClient starts from the current (at the time of connect) master binlog position. If you wish to
kick off from a specific filename or position, use `client.setBinlogFilename(filename)` + `client.setBinlogPosition(position)`.

> `client.connect()` is blocking (meaning that client will listen for events in the current thread). 
`client.connect(timeout)`, on the other hand, spawns a separate thread.  

#### Controlling event deserialization

> You might need it for several reasons: 
you don't want to waste time deserializing events you won't need; 
there is no EventDataDeserializer defined for the event type you are interested in (or there is but it contains a bug); 
you want certain type of events to be deserialized in a different way (perhaps *RowsEventData should contain table 
name and not id?); etc.

```java
EventDeserializer eventDeserializer = new EventDeserializer();

// do not deserialize EXT_DELETE_ROWS event data, return it as a byte array
eventDeserializer.setEventDataDeserializer(EventType.EXT_DELETE_ROWS, 
    new ByteArrayEventDataDeserializer()); 

// skip EXT_WRITE_ROWS event data altogether
eventDeserializer.setEventDataDeserializer(EventType.EXT_WRITE_ROWS, 
    new NullEventDataDeserializer());

// use custom event data deserializer for EXT_DELETE_ROWS
eventDeserializer.setEventDataDeserializer(EventType.EXT_DELETE_ROWS, 
    new EventDataDeserializer() {
        ...
    });

BinaryLogClient client = ...
client.setEventDeserializer(eventDeserializer);
```

#### Exposing BinaryLogClient through JMX

```java
MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

BinaryLogClient binaryLogClient = ...
ObjectName objectName = new ObjectName("mysql.binlog:type=BinaryLogClient");
mBeanServer.registerMBean(binaryLogClient, objectName);

// following bean accumulates various BinaryLogClient stats 
// (e.g. number of disconnects, skipped events)
BinaryLogClientStatistics stats = new BinaryLogClientStatistics(binaryLogClient);
ObjectName statsObjectName = new ObjectName("mysql.binlog:type=BinaryLogClientStatistics");
mBeanServer.registerMBean(stats, statsObjectName);
```

#### Using SSL

> Introduced in 1.0.0.

TLSv1.1 & TLSv1.2 require [JDK 7](http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6916074)+.  
Prior to MySQL 5.7.10, MySQL supported only TLSv1 
(see [Secure Connection Protocols and Ciphers](http://dev.mysql.com/doc/refman/5.7/en/secure-connection-protocols-ciphers.html)). 

> To check that MySQL server is [properly configured with SSL support](http://dev.mysql.com/doc/refman/5.7/en/using-secure-connections.html) -
`mysql -h host -u root -ptypeyourpasswordmaybe -e "show global variables like 'have_%ssl';"` ("Value" 
should be "YES"). State of the current session can be determined using `\s` ("SSL" should not be blank).

```java
System.setProperty("javax.net.ssl.trustStore", "/path/to/truststore.jks");
System.setProperty("javax.net.ssl.trustStorePassword","truststore.password");
System.setProperty("javax.net.ssl.keyStore", "/path/to/keystore.jks");
System.setProperty("javax.net.ssl.keyStorePassword", "keystore.password");

BinaryLogClient client = ...
client.setSSLMode(SSLMode.VERIFY_IDENTITY);
```

## Implementation notes

- data of numeric types (tinyint, etc) always returned signed(!) regardless of whether column definition includes "unsigned" keyword or not.
- data of var\*/\*text/\*blob types always returned as a byte array (for var\* this is true starting from 1.0.0). 

## Frequently Asked Questions

**Q**. How does a typical transaction look like?
 
**A**. GTID event (if gtid_mode=ON) -> QUERY event with "BEGIN" as sql -> ... -> XID event | QUERY event with "COMMIT" or "ROLLBACK" as sql. 

**Q**. EventData for inserted/updated/deleted rows has no information about table (except for some weird id). 
How do I make sense out of it?  

**A**. Each [WriteRowsEventData](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/event/WriteRowsEventData.java)/[UpdateRowsEventData](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/event/UpdateRowsEventData.java)/[DeleteRowsEventData](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/event/DeleteRowsEventData.java) event is preceded by [TableMapEventData](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/event/TableMapEventData.java) which
contains schema & table name. If for some reason you need to know column names (types, etc). - the easiest way is to

```sql
select TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, ORDINAL_POSITION, COLUMN_DEFAULT, IS_NULLABLE, 
DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, CHARACTER_OCTET_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE, 
CHARACTER_SET_NAME, COLLATION_NAME from INFORMATION_SCHEMA.COLUMNS;
# see https://dev.mysql.com/doc/refman/5.6/en/columns-table.html for more information
```

(yes, binary log DOES NOT include that piece of information).

You can find JDBC snippet [here](https://github.com/shyiko/mysql-binlog-connector-java/issues/24#issuecomment-43747417).

## Documentation

#### API overview

There are two entry points - [BinaryLogClient](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/BinaryLogClient.java) (which you can use to read binary logs from a MySQL server) and 
[BinaryLogFileReader](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/BinaryLogFileReader.java) (for offline log processing). Both of them rely on [EventDeserializer](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/event/deserialization/EventDeserializer.java) to deserialize 
stream of events. Each [Event](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/event/Event.java) consists of [EventHeader](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/event/EventHeader.java) (containing among other things reference to [EventType](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/event/EventType.java)) and 
[EventData](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/event/EventData.java). The aforementioned EventDeserializer has one [EventHeaderDeserializer](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/event/deserialization/EventHeaderDeserializer.java) ([EventHeaderV4Deserializer](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/event/deserialization/EventHeaderV4Deserializer.java) by default) 
and [a collection of EventDataDeserializer|s](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/event/deserialization/EventDeserializer.java#L82). If there is no EventDataDeserializer registered for
some particular type of Event - default EventDataDeserializer kicks in ([NullEventDataDeserializer](https://github.com/shyiko/mysql-binlog-connector-java/blob/master/src/main/java/com/github/shyiko/mysql/binlog/event/deserialization/NullEventDataDeserializer.java)).

#### MySQL Internals Manual

For the insight into the internals of MySQL look [here](https://dev.mysql.com/doc/internals/en/index.html). [MySQL Client/Server Protocol](http://dev.mysql.com/doc/internals/en/client-server-protocol.html) and [The Binary Log](http://dev.mysql.com/doc/internals/en/binary-log.html) sections are particularly useful as a reference documentation for the `**.binlog.network` and `**.binlog.event` packages.

## Real-world applications

Some of the OSS built on top of mysql-binlog-conector-java: 
[debezium](https://github.com/debezium/debezium) (distributed platform for change data capture),
[mardambey/mypipe](https://github.com/mardambey/mypipe) (MySQL to Apache Kafka replicator),
[ngocdaothanh/mydit](https://github.com/ngocdaothanh/mydit) (MySQL to MongoDB replicator),
[shyiko/rook](https://github.com/shyiko/rook) (generic Change Data Capture (CDC) toolkit).

It's also used [on a large scale](https://twitter.com/atwinmutt/status/626816601078300672) in MailChimp. You can read about it [here](http://devs.mailchimp.com/blog/powering-mailchimp-pro-reporting/).  

## Development

```sh
git clone https://github.com/shyiko/mysql-binlog-connector-java.git
cd mysql-binlog-connector-java
mvn # shows how to build, test, etc. project
```

## Contributing

In lieu of a formal styleguide, please take care to maintain the existing coding style.  
Executing `mvn checkstyle:check` within project directory should not produce any errors.  
If you are willing to install [vagrant](http://www.vagrantup.com/) (required by integration tests) it's highly recommended
to check (with `mvn clean verify`) that there are no test failures before sending a pull request.  
Additional tests for any new or changed functionality are also very welcomed.

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
