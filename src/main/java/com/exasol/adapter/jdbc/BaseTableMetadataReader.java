package com.exasol.adapter.jdbc;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.IdentifierConverter;
import com.exasol.adapter.metadata.ColumnMetadata;
import com.exasol.adapter.metadata.TableMetadata;

/**
 * This class maps metadata of tables from the remote source to Exasol.
 */
public class BaseTableMetadataReader extends AbstractMetadataReader implements TableMetadataReader {
    static final String NAME_COLUMN = "TABLE_NAME";
    static final String REMARKS_COLUMN = "REMARKS";
    static final String DEFAULT_TABLE_ADAPTER_NOTES = "";
    private static final Logger LOGGER = Logger.getLogger(BaseTableMetadataReader.class.getName());
    private static final Pattern UNQUOTED_IDENTIFIER_PATTERN = Pattern.compile("^[a-z][0-9a-z_]*");
    private static final int DEFAULT_MAX_MAPPED_TABLE_LIST_SIZE = 1000;
    protected ColumnMetadataReader columnMetadataReader;
    private final IdentifierConverter identifierConverter;

    /**
     * Create a new instance of a {@link TableMetadata}.
     *
     * @param connection           JDBC connection to remote data source
     * @param columnMetadataReader reader to be used to map the tables columns
     * @param properties           user-defined adapter properties
     * @param identifierConverter  converter between source and Exasol identifiers
     */
    public BaseTableMetadataReader(final Connection connection, final ColumnMetadataReader columnMetadataReader,
            final AdapterProperties properties, final IdentifierConverter identifierConverter) {
        super(connection, properties);
        this.columnMetadataReader = columnMetadataReader;
        this.identifierConverter = identifierConverter;
    }

    @Override
    public List<TableMetadata> mapTables(final ResultSet remoteTables, final List<String> selectedTables)
            throws SQLException {
        if (remoteTables.next()) {
            return extractTableMetadata(remoteTables, selectedTables);
        } else {
            LOGGER.warning(() -> "Table scan did not find any tables. This can mean that either" //
                    + " a) the source does not contain tables (yet)," + " b) the table type is not supported" //
                    + " c) the table scan filter criteria is incorrect or" //
                    + " d) the user does not have access permissions." //
                    + " Please check that the source actually contains tables. " //
                    + " Also check the spelling and exact case of any catalog or schema name you provided.");
            return Collections.emptyList();
        }
    }

    private List<TableMetadata> extractTableMetadata(final ResultSet remoteTables, final List<String> selectedTables)
            throws SQLException {
        final List<TableMetadata> translatedTables = new ArrayList<>();
        do {
            final String tableName = readTableName(remoteTables);
            if (tableIsSupported(selectedTables, tableName)) {
                final TableMetadata tableMetadata = mapTable(remoteTables, tableName);
                if (tableHasColumns(tableMetadata, tableName)) {
                    LOGGER.finer(() -> "Read table metadata: " + tableMetadata.describe());
                    translatedTables.add(tableMetadata);
                    validateSelectedTablesListSize(selectedTables);
                }
            }
        } while (remoteTables.next());
        return translatedTables;
    }

    private void validateSelectedTablesListSize(final List<String> selectedTables) {
        if (selectedTables.size() > DEFAULT_MAX_MAPPED_TABLE_LIST_SIZE) {
            throw new RemoteMetadataReaderException(
                    "The size of the list of the selected tables exceeded the default allowed maximum: "
                            + DEFAULT_MAX_MAPPED_TABLE_LIST_SIZE + ". "
                            + "Please, use the TABLE_FILTER property to define the list of tables you need.");
        }
    }

    protected boolean tableIsSupported(final List<String> selectedTables, final String tableName) {
        if (isTableIncludedByMapping(tableName)) {
            return tableIsSelected(selectedTables, tableName);
        } else {
            logSkippingUnsupportedTable(tableName);
            return false;
        }
    }

    private boolean tableIsSelected(final List<String> selectedTables, final String tableName) {
        if (isTableSelected(tableName, selectedTables)) {
            return tableIsNotInExclusionsList(tableName);
        } else {
            LOGGER.fine(() -> "Skipping filtered out table \"" + tableName + "\" when mapping remote metadata.");
            return false;
        }
    }

    @Override
    public boolean isTableIncludedByMapping(final String tableName) {
        return true;
    }

    protected boolean isTableSelected(final String tableName, final List<String> selectedTables) {
        return (selectedTables == null) || selectedTables.isEmpty() || selectedTables.contains(tableName);
    }

    protected boolean tableIsNotInExclusionsList(final String tableName) {
        if (isTableFilteredOut(tableName)) {
            logSkippingFilteredTable(tableName);
            return false;
        } else {
            return true;
        }
    }

    private boolean isTableFilteredOut(final String tableName) {
        final List<String> filteredTables = this.properties.getFilteredTables();
        if (filteredTables.isEmpty()) {
            return false;
        } else {
            return !filteredTables.contains(tableName);
        }
    }

    protected void logSkippingFilteredTable(final String tableName) {
        LOGGER.fine(
                () -> "Skipping table \"" + tableName + "\" when mapping remote data due to user-defined table filter");
    }

    protected boolean tableHasColumns(final TableMetadata tableMetadata, final String tableName) {
        if (tableMetadata.getColumns().isEmpty()) {
            logSkippingTableWithEmptyColumns(tableName);
            return false;
        } else {
            return true;
        }
    }

    protected void logSkippingTableWithEmptyColumns(final String tableName) {
        LOGGER.fine(() -> "Not mapping table \"" + tableName + "\" because it has no columns."
                + " This can happen if the view containing the columns is invalid"
                + " or if the Virtual Schema adapter does not support mapping the column types.");
    }

    protected TableMetadata mapTable(final ResultSet table, final String tableName) throws SQLException {
        final String comment = Optional.ofNullable(readComment(table)).orElse("");
        final List<ColumnMetadata> columns = this.columnMetadataReader.mapColumns(tableName);
        final String adapterNotes = DEFAULT_TABLE_ADAPTER_NOTES;
        return new TableMetadata(adjustIdentifierCase(tableName), adapterNotes, columns, comment);
    }

    private String adjustIdentifierCase(final String tableName) {
        return this.identifierConverter.convert(tableName);
    }

    protected String readTableName(final ResultSet remoteTables) throws SQLException {
        return remoteTables.getString(NAME_COLUMN);
    }

    protected String readComment(final ResultSet remoteTables) throws SQLException {
        return remoteTables.getString(REMARKS_COLUMN);
    }

    protected boolean isUnquotedIdentifier(final String identifier) {
        return UNQUOTED_IDENTIFIER_PATTERN.matcher(identifier).matches();
    }

    protected void logSkippingUnsupportedTable(final String tableName) {
        LOGGER.fine(() -> "Skipping unsupported table \"" + tableName + "\" when mapping remote metadata.");
    }
}
