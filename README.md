# Virtual Schema Common Module for JDBC-based Data Access

[![Build Status](https://travis-ci.com/exasol/virtual-schema-common-jdbc.svg?branch=master)](https://travis-ci.com/exasol/virtual-schema-common-jdbc)
[![Maven Central](https://img.shields.io/maven-central/v/com.exasol/virtual-schema-common-jdbc)](https://search.maven.org/artifact/com.exasol/virtual-schema-common-jdbc)

SonarCloud results:

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Avirtual-schema-common-jdbc&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.exasol%3Avirtual-schema-common-jdbc)

[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Avirtual-schema-common-jdbc&metric=security_rating)](https://sonarcloud.io/dashboard?id=com.exasol%3Avirtual-schema-common-jdbc)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Avirtual-schema-common-jdbc&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=com.exasol%3Avirtual-schema-common-jdbc)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Avirtual-schema-common-jdbc&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=com.exasol%3Avirtual-schema-common-jdbc)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Avirtual-schema-common-jdbc&metric=sqale_index)](https://sonarcloud.io/dashboard?id=com.exasol%3Avirtual-schema-common-jdbc)

[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Avirtual-schema-common-jdbc&metric=code_smells)](https://sonarcloud.io/dashboard?id=com.exasol%3Avirtual-schema-common-jdbc)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Avirtual-schema-common-jdbc&metric=coverage)](https://sonarcloud.io/dashboard?id=com.exasol%3Avirtual-schema-common-jdbc)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Avirtual-schema-common-jdbc&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=com.exasol%3Avirtual-schema-common-jdbc)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Avirtual-schema-common-jdbc&metric=ncloc)](https://sonarcloud.io/dashboard?id=com.exasol%3Avirtual-schema-common-jdbc)

# Overview

This module contains common parts for Virtual Schema Adapters that use JDBC to access the remote data source.

## Customer Support

This is an open source project which is officially supported by Exasol. For any question, you can contact our support team.

## Information for Users

* [Changelog](doc/changes/changelog.md)

## Information for Developers

* [Virtual Schema API Documentation][vs-api]
* [Developing and Testing an SQL Dialect](doc/development/developing_a_dialect.md)
* [Step-by-step guide to writing your own SQL dialect](doc/development/step_by_step_guide_to_writing_your_own_dialect.md)

## Dependencies

### Run Time Dependencies

Running the Virtual Schema requires a Java Runtime version 11 or later.

| Dependency                                                               | Purpose                                                | License                       |
|--------------------------------------------------------------------------|--------------------------------------------------------|-------------------------------|
| [JAXB][jaxb]                                                             | Java Architecture for XML Binding                      | CDDL-1.0                      |
| [JSON-P](https://javaee.github.io/jsonp/)                                | JSON Processing                                        | CDDL-1.0                      |
| [Exasol Script API][exasol-script-api]                                   | Accessing Exasol features                              | MIT License                   |
| [Exasol Virtual Schema Common Java][exasol-virtual-schema-common-java]   | Common module of Exasol Virtual Schemas adapters       | MIT License                   |
| [Exasol Error Reporting Java][error-reporting-java]                      | Unified error messages.                                | MIT License                   |

### Test Dependencies

| Dependency                                                               | Purpose                                                | License                       |
|--------------------------------------------------------------------------|--------------------------------------------------------|-------------------------------|
| [Apache Derby](https://db.apache.org/derby/)                             | Pure-Java embedded database                            | Apache License 2.0            |
| [Apache Maven](https://maven.apache.org/)                                | Build tool                                             | Apache License 2.0            |
| [Equals Verifier](https://jqno.nl/equalsverifier/)                       | Testing `equals(...)` and `hashCode()` contracts       | Apache License 2.0            |
| [Exasol Util Logging Testng][exasol-util-logging-testing]                | Test Utilities for java.util.logging                   | MIT License                   |
| [Java Hamcrest](http://hamcrest.org/JavaHamcrest/)                       | Checking for conditions in code via matchers           | BSD License                   |
| [JSONassert](http://jsonassert.skyscreamer.org/)                         | Compare JSON documents for semantic equality           | Apache License 2.0            |
| [JUnit](https://junit.org/junit5)                                        | Unit testing framework                                 | Eclipse Public License 1.0    |
| [Mockito](http://site.mockito.org/)                                      | Mocking framework                                      | MIT License                   |

### Maven Plug-ins

| Plug-in                                                                  | Purpose                                                | License                       |
|--------------------------------------------------------------------------|--------------------------------------------------------|-------------------------------|
| [Maven Compiler Plugin][maven-compiler-plugin]                           | Setting required Java version                          | Apache License 2.0            |
| [Maven Enforcer Plugin][maven-enforcer-plugin]                           | Controlling environment constants                      | Apache License 2.0            |
| [Maven GPG Plugin](https://maven.apache.org/plugins/maven-gpg-plugin/)   | Code signing                                           | Apache License 2.0            |
| [Maven Jacoco Plugin][maven-jacoco-plugin]                               | Code coverage metering                                 | Eclipse Public License 2.0    |
| [Maven Javadoc Plugin][maven-javadoc-plugin]                             | Creating a Javadoc JAR                                 | Apache License 2.0            |
| [Maven JAR Plugin](https://maven.apache.org/plugins/maven-jar-plugin)    | Creating an additional JAR with test classes           | Apache License 2.0            |
| [Maven Source Plugin][maven-source-plugin]                               | Creating a source code JAR                             | Apache License 2.0            |
| [Maven Surefire Plugin][maven-surefire-plugin]                           | Unit testing                                           | Apache License 2.0            |
| [Sonatype OSS Index Maven Plugin][sonatype-oss-index-maven-plugin]       | Checking Dependencies Vulnerability                    | ASL2                          |
| [Versions Maven Plugin][versions-maven-plugin]                           | Checking if dependencies updates are available         | Apache License 2.0            |

[vs-api]: https://github.com/exasol/virtual-schema-common-java/blob/master/doc/development/api/virtual_schema_api.md

[exasol-script-api]: https://docs.exasol.com/database_concepts/udf_scripts.htm
[exasol-util-logging-testing]: https://github.com/exasol/java-util-logging-testing
[exasol-virtual-schema-common-java]: https://github.com/exasol/virtual-schema-common-java
[jaxb]: https://javaee.github.io/jaxb-v2/
[maven-compiler-plugin]: https://maven.apache.org/plugins/maven-compiler-plugin/
[maven-enforcer-plugin]: http://maven.apache.org/enforcer/maven-enforcer-plugin/
[maven-jacoco-plugin]: https://www.eclemma.org/jacoco/trunk/doc/maven.html
[maven-javadoc-plugin]: https://maven.apache.org/plugins/maven-javadoc-plugin/
[maven-source-plugin]: https://maven.apache.org/plugins/maven-source-plugin/
[maven-surefire-plugin]: https://maven.apache.org/surefire/maven-surefire-plugin/
[sonatype-oss-index-maven-plugin]: https://sonatype.github.io/ossindex-maven/maven-plugin/
[versions-maven-plugin]: https://www.mojohaus.org/versions-maven-plugin/
[error-reporting-java]: https://github.com/exasol/error-reporting-java/