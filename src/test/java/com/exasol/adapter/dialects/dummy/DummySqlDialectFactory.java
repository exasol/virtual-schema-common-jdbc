package com.exasol.adapter.dialects.dummy;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.SqlDialectFactory;
import com.exasol.adapter.jdbc.ConnectionFactory;

public class DummySqlDialectFactory implements SqlDialectFactory {
    @Override
    public String getSqlDialectName() {
        return DummySqlDialect.NAME;
    }

    @Override
    public SqlDialect createSqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties) {
        return new DummySqlDialect(connectionFactory, properties);
    }

    @Override
    public String getSqlDialectVersion() {
        return "0.0.0";
    }
}