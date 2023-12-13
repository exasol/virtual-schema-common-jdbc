package com.exasol.adapter.dialects.rewriting;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import com.exasol.*;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.*;
import com.exasol.adapter.jdbc.ConnectionDefinitionBuilder;
import com.exasol.adapter.jdbc.RemoteMetadataReader;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.properties.DataTypeDetection;
import com.exasol.adapter.properties.DataTypeDetection.Strategy;
import com.exasol.adapter.sql.SqlStatement;
import com.exasol.errorreporting.ExaError;

/**
 * Abstract implementation of {@link QueryRewriter}.
 */
public abstract class AbstractQueryRewriter implements QueryRewriter {
        private static final Logger LOGGER = Logger.getLogger(AbstractQueryRewriter.class.getName());
        /** Dialect implementation */
        protected final SqlDialect dialect;
        /** Remote metadata reader */
        protected final RemoteMetadataReader remoteMetadataReader;
        /** Connection definition builder */
        protected final ConnectionDefinitionBuilder connectionDefinitionBuilder;

        /**
         * Create a new instance of a {@link AbstractQueryRewriter}.
         *
         * @param dialect                     dialect
         * @param remoteMetadataReader        remote metadata reader
         * @param connectionDefinitionBuilder builder for creating connection definitions
         */
        protected AbstractQueryRewriter(final SqlDialect dialect, final RemoteMetadataReader remoteMetadataReader,
                        final ConnectionDefinitionBuilder connectionDefinitionBuilder) {
                this.dialect = dialect;
                this.remoteMetadataReader = remoteMetadataReader;
                this.connectionDefinitionBuilder = connectionDefinitionBuilder;
        }

        @Override
        public String rewrite(final SqlStatement statement, final List<DataType> selectListDataTypes,
                        final ExaMetadata exaMetadata, final AdapterProperties properties)
                        throws AdapterException, SQLException {
                final String pushdownQuery = createPushdownQuery(statement, properties);
                final ExaConnectionInformation exaConnectionInformation = getConnectionInformation(exaMetadata,
                                properties);
                final String connectionDefinition = this.connectionDefinitionBuilder
                                .buildConnectionDefinition(properties, exaConnectionInformation);
                if (calculateDatatypes(selectListDataTypes, properties)) {
                        final String importStatement = generateImportStatement(connectionDefinition,
                                        selectListDataTypes, pushdownQuery);
                        LOGGER.finer(() -> "Import push-down statement:\n" + importStatement);
                        return importStatement;
                } else {
                        throw new AdapterException(ExaError.messageBuilder("E-VSCJDBC-46").message(
                                        "Property `IMPORT_DATA_TYPES` value 'FROM_RESULT_SET' is no longer supported.")
                                        .mitigation("Please remove the `IMPORT_DATA_TYPES` property from the virtual schema so the default value 'EXASOL_CALCULATED' is used.")
                                        .toString());
                }
        }

        private boolean calculateDatatypes(final List<DataType> selectListDataTypes,
                        final AdapterProperties properties) {
                return (DataTypeDetection.from(properties).getStrategy() == Strategy.EXASOL_CALCULATED)
                                && !selectListDataTypes.isEmpty();
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

        /**
         * Read the connection information from the metadata.
         *
         * @param exaMetadata metadata
         * @param properties  adapter properties
         * @return connection information
         * @throws AdapterException if access fails
         */
        protected ExaConnectionInformation getConnectionInformation(final ExaMetadata exaMetadata,
                        final AdapterProperties properties) throws AdapterException {
                if (properties.hasConnectionName()) {
                        final String connectionName = properties.getConnectionName();
                        try {
                                return exaMetadata.getConnection(connectionName);
                        } catch (final ExaConnectionAccessException exception) {
                                throw new AdapterException(ExaError.messageBuilder("E-VSCJDBC-8").message(
                                                "Unable to access information about the Exasol connection named {{connectionName}} trying to"
                                                                + " create a connection definition for rewritten query.")
                                                .parameter("connectionName", connectionName).toString(), exception);
                        }
                }
                return null;
        }

        /**
         * This method provides backwards compatibility. A class extending the {@link AbstractQueryRewriter} has two
         * options:
         * <ul>
         * <li>Option (O1): override the new method {@link #generateImportStatement(String, List, String)} enabling to
         * use the new parameter {@code selectListDataTypes} to determine the data types of the result set of the
         * query.</li>
         * <li>Option (O2): only override the old method {@link #generateImportStatement(String, String)} and by that
         * falling back to the old way, i.e. inferring the data types of the result set by inspecting its values.</li>
         * </ul>
         *
         * @param connectionDefinition connection definition to be used when connecting to the external source
         * @param selectListDataTypes  expected data types of result set
         * @param pushdownQuery        source data for the `IMPORT...FROM` statement
         * @return IMPORT statement to be executed on the Exasol database
         * @throws SQLException if any problem occurs
         */
        protected String generateImportStatement(final String connectionDefinition,
                        final List<DataType> selectListDataTypes, final String pushdownQuery) throws SQLException {
                return generateImportStatement(connectionDefinition, pushdownQuery);
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