# Virtual Schema Common JDBC 10.0.0, released 2022-09-13

Code name: Read Expected Datatypes From Request

## Summary

Starting with major version 8 Exasol database uses the capabilities reported by each virtual schema to provide select list data types for each push down request. Based on this information the JDBC virtual schemas no longer need to infer the data types of the result set by inspecting its values. Instead the JDBC virtual schemas can now use the information provided by the database.

## Features

* #120: Using the push down request to specify the data types of result set when available (8.x.x and later).

## Bug Fixes

* #119: Changed SQL generator to format decimals always with decimal point `.` independent of the locale.<br />
Before method `SqlGenerationVisitor.visit(SqlLiteralDouble)` for instance returned values with decimal point `,` when the locale was set to `en_DE`.

## Refactoring

* #117 Removed unnecessary dependency

## Dependency Updates

### Compile Dependency Updates

* Removed `com.exasol:java-util-logging-testing:2.0.1`
* Updated `com.exasol:virtual-schema-common-java:15.3.2` to `16.1.0`
* Removed `javax.xml.bind:jaxb-api:2.4.0-b180830.0359`

### Test Dependency Updates

* Added `com.exasol:java-util-logging-testing:2.0.1`

### Plugin Dependency Updates

* Updated `com.exasol:error-code-crawler-maven-plugin:1.1.1` to `1.1.2`
* Updated `com.exasol:project-keeper-maven-plugin:2.4.6` to `2.7.0`
* Updated `org.apache.maven.plugins:maven-enforcer-plugin:3.0.0` to `3.1.0`
