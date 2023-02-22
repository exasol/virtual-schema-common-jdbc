package com.exasol.adapter.jdbc;

import static com.exasol.adapter.jdbc.BaseTableMetadataReader.NAME_COLUMN;
import static com.exasol.adapter.jdbc.TableMetadataMockUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.BaseIdentifierConverter;
import com.exasol.adapter.metadata.*;
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
        final ResultSet resultSetMock = Mockito.mock(ResultSet.class);
        // TODO: limit number of returned tables, validate that changing the MAX_TABLE_COUNT property fixes problem
        when(resultSetMock.next()).thenReturn(true);
        when(resultSetMock.getString(NAME_COLUMN)).thenReturn("table");
        when(this.columnMetadataReaderMock.mapColumns("table"))
                .thenReturn(List.of(ColumnMetadata.builder().name("column").type(DataType.createBool()).build()));
        final TableMetadataReader metadataReader = createDefaultTableMetadataReader();
        final RemoteMetadataReaderException exception = assertThrows(RemoteMetadataReaderException.class,
                () -> metadataReader.mapTables(resultSetMock, Collections.emptyList()));
        assertAll(
                () -> assertThat(exception.getMessage(), containsString("E-VSCJDBC-42")),
                () -> assertThat(exception.getMessage(), containsString("1000"))
        );
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
}
