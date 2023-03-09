# Virtual Schema Common JDBC 10.3.0, released 2023-??-??

Code name: Escape Wildcards

## Summary

This release fixes lookup for tables with wildcards underscore `_` and percent `%`.

Even when putting double quotes around the table name older releases of VSCJDBC passed the name to the JDBC driver and the JDBC driver returned the metadata for all matching tables expanding potential wildcards.

The current release fixes this by escaping the wildcards before passing the table name to the JDBC driver.

## Bugfixes

* #136: Column lookup for tables is not escaping wildcards

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:error-reporting-java:1.0.0` to `1.0.1`

### Test Dependency Updates

* Updated `com.exasol:java-util-logging-testing:2.0.2` to `2.0.3`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.11.1` to `3.14`
* Updated `org.apache.derby:derby:10.15.2.0` to `10.16.1.1`
* Updated `org.junit.jupiter:junit-jupiter:5.9.1` to `5.9.2`
* Updated `org.mockito:mockito-junit-jupiter:4.9.0` to `5.1.1`
