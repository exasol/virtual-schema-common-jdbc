package com.exasol.adapter.jdbc;

import java.util.ServiceLoader;
import java.util.Set;

import com.exasol.adapter.AdapterFactory;
import com.exasol.logging.VersionCollector;

/**
 * This class implements a factory for the {@link AbstractJdbcAdapter}.
 * <p>
 * Note that this classes derived from this class must be registered in a resource file called
 * <code>META-INF/services/com.exasol.adapter.AdapterFactory</code> in order for the {@link ServiceLoader} to find it.
 * </p>
 */
public abstract class AbstractJdbcAdapterFactory implements AdapterFactory {
    private static final String ADAPTER_NAME = "JDBC Adapter";

    @Override
    public Set<String> getSupportedAdapterNames() {
        return Set.of(getSqlDialectName());
    }

    @Override
    public String getAdapterName() {
        return getSqlDialectName() + " " + ADAPTER_NAME;
    }

    @Override
    public String getAdapterVersion() {
        final VersionCollector versionCollector = new VersionCollector(
                "META-INF/maven/com.exasol/virtual-schema-common-jdbc/pom.properties");
        return versionCollector.getVersionNumber() + " (" + getSqlDialectName() + " " + getSqlDialectVersion() + ")";
    }

    /**
     * Get the name of the SQL dialect supported by the adapter this factory creates.
     *
     * @return SQL dialect name
     */
    protected abstract String getSqlDialectName();

    /**
     * Get the version of the SQL dialect supported by the adapter this factory creates.
     *
     * @return SQL dialect name
     */
    protected abstract String getSqlDialectVersion();
}