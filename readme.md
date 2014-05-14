# mysql-binlog-connector-java [![Build Status](https://travis-ci.org/shyiko/mysql-binlog-connector-java.png?branch=master)](https://travis-ci.org/shyiko/mysql-binlog-connector-java) [![Coverage Status](https://coveralls.io/repos/shyiko/mysql-binlog-connector-java/badge.png?branch=master)](https://coveralls.io/r/shyiko/mysql-binlog-connector-java?branch=master)

MySQL Binary Log connector.

Initially project was started as a fork of [open-replicator](https://code.google.com/p/open-replicator), but ended up as a complete rewrite. Key differences/features:

- automatic binlog filename/position resolution
- resumable disconnects
- plugable failover strategies
- JMX exposure (optionally with statistics)
- availability in Maven Central (deferred until everything is thoroughly tested)
- no third-party dependencies
- binlog_checksum support (for MySQL 5.6.2+ users)
- test suite over different versions of MySQL releases

## Usage

The latest development version always available through Sonatype Snapshots repository (see example below).

```xml
<dependencies>
    <dependency>
        <groupId>com.github.shyiko</groupId>
        <artifactId>mysql-binlog-connector-java</artifactId>
        <version>0.1.0-SNAPSHOT</version>
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

// start replication from oldest binlog
// remove these lines to start from the current master binlog position
client.setBinlogFilename("");
client.setBinlogPosition(4);

client.connect();
```

### Making client available through JMX

```java
MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

BinaryLogClient binaryLogClient = ...
ObjectName objectName = new ObjectName("mysql.binlog:type=BinaryLogClient");
mBeanServer.registerMBean(binaryLogClient, objectName);

// following bean accumulates various BinaryLogClient stats (e.g. number of disconnects, skipped events)
BinaryLogClientStatistics stats = new BinaryLogClientStatistics(binaryLogClient);
ObjectName statsObjectName = new ObjectName("mysql.binlog:type=BinaryLogClientStatistics");
mBeanServer.registerMBean(stats, statsObjectName);
```

## Implementation notes

- data of numeric types (tinyint, etc) always returned signed(!) regardless of whether column definition includes "unsigned" keyword or not
- data of \*text/\*blob types always returned as a byte array
- timestamp is timezone-sensitive

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

## Contributing

In lieu of a formal styleguide, please take care to maintain the existing coding style.  
Executing `mvn checkstyle:check` within project directory should not produce any errors.  
If you are willing to install [vagrant](http://www.vagrantup.com/) (required by integration tests) it's highly recommended
to check (with `mvn clean verify`) that there are no test failures before sending a pull request.  
Additional tests for any new or changed functionality are also very welcomed.

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
