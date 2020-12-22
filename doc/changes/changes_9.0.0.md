# Virtual Schema Common JDBC 9.0.0, released 2020-??-??

Code name:

## Refactoring

* #64: Refactored the `AbstractRemoteMetadataReader` and `TableMetadataReader` interface
* #75: Restricted amount of mapped tables in the remote schema to 1000.
* #77: Refactored SQL generation for scalar functions.
* #79: Added error builder to the project.

## Dependency Updates

* Updated `com.exasol:error-reporting-java:0.2.0` to `0.2.2`