# Virtual Schema Common JDBC 14.0.4, released 2026-06-08

Code name: Property Validation Regression Fix

## Summary

This release fixes a regression where `ALTER VIRTUAL SCHEMA ... SET` could skip validation for property changes that did not require refreshing remote metadata.

## Bugfixes

* #192: Fixed `JDBCAdapter.setProperties()` to validate merged adapter properties for all property changes, even when the change does not refresh schema metadata.

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:virtual-schema-common-java:18.0.2` to `18.0.3`
