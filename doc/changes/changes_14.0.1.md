# Virtual Schema Common JDBC 14.0.1, released 2026-04-22

Code name: Fix TELEMETRY property

## Summary

This release fixes a bug that caused validation of the `TELEMETRY` property to fail with error message

```
E-VSCJDBC-13: This dialect does not support property 'TELEMETRY'. Please, do not set this property.
```

## Bugfixes

* #169: Fix validation of `TELEMETRY` property
