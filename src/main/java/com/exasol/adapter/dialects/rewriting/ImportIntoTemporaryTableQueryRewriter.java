package com.exasol.adapter.dialects.rewriting;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.jdbc.*;
import com.exasol.adapter.metadata.DataType;

/**
 * Implementation of {@link AbstractQueryRewriter} to generate {@code IMPORT INTO (<columns description>) FROM JDBC}
 * queries.
 *
 * @see <a href="https://docs.exasol.com/sql/import.htm">https://docs.exasol.com/sql/import.htm</a>
 */
public class ImportIntoTemporaryTableQueryRewriter extends AbstractQueryRewriter {
    private static final Logger LOGGER = Logger.getLogger(ImportIntoTemporaryTableQueryRewriter.class.getName());
    /** JDBC connection factory */
    protected final ConnectionFactory connectionFactory;

    /**
     * Construct a new instance of {@link ImportIntoTemporaryTableQueryRewriter}.
     *
     * @param dialect              dialect
     * @param remoteMetadataReader remote metadata reader
     * @param connectionFactory    factory for the JDBC connection to remote data source
     */
    public ImportIntoTemporaryTableQueryRewriter(final SqlDialect dialect,
            final RemoteMetadataReader remoteMetadataReader, final ConnectionFactory connectionFactory) {
        this(dialect, remoteMetadataReader, connectionFactory, new BaseConnectionDefinitionBuilder());
    }

    /**
     * Construct a new instance of {@link ImportIntoTemporaryTableQueryRewriter}.
     *
     * @param dialect                     dialect
     * @param remoteMetadataReader        remote metadata reader
     * @param connectionFactory           factory for the JDBC connection to remote data source
     * @param connectionDefinitionBuilder custom connection definition builder
     */
    public ImportIntoTemporaryTableQueryRewriter(final SqlDialect dialect,
            final RemoteMetadataReader remoteMetadataReader, final ConnectionFactory connectionFactory,
            final ConnectionDefinitionBuilder connectionDefinitionBuilder) {
        super(dialect, remoteMetadataReader, connectionDefinitionBuilder);
        this.connectionFactory = connectionFactory;
    }

    /*
     * If selectListDataTypes is provided then use columns description from there otherwise infer columns description
     * from query.
     */
    @Override
    protected String generateImportStatement(final String connectionDefinition,
            final List<DataType> selectListDataTypes, final String pushdownQuery) throws SQLException {
        final String columnsDescription = (selectListDataTypes.isEmpty() //
                ? createColumnsDescriptionFromQuery(pushdownQuery)
                : createColumnsDescriptionFromExpectedResult(selectListDataTypes));
        return "IMPORT INTO (" + columnsDescription + ") FROM JDBC " + connectionDefinition + " STATEMENT '"
                + pushdownQuery.replace("'", "''") + "'";
    }

    private String createColumnsDescriptionFromQuery(final String query) throws SQLException {
        final ColumnMetadataReader columnMetadataReader = this.remoteMetadataReader.getColumnMetadataReader();
        final ResultSetMetadataReader resultSetMetadataReader = new ResultSetMetadataReader(
                this.connectionFactory.getConnection(), columnMetadataReader);
        final String columnsDescription = resultSetMetadataReader.describeColumns(query);
        LOGGER.finer(() -> "Import columns: " + columnsDescription);
        return columnsDescription;
    }

    private String createColumnsDescriptionFromExpectedResult(final List<DataType> types) {
        final StringBuilder builder = new StringBuilder();
        int columnNumber = 1;
        for (final DataType type : types) {
            if (columnNumber > 1) {
                builder.append(", ");
            }
            builder.append("c");
            builder.append(columnNumber);
            builder.append(" ");
            builder.append(type.toString());
            ++columnNumber;
        }
        return builder.toString();
    }
}