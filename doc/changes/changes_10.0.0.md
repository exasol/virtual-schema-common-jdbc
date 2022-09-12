# Virtual Schema Common JDBC 10.0.0, released 2022-??-??

Code name: Read Expected Datatypes From Request

## Summary

Starting with major version 8 Exasol database uses the capabilities reported by each virtual schema to provide select list data types for each push down request. Based on this information the JDBC virtual schemas no longer need to infer the data types of the result set by inspecting its values. Instead the JDBC virtual schemas can now use the information provided by the database.

## Features

* #120: Supported using the push down request to specify the datatypes of result set.

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:virtual-schema-common-java:15.3.2` to `16.0.0`
