package com.exasol.adapter.dialects;

import java.util.ServiceLoader;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.jdbc.ConnectionFactory;

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
     * @param connectionFactory factory that allows creating a connection to the remote data source
     * @param properties        adapter properties
     * @return new {@link SqlDialect} instance
     */
    public SqlDialect createSqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties);

    /**
     * Get the name of the SQL dialect this factory creates.
     *
     * @return SQL dialect name
     */
    public String getSqlDialectName();

    /**
     * Get the version of the SQL dialect this factory creates.
     *
     * @return SQL dialect version
     */
    public String getSqlDialectVersion();
}
