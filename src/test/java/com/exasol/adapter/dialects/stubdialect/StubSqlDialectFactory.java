package com.exasol.adapter.dialects.stubdialect;

import java.sql.Connection;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.SqlDialectFactory;

/**
 * Factory for the test stub SQL dialect.
 */
public class StubSqlDialectFactory implements SqlDialectFactory {
    @Override
    public String getSqlDialectName() {
        return StubSqlDialect.NAME;
    }

    @Override
    public SqlDialect createSqlDialect(final Connection connection, final AdapterProperties properties) {
        return new StubSqlDialect(connection, properties);
    }

    @Override
    public String getSqlDialectVersion() {
        return "0.0.0";
    }

}