package com.exasol.adapter.jdbc;

import static com.exasol.adapter.jdbc.RemoteMetadataReaderConstants.ANY_TABLE;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.adapternotes.SchemaAdapterNotes;
import com.exasol.adapter.adapternotes.SchemaAdapterNotesJsonConverter;
import com.exasol.adapter.dialects.IdentifierConverter;
import com.exasol.adapter.metadata.SchemaMetadata;
import com.exasol.adapter.metadata.TableMetadata;
import com.exasol.errorreporting.ExaError;

/**
 * Contains a common part of remote metadata readers.
 */
public abstract class AbstractRemoteMetadataReader extends AbstractMetadataReader implements RemoteMetadataReader {
    private static final Logger LOGGER = Logger.getLogger(AbstractRemoteMetadataReader.class.getName());
    /** Column metadata reader */
    protected final ColumnMetadataReader columnMetadataReader;
    /** Table metadata reader */
    protected final TableMetadataReader tableMetadataReader;
    /** Identifier converter */
    protected final IdentifierConverter identifierConverter;

    /**
     * Create a new instance of {@link AbstractRemoteMetadataReader}.
     *
     * @param connection SQl connection
     * @param properties adapter properties
     * @param exaMetadata metadata of the Exasol database
     */
    protected AbstractRemoteMetadataReader(final Connection connection, final AdapterProperties properties,
                final ExaMetadata exaMetadata) {
        super(connection, properties, exaMetadata);
        this.identifierConverter = createIdentifierConverter();
        this.columnMetadataReader = createColumnMetadataReader();
        this.tableMetadataReader = createTableMetadataReader();
    }

    /**
     * Create a reader that handles column metadata.
     * <p>
     * Override this method in cases where a remote data source needs specific handling of column metadata
     *
     * @return column metadata reader
     */
    protected abstract ColumnMetadataReader createColumnMetadataReader();

    /**
     * Create a reader that handles table metadata.
     * <p>
     * Override this method in cases where a remote data source needs specific handling of table metadata
     *
     * @return table metadata reader
     */
    protected abstract TableMetadataReader createTableMetadataReader();

    /**
     * Create a converter that translates identifiers from the remote data source to the Exasol representation.
     *
     * @return identifier converter
     */
    protected abstract IdentifierConverter createIdentifierConverter();

    /**
     * Get the remote column metadata reader.
     *
     * @return column metadata reader
     */
    @Override
    public final ColumnMetadataReader getColumnMetadataReader() {
        return this.columnMetadataReader;
    }

    /**
     * Get the table metadata reader.
     *
     * @return table metadata reader
     */
    @Override
    public final TableMetadataReader getTableMetadataReader() {
        return this.tableMetadataReader;
    }

    /**
     * Get the identifier converter.
     *
     * @return identifier converter
     */
    public IdentifierConverter getIdentifierConverter() {
        return this.identifierConverter;
    }

    @Override
    public SchemaMetadata readRemoteSchemaMetadata() {
        return readRemoteSchemaMetadataForSelectedTables(Collections.emptyList());
    }

    @Override
    public SchemaMetadata readRemoteSchemaMetadata(final List<String> selectedTables) {
        return readRemoteSchemaMetadataForSelectedTables(selectedTables);
    }

    private SchemaMetadata readRemoteSchemaMetadataForSelectedTables(final List<String> selectedTables) {
        try {
            return this.getSchemaMetadata(selectedTables);
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException(ExaError.messageBuilder("E-VSCJDBC-21")
                    .message("Unable to read remote schema metadata. SQL error: {{exceptionMessage|uq}}",
                            exception.getMessage())
                    .toString(), exception);
        }
    }

    private SchemaMetadata getSchemaMetadata(final List<String> selectedTables) throws SQLException {
        return new SchemaMetadata(this.getJsonAdapterNotes(), this.getTableMetadata(selectedTables));
    }

    private String getJsonAdapterNotes() {
        return SchemaAdapterNotesJsonConverter.getInstance().convertToJson(getSchemaAdapterNotes());
    }

    private List<TableMetadata> getTableMetadata(final List<String> selectedTables) throws SQLException {
        final DatabaseMetaData remoteMetadata = this.connection.getMetaData();
        return this.extractTableMetadata(remoteMetadata, selectedTables);
    }

    private List<TableMetadata> extractTableMetadata(final DatabaseMetaData remoteMetadata,
            final List<String> selectedTables) throws SQLException {
        final String catalogName = this.getCatalogNameFilter();
        final String schemaName = this.getSchemaNameFilter();
        logTablesScan(catalogName, schemaName);
        try (final ResultSet remoteTables = remoteMetadata.getTables(catalogName, schemaName, ANY_TABLE,
                getTableTypeFilter())) {
            return this.tableMetadataReader.mapTables(remoteTables, selectedTables);
        }
    }

    /**
     * Get the table type filter.
     * 
     * @return supported table types
     */
    protected String[] getTableTypeFilter() {
        final Set<String> supportedTableTypes = getSupportedTableTypes();
        return ((supportedTableTypes == null) || supportedTableTypes.isEmpty()) //
                ? null //
                : supportedTableTypes.toArray(new String[0]);
    }

    @Override
    public Set<String> getSupportedTableTypes() {
        return RemoteMetadataReaderConstants.DEFAULT_SUPPORTED_TABLE_TYPES;
    }

    /**
     * Log the tables scanning.
     * 
     * @param catalogName catalog name
     * @param schemaName  schema name
     */
    protected void logTablesScan(final String catalogName, final String schemaName) {
        LOGGER.fine(() -> {
            final StringBuilder builder = new StringBuilder("Scanning ");
            if (catalogName == null) {
                builder.append("any catalog, ");
            } else {
                builder.append("catalog \"");
                builder.append(catalogName);
                builder.append("\", ");
            }
            if (schemaName == null) {
                builder.append("any schema ");
            } else {
                builder.append("schema \"");
                builder.append(schemaName);
                builder.append("\" ");
            }
            builder.append("for contained tables of the following supported types: ");
            builder.append(String.join(", ", getSupportedTableTypes()));
            return builder.toString();
        });
    }

    @Override
    public SchemaAdapterNotes getSchemaAdapterNotes() {
        try {
            final DatabaseMetaData metadata = this.connection.getMetaData();
            return SchemaAdapterNotes.builder() //
                    .catalogSeparator(metadata.getCatalogSeparator()) //
                    .identifierQuoteString(metadata.getIdentifierQuoteString()) //
                    .storesLowerCaseIdentifiers(metadata.storesLowerCaseIdentifiers()) //
                    .storesUpperCaseIdentifiers(metadata.storesUpperCaseIdentifiers()) //
                    .storesMixedCaseIdentifiers(metadata.storesMixedCaseIdentifiers()) //
                    .supportsMixedCaseIdentifiers(metadata.supportsMixedCaseIdentifiers()) //
                    .storesLowerCaseQuotedIdentifiers(metadata.storesLowerCaseQuotedIdentifiers()) //
                    .storesUpperCaseQuotedIdentifiers(metadata.storesUpperCaseQuotedIdentifiers()) //
                    .storesMixedCaseQuotedIdentifiers(metadata.storesMixedCaseQuotedIdentifiers()) //
                    .supportsMixedCaseQuotedIdentifiers(metadata.supportsMixedCaseQuotedIdentifiers()) //
                    .areNullsSortedAtEnd(metadata.nullsAreSortedAtEnd()) //
                    .areNullsSortedAtStart(metadata.nullsAreSortedAtStart()) //
                    .areNullsSortedHigh(metadata.nullsAreSortedHigh()) //
                    .areNullsSortedLow(metadata.nullsAreSortedLow()) //
                    .build();
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException(
                    ExaError.messageBuilder("E-VSCJDBC-22")
                            .message("Unable to create schema adapter notes from remote schema.").toString(),
                    exception);
        }
    }
}