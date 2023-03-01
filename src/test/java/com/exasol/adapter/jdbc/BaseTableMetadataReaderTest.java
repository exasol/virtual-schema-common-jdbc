package com.exasol.adapter.jdbc;

import static com.exasol.adapter.jdbc.BaseTableMetadataReader.NAME_COLUMN;
import static com.exasol.adapter.jdbc.TableMetadataMockUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.BaseIdentifierConverter;
import com.exasol.adapter.metadata.*;
import com.exasol.adapter.properties.TableCountLimit;
import com.exasol.logging.CapturingLogHandler;

@ExtendWith(MockitoExtension.class)
class BaseTableMetadataReaderTest {
    @Mock
    private Connection connectionMock;
    @Mock
    private DatabaseMetaData remoteMetadataMock;
    @Mock
    private ResultSet tablesMock;
    @Mock
    private ColumnMetadataReader columnMetadataReaderMock;

    @Test
    void testIsTableIncludedByMapping() {
        assertThat(createDefaultTableMetadataReader().isTableIncludedByMapping("any name"), equalTo(true));
    }

    @Test
    void testMapTables() throws SQLException {
        mockTableCount(this.tablesMock, 2);
        mockTableName(this.tablesMock, TABLE_A, TABLE_B);
        mockTableComment(this.tablesMock, TABLE_A_COMMENT, TABLE_B_COMMENT);
        mockTableWithColumnsOfType(this.tablesMock, this.columnMetadataReaderMock, TABLE_A, DataType.createBool());
        mockTableWithColumnsOfType(this.tablesMock, this.columnMetadataReaderMock, TABLE_B, DataType.createDate());
        final List<TableMetadata> tables = createDefaultTableMetadataReader().mapTables(this.tablesMock,
                Collections.emptyList());
        final TableMetadata tableA = tables.get(0);
        final TableMetadata tableB = tables.get(1);
        assertAll(() -> assertThat(tables, iterableWithSize(2)), //
                () -> assertThat(tableA.getName(), equalTo(TABLE_A)),
                () -> assertThat(tableA.getComment(), equalTo(TABLE_A_COMMENT)),
                () -> assertThat(tableA.getAdapterNotes(),
                        equalTo(BaseTableMetadataReader.DEFAULT_TABLE_ADAPTER_NOTES)),
                () -> assertThat(tableA.getColumns().get(0).getName(), equalTo(COLUMN_A1)),
                () -> assertThat(tableB.getName(), equalTo(TABLE_B)),
                () -> assertThat(tableB.getComment(), equalTo(TABLE_B_COMMENT)),
                () -> assertThat(tableB.getAdapterNotes(),
                        equalTo(BaseTableMetadataReader.DEFAULT_TABLE_ADAPTER_NOTES)),
                () -> assertThat(tableB.getColumns().get(0).getName(), equalTo(COLUMN_B1)));
    }

    private TableMetadataReader createDefaultTableMetadataReader() {
        return createTableMetadataReaderWithProperties(AdapterProperties.emptyProperties());
    }

    private TableMetadataReader createTableMetadataReaderWithProperties(final AdapterProperties properties) {
        return new BaseTableMetadataReader(this.connectionMock, this.columnMetadataReaderMock, properties,
                BaseIdentifierConverter.createDefault());
    }

    protected void mockConnection() throws SQLException {
        when(this.connectionMock.getMetaData()).thenReturn(this.remoteMetadataMock);
    }

    @Test
    void testMapTablesIgnoresTablesThatHaveNoColumns() throws SQLException {
        mockTableCount(this.tablesMock, 2);
        mockTableName(this.tablesMock, TABLE_A, TABLE_B);
        mockTableComment(this.tablesMock, TABLE_A_COMMENT, TABLE_B_COMMENT);
        mockTableWithColumnsOfType(this.tablesMock, this.columnMetadataReaderMock, TABLE_A, DataType.createBool());
        final List<TableMetadata> tables = createDefaultTableMetadataReader().mapTables(this.tablesMock,
                Collections.emptyList());
        assertSingleTableByName(tables, TABLE_A);
    }

    private void assertSingleTableByName(final List<TableMetadata> tables, final String tableName) {
        assertAll(() -> assertThat(tables, iterableWithSize(1)), //
                () -> assertThat(tables.get(0).getName(), equalTo(tableName)));
    }

    @Test
    void testWarnIfTableScanResultsAreEmpty() throws SQLException {
        mockTableCount(this.tablesMock, 0);
        final CapturingLogHandler capturingLogHandler = new CapturingLogHandler();
        Logger.getLogger("com.exasol").addHandler(capturingLogHandler);
        createDefaultTableMetadataReader().mapTables(this.tablesMock, Collections.emptyList());
        assertThat(capturingLogHandler.getCapturedData(), containsString("Table scan did not find any tables."));
    }

    @Test
    void testValidateMappedTablesListSize() throws SQLException {
        final ResultSet resultSetMock = resultSetWithUnlimitedSize();
        when(this.columnMetadataReaderMock.mapColumns("table"))
                .thenReturn(List.of(ColumnMetadata.builder().name("column").type(DataType.createBool()).build()));
        final TableMetadataReader metadataReader = createDefaultTableMetadataReader();
        final List<String> noFilter = Collections.emptyList();
        final RemoteMetadataReaderException exception = assertThrows(RemoteMetadataReaderException.class,
                () -> metadataReader.mapTables(resultSetMock, noFilter));
        assertAll( //
                () -> assertThat(exception.getMessage(), containsString("E-VSCJDBC-42")), //
                () -> assertThat(exception.getMessage(), containsString("1000")) //
        );
    }

    // verify that the actual table limit is part of the error message
    @Test
    void testValidateMappedTablesListSizeWithProperty2000() throws SQLException {
        final ResultSet resultSetMock = Mockito.mock(ResultSet.class);
        when(resultSetMock.next()).thenReturn(true);
        when(resultSetMock.getString(NAME_COLUMN)).thenReturn("table");
        when(this.columnMetadataReaderMock.mapColumns("table"))
                .thenReturn(List.of(ColumnMetadata.builder().name("column").type(DataType.createBool()).build()));
        final TableMetadataReader metadataReader = createTableMetadataReaderWithProperties(
                new AdapterProperties(Map.of(TableCountLimit.MAXTABLES_PROPERTY, "2000")));
        final List<String> noFilter = Collections.emptyList();
        final RemoteMetadataReaderException exception = assertThrows(RemoteMetadataReaderException.class,
                () -> metadataReader.mapTables(resultSetMock, noFilter));
        assertAll(() -> assertThat(exception.getMessage(), containsString("E-VSCJDBC-42")),
                () -> assertThat(exception.getMessage(), containsString("2000")));
    }

    // verify that it does map 3000 tables when the parameter is set so
    @Test
    void testValidateMappedTablesListSizeWithProperty3000() throws SQLException {
        final ResultSet resultSetMock = resultSetWithSize(3000);
        when(this.columnMetadataReaderMock.mapColumns("table"))
                .thenReturn(List.of(ColumnMetadata.builder().name("column").type(DataType.createBool()).build()));
        final TableMetadataReader metadataReader = createTableMetadataReaderWithProperties(
                new AdapterProperties(Map.of(TableCountLimit.MAXTABLES_PROPERTY, "3000")));
        final List<TableMetadata> mappedTables = assertDoesNotThrow(
                () -> metadataReader.mapTables(resultSetMock, Collections.emptyList()));
        assertThat(mappedTables.size(), equalTo(3000));
    }

    @Test
    void testMapTablesWithFilteredTablesDefinedByUser() throws SQLException {
        mockSingleTableWithName(TABLE_A);
        final List<TableMetadata> tables = createDefaultTableMetadataReader().mapTables(this.tablesMock,
                List.of(TABLE_A));
        assertSingleTableByName(tables, TABLE_A);
    }

    private void mockSingleTableWithName(final String tableName) throws SQLException {
        mockTableCount(this.tablesMock, 1);
        mockTableName(this.tablesMock, tableName);
        mockTableWithColumnsOfType(this.tablesMock, this.columnMetadataReaderMock, tableName, DataType.createBool());
    }

    @Test
    void testMapTablesWithFilteredTablesDefinedByProperties() throws SQLException {
        mockSingleTableWithName(TABLE_A);
        final List<TableMetadata> tables = createTableMetadataReaderWithSingleFilteredTable(TABLE_A)
                .mapTables(this.tablesMock, Collections.emptyList());
        assertSingleTableByName(tables, TABLE_A);
    }

    private TableMetadataReader createTableMetadataReaderWithSingleFilteredTable(final String tableName) {
        final Map<String, String> properties = Map.of(AdapterProperties.TABLE_FILTER_PROPERTY, tableName);
        return createTableMetadataReaderWithProperties(new AdapterProperties(properties));
    }

    @Test
    void testMapTablesWithOneFilteredTableLeftOut() throws SQLException {
        mockTableCount(this.tablesMock, 2);
        mockTableName(this.tablesMock, TABLE_A, TABLE_B);
        mockTableWithColumnsOfType(this.tablesMock, this.columnMetadataReaderMock, TABLE_A, DataType.createBool());
        final List<TableMetadata> tables = createDefaultTableMetadataReader().mapTables(this.tablesMock,
                List.of(TABLE_A));
        assertSingleTableByName(tables, TABLE_A);
    }

    @Test
    void testMapTablesWithFilteredTablesDefinedByPropertiesleftOut() throws SQLException {
        mockTableCount(this.tablesMock, 2);
        mockTableName(this.tablesMock, TABLE_A, TABLE_B);
        final List<TableMetadata> tables = createTableMetadataReaderWithSingleFilteredTable(TABLE_B)
                .mapTables(this.tablesMock, List.of(TABLE_A));
        assertThat(tables, iterableWithSize(0));
    }

    /**
     * Limit number of returned tables to verify mapping the specified number of tables.
     *
     * @param size size of result set
     * @return result set mock
     * @throws SQLException in case of failure
     */
    private ResultSet resultSetWithSize(final int size) throws SQLException {
        final ResultSet resultSetMock = Mockito.mock(ResultSet.class);
        final AtomicInteger tableCount = new AtomicInteger(0);
        when(resultSetMock.next()).then( //
                (Answer<Boolean>) invocationOnMock -> tableCount.getAndIncrement() < size //
        );
        when(resultSetMock.getString(NAME_COLUMN)).thenReturn("table");
        return resultSetMock;
    }

    private ResultSet resultSetWithUnlimitedSize() throws SQLException {
        final ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(NAME_COLUMN)).thenReturn("table");
        return resultSet;
    }
}
