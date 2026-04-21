package com.exasol.adapter.dialects;

import java.util.ServiceLoader;

/**
 * This is the common interface for all factories that produce SQL dialects. *
 * <p>
 * Note that this classes derived from this class must be registered in a resource file called
 * <code>META-INF/services/com.exasol.adapter.dialects.SqlDialectFactory</code> in order for the {@link ServiceLoader}
 * to find it.
 * </p>
 */
public interface SqlDialectFactory {
    /**
     * Create a new {@link SqlDialect}.
     *
     * @param context the context for the SQL dialect
     * @return new {@link SqlDialect} instance
     */
    SqlDialect createSqlDialect(JDBCAdapterContext context);

    /**
     * Get the name of the SQL dialect this factory creates, e.g. {@code MYSQL}, {@code POSTGRESQL} or {@code EXASOL}.
     *
     * @return SQL dialect name
     */
    String getSqlDialectName();

    /**
     * Get a short tag for the adapter project. This will be used for telemetry to identify products.
     * <p>
     * Please make sure that this is the same short tag as in file {@code error_code_config.yml} of each adapter project.
     * <p>
     * Example values: {@code VSMYSQL}, {@code VSPG} (Postgres VS), {@code VSEXA}, {@code VSDY} (DynamoDB VS), {@code VSS3}, {@code VSADLG2}
     * 
     * @return short tag for the adapter project
     */
    String getAdapterProjectShortTag();

    /**
     * Get the version of the {@link SqlDialect}. This version will be used for logging and telemetry.
     * <p>
     * Adapters can use {@link com.exasol.logging.VersionCollector} to fetch the version from the metadata in the jar file. For example:
     * 
     * <pre>
     * new VersionCollector("META-INF/maven/com.exasol/mysql-virtual-schema/pom.properties").getVersionNumber()
     * </pre>
     *
     * @return Virtual Schema Adapter version
     */
    String getSqlDialectVersion();
}
