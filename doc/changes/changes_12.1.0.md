# Virtual Schema Common JDBC 12.1.0, released 2025-06-03

Code name: Support for TIMESTAMP types with fractional second precision greater than millis.

## Summary

This release adds support for mapping TIMESTAMP with fractional second precision greater than millis.
If the Virtual Schema Adapter runs on Exasol 8.32 or newer, TIMESTAMP precision up to nanoseconds can be used.

**Warning:** The API contains a breaking change. The method `createSqlDialect` in `SqlDialectFactory` now receives also
an `ExaMetadata` parameter.

## Features

* #160: Fix support for timestamp mapping with fractional seconds

## Dependency Updates

### Test Dependency Updates

* Updated `nl.jqno.equalsverifier:equalsverifier:3.19.1` to `3.19.4`
* Updated `org.junit.jupiter:junit-jupiter-params:5.12.0` to `5.13.0`
* Updated `org.mockito:mockito-junit-jupiter:5.16.0` to `5.18.0`

### Plugin Dependency Updates

* Updated `com.exasol:project-keeper-maven-plugin:4.5.0` to `5.1.0`
* Added `io.github.git-commit-id:git-commit-id-maven-plugin:9.0.1`
* Removed `io.github.zlika:reproducible-build-maven-plugin:0.17`
* Added `org.apache.maven.plugins:maven-artifact-plugin:3.6.0`
* Updated `org.apache.maven.plugins:maven-clean-plugin:3.4.0` to `3.4.1`
* Updated `org.apache.maven.plugins:maven-compiler-plugin:3.13.0` to `3.14.0`
* Updated `org.apache.maven.plugins:maven-deploy-plugin:3.1.3` to `3.1.4`
* Updated `org.apache.maven.plugins:maven-failsafe-plugin:3.5.2` to `3.5.3`
* Updated `org.apache.maven.plugins:maven-install-plugin:3.1.3` to `3.1.4`
* Updated `org.apache.maven.plugins:maven-javadoc-plugin:3.11.1` to `3.11.2`
* Updated `org.apache.maven.plugins:maven-surefire-plugin:3.5.2` to `3.5.3`
* Updated `org.codehaus.mojo:flatten-maven-plugin:1.6.0` to `1.7.0`
* Updated `org.jacoco:jacoco-maven-plugin:0.8.12` to `0.8.13`
* Updated `org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389` to `5.1.0.4751`
