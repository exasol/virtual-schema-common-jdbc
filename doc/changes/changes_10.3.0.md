# Virtual Schema Common JDBC 10.3.0, released 2023-03-10

Code name: Escape Wildcards

## Summary

This release fixes ambiguous results by escaping SQL wildcards such as underscore `_` and percent `%` in names of catalogs, schemas, and tables when retrieving column metadata from JDBC driver.

The release also adds a constructor enabling derived SQL dialects to add additional validators for adapter properties, hence removing the need to override method `AbstractSqlDialect.validateProperties()`.

## Bugfixes

* #136: Fixed column lookup for tables is not escaping wildcards
* #138: Enabled SQL dialects to add property validators

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:error-reporting-java:1.0.0` to `1.0.1`

### Test Dependency Updates

* Updated `com.exasol:java-util-logging-testing:2.0.2` to `2.0.3`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.11.1` to `3.14`
* Updated `org.junit.jupiter:junit-jupiter:5.9.1` to `5.9.2`
* Updated `org.mockito:mockito-junit-jupiter:4.9.0` to `5.1.1`
