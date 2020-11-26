package com.exasol.adapter.dialects;

import java.sql.SQLException;
import java.util.logging.Logger;

import com.exasol.adapter.jdbc.*;

/**
 * Implementation of {@link BaseQueryRewriter} to generate `IMPORT INTO FROM JDBC` queries.
 */
public class ImportIntoQueryRewriter extends BaseQueryRewriter {
    private static final Logger LOGGER = Logger.getLogger(ImportIntoQueryRewriter.class.getName());
    protected final ConnectionFactory connectionFactory;

    /**
     * Construct a new instance of {@link ImportIntoQueryRewriter}.
     *
     * @param dialect              dialect
     * @param remoteMetadataReader remote metadata reader
     * @param connectionFactory    factory for the JDBC connection to remote data source
     */
    public ImportIntoQueryRewriter(final SqlDialect dialect, final RemoteMetadataReader remoteMetadataReader,
            final ConnectionFactory connectionFactory) {
        super(dialect, remoteMetadataReader);
        this.connectionFactory = connectionFactory;
    }

    @Override
    protected String generatePushdownSql(final String connectionDefinition, final String query) throws SQLException {
        final String columnDescription = this.createImportColumnsDescription(query);
        return "IMPORT INTO (" + columnDescription + ") FROM JDBC " + connectionDefinition + " STATEMENT '"
                + query.replace("'", "''") + "'";
    }

    private String createImportColumnsDescription(final String query) throws SQLException {
        final ColumnMetadataReader columnMetadataReader = this.remoteMetadataReader.getColumnMetadataReader();
        final ResultSetMetadataReader resultSetMetadataReader = new ResultSetMetadataReader(
                this.connectionFactory.getConnection(), columnMetadataReader);
        final String columnsDescription = resultSetMetadataReader.describeColumns(query);
        LOGGER.finer(() -> "Import columns: " + columnsDescription);
        return columnsDescription;
    }
}
