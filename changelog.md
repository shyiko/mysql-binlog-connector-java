# Changelog
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased](https://github.com/shyiko/mysql-binlog-connector-java/compare/0.1.3...HEAD)
### Added
- Support for COM_BINLOG_DUMP_GTID (`BinaryLogClient`::[gtidSet](https://github.com/shyiko/mysql-binlog-connector-java/commit/3f30768791ef61ab0a83bd2bdb98af80bc799abd#diff-7addc4e3eed6e9254e088abc015ac8adR229)) ([#41](https://github.com/shyiko/mysql-binlog-connector-java/issues/41)).

### Fixed
- `EventHeaderV4Deserializer` eventLength handling ([#35](https://github.com/shyiko/mysql-binlog-connector-java/issues/35)).

## [0.1.3](https://github.com/shyiko/mysql-binlog-connector-java/compare/0.1.2...0.1.3) - 2015-03-30
### Fixed
- Deserialization of `BitSet`s in little-endian ([#34](https://github.com/shyiko/mysql-binlog-connector-java/issues/34)).

## [0.1.2](https://github.com/shyiko/mysql-binlog-connector-java/compare/0.1.1...0.1.2) - 2015-02-17
### Fixed
- TABLE_MAP/ROTATE `EventDataDeserializer`s handling ([#29](https://github.com/shyiko/mysql-binlog-connector-java/issues/29)).

## [0.1.1](https://github.com/shyiko/mysql-binlog-connector-java/compare/0.1.0...0.1.1) - 2014-12-08
### Fixed
- EXT_WRITE_ROWS event data deserialization when binlog_row_image is set to 'minimal' (default is 'full') ([#26](https://github.com/shyiko/mysql-binlog-connector-java/issues/26)).

## 0.1.0 - 2014-06-03
