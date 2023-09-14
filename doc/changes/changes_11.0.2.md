# Virtual Schema Common JDBC 11.0.2, released 2023-09-15

Code name: fix in jdbc connection management.

## Summary

Connection factory is reused (might improve performance on high-latency DBs). 
Close connections after the end of request.

## Features

* #151: Ensured all connections are closed on failure, fixed connection factory creation

## Dependency Updates

### Plugin Dependency Updates

* Updated `com.exasol:project-keeper-maven-plugin:2.9.9` to `2.9.11`
* Updated `org.apache.maven.plugins:maven-enforcer-plugin:3.3.0` to `3.4.0`
