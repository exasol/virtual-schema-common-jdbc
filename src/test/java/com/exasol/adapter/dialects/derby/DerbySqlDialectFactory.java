package com.exasol.adapter.dialects.derby;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.SqlDialectFactory;
import com.exasol.adapter.jdbc.ConnectionFactory;

/**
 * Factory for the test Apache Derby SQL dialect.
 */
public class DerbySqlDialectFactory implements SqlDialectFactory {
    @Override
    public String getSqlDialectName() {
        return DerbySqlDialect.NAME;
    }

    @Override
    public SqlDialect createSqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties,
                final ExaMetadata exaMetadata) {
        return new DerbySqlDialect(connectionFactory, properties, exaMetadata);
    }

    @Override
    public String getSqlDialectVersion() {
        return "0.0.0";
    }
}