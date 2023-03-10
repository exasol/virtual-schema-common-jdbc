# Virtual Schema Common JDBC 10.4.0, released 2023-03-10

Code name: Validators for Adapter Properties

## Summary

This release adds validators for adapter properties enabling derived SQL dialects to simply add these validator to the existing chain.
* `ImportProperty` checking the consistency of import and connection properties
* `SchemaNameProperty` checking mandatory property `SCHEMA_NAME`
* `MandatoryProperty` checking arbitrary mandatory properties

## Refactorings

#140: Created PropertyValidator for `checkImportPropertyConsistency()`

