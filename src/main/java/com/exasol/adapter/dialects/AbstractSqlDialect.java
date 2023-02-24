package com.exasol.adapter.dialects;

import static com.exasol.adapter.AdapterProperties.*;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.rewriting.SqlGenerationContext;
import com.exasol.adapter.dialects.rewriting.SqlGenerationVisitor;
import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.adapter.jdbc.RemoteMetadataReader;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.metadata.SchemaMetadata;
import com.exasol.adapter.properties.*;
import com.exasol.adapter.properties.PropertyValidator.PropertyValueValidator;
import com.exasol.adapter.sql.*;
import com.exasol.errorreporting.ExaError;

/**
 * Abstract implementation of a dialect. We recommend that every dialect should extend this abstract class.
 */
public abstract class AbstractSqlDialect implements SqlDialect {
    private static final Logger LOGGER = Logger.getLogger(AbstractSqlDialect.class.getName());
    private static final Pattern BOOLEAN_PROPERTY_VALUE_PATTERN = Pattern.compile("^TRUE$|^FALSE$",
            Pattern.CASE_INSENSITIVE);
    private static final Set<String> COMMON_SUPPORTED_PROPERTIES = Set.of(CONNECTION_NAME_PROPERTY,
            TABLE_FILTER_PROPERTY, EXCLUDED_CAPABILITIES_PROPERTY, DEBUG_ADDRESS_PROPERTY, LOG_LEVEL_PROPERTY,
            DataTypeDetection.STRATEGY_PROPERTY, TableCountLimit.MAXTABLES_PROPERTY);
    /** Factory that creates JDBC connection to the data source */
    protected final ConnectionFactory connectionFactory;
    private final Set<String> supportedProperties;
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
        this.supportedProperties = new HashSet<>(COMMON_SUPPORTED_PROPERTIES);
        this.supportedProperties.addAll(dialectSpecificProperties);
        this.propertyValidators = PropertyValidator.chain() //
                .add(new ConnectionNameValidator()) //
                .add(PropertyValidator.forStructureElement(supportsJdbcCatalogs(), "catalogs", CATALOG_NAME_PROPERTY))
                .add(DebugPortNumberValidator.validator()) //
                .add(PropertyValidator.forStructureElement(supportsJdbcSchemas(), "schemas", SCHEMA_NAME_PROPERTY))
                .add(ExceptionHandlingValidator.validator()) //
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
        validateSupportedPropertiesList();
//        validateConnectionNameProperty();
//        validateCatalogNameProperty();
//        validateSchemaNameProperty();
//        validateDebugOutputAddress();
        // validateExceptionHandling();
        this.propertyValidators.validate(this.properties);
    }

    /**
     * Validate that all given properties are supported by the dialect.
     *
     * @throws PropertyValidationException if validation fails
     */
    protected void validateSupportedPropertiesList() throws PropertyValidationException {
        final List<String> allProperties = new ArrayList<>(this.properties.keySet());
        for (final String property : allProperties) {
            if (!getSupportedProperties().contains(property)) {
                throw UnsupportedElement.validationException(property, property);
            }
        }
    }

    /**
     * Get a set of adapter properties that the dialect supports.
     *
     * @return set of supported properties
     */
    public Set<String> getSupportedProperties() {
        return this.supportedProperties;
    }

    /**
     * Create an exception for an unsupported property.
     *
     * @param unsupportedElement unsupported property name.
     * @param property           unsupported property name
     * @return exception
     */
    @Deprecated
    protected String createUnsupportedElementMessage(final String unsupportedElement, final String property) {
        return UnsupportedElement.message(unsupportedElement, property);
    }

//    private void validateConnectionNameProperty() throws PropertyValidationException {
//        if (!this.properties.hasConnectionName()) {
//            throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-14")
//                    .message("Please specify a connection using the property {{connectionNameProperty}}.")
//                    .parameter("connectionNameProperty", CONNECTION_NAME_PROPERTY).toString());
//        }
//    }

    static class ConnectionNameValidator implements PropertyValidator {
        @Override
        public void validate(final AdapterProperties properties) throws PropertyValidationException {
            if (!properties.hasConnectionName()) {
                throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-14")
                        .message("Please specify a connection using the property {{connectionNameProperty}}.")
                        .parameter("connectionNameProperty", CONNECTION_NAME_PROPERTY).toString());
            }
        }
    }

//    private void validateCatalogNameProperty() throws PropertyValidationException {
//        if (this.properties.containsKey(CATALOG_NAME_PROPERTY)
//                && (supportsJdbcCatalogs() == StructureElementSupport.NONE)) {
//            throw new PropertyValidationException(createUnsupportedElementMessage("catalogs", CATALOG_NAME_PROPERTY));
//        }
//    }

//    private void validateSchemaNameProperty() throws PropertyValidationException {
//        if (this.properties.containsKey(SCHEMA_NAME_PROPERTY)
//                && (supportsJdbcSchemas() == StructureElementSupport.NONE)) {
//            throw new PropertyValidationException(createUnsupportedElementMessage("schemas", SCHEMA_NAME_PROPERTY));
//        }
//    }

//    static class CatalogNameValidator implements PropertyValueValidator {
//        static PropertyValidator validator(final StructureElementSupport supportsJdbcCatalogs) {
//            return PropertyValidator.optional(CATALOG_NAME_PROPERTY, new CatalogNameValidator(supportsJdbcCatalogs));
//        }
//
//        private final StructureElementSupport supportsJdbcCatalogs;
//
//        public CatalogNameValidator(final StructureElementSupport supportsJdbcCatalogs) {
//            this.supportsJdbcCatalogs = supportsJdbcCatalogs;
//        }
//
//        @Override
//        public void validate(final String propertyValue) throws PropertyValidationException {
//            if (this.supportsJdbcCatalogs == StructureElementSupport.NONE) {
//                throw UnsupportedElement.validationException("catalogs", CATALOG_NAME_PROPERTY);
//            }
//        }
//    }

//    static class SchemaNameValidator implements PropertyValueValidator {
//        static PropertyValidator validator(final StructureElementSupport supportsJdbcSchemas) {
//            return PropertyValidator.optional(SCHEMA_NAME_PROPERTY, new SchemaNameValidator(supportsJdbcSchemas));
//        }
//
//        private final StructureElementSupport supportsJdbcSchemas;
//
//        public SchemaNameValidator(final StructureElementSupport supportsJdbcSchemas) {
//            this.supportsJdbcSchemas = supportsJdbcSchemas;
//        }
//
//        @Override
//        public void validate(final String propertyValue) throws PropertyValidationException {
//            if (this.supportsJdbcSchemas == StructureElementSupport.NONE) {
//                throw UnsupportedElement.validationException("schemas", SCHEMA_NAME_PROPERTY);
//            }
//        }
//    }

    /**
     * Validate the input of a boolean property.
     *
     * @param property property name
     * @throws PropertyValidationException if validation fails
     */
    protected void validateBooleanProperty(final String property) throws PropertyValidationException {
        if (this.properties.containsKey(property) //
                && !BOOLEAN_PROPERTY_VALUE_PATTERN.matcher(this.properties.get(property)).matches()) {
            throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-15")
                    .message("The value {{value}} for the property {{property}} is invalid. "
                            + "It has to be either 'true' or 'false' (case insensitive).")
                    .parameter("value", this.properties.get(property)) //
                    .parameter("property", property).toString());
        }
    }

    static class DebugPortNumberValidator implements PropertyValueValidator {
        static PropertyValidator validator() {
            return PropertyValidator.ignoreEmpty(DEBUG_ADDRESS_PROPERTY, new DebugPortNumberValidator());
        }

        /*
         * Note that this method intentionally does not throw a validation exception but rather creates log warnings.
         * This allows dropping a schema even if the debug output port is misconfigured. Logging falls back to local
         * logging in this case.
         */
        @Override
        public void validate(final String debugAddress) throws PropertyValidationException {
            final int colonLocation = debugAddress.lastIndexOf(':');
            if (colonLocation > 0) {
                final String portAsString = debugAddress.substring(colonLocation + 1);
                try {
                    final int port = Integer.parseInt(portAsString);
                    if ((port < 1) || (port > 65535)) {
                        LOGGER.warning(() -> ExaError.messageBuilder("W-VSCJDBC-40")
                                .message("Debug output port {{port|uq}} is out of range.", port) //
                                .mitigation("Port specified in property {{debugAddressProperty}} must have "
                                        + "the following format: <host>[:<port>], and be between 1 and 65535.")
                                .parameter("debugAddressProperty", DEBUG_ADDRESS_PROPERTY).toString());
                    }
                } catch (final NumberFormatException ex) {
                    LOGGER.warning(() -> ExaError.messageBuilder("W-VSCJDBC-39").message(
                            "Illegal debug output port {{portAsString}}. Property {{debugAddressProperty}} must have "
                                    + "the following format: <host>[:<port>], where port is a number between 1 and 65535.")
                            .parameter("debugAddressProperty", DEBUG_ADDRESS_PROPERTY)
                            .parameter("portAsString", portAsString).toString());
                }
            }
        }
    }

//    private void validateDebugOutputAddress() {
//        if (this.properties.containsKey(DEBUG_ADDRESS_PROPERTY)) {
//            final String debugAddress = this.properties.getDebugAddress();
//            if (!debugAddress.isEmpty()) {
//                validateDebugPortNumber(debugAddress);
//            }
//        }
//    }

//    // Note that this method intentionally does not throw a validation exception but rather creates log warnings. This
//    // allows dropping a schema even if the debug output port is misconfigured. Logging falls back to local logging in
//    // this case.
//    private void validateDebugPortNumber(final String debugAddress) {
//        final int colonLocation = debugAddress.lastIndexOf(':');
//        if (colonLocation > 0) {
//            final String portAsString = debugAddress.substring(colonLocation + 1);
//            try {
//                final int port = Integer.parseInt(portAsString);
//                if ((port < 1) || (port > 65535)) {
//                    LOGGER.warning(() -> ExaError.messageBuilder("W-VSCJDBC-40")
//                            .message("Debug output port {{port|uq}} is out of range.", port) //
//                            .mitigation("Port specified in property {{debugAddressProperty}} must have "
//                                    + "the following format: <host>[:<port>], and be between 1 and 65535.")
//                            .parameter("debugAddressProperty", DEBUG_ADDRESS_PROPERTY).toString());
//                }
//            } catch (final NumberFormatException ex) {
//                LOGGER.warning(() -> ExaError.messageBuilder("W-VSCJDBC-39").message(
//                        "Illegal debug output port {{portAsString}}. Property {{debugAddressProperty}} must have "
//                                + "the following format: <host>[:<port>], where port is a number between 1 and 65535.")
//                        .parameter("debugAddressProperty", DEBUG_ADDRESS_PROPERTY)
//                        .parameter("portAsString", portAsString).toString());
//            }
//        }
//    }

    static class ExceptionHandlingValidator implements PropertyValueValidator {
        static PropertyValidator validator() {
            return PropertyValidator.ignoreEmpty(EXCEPTION_HANDLING_PROPERTY, new ExceptionHandlingValidator());
        }

        @Override
        public void validate(final String exceptionHandling) throws PropertyValidationException {
            for (final SqlDialect.ExceptionHandlingMode mode : SqlDialect.ExceptionHandlingMode.values()) {
                if (mode.name().equals(exceptionHandling)) {
                    return;
                }
            }
            throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-16")
                    .message("Invalid value {{exceptionHandlingValue}} for property {{exceptionHandlingProperty}}.")
                    .parameter("exceptionHandlingValue", exceptionHandling)
                    .parameter("exceptionHandlingProperty", EXCEPTION_HANDLING_PROPERTY)
                    .mitigation("Choose one of: {{availableValues|uq}}.", Arrays.stream(ExceptionHandlingMode.values())
                            .map(Enum::toString).collect(Collectors.toList()).toString())
                    .toString());
        }
    }

//    private void validateExceptionHandling() throws PropertyValidationException {
//        if (this.properties.containsKey(EXCEPTION_HANDLING_PROPERTY)) {
//            final String exceptionHandling = this.properties.getExceptionHandling();
//            if (!((exceptionHandling == null) || exceptionHandling.isEmpty())) {
//                for (final SqlDialect.ExceptionHandlingMode mode : SqlDialect.ExceptionHandlingMode.values()) {
//                    if (!mode.name().equals(exceptionHandling)) {
//                        throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-16").message(
//                                "Invalid value {{exceptionHandlingValue}} for property {{exceptionHandlingProperty}}.")
//                                .parameter("exceptionHandlingValue", exceptionHandling)
//                                .parameter("exceptionHandlingProperty", EXCEPTION_HANDLING_PROPERTY)
//                                .mitigation("Choose one of: {{availableValues|uq}}.",
//                                        Arrays.stream(ExceptionHandlingMode.values()).map(Enum::toString)
//                                                .collect(Collectors.toList()).toString())
//                                .toString());
//                    }
//                }
//            }
//        }
//    }

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
     */
    protected void validateCastNumberToDecimalProperty(final String castNumberToDecimalProperty)
            throws PropertyValidationException {
        if (this.properties.containsKey(castNumberToDecimalProperty)) {
            final Pattern pattern = Pattern.compile("\\s*(\\d+)\\s*,\\s*(\\d+)\\s*");
            final String precisionAndScale = this.properties.get(castNumberToDecimalProperty);
            final Matcher matcher = pattern.matcher(precisionAndScale);
            if (!matcher.matches()) {
                throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-19")
                        .message("Unable to parse adapter property {{propertyName}} value {{value}}" //
                                + " into a number's precision and scale." //
                                + " The required format is '<precision>,<scale>' where both are integer numbers.", //
                                castNumberToDecimalProperty, precisionAndScale) //
                        .toString());
            }
        }
    }
}