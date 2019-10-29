package com.exasol.adapter.dialects.derby;

import static com.exasol.adapter.AdapterProperties.*;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.capabilities.*;
import com.exasol.adapter.dialects.*;
import com.exasol.adapter.jdbc.BaseRemoteMetadataReader;
import com.exasol.adapter.jdbc.RemoteMetadataReader;

/**
 * This class implements the Apache Derby SQL Dialect.
 *
 * @see <a href="https://db.apache.org/derby/">Apache Derby</a>
 */
public class DerbySqlDialect extends AbstractSqlDialect {
    static final String NAME = "DERBY";
    private static final Capabilities CAPABILITIES = createCapabilityList();
    private static final List<String> SUPPORTED_PROPERTIES = Arrays.asList(SQL_DIALECT_PROPERTY,
            CONNECTION_NAME_PROPERTY, CONNECTION_STRING_PROPERTY, USERNAME_PROPERTY, PASSWORD_PROPERTY,
            CATALOG_NAME_PROPERTY, SCHEMA_NAME_PROPERTY, TABLE_FILTER_PROPERTY, EXCLUDED_CAPABILITIES_PROPERTY,
            DEBUG_ADDRESS_PROPERTY, LOG_LEVEL_PROPERTY);

    private static Capabilities createCapabilityList() {
        return Capabilities.builder().addMain(MainCapability.ORDER_BY_EXPRESSION)
                .addScalarFunction(ScalarFunctionCapability.ADD)
                .addAggregateFunction(AggregateFunctionCapability.COUNT_STAR).addLiteral(LiteralCapability.NULL)
                .addPredicate(PredicateCapability.AND).build();
    }

    /**
     * Create a new instance of a {@link DerbySqlDialect}.
     *
     * @param connection JDBC connection to the Apache Derby database
     * @param properties user-defined adapter properties
     */
    public DerbySqlDialect(final Connection connection, final AdapterProperties properties) {
        super(connection, properties);
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
        return "\"" + identifier + "\"";
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
    protected RemoteMetadataReader createRemoteMetadataReader() {
        return new BaseRemoteMetadataReader(this.connection, this.properties);
    }

    @Override
    protected QueryRewriter createQueryRewriter() {
        return new BaseQueryRewriter(this, this.remoteMetadataReader, this.connection);
    }

    @Override
    protected List<String> getSupportedProperties() {
        return SUPPORTED_PROPERTIES;
    }
}