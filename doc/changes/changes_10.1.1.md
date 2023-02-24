# Virtual Schema Common JDBC 10.1.1, released 2023-??-??

Code name: More Tables

## Summary
Allowed to change the limit of 1000 mapped tables by introducing new adapter property `MAX_TABLE_COUNT`.

## Features

* #133: New adapter property `MAX_TABLE_COUNT` allows to override the default limit of 1000 tables.

## Dependency Updates

### Plugin Dependency Updates

* Updated `com.exasol:error-code-crawler-maven-plugin:1.2.1` to `1.2.2`
* Updated `com.exasol:project-keeper-maven-plugin:2.9.1` to `2.9.3`
* Updated `org.apache.maven.plugins:maven-surefire-plugin:3.0.0-M7` to `3.0.0-M8`
* Updated `org.codehaus.mojo:versions-maven-plugin:2.13.0` to `2.14.2`
