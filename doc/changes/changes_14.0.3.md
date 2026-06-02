# Virtual Schema Common JDBC 14.0.3, released 2026-??-??

Code name:

## Summary

## Bugfixes

* #163: Fixed `JDBCAdapter.pushdown()` clearing the connection cache after successful requests.
* #173: Fixed deserialization of column adapter notes when `typeName` is absent.
* #175: Fixed `IMPORT_DATA_TYPES` strategy resolution for deprecated `FROM_RESULT_SET` values.

## Dependency Updates

### Plugin Dependency Updates

* Updated `com.exasol:project-keeper-maven-plugin:5.5.2` to `5.6.2`
