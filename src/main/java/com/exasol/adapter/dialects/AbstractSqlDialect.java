package com.exasol.adapter.dialects;

import static com.exasol.adapter.AdapterProperties.*;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.rewriting.SqlGenerationContext;
import com.exasol.adapter.dialects.rewriting.SqlGenerationVisitor;
import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.adapter.jdbc.RemoteMetadataReader;
import com.exasol.adapter.metadata.SchemaMetadata;
import com.exasol.adapter.sql.*;
import com.exasol.errorreporting.ExaError;

/**
 * Abstract implementation of a dialect. We recommend that every dialect should extend this abstract class.
 */
public abstract class AbstractSqlDialect implements SqlDialect {
    private static final Logger LOGGER = Logger.getLogger(AbstractSqlDialect.class.getName());
    private static final Pattern BOOLEAN_PROPERTY_VALUE_PATTERN = Pattern.compile("^TRUE$|^FALSE$",
            Pattern.CASE_INSENSITIVE);
    private static final Set<String> COMMON_SUPPORTED_PROPERTIES = Set.of(SQL_DIALECT_PROPERTY,
            CONNECTION_NAME_PROPERTY, TABLE_FILTER_PROPERTY, EXCLUDED_CAPABILITIES_PROPERTY, DEBUG_ADDRESS_PROPERTY,
            LOG_LEVEL_PROPERTY);
    protected final ConnectionFactory connectionFactory;
    private final Set<String> supportedProperties;
    protected Set<ScalarFunction> omitParenthesesMap = EnumSet.noneOf(ScalarFunction.class);
    protected AdapterProperties properties;

    /**
     * Create a new instance of an {@link AbstractSqlDialect}.
     *
     * @param connectionFactory         factory for JDBC connection to remote data source
     * @param properties                user properties
     * @param dialectSpecificProperties a set of properties that dialect supports additionally to the common set *
     *                                  {@link com.exasol.adapter.dialects.AbstractSqlDialect#COMMON_SUPPORTED_PROPERTIES}
     */
    public AbstractSqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties,
            final Set<String> dialectSpecificProperties) {
        this.connectionFactory = connectionFactory;
        this.properties = properties;
        this.supportedProperties = new HashSet<>(COMMON_SUPPORTED_PROPERTIES);
        this.supportedProperties.addAll(dialectSpecificProperties);
    }

    /**
     * Add additional dialect-specific supported properties that are not in the
     * {@link com.exasol.adapter.dialects.AbstractSqlDialect#COMMON_SUPPORTED_PROPERTIES} set.
     *
     * @param additionalPropertiesToSupport list of properties names
     */
    protected void addAdditionalSupportedProperties(final List<String> additionalPropertiesToSupport) {
        this.supportedProperties.addAll(additionalPropertiesToSupport);
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

    /**
     * Get a set of adapter properties that the dialect supports.
     *
     * @return set of supported properties
     */
    public Set<String> getSupportedProperties() {
        return this.supportedProperties;
    }

    @Override
    public Map<ScalarFunction, String> getPrefixFunctionAliases() {
        final Map<ScalarFunction, String> aliases = new EnumMap<>(ScalarFunction.class);
        aliases.put(ScalarFunction.NEG, "-");
        return aliases;
    }

    @Override
    public String rewriteQuery(final SqlStatement statement, final ExaMetadata exaMetadata)
            throws AdapterException, SQLException {
        return createQueryRewriter().rewrite(statement, exaMetadata, this.properties);
    }

    @Override
    public SchemaMetadata readSchemaMetadata() {
        return createRemoteMetadataReader().readRemoteSchemaMetadata();
    }

    @Override
    public SchemaMetadata readSchemaMetadata(final List<String> tables) {
        return createRemoteMetadataReader().readRemoteSchemaMetadata(tables);
    }

    public String quoteLiteralStringWithSingleQuote(final String value) {
        if (value == null) {
            return "NULL";
        } else {
            return "'" + value.replace("'", "''") + "'";
        }
    }

    protected String quoteIdentifierWithDoubleQuotes(final String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public void validateProperties() throws PropertyValidationException {
        validateSupportedPropertiesList();
        validateConnectionNameProperty();
        validateCatalogNameProperty();
        validateSchemaNameProperty();
        validateDebugOutputAddress();
        validateExceptionHandling();
    }

    protected void validateSupportedPropertiesList() throws PropertyValidationException {
        final List<String> allProperties = new ArrayList<>(this.properties.keySet());
        for (final String property : allProperties) {
            if (!getSupportedProperties().contains(property)) {
                throw new PropertyValidationException(createUnsupportedElementMessage(property, property));
            }
        }
    }

    protected String createUnsupportedElementMessage(final String unsupportedElement, final String property) {
        return "The dialect " + this.properties.getSqlDialect() + " does not support " + unsupportedElement
                + " property. Please, do not set the \"" + property + "\" property.";
    }

    private void validateConnectionNameProperty() throws PropertyValidationException {
        if (!this.properties.hasConnectionName()) {
            throw new PropertyValidationException(
                    "Please specify a connection using the property \"" + CONNECTION_NAME_PROPERTY + "\".");
        }
    }

    private void validateCatalogNameProperty() throws PropertyValidationException {
        if (this.properties.containsKey(CATALOG_NAME_PROPERTY)
                && (supportsJdbcCatalogs() == StructureElementSupport.NONE)) {
            throw new PropertyValidationException(createUnsupportedElementMessage("catalogs", CATALOG_NAME_PROPERTY));
        }
    }

    private void validateSchemaNameProperty() throws PropertyValidationException {
        if (this.properties.containsKey(SCHEMA_NAME_PROPERTY)
                && (supportsJdbcSchemas() == StructureElementSupport.NONE)) {
            throw new PropertyValidationException(createUnsupportedElementMessage("schemas", SCHEMA_NAME_PROPERTY));
        }
    }

    protected void validateBooleanProperty(final String property) throws PropertyValidationException {
        if (this.properties.containsKey(property) //
                && !BOOLEAN_PROPERTY_VALUE_PATTERN.matcher(this.properties.get(property)).matches()) {
            throw new PropertyValidationException("The value '" + this.properties.get(property) + "' for the property "
                    + property + " is invalid. It has to be either 'true' or 'false' (case insensitive).");
        }
    }

    private void validateDebugOutputAddress() {
        if (this.properties.containsKey(DEBUG_ADDRESS_PROPERTY)) {
            final String debugAddress = this.properties.getDebugAddress();
            if (!debugAddress.isEmpty()) {
                validateDebugPortNumber(debugAddress);
            }
        }
    }

    // Note that this method intentionally does not throw a validation exception but rather creates log warnings. This
    // allows dropping a schema even if the debug output port is misconfigured. Logging falls back to local logging in
    // this case.
    private void validateDebugPortNumber(final String debugAddress) {
        final int colonLocation = debugAddress.lastIndexOf(':');
        if (colonLocation > 0) {
            final String portAsString = debugAddress.substring(colonLocation + 1);
            try {
                final int port = Integer.parseInt(portAsString);
                if ((port < 1) || (port > 65535)) {
                    LOGGER.warning(() -> "Debug output port " + port + " is out of range. Port specified in property "
                            + DEBUG_ADDRESS_PROPERTY
                            + "must have following format: <host>[:<port>], and be between 1 and 65535.");
                }
            } catch (final NumberFormatException ex) {
                LOGGER.warning(() -> "Illegal debug output port \"" + portAsString + "\". Property "
                        + DEBUG_ADDRESS_PROPERTY
                        + "must have following format: <host>[:<port>], where port is a number between 1 and 65535.");
            }
        }
    }

    private void validateExceptionHandling() throws PropertyValidationException {
        if (this.properties.containsKey(EXCEPTION_HANDLING_PROPERTY)) {
            final String exceptionHandling = this.properties.getExceptionHandling();
            if (!((exceptionHandling == null) || exceptionHandling.isEmpty())) {
                for (final SqlDialect.ExceptionHandlingMode mode : SqlDialect.ExceptionHandlingMode.values()) {
                    if (!mode.name().equals(exceptionHandling)) {
                        throw new PropertyValidationException(
                                "Invalid value '" + exceptionHandling + "' for property " + EXCEPTION_HANDLING_PROPERTY
                                        + ". Choose one of: " + ExceptionHandlingMode.IGNORE_INVALID_VIEWS.name() + ", "
                                        + ExceptionHandlingMode.NONE.name());
                    }
                }
            }
        }
    }

    protected void checkImportPropertyConsistency(final String importFromProperty, final String connectionProperty)
            throws PropertyValidationException {
        final boolean isDirectImport = this.properties.isEnabled(importFromProperty);
        final String value = this.properties.get(connectionProperty);
        final boolean connectionIsEmpty = ((value == null) || value.isEmpty());
        if (isDirectImport) {
            if (connectionIsEmpty) {
                throw new PropertyValidationException("You defined the property " + importFromProperty
                        + ", please also define " + connectionProperty);
            }
        } else {
            if (!connectionIsEmpty) {
                throw new PropertyValidationException("You defined the property " + connectionProperty
                        + " without setting " + importFromProperty + " to 'TRUE'. This is not allowed");
            }
        }
    }

    protected void validateCastNumberToDecimalProperty(final String castNumberToDecimalProperty)
            throws PropertyValidationException {
        if (this.properties.containsKey(castNumberToDecimalProperty)) {
            final Pattern pattern = Pattern.compile("\\s*(\\d+)\\s*,\\s*(\\d+)\\s*");
            final String precisionAndScale = this.properties.get(castNumberToDecimalProperty);
            final Matcher matcher = pattern.matcher(precisionAndScale);
            if (!matcher.matches()) {
                throw new PropertyValidationException(ExaError.messageBuilder("E-VSJ-1").message(
                        "Unable to parse adapter property {{propertyName}} value {{value}} into a number's precision and scale. The required format is '<precision>,<scale>', where both are integer numbers.")
                        .parameter("propertyName", castNumberToDecimalProperty).parameter("value", precisionAndScale)
                        .toString());
            }
        }
    }
}