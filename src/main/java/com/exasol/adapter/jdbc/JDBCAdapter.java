package com.exasol.adapter.jdbc;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

import com.exasol.ExaMetadata;
import com.exasol.adapter.*;
import com.exasol.adapter.capabilities.*;
import com.exasol.adapter.dialects.*;
import com.exasol.adapter.metadata.SchemaMetadata;
import com.exasol.adapter.metadata.SchemaMetadataInfo;
import com.exasol.adapter.request.*;
import com.exasol.adapter.response.*;
import com.exasol.errorreporting.ExaError;

import static com.exasol.adapter.jdbc.JDBCAdapterProperties.JDBC_MAXTABLES_PROPERTY;

/**
 * This class implements main logic for different types of requests a virtual schema JDBC adapter can receive.
 */
public class JDBCAdapter implements VirtualSchemaAdapter {
    private static final String SCALAR_FUNCTION_PREFIX = "FN_";
    private static final String PREDICATE_PREFIX = "FN_PRED_";
    private static final String AGGREGATE_FUNCTION_PREFIX = "FN_AGG_";
    private static final String LITERAL_PREFIX = "LITERAL_";
    private static final String TABLES_PROPERTY = "TABLE_FILTER";
    private static final Logger LOGGER = Logger.getLogger(JDBCAdapter.class.getName());
    private final SqlDialectFactory sqlDialectFactory;

    /**
     * Construct a new instance of {@link JDBCAdapter}
     *
     * @param sqlDialectFactory {@link SqlDialectFactory} for creating {@link SqlDialect}
     */
    public JDBCAdapter(final SqlDialectFactory sqlDialectFactory) {
        this.sqlDialectFactory = sqlDialectFactory;
    }

    @Override
    public CreateVirtualSchemaResponse createVirtualSchema(final ExaMetadata exasolMetadata,
            final CreateVirtualSchemaRequest request) throws AdapterException {
        logCreateVirtualSchemaRequestReceived(request);
        final AdapterProperties properties = getPropertiesFromRequest(request);
        validateProperties(properties);
        try {
            final SchemaMetadata remoteMeta = readMetadata(properties, exasolMetadata);
            return CreateVirtualSchemaResponse.builder().schemaMetadata(remoteMeta).build();
        } catch (final SQLException exception) {
            throw new AdapterException(ExaError.messageBuilder("E-VSCJDBC-25")
                    .message("Unable create Virtual Schema \"{{virtualSchemaName|uq}}\". Cause: {{cause|uq}}",
                            request.getVirtualSchemaName(), exception.getMessage())
                    .toString(), exception);
        }
    }

    /**
     * Log a create virtual schema request.
     *
     * @param request create request
     */
    protected void logCreateVirtualSchemaRequestReceived(final CreateVirtualSchemaRequest request) {
        LOGGER.fine(() -> "Received request to create Virutal Schema \"" + request.getVirtualSchemaName() + "\".");
    }

    private AdapterProperties getPropertiesFromRequest(final AdapterRequest request) {
        return new AdapterProperties(request.getSchemaMetadataInfo().getProperties());
    }

    private SchemaMetadata readMetadata(final AdapterProperties properties, final ExaMetadata exasolMetadata)
            throws SQLException, PropertyValidationException {
        final List<String> tables = properties.getFilteredTables();
        final ConnectionFactory connectionFactory = new RemoteConnectionFactory(exasolMetadata, properties);
        final SqlDialect dialect = createDialect(connectionFactory, properties);
        dialect.validateProperties();
        if (tables.isEmpty()) {
            return dialect.readSchemaMetadata();
        }
        return dialect.readSchemaMetadata(tables);
    }

    private SqlDialect createDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties) {
        return this.sqlDialectFactory.createSqlDialect(connectionFactory, properties);
    }

    @Override
    public DropVirtualSchemaResponse dropVirtualSchema(final ExaMetadata metadata,
            final DropVirtualSchemaRequest request) {
        logDropVirtualSchemaRequestReceived(request);
        return DropVirtualSchemaResponse.builder().build();
    }

    /**
     * Log drop virtual schema request.
     *
     * @param request drop request
     */
    protected void logDropVirtualSchemaRequestReceived(final DropVirtualSchemaRequest request) {
        LOGGER.fine(() -> "Received request to drop Virtual Schema \"" + request.getVirtualSchemaName() + "\".");
    }

    @Override
    public RefreshResponse refresh(final ExaMetadata metadata, final RefreshRequest request) throws AdapterException {
        try {
            final SchemaMetadata remoteMetadata = this.getRemoteMetadata(metadata, request);
            return RefreshResponse.builder().schemaMetadata(remoteMetadata).build();
        } catch (final SQLException | PropertyValidationException exception) {
            throw new AdapterException(ExaError.messageBuilder("E-VSCJDBC-26").message(
                    "Unable refresh metadata of Virtual Schema \"{{virtualSchemaName|uq}}\". Cause: {{cause|uq}}",
                    request.getSchemaMetadataInfo().getSchemaName(), exception.getMessage()).toString(), exception);
        }
    }

    private SchemaMetadata getRemoteMetadata(final ExaMetadata metadata, final RefreshRequest request)
            throws PropertyValidationException, SQLException {
        final AdapterProperties properties = getPropertiesFromRequest(request);
        if (request.refreshesOnlySelectedTables()) {
            final List<String> tables = request.getTables();
            return readMetadata(properties, tables, metadata);
        } else {
            return readMetadata(properties, metadata);
        }
    }

    /**
     * Read the schema metadata.
     *
     * @param properties           adapter properties
     * @param remoteTableAllowList allow list for remote tables
     * @param exasolMetadata       exasol metadata
     * @return schema metadata
     * @throws PropertyValidationException if properties are invalid
     */
    protected SchemaMetadata readMetadata(final AdapterProperties properties, final List<String> remoteTableAllowList,
            final ExaMetadata exasolMetadata) throws PropertyValidationException {
        final ConnectionFactory connectionFactory = new RemoteConnectionFactory(exasolMetadata, properties);
        final SqlDialect dialect = createDialect(connectionFactory, properties);
        dialect.validateProperties();
        return dialect.readSchemaMetadata(remoteTableAllowList);
    }

    @Override
    public SetPropertiesResponse setProperties(final ExaMetadata metadata, final SetPropertiesRequest request)
            throws AdapterException {
        final Map<String, String> requestRawProperties = request.getProperties();
        final SchemaMetadataInfo schemaMetadataInfo = request.getSchemaMetadataInfo();
        final Map<String, String> mergedRawProperties = mergeProperties(schemaMetadataInfo.getProperties(),
                requestRawProperties);
        final AdapterProperties mergedProperties = new AdapterProperties(mergedRawProperties);

        validateProperties(mergedProperties);

        if (AdapterProperties.isRefreshingVirtualSchemaRequired(requestRawProperties)) {
            final List<String> tableFilter = getTableFilter(mergedRawProperties);
            final SchemaMetadata remoteMeta = readMetadata(mergedProperties, tableFilter, metadata);
            return SetPropertiesResponse.builder().schemaMetadata(remoteMeta).build();
        }
        return SetPropertiesResponse.builder().schemaMetadata(null).build();
    }

    /**
     * Validate the given properties to be compatible / complete for the adapter
     *
     * Any overriding implementation should call the super method to avoid missing checks!
     *
     * @param properties The complete set of properties to be validated
     * @throws PropertyValidationException if any single property or combination is invalid or missing
     */
    protected  void validateAdapterProperties(AdapterProperties properties) throws PropertyValidationException {
        if (properties.containsKey(JDBC_MAXTABLES_PROPERTY)) {
            int result = 0;
            try {
                result = Integer.parseUnsignedInt(properties.get(JDBC_MAXTABLES_PROPERTY));
            } catch (NumberFormatException e) {
                // pass
            }
            if( result == 0 ) {
                throw new PropertyValidationException(ExaError.messageBuilder("E-VSCJDBC-43") //
                        .message("Invalid parameter value.") //
                        .mitigation(
                                "The adapter property {{max_tables_property}}" + " if present, must be a positive integer.") //
                        .parameter("max_tables_property", JDBC_MAXTABLES_PROPERTY) //
                        .toString());
            }
        }
    }

    // Validate properties for both the base adapter and the sql dialect
    private void validateProperties(AdapterProperties properties) throws PropertyValidationException {
        validateAdapterProperties(properties);
        if( this.sqlDialectFactory != null ) {
            this.sqlDialectFactory.createSqlDialect(null, properties).validateProperties();
        }
    }

    private Map<String, String> mergeProperties(final Map<String, String> previousRawProperties,
            final Map<String, String> requestRawProperties) {
        final Map<String, String> mergedRawProperties = new HashMap<>(previousRawProperties);
        for (final Map.Entry<String, String> requestRawProperty : requestRawProperties.entrySet()) {
            if (requestRawProperty.getValue() == null) {
                mergedRawProperties.remove(requestRawProperty.getKey());
            } else {
                mergedRawProperties.put(requestRawProperty.getKey(), requestRawProperty.getValue());
            }
        }
        return mergedRawProperties;
    }

    private List<String> getTableFilter(final Map<String, String> properties) {
        final String tableNames = properties.get(TABLES_PROPERTY);
        if ((tableNames != null) && !tableNames.isEmpty()) {
            final List<String> tables = Arrays.asList(tableNames.split(","));
            for (int i = 0; i < tables.size(); ++i) {
                tables.set(i, tables.get(i).trim());
            }
            return tables;
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public GetCapabilitiesResponse getCapabilities(final ExaMetadata exaMetadata, final GetCapabilitiesRequest request)
            throws AdapterException {
        LOGGER.fine(() -> "Received request to list the adapter's capabilites.");
        final AdapterProperties properties = getPropertiesFromRequest(request);
        final ConnectionFactory connectionFactory = new RemoteConnectionFactory(exaMetadata, properties);
        final SqlDialect dialect = createDialect(connectionFactory, properties);
        final Capabilities capabilities = dialect.getCapabilities();
        final Capabilities excludedCapabilities = getExcludedCapabilities(properties);
        capabilities.subtractCapabilities(excludedCapabilities);
        return GetCapabilitiesResponse //
                .builder()//
                .capabilities(capabilities)//
                .build();
    }

    private Capabilities getExcludedCapabilities(final AdapterProperties properties) {
        if (properties.containsKey(AdapterProperties.EXCLUDED_CAPABILITIES_PROPERTY)) {
            final String excludedCapabilitiesStr = properties.getExcludedCapabilities();
            final Capabilities.Builder builder = parseExcludedCapabilities(excludedCapabilitiesStr);
            return builder.build();
        } else {
            LOGGER.config(() -> "Excluded Capabilities: none");
            return Capabilities.builder().build();
        }
    }

    private Capabilities.Builder parseExcludedCapabilities(final String excludedCapabilitiesString) {
        final Capabilities.Builder builder = Capabilities.builder();
        LOGGER.config(() -> "Excluded Capabilities: "
                + (excludedCapabilitiesString.isEmpty() ? "none" : excludedCapabilitiesString));
        for (String capability : excludedCapabilitiesString.split(",")) {
            capability = capability.trim();
            if (capability.isEmpty()) {
                continue;
            }
            if (capability.startsWith(LITERAL_PREFIX)) {
                final String literalCapabilities = capability.replaceFirst(LITERAL_PREFIX, "");
                builder.addLiteral(LiteralCapability.valueOf(literalCapabilities));
            } else if (capability.startsWith(AGGREGATE_FUNCTION_PREFIX)) {
                final String aggregateFunctionCap = capability.replaceFirst(AGGREGATE_FUNCTION_PREFIX, "");
                builder.addAggregateFunction(AggregateFunctionCapability.valueOf(aggregateFunctionCap));
            } else if (capability.startsWith(PREDICATE_PREFIX)) {
                final String predicateCapabilities = capability.replaceFirst(PREDICATE_PREFIX, "");
                builder.addPredicate(PredicateCapability.valueOf(predicateCapabilities));
            } else if (capability.startsWith(SCALAR_FUNCTION_PREFIX)) {
                final String scalarFunctionCapabilities = capability.replaceFirst(SCALAR_FUNCTION_PREFIX, "");
                builder.addScalarFunction(ScalarFunctionCapability.valueOf(scalarFunctionCapabilities));
            } else {
                builder.addMain(MainCapability.valueOf(capability));
            }
        }
        return builder;
    }

    @Override
    public PushDownResponse pushdown(final ExaMetadata exaMetadata, final PushDownRequest request)
            throws AdapterException {
        try {
            final AdapterProperties properties = getPropertiesFromRequest(request);
            final ConnectionFactory connectionFactory = new RemoteConnectionFactory(exaMetadata, properties);
            final SqlDialect dialect = createDialect(connectionFactory, properties);
            final String importFromPushdownQuery = dialect.rewriteQuery(request.getSelect(),
                    request.getSelectListDataTypes(), exaMetadata);
            return PushDownResponse.builder().pushDownSql(importFromPushdownQuery).build();
        } catch (final SQLException exception) {
            throw new AdapterException(ExaError.messageBuilder("E-VSCJDBC-27")
                    .message("Unable to execute push-down request. Cause: {{cause|uq}}", exception.getMessage())
                    .toString(), exception);
        }
    }
}