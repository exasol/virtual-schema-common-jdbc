package com.exasol.adapter.dialects.rewriting;

import java.sql.SQLException;
import java.util.List;

import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.jdbc.*;
import com.exasol.adapter.metadata.DataType;

/**
 * Implementation of {@link AbstractQueryRewriter} to generate {@code IMPORT FROM JDBC} queries.
 * 
 * @see <a href="https://docs.exasol.com/sql/import.htm">https://docs.exasol.com/sql/import.htm</a>
 */
public class ImportFromJDBCQueryRewriter extends AbstractQueryRewriter {
    /**
     * Construct a new instance of {@link ImportFromJDBCQueryRewriter}.
     *
     * @param dialect              dialect
     * @param remoteMetadataReader remote metadata reader
     */
    public ImportFromJDBCQueryRewriter(final SqlDialect dialect, final RemoteMetadataReader remoteMetadataReader) {
        this(dialect, remoteMetadataReader, new BaseConnectionDefinitionBuilder());
    }

    /**
     * Construct a new instance of {@link ImportFromJDBCQueryRewriter}.
     *
     * @param dialect                     dialect
     * @param remoteMetadataReader        remote metadata reader
     * @param connectionDefinitionBuilder custom connection definition builder
     */
    public ImportFromJDBCQueryRewriter(final SqlDialect dialect, final RemoteMetadataReader remoteMetadataReader,
            final ConnectionDefinitionBuilder connectionDefinitionBuilder) {
        super(dialect, remoteMetadataReader, connectionDefinitionBuilder);
    }

    @Override
    protected String generateImportStatement(final String connectionDefinition, List<DataType> selectListDataTypes, final String pushdownQuery)
            throws SQLException {
        return "IMPORT FROM JDBC " + connectionDefinition + " STATEMENT '" + pushdownQuery.replace("'", "''") + "'";
    }
}