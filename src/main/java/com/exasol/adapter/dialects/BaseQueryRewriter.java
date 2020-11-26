package com.exasol.adapter.dialects;

import java.sql.SQLException;
import java.util.logging.Logger;

import com.exasol.*;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.jdbc.*;
import com.exasol.adapter.sql.SqlNodeVisitor;
import com.exasol.adapter.sql.SqlStatement;

/**
 * Base implementation of {@link QueryRewriter}.
 */
public abstract class BaseQueryRewriter implements QueryRewriter {
    private static final Logger LOGGER = Logger.getLogger(BaseQueryRewriter.class.getName());
    protected final SqlDialect dialect;
    protected final RemoteMetadataReader remoteMetadataReader;
    protected final ConnectionDefinitionBuilder connectionDefinitionBuilder;

    /**
     * Create a new instance of a {@link BaseQueryRewriter}.
     *
     * @param dialect              dialect
     * @param remoteMetadataReader remote metadata reader
     */
    protected BaseQueryRewriter(final SqlDialect dialect, final RemoteMetadataReader remoteMetadataReader) {
        this.dialect = dialect;
        this.remoteMetadataReader = remoteMetadataReader;
        this.connectionDefinitionBuilder = createConnectionDefinitionBuilder();
    }

    /**
     * Create the connection definition builder.
     * <p>
     * Override this method in case you need connection definitions that differ from the regular JDBC style.
     *
     * @return connection definition builder
     */
    protected ConnectionDefinitionBuilder createConnectionDefinitionBuilder() {
        return new BaseConnectionDefinitionBuilder();
    }

    @Override
    public String rewrite(final SqlStatement statement, final ExaMetadata exaMetadata,
            final AdapterProperties properties) throws AdapterException, SQLException {
        final String query = createPushdownQuery(statement, properties);
        final ExaConnectionInformation exaConnectionInformation = getConnectionInformation(exaMetadata, properties);
        final String connectionDefinition = this.connectionDefinitionBuilder.buildConnectionDefinition(properties,
                exaConnectionInformation);
        final String importFromPushdownQuery = generatePushdownSql(connectionDefinition, query);
        LOGGER.finer(() -> "Import from push-down query:\n" + importFromPushdownQuery);
        return importFromPushdownQuery;
    }

    private String createPushdownQuery(final SqlStatement statement, final AdapterProperties properties)
            throws AdapterException {
        final SqlGenerationContext context = new SqlGenerationContext(properties.getCatalogName(),
                properties.getSchemaName(), false);
        final SqlNodeVisitor<String> sqlGeneratorVisitor = this.dialect.getSqlGenerationVisitor(context);
        final String pushdownQuery = statement.accept(sqlGeneratorVisitor);
        LOGGER.finer(() -> "Push-down query generated with " + sqlGeneratorVisitor.getClass().getSimpleName() + ":\n"
                + pushdownQuery);
        return pushdownQuery;
    }

    protected ExaConnectionInformation getConnectionInformation(final ExaMetadata exaMetadata,
            final AdapterProperties properties) throws AdapterException {
        final ExaConnectionInformation exaConnectionInformation;
        if (properties.hasConnectionName()) {
            final String connectionName = properties.getConnectionName();
            try {
                exaConnectionInformation = exaMetadata.getConnection(connectionName);
            } catch (final ExaConnectionAccessException exception) {
                throw new AdapterException("Unable to access information about the Exasol connection named \""
                        + connectionName + "\" trying to create a connection definition for rewritten query.",
                        exception);
            }
        } else {
            exaConnectionInformation = null;
        }
        return exaConnectionInformation;
    }

    /**
     * Generate a query to be execute in the Exasol database, that wraps the passed query to be executed in an external
     * source.
     *
     * @param connectionDefinition the connection definition to be used when connecting to the external datasource
     * @param query                the query to be executed in the external data source
     * @return the query to be executed in the Exasol database
     * @throws SQLException if any problem occurs
     */
    protected abstract String generatePushdownSql(final String connectionDefinition, final String query)
            throws SQLException;

}