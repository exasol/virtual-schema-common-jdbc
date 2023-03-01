package com.exasol.adapter.dialects.dummy;

import static com.exasol.adapter.AdapterProperties.*;

import java.sql.SQLException;
import java.util.Set;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.capabilities.Capabilities;
import com.exasol.adapter.dialects.AbstractSqlDialect;
import com.exasol.adapter.dialects.QueryRewriter;
import com.exasol.adapter.dialects.rewriting.ImportIntoTemporaryTableQueryRewriter;
import com.exasol.adapter.dialects.rewriting.SqlGenerationContext;
import com.exasol.adapter.jdbc.*;

public class DummySqlDialect extends AbstractSqlDialect {
    static final String NAME = "DUMMYDIALECT";

    public DummySqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties) {
        super(connectionFactory, properties,
                Set.of(SCHEMA_NAME_PROPERTY, CATALOG_NAME_PROPERTY, EXCEPTION_HANDLING_PROPERTY));
    }

    @Override
    public String getName() {
        return NAME;
    }

    public AdapterProperties getProperties() {
        return this.properties;
    }

    @Override
    public Capabilities getCapabilities() {
        return null;
    }

    @Override
    public StructureElementSupport supportsJdbcCatalogs() {
        return StructureElementSupport.NONE;
    }

    @Override
    public StructureElementSupport supportsJdbcSchemas() {
        return StructureElementSupport.NONE;
    }

    @Override
    public String applyQuote(final String identifier) {
        return super.quoteIdentifierWithDoubleQuotes(identifier);
    }

    @Override
    public boolean requiresCatalogQualifiedTableNames(final SqlGenerationContext context) {
        return false;
    }

    @Override
    public boolean requiresSchemaQualifiedTableNames(final SqlGenerationContext context) {
        return false;
    }

    @Override
    public NullSorting getDefaultNullSorting() {
        return null;
    }

    @Override
    public String getStringLiteral(final String value) {
        return super.quoteLiteralStringWithSingleQuote(value);
    }

    @Override
    protected RemoteMetadataReader createRemoteMetadataReader() {
        try {
            return new BaseRemoteMetadataReader(this.connectionFactory.getConnection(), this.properties);
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException("Unable to create a metadata reader for the Dummy dialect.",
                    exception);
        }
    }

    @Override
    protected QueryRewriter createQueryRewriter() {
        return new ImportIntoTemporaryTableQueryRewriter(this, createRemoteMetadataReader(), this.connectionFactory);
    }
}