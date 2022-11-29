# Virtual Schema Common Module for JDBC-based Data Access

[![Build Status](https://github.com/exasol/virtual-schema-common-jdbc/actions/workflows/ci-build.yml/badge.svg)](https://github.com/exasol/virtual-schema-common-jdbc/actions/workflows/ci-build.yml)
[![Maven Central – Virtual Schema Common JDBC](https://img.shields.io/maven-central/v/com.exasol/virtual-schema-common-jdbc)](https://search.maven.org/artifact/com.exasol/virtual-schema-common-jdbc)

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

This module is part of a larger project called [Virtual Schemas](https://github.com/exasol/virtual-schemas) covering JDBC based dialects as well as others, see complete list of [dialects](https://github.com/exasol/virtual-schemas/blob/main/doc/user-guide/dialects.md).


## Customer Support

This is an open source project which is officially supported by Exasol. For any question, you can contact our support team.

## Information for Users

* [Changelog](doc/changes/changelog.md)
* [Dependencies](dependencies.md)

### Adapter Properties for JDBC-Based Virtual Schemas

Besides the properties for all Virtual Schemas there is a property specific to JDBC-based Virtual Schemas.

#### Property `IMPORT_DATA_TYPES`

Supported values:

| Value | Description |
|-------|-------------|
| `EXASOL_CALCULATED` (default) | Use data types calculated by Exasol database from the query and connection metadata. |
| `FROM_RESULT_SET` | Infer data types from values of the result set. |

The algorithm behind `EXASOL_CALCULATED` was introduced with VSCJDBC version 10.0.0 and is only available with Exasol 7.1.14, Exasol 8.6.0 and above.

Unfortunately the new algorithm shows problems in scenarios with     
* data type `CHAR` or `VARCHAR`
* 8-bit character sets with encodings like `latin1` or `ISO-8859-1`
* characters being not strictly ASCII, e.g. German umlaut "Ü"

In such scenarios using property `IMPORT_DATA_TYPES` you can deactivate the new algorithm. VSCJDBC will then infer encoding UTF-8 from the data values in the result set which allows Exasol database to accept these values.

Here is an example:

```sql
CREATE VIRTUAL SCHEMA <virtual schema name>
    USING SCHEMA_FOR_VS_SCRIPT.<adapter>
    WITH CONNECTION_NAME = '<connection>'
    IMPORT_DATA_TYPES = 'FROM_RESULT_SET' ;
```

## Information for Developers

* [Virtual Schema API Documentation][vs-api]
* [Developing and Testing an SQL Dialect](doc/development/developing_a_dialect.md)
* [Step-by-step guide to writing your own SQL dialect](doc/development/step_by_step_guide_to_writing_your_own_dialect.md)

[vs-api]: https://github.com/exasol/virtual-schema-common-java/blob/main/doc/development/api/virtual_schema_api.md