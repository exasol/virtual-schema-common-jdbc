package com.exasol.adapter.dialects.derby;

import com.exasol.adapter.dialects.*;

/**
 * Factory for the test Apache Derby SQL dialect.
 */
public class DerbySqlDialectFactory implements SqlDialectFactory {
    private static boolean closed;

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

    @Override
    public void close() {
        closed = true;
    }

    public static void resetClosedFlag() {
        closed = false;
    }

    public static boolean wasClosed() {
        return closed;
    }
}
