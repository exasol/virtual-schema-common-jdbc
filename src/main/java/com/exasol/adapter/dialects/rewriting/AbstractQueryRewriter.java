package com.exasol.adapter.dialects.rewriting;

import java.sql.SQLException;
import java.util.logging.Logger;

import com.exasol.*;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.*;
import com.exasol.adapter.jdbc.*;
import com.exasol.adapter.sql.SqlStatement;
import com.exasol.errorreporting.ExaError;

/**
 * Abstract implementation of {@link QueryRewriter}.
 */
public abstract class AbstractQueryRewriter implements QueryRewriter {
    private static final Logger LOGGER = Logger.getLogger(AbstractQueryRewriter.class.getName());
    protected final SqlDialect dialect;
    protected final RemoteMetadataReader remoteMetadataReader;
    protected final ConnectionDefinitionBuilder connectionDefinitionBuilder;

    /**
     * Create a new instance of a {@link AbstractQueryRewriter}.
     *
     * @param dialect              dialect
     * @param remoteMetadataReader remote metadata reader
     */
    protected AbstractQueryRewriter(final SqlDialect dialect, final RemoteMetadataReader remoteMetadataReader) {
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
        final String pushdownQuery = createPushdownQuery(statement, properties);
        final ExaConnectionInformation exaConnectionInformation = getConnectionInformation(exaMetadata, properties);
        final String connectionDefinition = this.connectionDefinitionBuilder.buildConnectionDefinition(properties,
                exaConnectionInformation);
        final String importStatement = generateImportStatement(connectionDefinition, pushdownQuery);
        LOGGER.finer(() -> "Import push-down statement:\n" + importStatement);
        return importStatement;
    }

    private String createPushdownQuery(final SqlStatement statement, final AdapterProperties properties)
            throws AdapterException {
        final SqlGenerationContext context = new SqlGenerationContext(properties.getCatalogName(),
                properties.getSchemaName(), false);
        final SqlGenerator sqlGenerator = this.dialect.getSqlGenerator(context);
        final String pushdownQuery = sqlGenerator.generateSqlFor(statement);
        LOGGER.finer(() -> "Push-down query generated with " + sqlGenerator.getClass().getSimpleName() + ":\n"
                + pushdownQuery);
        return pushdownQuery;
    }

    protected ExaConnectionInformation getConnectionInformation(final ExaMetadata exaMetadata,
            final AdapterProperties properties) throws AdapterException {
        if (properties.hasConnectionName()) {
            final String connectionName = properties.getConnectionName();
            try {
                return exaMetadata.getConnection(connectionName);
            } catch (final ExaConnectionAccessException exception) {
                throw new AdapterException(ExaError.messageBuilder("E-VS-COM-JDBC-8").message(
                        "Unable to access information about the Exasol connection named {{connectionName}} trying to"
                                + " create a connection definition for rewritten query.")
                        .parameter("connectionName", connectionName).toString(), exception);
            }
        }
        return null;
    }

    /**
     * Generate an IMPORT statement to be executed in the Exasol database, using the passed pushdown query to be
     * executed in the external source be as source data.
     *
     * @param connectionDefinition connection definition to be used when connecting to the external source
     * @param pushdownQuery        source data for the `IMPORT...FROM` statement
     * @return IMPORT statement to be executed on the Exasol database
     * @throws SQLException if any problem occurs
     */
    protected abstract String generateImportStatement(final String connectionDefinition, final String pushdownQuery)
            throws SQLException;
}