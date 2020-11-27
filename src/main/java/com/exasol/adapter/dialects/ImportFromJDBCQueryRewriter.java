package com.exasol.adapter.dialects;

import java.sql.SQLException;

import com.exasol.adapter.jdbc.RemoteMetadataReader;

/**
 * Implementation of {@link AbstractQueryRewriter} to generate {@code IMPORT FROM JDBC} queries.
 */
public class ImportFromJDBCQueryRewriter extends AbstractQueryRewriter {

    /**
     * Construct a new instance of {@link ImportFromJDBCQueryRewriter}.
     *
     * @param dialect              dialect
     * @param remoteMetadataReader remote metadata reader
     */
    public ImportFromJDBCQueryRewriter(final SqlDialect dialect, final RemoteMetadataReader remoteMetadataReader) {
        super(dialect, remoteMetadataReader);
    }

    @Override
    protected String generateImportStatement(final String connectionDefinition, final String pushdownQuery)
            throws SQLException {
        return "IMPORT FROM JDBC " + connectionDefinition + " STATEMENT '" + pushdownQuery.replace("'", "''") + "'";
    }

}
