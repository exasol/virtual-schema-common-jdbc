package com.exasol.adapter.dialects.dummy;

import static com.exasol.adapter.AdapterProperties.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.capabilities.Capabilities;
import com.exasol.adapter.dialects.*;
import com.exasol.adapter.jdbc.*;

public class DummySqlDialect extends AbstractSqlDialect {
    static final String NAME = "DUMMYDIALECT";
    private static final List<String> SUPPORTED_PROPERTIES = createSupportedPropertiesList();

    private static List<String> createSupportedPropertiesList() {
        return addAdditionalSupportedProperties(Arrays.asList(SCHEMA_NAME_PROPERTY, EXCEPTION_HANDLING_PROPERTY));
    }

    public DummySqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties) {
        super(connectionFactory, properties);
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
        return "\"" + identifier + "\"";
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
    protected List<String> getSupportedProperties() {
        return SUPPORTED_PROPERTIES;
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
        return new BaseQueryRewriter(this, createRemoteMetadataReader(), this.connectionFactory);
    }
}