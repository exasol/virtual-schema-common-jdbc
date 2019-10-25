package com.exasol.adapter.jdbc;

import java.util.ServiceLoader;
import java.util.Set;

import com.exasol.adapter.AdapterFactory;
import com.exasol.adapter.VirtualSchemaAdapter;
import com.exasol.adapter.dialects.SqlDialectRegistry;
import com.exasol.logging.VersionCollector;

/**
 * This class implements a factory for the {@link JdbcAdapter}.
 * <p>
 * Note that this class must be registered in a resource file called
 * <code>META-INF/services/com.exasol.adapter.AdapterFactory</code> in order for the {@link ServiceLoader} to find it.
 */
public class JdbcAdapterFactory implements AdapterFactory {
    private static final String ADAPTER_NAME = "JDBC Adapter";

    @Override
    public Set<String> getSupportedAdapterNames() {
        return SqlDialectRegistry.getInstance().getRegisteredAdapterNames();
    }

    @Override
    public VirtualSchemaAdapter createAdapter() {
        return new JdbcAdapter();
    }

    @Override
    public String getAdapterVersion() {
        final VersionCollector versionCollector = new VersionCollector(
                "META-INF/maven/com.exasol/virtual-schema-common-jdbc/pom.properties");
        return versionCollector.getVersionNumber();
    }

    @Override
    public String getAdapterName() {
        return ADAPTER_NAME;
    }
}