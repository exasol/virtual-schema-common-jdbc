package com.exasol.adapter.dialects.derby;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.VirtualSchemaAdapter;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.jdbc.AbstractJdbcAdapter;
import com.exasol.adapter.jdbc.ConnectionFactory;

/**
 * Virtual Schema Adapter for Apache Derby.
 * <p>
 * This adapter is intended for integration tests of the JDBC Virtual Schema adapter only.
 * </p>
 */
public class DerbyAdapter extends AbstractJdbcAdapter implements VirtualSchemaAdapter {
    @Override
    protected SqlDialect createDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties) {
        return new DerbySqlDialect(connectionFactory, properties);
    }
}