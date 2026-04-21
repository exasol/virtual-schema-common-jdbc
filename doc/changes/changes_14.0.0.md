# Virtual Schema Common JDBC 14.0.0, released 2026-04-21

Code name: Anonymous feature tracking

## Summary

This release adds anonymous feature tracking using the [telemetry-java](https://github.com/exasol/telemetry-java) library. When you integrate this new version into another product, please observe the [required user documentation](https://github.com/exasol/telemetry-java/blob/main/doc/integration-guide.md#required-documentation).

## Breaking Changes

* The release removes methods that were deprecated in [10.2.0](changes_10.2.0.md) and [10.4.0](changes_10.4.0.md).
* Interface `SqlDialectFactory` was modified and implementors need to adapt the following:
  * Method `createSqlDialect()` now receives a single argument of type `JDBCAdapterContext`. The existing three arguments of type `ConnectionFactory`, `AdapterProperties` and `ExaMetadata` are now available via getters from `JDBCAdapterContext`. This class also gives you access to the `TelemetryClient`. You can use it to send adapter specific feature tracking.
  * New method `getAdapterProjectShortTag()` must return the adapter's project short tag as defined in `error_code_config.yml`, e.g. `VSMYSQL` or `VSS3`.

## Features

* #167: Add anonymous feature tracking

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:error-reporting-java:1.0.1` to `1.0.2`
* Updated `com.exasol:virtual-schema-common-java:17.1.2` to `18.0.0`

### Test Dependency Updates

* Updated `com.exasol:java-util-logging-testing:2.0.3` to `2.0.4`
* Updated `org.junit.jupiter:junit-jupiter-params:5.13.0` to `5.14.3`
* Updated `org.mockito:mockito-junit-jupiter:5.18.0` to `5.23.0`

### Plugin Dependency Updates

* Updated `com.exasol:error-code-crawler-maven-plugin:2.0.5` to `2.0.6`
* Updated `com.exasol:project-keeper-maven-plugin:5.4.3` to `5.4.6`
* Updated `org.apache.maven.plugins:maven-compiler-plugin:3.14.1` to `3.15.0`
* Updated `org.apache.maven.plugins:maven-jar-plugin:3.4.2` to `3.5.0`
* Updated `org.apache.maven.plugins:maven-resources-plugin:3.3.1` to `3.4.0`
* Updated `org.apache.maven.plugins:maven-source-plugin:3.2.1` to `3.4.0`
* Updated `org.codehaus.mojo:versions-maven-plugin:2.19.1` to `2.21.0`
* Updated `org.sonarsource.scanner.maven:sonar-maven-plugin:5.2.0.4988` to `5.5.0.6356`
* Updated `org.sonatype.central:central-publishing-maven-plugin:0.9.0` to `0.10.0`
