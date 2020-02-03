package com.exasol.adapter.dialects;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.jdbc.ConnectionFactory;

/**
 * This is the common interface for all factories that produce SQL dialects.
 */
public interface SqlDialectFactory {
    /**
     * Create an instance of the SQL dialect adapter matching the dialect name.
     *
     * @param connectionFactory Factory for JDBC connection to the remote data source
     * @param properties        user-defined adapter properties
     * @return SQL dialect adapter
     */
    public SqlDialect createSqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties);

    /**
     * Get the name of the SQL dialect this factory can produce.
     *
     * @return SQL dialect name
     */
    public String getSqlDialectName();

    /**
     * Get the version of the SQL dialect.
     *
     * @return SQL dialect version
     */
    public String getSqlDialectVersion();
}