# Virtual Schema Common JDBC 9.0.0, released 2020-??-??

Code name: Single-stage loading

In release 9.0.0, we optimized adapter loading by reducing the number of plug-in loading stages from two to one.

## Information for Developers

Since we removed the extra plug-in loading step for the SQL dialects, please note the following breaking changes in
design and interface:

* Removed `SqlDialectRegistry`.
* Replaced `JdbcAdapter` by an `AbstractJdbcAdapter` that a adapter for the dialect must now extend.
* Replaced `JdbcAdapterFactory` by an `AbstractJdbcAdapterFactory` that a dialect-specific adapter factory must extend.
* SQL Dialect factories are no longer needed.
* Dialect implementations must register an adapter factory in their service description now instead of a dialect factory
  in the resources under `META-INF/services/com.exasol.adapter.AdapterFactory`.
* Dialect implementations must remove the registration for the dialect factor under
  `META-INF/services/com.exasol.adapter.dialects.SqlDialectFactory`.
* Removed `SqlDialectRegistryException`.
* Removed `SqlDialectFactoryException`.

## Refactoring

* #64: Refactored the `AbstractRemoteMetadataReader` and `TableMetadataReader` interface
* #75: Restricted amount of mapped tables in the remote schema to 1000.
* #77: Refactored SQL generation for scalar functions.
* #79: Added error builder to the project.
* #81: Updated to the latest virtual-schema-common-java.

## Dependency Updates

* Updated `com.exasol:error-reporting-java:0.2.0` to `0.2.2`
* Updated `com.exasol:virtual-schema-common-java:13.0.0` to `14.0.0`