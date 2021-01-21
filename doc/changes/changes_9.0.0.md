# Virtual Schema Common JDBC 9.0.0, released 2020-??-??

Code name: 

## Refactoring

* #64: Refactored the `AbstractRemoteMetadataReader` and `TableMetadataReader` interface
* #75: Restricted amount of mapped tables in the remote schema to 1000.
* #77: Refactored SQL generation for scalar functions.
* #79: Added error builder to the project.
* #81: Updated to the latest virtual-schema-common-java.

## Dependency Updates

* Updated `com.exasol:error-reporting-java:0.2.0` to `0.2.2`
* Updated `com.exasol:virtual-schema-common-java:13.0.0` to `14.0.0`