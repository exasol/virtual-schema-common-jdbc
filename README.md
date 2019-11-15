# Virtual Schema Common Module for JDBC-based Data Access

[![Build Status](https://travis-ci.com/exasol/virtual-schema-common-jdbc.svg?branch=master)](https://travis-ci.com/exasol/virtual-schema-common-jdbc)

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

## Information for Developers 

Please refer to the [documentation in the main Virtual Schema project](https://github.com/exasol/virtual-schemas/blob/master/README.md).

## Dependencies

### Run Time Dependencies

Running the Virtual Schema requires a Java Runtime version 11 or later.

| Dependency                                                                          | Purpose                                                | License                       |
|-------------------------------------------------------------------------------------|--------------------------------------------------------|-------------------------------|
| [JSON-P](https://javaee.github.io/jsonp/)                                           | JSON Processing                                        | CDDL-1.0                      |
| [Exasol Script API](https://docs.exasol.com/database_concepts/udf_scripts.htm)      | Accessing Exasol features                              | MIT License                   |
| JDBC driver(s), depending on data source                                            | Connecting to the data source                          | Check driver documentation    |

### Test Dependencies

| Dependency                                                                          | Purpose                                                | License                       |
|-------------------------------------------------------------------------------------|--------------------------------------------------------|-------------------------------|
| [Apache Derby](https://db.apache.org/derby/)                                        | Pure-Java embedded database                            | Apache License 2.0            |
| [Apache HTTP Components](http://hc.apache.org/)                                     | HTTP communication                                     | Apache License 2.0            |
| [Apache Maven](https://maven.apache.org/)                                           | Build tool                                             | Apache License 2.0            |
| [Equals Verifier](https://jqno.nl/equalsverifier/)                                  | Testing `equals(...)` and `hashCode()` contracts       | Apache License 2.0            |
| [Exasol Virtual Schema Common](https://github.com/exasol/virtual-schema-common-java)| Common module of Exasol Virtual Schemas adapters       | MIT License                   |
| [Exec Maven Plugin](https://www.mojohaus.org/exec-maven-plugin/)                    | Helps execute system and Java programs.                | Apache License 2.0            |
| [Java Hamcrest](http://hamcrest.org/JavaHamcrest/)                                  | Checking for conditions in code via matchers           | BSD License                   |
| [JSONassert](http://jsonassert.skyscreamer.org/)                                    | Compare JSON documents for semantic equality           | Apache License 2.0            |
| [JUnit](https://junit.org/junit5)                                                   | Unit testing framework                                 | Eclipse Public License 1.0    |
| [Mockito](http://site.mockito.org/)                                                 | Mocking framework                                      | MIT License                   |
| [SnakeYaml](https://bitbucket.org/asomov/snakeyaml/src/default/)                    | YAML parsing                                           | Apache License 2.0            |

### Maven Plug-ins

| Plug-in                                                                             | Purpose                                                | License                       |
|-------------------------------------------------------------------------------------|--------------------------------------------------------|-------------------------------|
| [Maven Compiler Plugin](https://maven.apache.org/plugins/maven-compiler-plugin/)    | Setting required Java version                          | Apache License 2.0            |
| [Maven GPG Plugin](https://maven.apache.org/plugins/maven-gpg-plugin/)              | Code signing                                           | Apache License 2.0            |
| [Maven Javadoc Plugin](https://maven.apache.org/plugins/maven-javadoc-plugin/)      | Creating a Javadoc JAR                                 | Apache License 2.0            |
| [Maven Jacoco Plugin](https://www.eclemma.org/jacoco/trunk/doc/maven.html)          | Code coverage metering                                 | Eclipse Public License 2.0    |
| [Maven JAR Plugin](https://maven.apache.org/plugins/maven-jar-plugin)               | Creating an additional JAR with test classes           | Apache License 2.0            |
| [Maven Source Plugin](https://maven.apache.org/plugins/maven-source-plugin/)        | Creating a source code JAR                             | Apache License 2.0            |
| [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)   | Unit testing                                           | Apache License 2.0            |
