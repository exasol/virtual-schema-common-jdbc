# Virtual Schema Common JDBC 9.0.0, released 2020-??-??

Code name:

In release 9.0.0, we optimized adapter loading by limiting the number of SQL dialects to be loaded to one.

## Information for Developers

Since we limited the number of SQL dialects to be loaded to one, please note the following breaking changes in
design and interface:

* Removed `SqlDialectRegistry`.
* Removed `SqlDialectRegistryException`.
* Removed `SqlDialectFactoryException`.

## Refactoring

* #64: Refactored the `AbstractRemoteMetadataReader` and `TableMetadataReader` interface
* #75: Restricted the amount of mapped tables in the remote schema to 1000.
* #77: Refactored SQL generation for scalar functions.
* #79: Added error builder to the project.
* #81: Updated to the latest virtual-schema-common-java.
* #82: Migrated common tools for implementing a new dialect from the `virtual-schemas` repository.
* #86: Remove dialect-level plug-in mechanism.
* #88: Limit number of SQL dialects to one at loading time.

## Dependency Updates

* Updated `com.exasol:error-reporting-java:0.2.0` to `0.2.2`
* Updated `com.exasol:virtual-schema-common-java:13.0.0` to `14.0.0`
