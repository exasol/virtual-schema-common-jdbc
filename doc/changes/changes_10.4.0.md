# Virtual Schema Common JDBC 10.4.0, released 2023-??-??

Code name: Refactoring

## Summary

This release adds validators for adapter properties enabling derived SQL dialects to simply add these validator to the existing chain.
* `ImportProperty` checking the consistency of import and connection properties
* `SchemaNameProperty` checking mandatory property `SCHEMA_NAME`
* `MandatoryProperty` checking arbitrary mandatory properties

## Refactorings

#140: Created PropertyValidator for `checkImportPropertyConsistency()`

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:error-reporting-java:1.0.0` to `1.0.1`

### Test Dependency Updates

* Updated `com.exasol:java-util-logging-testing:2.0.2` to `2.0.3`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.11.1` to `3.14`
* Updated `org.junit.jupiter:junit-jupiter:5.9.1` to `5.9.2`
* Updated `org.mockito:mockito-junit-jupiter:4.9.0` to `5.1.1`
