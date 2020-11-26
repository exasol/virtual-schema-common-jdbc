package com.exasol.adapter.dialects;

import java.sql.SQLException;

import com.exasol.adapter.jdbc.RemoteMetadataReader;

/**
 * Implementation of {@link BaseQueryRewriter} to generate `IMPORT FROM JDBC` queries.
 */
public class ImportFromJDBCQueryRewriter extends BaseQueryRewriter {

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
    protected String generatePushdownSql(final String connectionDefinition, final String query) throws SQLException {
        return "IMPORT FROM JDBC " + connectionDefinition + " STATEMENT '" + query.replace("'", "''") + "'";
    }

}
