package com.exasol.adapter.jdbc;

import java.sql.Connection;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.IdentifierConverter;

public class DummyRemoteMetadataReader extends AbstractRemoteMetadataReader {
    public DummyRemoteMetadataReader(final Connection connection, final AdapterProperties properties,
            final ExaMetadata exaMetadata) {
        super(connection, properties, exaMetadata);
    }

    @Override
    protected ColumnMetadataReader createColumnMetadataReader() {
        return null;
    }

    @Override
    protected TableMetadataReader createTableMetadataReader() {
        return null;
    }

    @Override
    protected IdentifierConverter createIdentifierConverter() {
        return null;
    }
}