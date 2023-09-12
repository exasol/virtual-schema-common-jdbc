package com.exasol.adapter.jdbc;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

import com.exasol.ExaMetadata;
import com.exasol.adapter.*;
import com.exasol.adapter.capabilities.*;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.SqlDialectFactory;
import com.exasol.adapter.metadata.SchemaMetadata;
import com.exasol.adapter.metadata.SchemaMetadataInfo;
import com.exasol.adapter.properties.PropertyValidationException;
import com.exasol.adapter.properties.TableCountLimit;
import com.exasol.adapter.request.*;
import com.exasol.adapter.response.*;
import com.exasol.errorreporting.ExaError;

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
    private RemoteConnectionFactory connectionFactory = null;

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
        try {
            final SqlDialect dialect = createDialectAndValidateProperties(exasolMetadata, properties);
            final SchemaMetadata remoteMeta = getRemoteMetadata(dialect, properties.getFilteredTables());
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
        LOGGER.fine(() -> "Received request to create Virtual Schema \"" + request.getVirtualSchemaName() + "\".");
    }

    private AdapterProperties getPropertiesFromRequest(final AdapterRequest request) {
        return new AdapterProperties(request.getSchemaMetadataInfo().getProperties());
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
            final AdapterProperties properties = getPropertiesFromRequest(request);
            final SqlDialect dialect = createDialectAndValidateProperties(metadata, properties);
            final SchemaMetadata remoteMetadata = request.refreshesOnlySelectedTables() //
                    ? dialect.readSchemaMetadata(request.getTables())
                    : getRemoteMetadata(dialect, properties.getFilteredTables());
            return RefreshResponse.builder().schemaMetadata(remoteMetadata).build();
        } catch (final SQLException | PropertyValidationException exception) {
            throw new AdapterException(ExaError.messageBuilder("E-VSCJDBC-26").message(
                    "Unable refresh metadata of Virtual Schema \"{{virtualSchemaName|uq}}\". Cause: {{cause|uq}}",
                    request.getSchemaMetadataInfo().getSchemaName(), exception.getMessage()).toString(), exception);
        }
    }

    private SchemaMetadata getRemoteMetadata(final SqlDialect sqlDialect, final List<String> tables)
            throws SQLException {
        if (tables.isEmpty()) {
            return sqlDialect.readSchemaMetadata();
        } else {
            return sqlDialect.readSchemaMetadata(tables);
        }
    }

    /**
     * Read the schema metadata.
     *
     * @param properties           adapter properties
     * @param remoteTableAllowList allow list for remote tables
     * @param exasolMetadata       ExaMetadata
     * @return schema metadata
     * @throws PropertyValidationException if properties are invalid
     *
     * @deprecated Please do not use this method.
     */
    @Deprecated(since = "10.2.0")
    protected SchemaMetadata readMetadata(final AdapterProperties properties, final List<String> remoteTableAllowList,
            final ExaMetadata exasolMetadata) throws PropertyValidationException {
        final SqlDialect dialect = createDialectAndValidateProperties(exasolMetadata, properties);
        return dialect.readSchemaMetadata(remoteTableAllowList);
    }

    @Override
    public SetPropertiesResponse setProperties(final ExaMetadata metadata, final SetPropertiesRequest request)
            throws AdapterException {
        final SchemaMetadataInfo schemaMetadataInfo = request.getSchemaMetadataInfo();
        final Map<String, String> requestRawProperties = request.getProperties();
        final Map<String, String> mergedRawProperties = mergeProperties(schemaMetadataInfo.getProperties(),
                requestRawProperties);
        final AdapterProperties mergedProperties = new AdapterProperties(mergedRawProperties);
        final SqlDialect dialect = createDialectAndValidateProperties(metadata, mergedProperties);

        if (requiresRefreshOfVirtualSchema(requestRawProperties)) {
            final List<String> tableFilter = getTableFilter(mergedRawProperties);
            final SchemaMetadata remoteMeta = dialect.readSchemaMetadata(tableFilter);
            return SetPropertiesResponse.builder().schemaMetadata(remoteMeta).build();
        } else {
            return SetPropertiesResponse.builder().schemaMetadata(null).build();
        }
    }

    private boolean requiresRefreshOfVirtualSchema(final Map<String, String> properties) {
        return properties.containsKey(TableCountLimit.MAXTABLES_PROPERTY)
                || AdapterProperties.isRefreshingVirtualSchemaRequired(properties);
    }

    // TODO: can metadata and properties be changed during connection lifetime? If yes, our connection factory
    //  is implemented wrongly
    private RemoteConnectionFactory getOrCreateConnectionFactory(final ExaMetadata metadata,
                                                                 final AdapterProperties properties) {
        if (this.connectionFactory == null)
            this.connectionFactory = new RemoteConnectionFactory(metadata, properties);
        return this.connectionFactory;
    }

    private SqlDialect createDialectAndValidateProperties(final ExaMetadata metadata,
            final AdapterProperties properties) throws PropertyValidationException {
        final SqlDialect dialect = createDialect(metadata, properties);
        dialect.validateProperties();
        return dialect;
    }

    private SqlDialect createDialect(final ExaMetadata metadata, final AdapterProperties properties) {
        final ConnectionFactory factory = this.getOrCreateConnectionFactory(metadata, properties);
        return this.sqlDialectFactory.createSqlDialect(factory, properties);
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
        final SqlDialect dialect = createDialect(exaMetadata, properties);
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
            final SqlDialect dialect = createDialect(exaMetadata, properties);
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