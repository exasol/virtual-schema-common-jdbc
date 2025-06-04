package com.exasol.adapter.jdbc;

import static com.exasol.adapter.jdbc.TableMetadataMockUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.*;
import java.util.*;

import com.exasol.ExaMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.adapternotes.SchemaAdapterNotes;
import com.exasol.adapter.dialects.IdentifierCaseHandling;
import com.exasol.adapter.metadata.*;

@ExtendWith(MockitoExtension.class)
class BaseRemoteMetadataReaderTest {
    private static final String IDENTIFIER_QUOTE_STRING = "identifier-quote-string";
    private static final String CATALOG_SEPARATOR = "catalog-separator";
    private static final String ESCAPE_STRING = "\\";

    @Mock
    private Connection connectionMock;

    @Mock
    private ExaMetadata exaMetadataMock;

    @BeforeEach
    void beforeEach() {
        lenient().when(exaMetadataMock.getDatabaseVersion()).thenReturn("8.34.0");
    }

    @Test
    void testReadEmptyRemoteMetadata() throws RemoteMetadataReaderException, SQLException {
        final DatabaseMetaData remoteMetadataMock = mockSupportingMetadata(false);
        setupConnectionMetadata(remoteMetadataMock);
        mockGetAllTablesReturnsEmptyList(remoteMetadataMock);
        assertThat(readMockedSchemaMetadata().getTables(), emptyIterableOf(TableMetadata.class));
    }

    private void mockGetAllTablesReturnsEmptyList(final DatabaseMetaData remoteMetadataMock) throws SQLException {
        final ResultSet remoteTablesMock = Mockito.mock(ResultSet.class);
        when(remoteTablesMock.next()).thenReturn(false);
        mockGetAllTables(remoteMetadataMock, remoteTablesMock);
    }

    private SchemaMetadata readMockedSchemaMetadata() {
        return readMockedSchemaMetadataWithProperties(AdapterProperties.emptyProperties());
    }

    private SchemaMetadata readMockedSchemaMetadataWithProperties(final AdapterProperties properties) {
        final RemoteMetadataReader reader = buildBaseRemoteMetadataReader(properties);
        return reader.readRemoteSchemaMetadata();
    }

    @Test
    void testReadRemoteMetadata() throws RemoteMetadataReaderException, SQLException {
        final DatabaseMetaData remoteMetadataMock = mockSupportingMetadata(true);
        setupConnectionMetadata(remoteMetadataMock);
        mockGetColumnsCalls(remoteMetadataMock);
        mockGetTableCalls(remoteMetadataMock);
        final SchemaMetadata metadata = readMockedSchemaMetadata();
        final List<TableMetadata> tables = metadata.getTables();
        final TableMetadata tableAMetadata = tables.get(0);
        final TableMetadata tableBMetadata = tables.get(1);
        final List<ColumnMetadata> columnsAMetadata = tableAMetadata.getColumns();
        final List<ColumnMetadata> columnsBMetadata = tableBMetadata.getColumns();
        assertAll(() -> assertThat(tables, iterableWithSize(2)),
                () -> assertThat(tableAMetadata.getName(), equalTo(TABLE_A)),
                () -> assertThat(columnsAMetadata, iterableWithSize(2)),
                () -> assertThat(tableBMetadata.getName(), equalTo(TABLE_B)),
                () -> assertThat(columnsBMetadata, iterableWithSize(3)));
    }

    private void mockGetColumnsCalls(final DatabaseMetaData remoteMetadataMock) throws SQLException {
        mockTableA(remoteMetadataMock);
        mockTableB(remoteMetadataMock);
    }

    private void mockTableA(final DatabaseMetaData remoteMetadataMock) throws SQLException {
        final ResultSet tableAColumns = Mockito.mock(ResultSet.class);
        when(tableAColumns.next()).thenReturn(true, true, false);
        when(tableAColumns.getString(BaseColumnMetadataReader.TYPE_NAME_COLUMN)).thenReturn("BOOLEAN", "DATE");
        when(tableAColumns.getString(BaseColumnMetadataReader.NAME_COLUMN)).thenReturn("COLUMN_A1", "COLUMN_A2");
        when(tableAColumns.getInt(BaseColumnMetadataReader.DATA_TYPE_COLUMN)).thenReturn(Types.BOOLEAN, Types.DATE);
        when(remoteMetadataMock.getColumns(any(), any(), eq(escapeSqlWildCards(TABLE_A)), any()))
                .thenReturn(tableAColumns);
    }

    private void mockTableB(final DatabaseMetaData remoteMetadataMock) throws SQLException {
        final ResultSet tableBColumns = Mockito.mock(ResultSet.class);
        when(tableBColumns.next()).thenReturn(true, true, true, false);
        when(tableBColumns.getString(BaseColumnMetadataReader.TYPE_NAME_COLUMN)).thenReturn("BOOLEAN", "DOUBLE");
        when(tableBColumns.getInt(BaseColumnMetadataReader.DATA_TYPE_COLUMN)).thenReturn(Types.BOOLEAN, Types.DOUBLE);
        when(tableBColumns.getString(BaseColumnMetadataReader.NAME_COLUMN)).thenReturn("COLUMN_B1", "COLUMN_B2",
                "COLUMN_B3");
        when(remoteMetadataMock.getColumns(any(), any(), eq(escapeSqlWildCards(TABLE_B)), any()))
                .thenReturn(tableBColumns);
    }

    private String escapeSqlWildCards(final String string) {
        return WildcardEscaper.instance(ESCAPE_STRING).escape(string);
    }

    private void mockGetTableCalls(final DatabaseMetaData remoteMetadataMock) throws SQLException {
        final ResultSet tablesMock = Mockito.mock(ResultSet.class);
        mockTableCount(tablesMock, 2);
        mockTableName(tablesMock, TABLE_A, TABLE_B);
        mockGetAllTables(remoteMetadataMock, tablesMock);
    }

    private void mockGetAllTables(final DatabaseMetaData remoteMetadataMock, final ResultSet tables)
            throws SQLException {
        when(remoteMetadataMock.getTables(any(), any(), any(), any())).thenReturn(tables);
    }

    protected DatabaseMetaData mockSupportingMetadata(final boolean mockGetSearchStringEscape) throws SQLException {
        final DatabaseMetaData remoteMetadataMock = Mockito.mock(DatabaseMetaData.class);
        when(remoteMetadataMock.getCatalogSeparator()).thenReturn(CATALOG_SEPARATOR);
        when(remoteMetadataMock.getIdentifierQuoteString()).thenReturn(IDENTIFIER_QUOTE_STRING);
        when(remoteMetadataMock.storesLowerCaseIdentifiers()).thenReturn(true);
        when(remoteMetadataMock.storesUpperCaseIdentifiers()).thenReturn(true);
        when(remoteMetadataMock.storesMixedCaseIdentifiers()).thenReturn(true);
        when(remoteMetadataMock.supportsMixedCaseIdentifiers()).thenReturn(true);
        when(remoteMetadataMock.storesLowerCaseQuotedIdentifiers()).thenReturn(true);
        when(remoteMetadataMock.storesUpperCaseQuotedIdentifiers()).thenReturn(true);
        when(remoteMetadataMock.storesMixedCaseQuotedIdentifiers()).thenReturn(true);
        when(remoteMetadataMock.supportsMixedCaseQuotedIdentifiers()).thenReturn(true);
        when(remoteMetadataMock.nullsAreSortedAtEnd()).thenReturn(true);
        when(remoteMetadataMock.nullsAreSortedAtStart()).thenReturn(true);
        when(remoteMetadataMock.nullsAreSortedHigh()).thenReturn(true);
        when(remoteMetadataMock.nullsAreSortedLow()).thenReturn(true);
        if (mockGetSearchStringEscape) {
            when(remoteMetadataMock.getSearchStringEscape()).thenReturn(ESCAPE_STRING);
        }
        return remoteMetadataMock;
    }

    @Test
    void testReadRemoteDataSkippingFilteredTables() throws SQLException {
        final DatabaseMetaData remoteMetadataMock = mockSupportingMetadata(true);
        setupConnectionMetadata(remoteMetadataMock);
        final Map<String, String> rawProperties = new HashMap<>();
        rawProperties.put(AdapterProperties.TABLE_FILTER_PROPERTY, TABLE_B);
        mockTableB(remoteMetadataMock);
        mockGetTableCalls(remoteMetadataMock);
        final SchemaMetadata metadata = readMockedSchemaMetadataWithProperties(new AdapterProperties(rawProperties));
        final List<TableMetadata> tables = metadata.getTables();
        final TableMetadata tableAMetadata = tables.get(0);
        assertAll(() -> assertThat(tables, iterableWithSize(1)),
                () -> assertThat(tableAMetadata.getName(), equalTo(TABLE_B)));
    }

    @Test
    void testCreateSchemaAdapterNotes() throws SQLException {
        final DatabaseMetaData remoteMetadataMock = mockSupportingMetadata(false);
        setupConnectionMetadata(remoteMetadataMock);
        final RemoteMetadataReader reader = buildBaseRemoteMetadataReader();
        final SchemaAdapterNotes notes = reader.getSchemaAdapterNotes();
        assertAll(() -> assertThat(notes.getCatalogSeparator(), equalTo(CATALOG_SEPARATOR)),
                () -> assertThat(notes.getIdentifierQuoteString(), equalTo(IDENTIFIER_QUOTE_STRING)),
                () -> assertThat(notes.storesLowerCaseIdentifiers(), equalTo(true)),
                () -> assertThat(notes.storesUpperCaseIdentifiers(), equalTo(true)),
                () -> assertThat(notes.storesMixedCaseIdentifiers(), equalTo(true)),
                () -> assertThat(notes.supportsMixedCaseIdentifiers(), equalTo(true)),
                () -> assertThat(notes.storesLowerCaseQuotedIdentifiers(), equalTo(true)),
                () -> assertThat(notes.storesUpperCaseQuotedIdentifiers(), equalTo(true)),
                () -> assertThat(notes.storesMixedCaseQuotedIdentifiers(), equalTo(true)),
                () -> assertThat(notes.supportsMixedCaseQuotedIdentifiers(), equalTo(true)),
                () -> assertThat(notes.areNullsSortedAtStart(), equalTo(true)),
                () -> assertThat(notes.areNullsSortedAtEnd(), equalTo(true)),
                () -> assertThat(notes.areNullsSortedHigh(), equalTo(true)),
                () -> assertThat(notes.areNullsSortedLow(), equalTo(true)));
    }

    @Test
    void testReadRemoteMetadataWithAdapterNotes() throws RemoteMetadataReaderException, SQLException {
        final DatabaseMetaData remoteMetadataMock = mockSupportingMetadata(true);
        setupConnectionMetadata(remoteMetadataMock);
        final ResultSet tablesMock = Mockito.mock(ResultSet.class);
        mockTableCount(tablesMock, 1);
        mockTableName(tablesMock, TABLE_A);
        mockGetAllTables(remoteMetadataMock, tablesMock);
        mockTableA(remoteMetadataMock);
        final SchemaMetadata metadata = readMockedSchemaMetadata();
        final List<TableMetadata> tables = metadata.getTables();
        final TableMetadata tableAMetadata = tables.get(0);
        final List<ColumnMetadata> columnsAMetadata = tableAMetadata.getColumns();
        assertAll(() -> assertThat(metadata.getAdapterNotes(), startsWith("{\"catalogSeparator\":")),
                () -> assertThat(tables, iterableWithSize(1)),
                () -> assertThat(tableAMetadata.getName(), equalTo(TABLE_A)),
                () -> assertThat(columnsAMetadata, iterableWithSize(2)));
    }

    @Test
    void testGetCatalogNameFilterDefaultsToAny() {
        final RemoteMetadataReader reader = buildBaseRemoteMetadataReader();
        assertThat(reader.getCatalogNameFilter(), equalTo(RemoteMetadataReaderConstants.ANY_CATALOG));
    }

    @Test
    void testGetCatalogNameFilter() {
        final Map<String, String> rawProperties = new HashMap<>();
        final String expectedCatalog = "FOO";
        rawProperties.put(AdapterProperties.CATALOG_NAME_PROPERTY, expectedCatalog);
        final RemoteMetadataReader reader = new BaseRemoteMetadataReader(null, new AdapterProperties(rawProperties),
                exaMetadataMock);
        assertThat(reader.getCatalogNameFilter(), equalTo(expectedCatalog));
    }

    @Test
    void testGetSchemaNameFilterDefaultsToAny() {
        final RemoteMetadataReader reader = buildBaseRemoteMetadataReader();
        assertThat(reader.getSchemaNameFilter(), equalTo(RemoteMetadataReaderConstants.ANY_SCHEMA));
    }

    @Test
    void testGetSchemaNameFilter() {
        final Map<String, String> rawProperties = new HashMap<>();
        final String expectedSchema = "BAR";
        rawProperties.put(AdapterProperties.SCHEMA_NAME_PROPERTY, expectedSchema);
        final RemoteMetadataReader reader = new BaseRemoteMetadataReader(null, new AdapterProperties(rawProperties),
                exaMetadataMock);
        assertThat(reader.getSchemaNameFilter(), equalTo(expectedSchema));
    }

    // Don't mix this test up with the one for filtered tables. In the refresh request users can limit the tables they
    // want refreshed. This is a different mechanism that coexists with the table filter via property. Both have to
    // work together.
    @Test
    void testReadRemoteDataSkippingForSelectedTablesOnly() throws SQLException {
        final DatabaseMetaData remoteMetadataMock = mockSupportingMetadata(true);
        setupConnectionMetadata(remoteMetadataMock);
        mockGetTableCalls(remoteMetadataMock);
        mockTableA(remoteMetadataMock);
        final RemoteMetadataReader reader = buildBaseRemoteMetadataReader();
        final SchemaMetadata metadata = reader.readRemoteSchemaMetadata(List.of(TABLE_A));
        final List<TableMetadata> tables = metadata.getTables();
        final TableMetadata tableAMetadata = tables.get(0);
        assertAll(() -> assertThat(tables, iterableWithSize(1)),
                () -> assertThat(tableAMetadata.getName(), equalTo(TABLE_A)));
    }

    @Test
    void testCreateIdentifierConverter() {
        final AdapterProperties emptyProperties = AdapterProperties.emptyProperties();
        final BaseRemoteMetadataReader reader = new BaseRemoteMetadataReader(null, emptyProperties, exaMetadataMock);
        reader.createIdentifierConverter();
        assertAll(
                () -> assertThat(reader.getIdentifierConverter().getUnquotedIdentifierHandling(),
                        equalTo(IdentifierCaseHandling.INTERPRET_AS_UPPER)),
                () -> assertThat(reader.getIdentifierConverter().getQuotedIdentifierHandling(),
                        equalTo(IdentifierCaseHandling.INTERPRET_CASE_SENSITIVE)));
    }

    @Test
    void testGetSupportedTableTypes() {
        assertThat(buildBaseRemoteMetadataReader()
                        .getSupportedTableTypes(),
                containsInAnyOrder("TABLE", "VIEW", "SYSTEM TABLE"));
    }

    @Test
    void testEscapeSchemaName() throws SQLException {
        verifyEscapeSchemaOrCatalog(null, escapeSqlWildCards("THE_SCHEMA"), Map.of("SCHEMA_NAME", "THE_SCHEMA"));
    }

    @Test
    void testEscapeCatalogName() throws SQLException {
        verifyEscapeSchemaOrCatalog("THE_CATALOG", null, Map.of("CATALOG_NAME", "THE_CATALOG"));
    }

    private void setupConnectionMetadata(final DatabaseMetaData remoteMetadata) throws SQLException {
        when(connectionMock.getMetaData()).thenReturn(remoteMetadata);
    }

    private BaseRemoteMetadataReader buildBaseRemoteMetadataReader() {
        return buildBaseRemoteMetadataReader(AdapterProperties.emptyProperties());
    }

    private BaseRemoteMetadataReader buildBaseRemoteMetadataReader(final AdapterProperties properties) {
        return new BaseRemoteMetadataReader(connectionMock, properties, exaMetadataMock);
    }

    void verifyEscapeSchemaOrCatalog(final String cat, final String schema, final Map<String, String> properties)
            throws SQLException {
        final DatabaseMetaData metadataMock = mock(DatabaseMetaData.class);
        when(metadataMock.getSearchStringEscape()).thenReturn(ESCAPE_STRING);
        final Connection connection = mock(Connection.class);
        when(connection.getMetaData()).thenReturn(metadataMock);
        String tableName = escapeSqlWildCards("THE_TABLE");
        when(metadataMock.getColumns(cat, schema, tableName, "%"))
                .thenThrow(new SpecialException());
        final BaseColumnMetadataReader testee = new BaseColumnMetadataReader(connection,
                new AdapterProperties(properties), exaMetadataMock, null);
        assertThrows(SpecialException.class, () -> testee.mapColumns("THE_TABLE"));
    }

    static class SpecialException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}