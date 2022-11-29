# Virtual Schema Common JDBC 10.1.0, released 2022-11-30

Code name: Adapter Property to Configure Data Type Detection for Import Statement

## Summary

Version 10.0.0 introduced enhanced detection for data types of result sets.

Unfortunately the new algorithm shows problems in scenarios with
* data type `CHAR` or `VARCHAR`
* 8-bit character sets with encodings like `latin1` or `ISO-8859-1`
* characters being not strictly ASCII, e.g. German umlaut "Ü"

The current release therefore introduces an additional adapter property `IMPORT_DATA_TYPES` to configure the data type detection. For details please see the User Guide.

## Features

* #130: Enabled configuration of Data Type Detection for `IMPORT` statement.

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:error-reporting-java:0.4.1` to `1.0.0`
* Updated `com.exasol:virtual-schema-common-java:16.1.1` to `16.1.2`

### Test Dependency Updates

* Updated `com.exasol:java-util-logging-testing:2.0.1` to `2.0.2`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.10` to `3.11.1`
* Updated `org.junit.jupiter:junit-jupiter:5.8.2` to `5.9.1`
* Updated `org.mockito:mockito-junit-jupiter:4.6.1` to `4.9.0`
* Updated `org.skyscreamer:jsonassert:1.5.0` to `1.5.1`

### Plugin Dependency Updates

* Updated `com.exasol:error-code-crawler-maven-plugin:1.1.2` to `1.2.1`
* Updated `com.exasol:project-keeper-maven-plugin:2.7.0` to `2.9.1`
* Updated `io.github.zlika:reproducible-build-maven-plugin:0.15` to `0.16`
* Updated `org.apache.maven.plugins:maven-deploy-plugin:3.0.0-M1` to `3.0.0`
* Updated `org.apache.maven.plugins:maven-javadoc-plugin:3.4.0` to `3.4.1`
* Updated `org.apache.maven.plugins:maven-surefire-plugin:3.0.0-M5` to `3.0.0-M7`
* Updated `org.codehaus.mojo:flatten-maven-plugin:1.2.7` to `1.3.0`
* Updated `org.codehaus.mojo:versions-maven-plugin:2.10.0` to `2.13.0`
