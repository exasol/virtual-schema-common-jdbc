package com.exasol.adapter.jdbc;

import java.sql.Connection;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;

/**
 * This class contains parts that are used commonly across all types of metadata readers.
 *
 * @see BaseRemoteMetadataReader
 * @see BaseTableMetadataReader
 * @see BaseColumnMetadataReader
 */
public abstract class AbstractMetadataReader implements MetadataReader {
    /** Adapter properties */
    protected final AdapterProperties properties;
    /** Metadata of the Exasol database */
    protected final ExaMetadata exaMetadata;
    /** Connection */
    protected final Connection connection;

    /**
     * Create an {@link AbstractMetadataReader}.
     *
     * @param connection JDBC connection to remote data source
     * @param properties user-defined adapter properties
     * @param exaMetadata metadata of the Exasol database
     */
    protected AbstractMetadataReader(final Connection connection, final AdapterProperties properties,
                                     final ExaMetadata exaMetadata) {
        this.properties = properties;
        this.connection = connection;
        this.exaMetadata = exaMetadata;
    }

    /**
     * Get the catalog name that is applied as filter criteria when looking up remote metadata.
     *
     * @return catalog name or <code>null</code> if metadata lookups are not limited by catalog
     */
    @Override
    public String getCatalogNameFilter() {
        return this.properties.getCatalogName();
    }

    /**
     * Get the schema name that is applied as filter criteria when looking up remote metadata.
     *
     * @return schema name or <code>null</code> if metadata lookups are not limited by schema
     */
    @Override
    public String getSchemaNameFilter() {
        return this.properties.getSchemaName();
    }
}