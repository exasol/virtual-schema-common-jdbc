# Virtual Schema Common JDBC 10.5.0, released 2023-03-15

Code name: Escape SQL Wild Cards Optionally

## Summary

Release 10.3.0 introduced escaping wild cards in the names of database schemas and tables when retrieving column metadata from JDBC.

The current release fixes two problems in this area

| Problem                                                                                                                                              | Fix                                                                                                                |
|------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| VSCJDBC also escaped wild cards in the name of the database catalog, conflicting with the parameter's documentation as literal string.               | Do not escape potential wild cards in the name of the database catalog.                                            |
| VSCJDBC always used the backslash as escape string, while there are SQL dialects with different escape string, e.g. VSORA using a forward slash `/`. | Use `java.sql.DatabaseMetaData.getSearchStringEscape()` to inquire the escape string for the specific SQL dialect. |

Additionally the current release makes wild card escaping optional. In case of problems SQL dialects then can simply override `BaseColumnMetadataReader.getColumnMetadata`:
```java
@Override
protected ResultSet getColumnMetadata(String catalogName, String schemaName, String tableName) throws SQLException {
     return getColumnMetadataAllowingPatterns(catalogName, schemaName, tableName);
}
```

## Bugfixes

* #142: Fixed escaping wildcards in column lookup and made escaping optional

## Dependency Updates

### Test Dependency Updates

* Updated `org.apache.derby:derby:10.15.2.0` to `10.16.1.1`
* Updated `org.mockito:mockito-junit-jupiter:5.1.1` to `5.2.0`

### Plugin Dependency Updates

* Updated `com.exasol:project-keeper-maven-plugin:2.9.3` to `2.9.4`
