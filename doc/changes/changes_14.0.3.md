# Virtual Schema Common JDBC 14.0.3, released 2026-??-??

Code name:

## Summary

Fix cleanup and lazy initialization of cached JDBC connections in `JDBCAdapter`.

## Bugfixes

* #174: Fix JDBCAdapter connection cleanup and lazy connection factory race
* #173: Fixed deserialization of column adapter notes when `typeName` is absent.

## Dependency Updates

### Plugin Dependency Updates

* Updated `com.exasol:project-keeper-maven-plugin:5.5.2` to `5.6.2`
