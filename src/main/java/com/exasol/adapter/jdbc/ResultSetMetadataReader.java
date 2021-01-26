package com.exasol.adapter.jdbc;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.exasol.adapter.metadata.DataType;
import com.exasol.errorreporting.ExaError;

/**
 * This class creates a textual description of the result columns of a push-down query.
 * <p>
 * The columns description is necessary to prepare the <code>IMPORT</code> statement in which the push-down query is
 * executed.
 */
public class ResultSetMetadataReader {
    private static final Logger LOGGER = Logger.getLogger(ResultSetMetadataReader.class.getName());
    private final Connection connection;
    private final ColumnMetadataReader columnMetadataReader;

    /**
     * Create a new instance of a {@link ResultSetMetadataReader}.
     *
     * @param connection           connection to the remote data source
     * @param columnMetadataReader column metadata reader used to translate the column types
     */
    public ResultSetMetadataReader(final Connection connection, final ColumnMetadataReader columnMetadataReader) {
        this.connection = connection;
        this.columnMetadataReader = columnMetadataReader;
    }

    /**
     * Generate a textual description of the result columns of the push-down query.
     *
     * @param query push-down query
     * @return string describing the columns (names and types)
     */
    public String describeColumns(final String query) {
        LOGGER.fine(() -> "Generating columns description for push-down query using "
                + this.columnMetadataReader.getClass().getSimpleName() + ":\n" + query);
        try (final PreparedStatement statement = this.connection.prepareStatement(query)) {
            final ResultSetMetaData metadata = statement.getMetaData();
            final List<DataType> types = mapResultMetadataToExasolDataTypes(metadata);
            validateColumnTypes(types, query);
            final String columnsDescription = createColumnDescriptionFromDataTypes(types);
            LOGGER.fine(() -> "Columns description: " + columnsDescription);
            return columnsDescription;
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException(ExaError.messageBuilder("E-VS-COM-JDBC-30").message(
                    "Unable to read remote metadata for push-down query trying to generate result column description.")
                    .mitigation("Please, make sure that you provided valid CATALOG_NAME "
                            + "and SCHEMA_NAME properties if required. Caused by: {{cause}}")
                    .parameter("cause", exception.getMessage()).toString(), exception);
        }
    }

    private void validateColumnTypes(final List<DataType> types, final String query) {
        final List<Integer> illegalColumns = new ArrayList<>();
        int column = 1;
        for (final DataType type : types) {
            if (!type.isSupported()) {
                illegalColumns.add(column);
            }
            ++column;
        }
        if (!illegalColumns.isEmpty()) {
            throw new RemoteMetadataReaderException(ExaError.messageBuilder("E-VS-COM-JDBC-31")
                    .message("Unsupported data type(s) in column(s) in query: {{unsupportedColumns}}.")
                    .unquotedParameter("unsupportedColumns",
                            illegalColumns.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                    .mitigation("Please remove those columns from your query:\n{{query}}")
                    .unquotedParameter("query", query).toString());
        }
    }

    private String createColumnDescriptionFromDataTypes(final List<DataType> types) {
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

    private List<DataType> mapResultMetadataToExasolDataTypes(final ResultSetMetaData metadata) throws SQLException {
        final int columnCount = metadata.getColumnCount();
        final List<DataType> types = new ArrayList<>(columnCount);
        for (int columnNumber = 1; columnNumber <= columnCount; ++columnNumber) {
            final JDBCTypeDescription jdbcTypeDescription = getJdbcTypeDescription(metadata, columnNumber);
            final DataType type = this.columnMetadataReader.mapJdbcType(jdbcTypeDescription);
            types.add(type);
        }
        return types;
    }

    protected static JDBCTypeDescription getJdbcTypeDescription(final ResultSetMetaData metadata,
            final int columnNumber) throws SQLException {
        final int jdbcType = metadata.getColumnType(columnNumber);
        final int jdbcPrecisions = metadata.getPrecision(columnNumber);
        final int jdbcScales = metadata.getScale(columnNumber);
        return new JDBCTypeDescription(jdbcType, jdbcScales, jdbcPrecisions, 0,
                metadata.getColumnTypeName(columnNumber));
    }
}