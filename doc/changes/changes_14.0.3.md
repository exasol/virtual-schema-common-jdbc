# Virtual Schema Common JDBC 14.0.3, released 2026-??-??

Code name:

## Summary

Fix cleanup and lazy initialization of cached JDBC connections in `JDBCAdapter`.

The release also improves the error message in case of invalid capability names. The user now gets a helpful message with available capability names.

The release also makes interfaces `SqlDialectFactory`, `SqlDialect` and `ConnectionFactory` extend `AutoClosable`. This allows adapters to cleanup resources. Both interfaces provide a `default` implementation of the `close()` method, so implementors don't need to change.

## Bugfixes

* #178: Fixed `SqlGenerationHelper.selectListRequiresCasts()` to stop scanning columns after the first cast requirement was found.
* #174: Fix JDBCAdapter connection cleanup and lazy connection factory race
* #176: Harden Kerberos configuration file generation by escaping JAAS principals, rejecting line breaks and using direct Base64 string decoding. The current JVM-wide Kerberos system property limitation is now documented explicitly.
* #182: Fixed dependency on mutating `subtractCapabilities()` method
* #163: Fixed `JDBCAdapter.pushdown()` clearing the connection cache after successful requests.
* #173: Fixed deserialization of column adapter notes when `typeName` is absent.
* #177: Fixed JDBC metadata mapping for invalid and oversized character types.

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:virtual-schema-common-java:18.0.1` to `18.0.2`

### Test Dependency Updates

* Added `org.itsallcode:hamcrest-auto-matcher:0.8.3`
* Updated `org.junit.jupiter:junit-jupiter-params:5.14.3` to `5.14.4`

### Plugin Dependency Updates

* Updated `com.exasol:project-keeper-maven-plugin:5.5.2` to `5.6.2`
