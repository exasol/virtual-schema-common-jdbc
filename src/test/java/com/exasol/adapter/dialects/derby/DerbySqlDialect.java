package com.exasol.adapter.dialects.derby;

import static com.exasol.adapter.AdapterProperties.CATALOG_NAME_PROPERTY;
import static com.exasol.adapter.AdapterProperties.SCHEMA_NAME_PROPERTY;

import java.sql.SQLException;
import java.util.Set;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.capabilities.*;
import com.exasol.adapter.dialects.AbstractSqlDialect;
import com.exasol.adapter.dialects.QueryRewriter;
import com.exasol.adapter.dialects.rewriting.ImportIntoTemporaryTableQueryRewriter;
import com.exasol.adapter.dialects.rewriting.SqlGenerationContext;
import com.exasol.adapter.jdbc.*;

/**
 * This class implements the Apache Derby SQL Dialect.
 *
 * @see <a href="https://db.apache.org/derby/">Apache Derby</a>
 */
public class DerbySqlDialect extends AbstractSqlDialect {
    static final String NAME = "DERBY";
    private static final Capabilities CAPABILITIES = createCapabilityList();

    private static Capabilities createCapabilityList() {
        return Capabilities.builder().addMain(MainCapability.ORDER_BY_EXPRESSION)
                .addScalarFunction(ScalarFunctionCapability.ADD)
                .addAggregateFunction(AggregateFunctionCapability.COUNT_STAR).addLiteral(LiteralCapability.NULL)
                .addPredicate(PredicateCapability.AND).build();
    }

    /**
     * Create a new instance of a {@link DerbySqlDialect}.
     *
     * @param connectionFactory factory for JDBC connection to the Apache Derby database
     * @param properties        user-defined adapter properties
     */
    public DerbySqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties,
                final ExaMetadata exaMetadata) {
        super(connectionFactory, properties, exaMetadata, Set.of(CATALOG_NAME_PROPERTY, SCHEMA_NAME_PROPERTY));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Capabilities getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public StructureElementSupport supportsJdbcCatalogs() {
        return StructureElementSupport.NONE;
    }

    @Override
    public StructureElementSupport supportsJdbcSchemas() {
        return StructureElementSupport.MULTIPLE;
    }

    /**
     * @see <a href="https://db.apache.org/derby/docs/10.14/ref/rrefschemaname.html">&quot;schemaName&quot; (Derby
     *      reference manual)</a>
     */
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
        return true;
    }

    /**
     * @see <a href="https://db.apache.org/derby/docs/10.14/ref/rrefsqlj13658.html">&quot;ORDER BY clause&quot; (DERBY
     *      reference manual)</a>
     */
    @Override
    public NullSorting getDefaultNullSorting() {
        return NullSorting.NULLS_SORTED_LOW;
    }

    @Override
    public String getStringLiteral(final String value) {
        return super.quoteLiteralStringWithSingleQuote(value);
    }

    @Override
    protected RemoteMetadataReader createRemoteMetadataReader() {
        try {
            return new BaseRemoteMetadataReader(this.connectionFactory.getConnection(), this.properties, this.exaMetadata);
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException("Unable to create a metadata reader for Derby.", exception);
        }
    }

    @Override
    protected QueryRewriter createQueryRewriter() {
        return new ImportIntoTemporaryTableQueryRewriter(this, createRemoteMetadataReader(), this.connectionFactory);
    }
}