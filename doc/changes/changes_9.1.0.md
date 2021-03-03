# Virtual Schema Common JDBC 9.1.0, released 2021-03-??

Code name: Added abstract scalar function integration tests

## Features

#101: Moved the abstract scalar function integration tests from postgresql-virtual-schema to this repository.

## Bug Fixes

* #99: Moved to this repository the double and exactnumeric literals formatting from `virtual-schema-common-java`(https://github.com/exasol/virtual-schema-common-java)

## Dependency Updates

### Test Dependencies

* Updated `com.exasol:virtual-schema-common-java:15.0.0` to `15.0.1`
* Added `com.exasol:hamcrest-resultset-matcher:1.4.0`
* Added `org.yaml:snakeyaml:1.27`