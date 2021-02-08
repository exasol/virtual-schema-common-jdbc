package com.exasol.adapter.jdbc;

import java.util.NoSuchElementException;
import java.util.ServiceLoader;

import com.exasol.adapter.AdapterFactory;
import com.exasol.adapter.VirtualSchemaAdapter;
import com.exasol.adapter.dialects.SqlDialectFactory;
import com.exasol.logging.VersionCollector;

/**
 * This class implements a factory for the {@link JDBCAdapter}.
 */
public class JDBCAdapterFactory implements AdapterFactory {
    private static final String ADAPTER_NAME = "JDBC Adapter";
    private final SqlDialectFactory sqlDialectFactory = loadSqlDialectFactory();

    private SqlDialectFactory loadSqlDialectFactory() {
        final ServiceLoader<SqlDialectFactory> sqlDialectFactoryLoader = ServiceLoader.load(SqlDialectFactory.class);
        return sqlDialectFactoryLoader.findFirst()
                .orElseThrow(() -> new NoSuchElementException("No SqlDialectFactory was found."));
    }

    private String getSqlDialectName() {
        return this.sqlDialectFactory.getSqlDialectName();
    }

    @Override
    public String getAdapterName() {
        return this.getSqlDialectName() + " " + ADAPTER_NAME;
    }

    @Override
    public String getAdapterVersion() {
        final VersionCollector versionCollector = new VersionCollector(
                "META-INF/maven/com.exasol/virtual-schema-common-jdbc/pom.properties");
        return versionCollector.getVersionNumber() + " (" + this.getSqlDialectName() + " "
                + this.sqlDialectFactory.getSqlDialectVersion() + ")";
    }

    /**
     * Create a new instance of the Virtual Schema Adapter
     *
     * @return new instance
     */
    @Override
    public VirtualSchemaAdapter createAdapter() {
        return new JDBCAdapter(this.sqlDialectFactory);
    }
}