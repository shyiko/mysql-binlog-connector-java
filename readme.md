# mysql-binlog-connector-java [![Build Status](https://travis-ci.org/shyiko/mysql-binlog-connector-java.png?branch=master)](https://travis-ci.org/shyiko/mysql-binlog-connector-java) [![Coverage Status](https://coveralls.io/repos/shyiko/mysql-binlog-connector-java/badge.png?branch=master)](https://coveralls.io/r/shyiko/mysql-binlog-connector-java?branch=master)

MySQL Binary Log connector.

Initially project was started as a fork of [open-replicator](https://code.google.com/p/open-replicator), but ended up as a complete rewrite. Key differences/features:

- automatic binlog filename/position resolution
- resumable disconnects
- plugable failover strategies
- JMX exposure (optionally with statistics)
- availability in Maven Central
- no third-party dependencies
- binlog_checksum support (for MySQL 5.6.2+ users)
- test suite over different versions of MySQL releases
- support Gtid mode for MySQL and MariaDB

## Usage

Get the latest JAR(s) from [here](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.shyiko%22%20AND%20a%3A%22mysql-binlog-connector-java%22). Alternatively you can include following Maven dependency (available through Maven Central):

```xml
<dependency>
    <groupId>com.github.shyiko</groupId>
    <artifactId>mysql-binlog-connector-java</artifactId>
    <version>0.2.1</version>
</dependency>
```

The latest development version always available through Sonatype Snapshots repository (as shown below).

```xml
<dependencies>
    <dependency>
        <groupId>com.github.shyiko</groupId>
        <artifactId>mysql-binlog-connector-java</artifactId>
        <version>0.2.2-SNAPSHOT</version>
    </dependency>
</dependencies>

<repositories>
    <repository>
    <id>sonatype-snapshots</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
    <releases>
        <enabled>false</enabled>
    </releases>
    </repository>
</repositories>
```

### Reading Binary Log file

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

### Tapping into MySQL replication stream

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

### Using GTID Mode

```java
BinaryLogClient client = new BinaryLogClient("hostname", 3306, "username", "password");
client.registerEventListener(new EventListener() {

    @Override
    public void onEvent(Event event) {
        ...
    }
});
client.setGtid("");// Use GTID mode, start from begin
client.setGtid("0-3306-12");// MariaDB GTID format
client.setGtid("3E11FA47-71CA-11E1-9E33-C80AA9429562:1-5");// MySQL GTID format
client.connect();// Will autodetect MariaDB server
// Use client.isMariaDB() to check is MariaDB server
// Use client.getGtid() to get current GTID, this value will auto update by event.
```

### Controlling event deserialization

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

### Making client available through JMX

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

## Implementation notes

- data of numeric types (tinyint, etc) always returned signed(!) regardless of whether column definition includes "unsigned" keyword or not
- data of \*text/\*blob types always returned as a byte array

## Frequently Asked Questions

Q: How do I get column names of a table?   
A: The easiest way is to use JDBC (as described [here](https://github.com/shyiko/mysql-binlog-connector-java/issues/24#issuecomment-43747417)). Binary log itself does not contain that piece of information.

## Documentation

For the insight into the internals of MySQL look [here](https://dev.mysql.com/doc/internals/en/index.html). [MySQL Client/Server Protocol](http://dev.mysql.com/doc/internals/en/client-server-protocol.html) and [The Binary Log](http://dev.mysql.com/doc/internals/en/binary-log.html) sections are particularly useful as a reference documentation for the `com.**.mysql.binlog.network` and `com.**.mysql.binlog.event` packages.

## Development

```sh
git clone https://github.com/shyiko/mysql-binlog-connector-java.git
cd mysql-binlog-connector-java
mvn # shows how to build, test, etc. project
```

## Used by

* [rook](https://github.com/shyiko/rook) - Change Data Capture (CDC) toolkit for keeping system layers in sync with the database.
* [mydit](https://github.com/ngocdaothanh/mydit) - MySQL to MongoDB Replicator.

## Contributing

In lieu of a formal styleguide, please take care to maintain the existing coding style.  
Executing `mvn checkstyle:check` within project directory should not produce any errors.  
If you are willing to install [vagrant](http://www.vagrantup.com/) (required by integration tests) it's highly recommended
to check (with `mvn clean verify`) that there are no test failures before sending a pull request.  
Additional tests for any new or changed functionality are also very welcomed.

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
