package com.exasol.adapter.jdbc;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.IdentifierConverter;
import com.exasol.adapter.metadata.ColumnMetadata;
import com.exasol.adapter.metadata.TableMetadata;
import com.exasol.adapter.properties.TableCountLimit;
import com.exasol.errorreporting.ExaError;

/**
 * This class maps metadata of tables from the remote source to Exasol.
 */
public class BaseTableMetadataReader extends AbstractMetadataReader implements TableMetadataReader {
    static final String NAME_COLUMN = "TABLE_NAME";
    static final String REMARKS_COLUMN = "REMARKS";
    /**
     * Default adapter notes to be added to tables (empty)
     */
    static final String DEFAULT_TABLE_ADAPTER_NOTES = "";
    private static final Logger LOGGER = Logger.getLogger(BaseTableMetadataReader.class.getName());
    private static final Pattern UNQUOTED_IDENTIFIER_PATTERN = Pattern.compile("^[a-z][0-9a-z_]*");

    /**
     * Column metadata reader
     */
    protected ColumnMetadataReader columnMetadataReader;
    private final IdentifierConverter identifierConverter;

    /**
     * Create a new instance of a {@link TableMetadata}.
     *
     * @param connection           JDBC connection to remote data source
     * @param columnMetadataReader reader to be used to map the tables columns
     * @param properties           user-defined adapter properties
     * @param exaMetadata          metadata of the Exasol database
     * @param identifierConverter  converter between source and Exasol identifiers
     */
    public BaseTableMetadataReader(final Connection connection, final ColumnMetadataReader columnMetadataReader,
                final AdapterProperties properties, final ExaMetadata exaMetadata,
                final IdentifierConverter identifierConverter) {
        super(connection, properties, exaMetadata);
        this.columnMetadataReader = columnMetadataReader;
        this.identifierConverter = identifierConverter;
    }

    @Override
    public List<TableMetadata> mapTables(final ResultSet remoteTables, final List<String> filteredTables)
            throws SQLException {
        if (remoteTables.next()) {
            return extractTableMetadata(remoteTables, filteredTables);
        } else {
            LOGGER.warning(() -> ExaError.messageBuilder("W-VSCJDBC-35")
                    .message("Table scan did not find any tables. This can mean that either" //
                            + " a) the source does not contain tables (yet)," + " b) the table type is not supported" //
                            + " c) the table scan filter criteria is incorrect or" //
                            + " d) the user does not have access permissions.")
                    .mitigation("Please check that the source actually contains tables. " //
                            + " Also check the spelling and exact case of any catalog or schema name you provided.")
                    .toString());
            return Collections.emptyList();
        }
    }

    private List<TableMetadata> extractTableMetadata(final ResultSet remoteTables, final List<String> filteredTables)
            throws SQLException {
        final List<TableMetadata> mappedTables = new ArrayList<>();
        final TableCountLimit tableCountLimit = TableCountLimit.from(this.properties);
        do {
            final Optional<TableMetadata> tableMetadata = getTableMetadata(remoteTables, filteredTables);
            tableMetadata.ifPresent(mappedTables::add);
            tableCountLimit.validateNumberOfTables(mappedTables.size());
        } while (remoteTables.next());
        return mappedTables;
    }

    private Optional<TableMetadata> getTableMetadata(final ResultSet remoteTables, final List<String> filteredTables)
            throws SQLException {
        final String tableName = readTableName(remoteTables);
        if (isTableSupported(filteredTables, tableName)) {
            return getTableMetadata(remoteTables, tableName);
        }
        return Optional.empty();
    }

    /**
     * Read the table name from a result set.
     *
     * @param remoteTables result set
     * @return table name
     * @throws SQLException if something goes wrong
     */
    protected String readTableName(final ResultSet remoteTables) throws SQLException {
        return remoteTables.getString(NAME_COLUMN);
    }

    private Optional<TableMetadata> getTableMetadata(final ResultSet remoteTables, final String tableName)
            throws SQLException {
        final TableMetadata tableMetadata = mapTable(remoteTables, tableName);
        if (tableHasColumns(tableMetadata)) {
            return Optional.of(tableMetadata);
        } else {
            logSkippingTableWithEmptyColumns(tableName);
            return Optional.empty();
        }
    }

    /**
     * Read the table metadata from a result set.
     *
     * @param table     result set
     * @param tableName table to read (name)
     * @return table metadata
     * @throws SQLException if reading fails
     */
    protected TableMetadata mapTable(final ResultSet table, final String tableName) throws SQLException {
        final String comment = Optional.ofNullable(readComment(table)).orElse("");
        final List<ColumnMetadata> columns = this.columnMetadataReader.mapColumns(tableName);
        return new TableMetadata(adjustIdentifierCase(tableName), DEFAULT_TABLE_ADAPTER_NOTES, columns, comment);
    }

    /**
     * Convert the given identifier to the proper casing using the underlying {@link IdentifierConverter}.
     *
     * @param tableName Table name as provided by source
     * @return Table name as required by the virtual schema host database
     */
    protected String adjustIdentifierCase(final String tableName) {
        return this.identifierConverter.convert(tableName);
    }

    /**
     * Read the comment from a result set.
     *
     * @param remoteTables result set
     * @return comment
     * @throws SQLException if something goes wrong
     */
    protected String readComment(final ResultSet remoteTables) throws SQLException {
        return remoteTables.getString(REMARKS_COLUMN);
    }

    /**
     * Check if a table has columns.
     *
     * @param tableMetadata table metadata
     * @return {@code true} if table has columns
     */
    protected boolean tableHasColumns(final TableMetadata tableMetadata) {
        return !tableMetadata.getColumns().isEmpty();
    }

    /**
     * Check if a table is supported.
     *
     * @param filteredTables filtered tables
     * @param tableName      table to check
     * @return {@code true} if table is supported
     */
    protected boolean isTableSupported(final List<String> filteredTables, final String tableName) {
        if (isTableIncludedByMapping(tableName)) {
            return isFilteredTable(filteredTables, tableName);
        } else {
            logSkippingUnsupportedTable(tableName);
            return false;
        }
    }

    @Override
    public boolean isTableIncludedByMapping(final String tableName) {
        return true;
    }

    /**
     * Log the skipping of an unsupported table.
     *
     * @param tableName table name
     */
    protected void logSkippingUnsupportedTable(final String tableName) {
        LOGGER.fine(() -> "Skipping unsupported table \"" + tableName + "\" when mapping remote metadata.");
    }

    private boolean isFilteredTable(final List<String> filteredTables, final String tableName) {
        if (isFilteredByProperties(tableName) && isFiltered(tableName, filteredTables)) {
            return true;
        } else {
            LOGGER.fine(() -> "Skipping filtered out table \"" + tableName
                    + "\" when mapping remote metadata due to request properties or user-defined table filter.");
            return false;
        }
    }

    private boolean isFilteredByProperties(final String tableName) {
        return this.isFiltered(tableName, this.properties.getFilteredTables());
    }

    /**
     * Check if a given table is filtered.
     *
     * @param tableName      table to check
     * @param filteredTables list of filtered tables
     * @return {@code true} if the table is filtered
     */
    private boolean isFiltered(final String tableName, final List<String> filteredTables) {
        return includeAllTables(filteredTables) || filteredTables.contains(tableName);
    }

    /**
     * Check if no tables are filtered.
     *
     * @param filteredTables filtered tables
     * @return {@code true} if no filter is applied
     */
    protected boolean includeAllTables(final List<String> filteredTables) {
        return (filteredTables == null) || filteredTables.isEmpty();
    }

    /**
     * Log the skipping of a table with no columns.
     *
     * @param tableName table name
     */
    protected void logSkippingTableWithEmptyColumns(final String tableName) {
        LOGGER.fine(() -> "Not mapping table \"" + tableName + "\" because it has no columns."
                + " This can happen if the view containing the columns is invalid"
                + " or if the Virtual Schema adapter does not support mapping the column types.");
    }

    /**
     * Check if an identifier is unquoted.
     *
     * @param identifier identifier to check
     * @return {@code true} if it's unquoted
     */
    protected boolean isUnquotedIdentifier(final String identifier) {
        return UNQUOTED_IDENTIFIER_PATTERN.matcher(identifier).matches();
    }
}
