package com.exasol.adapter.dialects.derby;

import java.util.ServiceLoader;

import com.exasol.adapter.VirtualSchemaAdapter;
import com.exasol.adapter.jdbc.AbstractJdbcAdapterFactory;

/**
 * Factory of the Derby Virtual Schema adapter.
 * <p>
 * Note that classes like this must be registered in a resource file called
 * <code>META-INF/services/com.exasol.adapter.AdapterFactory</code> in order for the {@link ServiceLoader} to find it.
 * </p>
 */
public class DerbyAdapterFactory extends AbstractJdbcAdapterFactory {
    @Override
    public VirtualSchemaAdapter createAdapter() {
        return new DerbyAdapter();
    }

    @Override
    protected String getSqlDialectName() {
        return DerbySqlDialect.NAME;
    }

    @Override
    protected String getSqlDialectVersion() {
        return "0.0.0"; // Bogus version for the Derby SQL dialec. Remember that this adapter is for testing only.
    }
}