# Virtual Schema Common JDBC 11.0.0, released 2023-07-06

Code name: Dependency Update on top of 10.5.0

## Summary

This release updates dependencies including [version 17.0.0](https://github.com/exasol/virtual-schema-common-java/releases/tag/17.0.0) of VSCOMJAVA which removed some adapter properties.

The major version of VSCJDBC has been incremented, too, to signal changes in the public API of VSCOMJAVA to downstream dependencies.

## Refactoring

* #145: Updated dependencies

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:virtual-schema-common-java:16.2.0` to `17.0.0`

### Test Dependency Updates

* Updated `nl.jqno.equalsverifier:equalsverifier:3.14` to `3.14.3`
* Updated `org.junit.jupiter:junit-jupiter:5.9.2` to `5.9.3`
* Updated `org.mockito:mockito-junit-jupiter:5.2.0` to `5.4.0`

### Plugin Dependency Updates

* Updated `com.exasol:error-code-crawler-maven-plugin:1.2.2` to `1.2.3`
* Updated `com.exasol:project-keeper-maven-plugin:2.9.4` to `2.9.8`
* Updated `org.apache.maven.plugins:maven-compiler-plugin:3.10.1` to `3.11.0`
* Updated `org.apache.maven.plugins:maven-deploy-plugin:3.1.0` to `3.1.1`
* Updated `org.apache.maven.plugins:maven-enforcer-plugin:3.2.1` to `3.3.0`
* Updated `org.apache.maven.plugins:maven-javadoc-plugin:3.4.1` to `3.5.0`
* Updated `org.apache.maven.plugins:maven-surefire-plugin:3.0.0-M8` to `3.0.0`
* Added `org.basepom.maven:duplicate-finder-maven-plugin:1.5.1`
* Updated `org.codehaus.mojo:flatten-maven-plugin:1.3.0` to `1.4.1`
* Updated `org.codehaus.mojo:versions-maven-plugin:2.14.2` to `2.15.0`
* Updated `org.jacoco:jacoco-maven-plugin:0.8.8` to `0.8.9`
