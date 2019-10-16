package com.exasol.adapter.dialects.derby;

import java.sql.Connection;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.SqlDialectFactory;

/**
 * Factory for the test Apache Derby SQL dialect.
 */
public class DerbySqlDialectFactory implements SqlDialectFactory {
    @Override
    public String getSqlDialectName() {
        return DerbySqlDialect.NAME;
    }

    @Override
    public SqlDialect createSqlDialect(final Connection connection, final AdapterProperties properties) {
        return new DerbySqlDialect(connection, properties);
    }
}