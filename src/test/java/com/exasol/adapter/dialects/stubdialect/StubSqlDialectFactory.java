package com.exasol.adapter.dialects.stubdialect;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.SqlDialectFactory;
import com.exasol.adapter.jdbc.ConnectionFactory;

/**
 * Factory for the test stub SQL dialect.
 */
public class StubSqlDialectFactory implements SqlDialectFactory {
    @Override
    public String getSqlDialectName() {
        return StubSqlDialect.NAME;
    }

    @Override
    public SqlDialect createSqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties) {
        return new StubSqlDialect(connectionFactory, properties);
    }

    @Override
    public String getSqlDialectVersion() {
        return "0.0.0";
    }
}