# Virtual Schema Common JDBC 14.0.3, released 2026-??-??

Code name:

## Summary

Fix cleanup and lazy initialization of cached JDBC connections in `JDBCAdapter`.

The release also makes interface `SqlDialectFactory` and `SqlDialect` extend `AutoClosable`. This allows adapters to cleanup resources. Both interfaces provide a `default` implementation of the `close()` method, so implementors don't need to change.

## Bugfixes

* #174: Fix JDBCAdapter connection cleanup and lazy connection factory race
* #173: Fixed deserialization of column adapter notes when `typeName` is absent.

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:virtual-schema-common-java:18.0.1` to `18.0.2`

### Plugin Dependency Updates

* Updated `com.exasol:project-keeper-maven-plugin:5.5.2` to `5.6.2`
