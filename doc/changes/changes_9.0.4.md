# Virtual Schema Common JDBC 9.0.4, released 2021-11-10

Code name: Design model and dependency updates

## Summary

Version 9.0.4 of `virtual-schema-common-jdbc` updates the UML design diagrams and updates dependencies.

## Refactorings

* #73: Refactored table metadata reading with filtered tables.
* #109: Switch the build to GitHub actions.
* #112: Updated dependencies, prepared for a release.

## Features

* #91: Added high level design document

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:virtual-schema-common-java:15.1.0` to `15.3.0`

### Test Dependency Updates

* Updated `nl.jqno.equalsverifier:equalsverifier:3.6.1` to `3.7.2`
* Updated `org.junit.jupiter:junit-jupiter:5.7.2` to `5.8.1`
* Updated `org.mockito:mockito-junit-jupiter:3.10.0` to `4.0.0`

### Plugin Dependency Updates

* Updated `com.exasol:error-code-crawler-maven-plugin:0.2.0` to `0.7.1`
* Updated `com.exasol:project-keeper-maven-plugin:0.7.0` to `1.3.0`
* Updated `org.apache.maven.plugins:maven-gpg-plugin:1.6` to `3.0.1`
* Updated `org.apache.maven.plugins:maven-javadoc-plugin:3.2.0` to `3.3.1`
* Updated `org.jacoco:jacoco-maven-plugin:0.8.6` to `0.8.7`
