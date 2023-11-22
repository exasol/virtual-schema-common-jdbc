# Virtual Schema Common JDBC 11.0.3, released 2023-??-??

Code name:

## Summary

**Note:** This release excludes vulnerability CVE-2022-46337 in test dependency `org.apache.derby:derby:jar:10.14.2.0`. Newer versions don’t support Java 8 any more.

## Security

* #154: Updated dependencies

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:virtual-schema-common-java:17.0.0` to `17.0.1`

### Test Dependency Updates

* Updated `nl.jqno.equalsverifier:equalsverifier:3.14.3` to `3.15.3`
* Updated `org.junit.jupiter:junit-jupiter:5.9.3` to `5.10.1`
* Updated `org.mockito:mockito-junit-jupiter:5.4.0` to `5.7.0`

### Plugin Dependency Updates

* Updated `com.exasol:error-code-crawler-maven-plugin:1.3.0` to `1.3.1`
* Updated `com.exasol:project-keeper-maven-plugin:2.9.11` to `2.9.16`
* Updated `org.apache.maven.plugins:maven-enforcer-plugin:3.4.0` to `3.4.1`
* Added `org.apache.maven.plugins:maven-failsafe-plugin:3.2.2`
* Updated `org.apache.maven.plugins:maven-javadoc-plugin:3.5.0` to `3.6.2`
* Updated `org.apache.maven.plugins:maven-surefire-plugin:3.1.2` to `3.2.2`
* Updated `org.codehaus.mojo:versions-maven-plugin:2.16.0` to `2.16.1`
* Updated `org.jacoco:jacoco-maven-plugin:0.8.10` to `0.8.11`
* Updated `org.sonarsource.scanner.maven:sonar-maven-plugin:3.9.1.2184` to `3.10.0.2594`
