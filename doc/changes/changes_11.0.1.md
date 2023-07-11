# Virtual Schema Common JDBC 11.0.1, released 2023-07-11

Code name: Fix Issue With Integer Constants in `GROUP BY`

## Summary

This release fixes an issue with queries using `DISTINCT` with integer constants. The Exasol SQL processor turns `DISTINCT <integer>` into `GROUP BY <integer>` before push-down as an optimization. The adapter must not feed this back as Exasol interprets integers in `GROUP BY` clauses as column numbers which could lead to invalid results or the following error:

```
42000:Wrong column number. Too small value 0 as select list column reference in GROUP BY (smallest possible value is 1)
```

To fix this, VSCJDBC now replaces integer constants in `GROUP BY` clauses with a constant string.

Please that you can still safely use `GROUP BY <column-number>` in your original query, since Exasol internally converts this to `GROUP BY "<column-name>"`, so that the virtual schema adapter can tell both situations apart.

## Bugfixes

* #149: Fixed issue with integer constants in `GROUP BY`

## Documentation

* #147: Removed duplicate documentation about logging

## Dependency Updates

### Plugin Dependency Updates

* Updated `com.exasol:error-code-crawler-maven-plugin:1.2.3` to `1.3.0`
* Updated `com.exasol:project-keeper-maven-plugin:2.9.8` to `2.9.9`
* Updated `org.apache.maven.plugins:maven-clean-plugin:3.1.0` to `2.5`
* Updated `org.apache.maven.plugins:maven-gpg-plugin:3.0.1` to `3.1.0`
* Updated `org.apache.maven.plugins:maven-install-plugin:2.5.2` to `2.4`
* Updated `org.apache.maven.plugins:maven-resources-plugin:3.2.0` to `2.6`
* Updated `org.apache.maven.plugins:maven-site-plugin:3.9.1` to `3.3`
* Updated `org.apache.maven.plugins:maven-surefire-plugin:3.0.0` to `3.1.2`
* Updated `org.basepom.maven:duplicate-finder-maven-plugin:1.5.1` to `2.0.1`
* Updated `org.codehaus.mojo:flatten-maven-plugin:1.4.1` to `1.5.0`
* Updated `org.codehaus.mojo:versions-maven-plugin:2.15.0` to `2.16.0`
* Updated `org.jacoco:jacoco-maven-plugin:0.8.9` to `0.8.10`
