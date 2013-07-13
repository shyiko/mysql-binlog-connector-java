# mysql-binlog-connector-java

MySQL Binary Log connector.

Initially project was started as a fork of [open-replicator](https://code.google.com/p/open-replicator), but ended up as a complete rewrite. Key differences (features):

- automatic binlog filename/position resolution
- resumable disconnects
- plugable failover strategies
- JMX exposure (optionally with statistics)
- availability in Maven Central (deferred until everything is thoroughly tested)
- no third-party dependencies
- binlog_checksum support (for MySQL 5.6.2+ users)
- test suite over different versions of MySQL releases

## Usage

### Reading Binary Log file

    File binlogFile = ...
    BinaryLogFileReader reader = new BinaryLogFileReader(binlogFile);
    try {
        for (Event event; (event = reader.readEvent()) != null; ) {
            ...
        }
    } finally {
        reader.close();
    }

### Tapping into MySQL replication stream

    BinaryLogClient client = new BinaryLogClient("hostname", 3306, "username", "password")
    client.registerEventListener(new EventListener() {

        @Override
        public void onEvent(Event event) {
            ...
        }
    });
    client.connect();

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
