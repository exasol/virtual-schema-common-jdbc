package com.exasol.adapter.dialects;

import static com.exasol.adapter.AdapterProperties.*;

import java.sql.SQLException;
import java.util.*;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.rewriting.SqlGenerationContext;
import com.exasol.adapter.dialects.rewriting.SqlGenerationVisitor;
import com.exasol.adapter.dialects.validators.*;
import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.adapter.jdbc.RemoteMetadataReader;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.metadata.SchemaMetadata;
import com.exasol.adapter.properties.*;
import com.exasol.adapter.sql.*;
import com.exasol.errorreporting.ExaError;

/**
 * Abstract implementation of a dialect. We recommend that every dialect should extend this abstract class.
 */
public abstract class AbstractSqlDialect implements SqlDialect {
    private static final Set<String> COMMON_SUPPORTED_PROPERTIES = Set.of(CONNECTION_NAME_PROPERTY,
            TABLE_FILTER_PROPERTY, EXCLUDED_CAPABILITIES_PROPERTY, DEBUG_ADDRESS_PROPERTY, LOG_LEVEL_PROPERTY,
            DataTypeDetection.STRATEGY_PROPERTY, TableCountLimit.MAXTABLES_PROPERTY);
    /** Factory that creates JDBC connection to the data source */
    protected final ConnectionFactory connectionFactory;
    private final SupportedPropertiesValidator supportedProperties;

    /** Set of functions for which the adapter should omit parentheses */
    protected Set<ScalarFunction> omitParenthesesMap = EnumSet.noneOf(ScalarFunction.class);
    /** Adapter properties */
    protected AdapterProperties properties;
    private final ValidatorChain propertyValidators;

    /**
     * Create a new instance of an {@link AbstractSqlDialect}.
     *
     * @param connectionFactory         factory for JDBC connection to remote data source
     * @param properties                user properties
     * @param dialectSpecificProperties a set of properties that dialect supports additionally to the common set
     *                                  {@link com.exasol.adapter.dialects.AbstractSqlDialect#COMMON_SUPPORTED_PROPERTIES}
     */
    protected AbstractSqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties,
            final Set<String> dialectSpecificProperties) {
        this.connectionFactory = connectionFactory;
        this.properties = properties;
        this.supportedProperties = new SupportedPropertiesValidator() //
                .add(COMMON_SUPPORTED_PROPERTIES) //
                .add(dialectSpecificProperties);
        this.propertyValidators = PropertyValidator.chain() //
                .add(this.supportedProperties) //
                .add(ConnectionNameProperty.validator()) //
                .add(PropertyValidator.forStructureElement(supportsJdbcCatalogs(), "catalogs", CATALOG_NAME_PROPERTY))
                .add(DebugPortNumberProperty.validator()) //
                .add(PropertyValidator.forStructureElement(supportsJdbcSchemas(), "schemas", SCHEMA_NAME_PROPERTY))
                .add(ExceptionHandlingProperty.validator()) //
                .add(DataTypeDetection.getValidator()) //
                .add(TableCountLimit.getValidator());
    }

    /**
     * Add additional dialect-specific supported properties that are not in the
     * {@link com.exasol.adapter.dialects.AbstractSqlDialect#COMMON_SUPPORTED_PROPERTIES} set.
     *
     * @param additionalPropertiesToSupport list of properties names
     */
    protected void addAdditionalSupportedProperties(final List<String> additionalPropertiesToSupport) {
        this.supportedProperties.add(additionalPropertiesToSupport);
    }

    /**
     * Create the {@link RemoteMetadataReader} that is used to get the database metadata from the remote source.
     * <p>
     * Override this method in the concrete SQL dialect implementation to choose the right metadata reader.
     *
     * @return metadata reader
     */
    protected abstract RemoteMetadataReader createRemoteMetadataReader();

    /**
     * Create the {@link QueryRewriter} that is used to create the final SQL query sent back from the Virtual Schema
     * backend to the Virtual Schema frontend in a push-down scenario.
     * <p>
     * Override this method in the concrete SQL dialect implementation to choose the right query rewriter.
     *
     * @return query rewriter
     */
    protected abstract QueryRewriter createQueryRewriter();

    @Override
    public String getTableCatalogAndSchemaSeparator() {
        return ".";
    }

    @Override
    public boolean omitParentheses(final ScalarFunction function) {
        return this.omitParenthesesMap.contains(function);
    }

    @Override
    public SqlGenerator getSqlGenerator(final SqlGenerationContext context) {
        return new SqlGenerationVisitor(this, context);
    }

    @Override
    public Map<ScalarFunction, String> getScalarFunctionAliases() {
        return new EnumMap<>(ScalarFunction.class);
    }

    @Override
    public Map<AggregateFunction, String> getAggregateFunctionAliases() {
        return new EnumMap<>(AggregateFunction.class);
    }

    @Override
    public Map<ScalarFunction, String> getBinaryInfixFunctionAliases() {
        final Map<ScalarFunction, String> aliases = new EnumMap<>(ScalarFunction.class);
        aliases.put(ScalarFunction.ADD, "+");
        aliases.put(ScalarFunction.SUB, "-");
        aliases.put(ScalarFunction.MULT, "*");
        aliases.put(ScalarFunction.FLOAT_DIV, "/");
        return aliases;
    }

    @Override
    public Map<ScalarFunction, String> getPrefixFunctionAliases() {
        final Map<ScalarFunction, String> aliases = new EnumMap<>(ScalarFunction.class);
        aliases.put(ScalarFunction.NEG, "-");
        return aliases;
    }

    @Override
    public String rewriteQuery(final SqlStatement statement, final List<DataType> selectListDataTypes,
            final ExaMetadata exaMetadata) throws AdapterException, SQLException {
        return createQueryRewriter().rewrite(statement, selectListDataTypes, exaMetadata, this.properties);
    }

    @Override
    public SchemaMetadata readSchemaMetadata() {
        return createRemoteMetadataReader().readRemoteSchemaMetadata();
    }

    @Override
    public SchemaMetadata readSchemaMetadata(final List<String> tables) {
        return createRemoteMetadataReader().readRemoteSchemaMetadata(tables);
    }

    /**
     * Quote a string literal with single quotes.
     *
     * @param value string literal
     * @return quoted string
     */
    public String quoteLiteralStringWithSingleQuote(final String value) {
        if (value == null) {
            return "NULL";
        } else {
            return "'" + value.replace("'", "''") + "'";
        }
    }

    /**
     * Quote an identifier with double quotes.
     *
     * @param identifier identifier to quote
     * @return quoted identifier
     */
    protected String quoteIdentifierWithDoubleQuotes(final String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public void validateProperties() throws PropertyValidationException {
        this.propertyValidators.validate(this.properties);
    }

    /**
     * Validate that all given properties are supported by the dialect.
     *
     * @throws PropertyValidationException if validation fails
     *
     * @deprecated Should be removed as search on GitHub did not detect any call to this method by any other virtual
     *             schema.
     */
    @Deprecated(since = "11.0.0")
    protected void validateSupportedPropertiesList() throws PropertyValidationException {
        this.supportedProperties.validate(this.properties);
    }

    /**
     * Get a set of adapter properties that the dialect supports.
     *
     * @return set of supported properties
     */
    public Set<String> getSupportedProperties() {
        return this.supportedProperties.getSupportedProperties();
    }

    /**
     * Create an exception for an unsupported property.
     *
     * @param unsupportedElement unsupported property name.
     * @param property           unsupported property name
     * @return exception
     *
     * @deprecated Should be removed as search on GitHub did not detect any call to this method by any other virtual
     *             schema.
     */
    @Deprecated(since = "11.0.0")
    protected String createUnsupportedElementMessage(final String unsupportedElement, final String property) {
        return SupportedPropertiesValidator.createUnsupportedElementMessage(property);
    }

    /**
     * Validate the input of a boolean property.
     *
     * @param propertyName property name
     * @throws PropertyValidationException if validation fails
     *
     * @deprecated Please use {@link BooleanProperty#validator(String)} instead.
     */
    @Deprecated(since = "11.0.0")
    protected void validateBooleanProperty(final String propertyName) throws PropertyValidationException {
        BooleanProperty.validator(propertyName).validate(this.properties);
    }

    /**
     * Check if the import properties make sense.
     *
     * @param importFromProperty import from property
     * @param connectionProperty connection property
     * @throws PropertyValidationException if check fails
     */
    protected void checkImportPropertyConsistency(final String importFromProperty, final String connectionProperty)
            throws PropertyValidationException {
        final boolean isDirectImport = this.properties.isEnabled(importFromProperty);
        final String value = this.properties.get(connectionProperty);
        final boolean connectionIsEmpty = ((value == null) || value.isEmpty());
        if (isDirectImport) {
            if (connectionIsEmpty) {
                throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-17")
                        .message("You defined the property {{importFromProperty}}.", importFromProperty)
                        .mitigation("Please also define {{connectionProperty}}.", connectionProperty) //
                        .toString());
            }
        } else {
            if (!connectionIsEmpty) {
                throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-18")
                        .message("You defined the property {{connectionProperty}}" //
                                + " without setting {{importFromProperty}} to 'TRUE'." //
                                + " This is not allowed.", //
                                connectionProperty, importFromProperty) //
                        .toString());
            }
        }
    }

    /**
     * Validate the value of the castNumberToDecimalProperty.
     *
     * @param castNumberToDecimalProperty property name
     * @throws PropertyValidationException if validation fails
     *
     * @deprecated Please use {@link CastNumberToDecimalProperty#validator(String)} instead.
     */
    @Deprecated(since = "11.0.0")
    protected void validateCastNumberToDecimalProperty(final String castNumberToDecimalProperty)
            throws PropertyValidationException {
        CastNumberToDecimalProperty.validator(castNumberToDecimalProperty).validate(this.properties);
    }
}