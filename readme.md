# mysql-binlog-connector-java

MySQL 5.1.18+ Binary Log connector.

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
