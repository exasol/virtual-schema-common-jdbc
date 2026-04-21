package com.exasol.adapter.dialects.derby;

import com.exasol.adapter.dialects.*;

/**
 * Factory for the test Apache Derby SQL dialect.
 */
public class DerbySqlDialectFactory implements SqlDialectFactory {
    @Override
    public String getSqlDialectName() {
        return DerbySqlDialect.NAME;
    }

    @Override
    public SqlDialect createSqlDialect(final JDBCAdapterContext context) {
        return new DerbySqlDialect(context);
    }

    @Override
    public String getAdapterProjectShortTag() {
        return "VSDERBY";
    }

    @Override
    public String getSqlDialectVersion() {
        return "0.0.0";
    }
}
