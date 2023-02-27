# Virtual Schema Common JDBC 10.1.1, released 2023-??-??

Code name: More Tables

## Summary

Allowed to ignore or raise the limit of 1000 mapped tables that was introduced in version 9.0.0.

Additionally the property validation has been refactored which enabled to mark the following methods as deprecated:
* `JDBCAdapter.readMetadata()`
* `AbstractSqlDialect.validateSupportedPropertiesList()`
* `AbstractSqlDialect.createUnsupportedElementMessage()`
* `AbstractSqlDialect.validateBooleanProperty()`
* `AbstractSqlDialect.validateCastNumberToDecimalProperty()`

Due to this potentially change this release increments the major version number of VSCJDBC from 10 to 11.

## Features

* #133: New adapter property 'MAX_TABLE_COUNT' allows to override the default limit of 1000.

## Dependency Updates

### Plugin Dependency Updates

* Updated `com.exasol:error-code-crawler-maven-plugin:1.2.1` to `1.2.2`
* Updated `com.exasol:project-keeper-maven-plugin:2.9.1` to `2.9.3`
* Updated `org.apache.maven.plugins:maven-surefire-plugin:3.0.0-M7` to `3.0.0-M8`
* Updated `org.codehaus.mojo:versions-maven-plugin:2.13.0` to `2.14.2`
