package com.exasol.adapter.dialects.stubdialect;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.exasol.adapter.adapternotes.SchemaAdapterNotes;
import com.exasol.adapter.jdbc.*;
import com.exasol.adapter.metadata.*;

public class StubMetadataReader implements RemoteMetadataReader {
    private final List<TableMetadata> allTables = createDummyTableList();

    private List<TableMetadata> createDummyTableList() {
        final TableMetadata table1 = new TableMetadata("T1", "",
                List.of(ColumnMetadata.builder().name("C1").type(DataType.createDouble()).build()), "");
        final TableMetadata table2 = new TableMetadata("T2", "",
                List.of(ColumnMetadata.builder().name("C1").type(DataType.createDate()).build()), "");
        final List<TableMetadata> tables = List.of(table1, table2);
        return tables;
    }

    @Override
    public SchemaMetadata readRemoteSchemaMetadata() {
        return new SchemaMetadata("", this.allTables);
    }

    @Override
    public SchemaMetadata readRemoteSchemaMetadata(final List<String> tables) {
        final List<TableMetadata> filteredTables = this.allTables //
                .stream() //
                .filter(tableMetadata -> tables.contains(tableMetadata.getName())) //
                .collect(Collectors.toList());
        return new SchemaMetadata("", filteredTables);
    }

    @Override
    public SchemaAdapterNotes getSchemaAdapterNotes() {
        return SchemaAdapterNotes.builder().build();
    }

    @Override
    public ColumnMetadataReader getColumnMetadataReader() {
        return null;
    }

    @Override
    public TableMetadataReader getTableMetadataReader() {
        return null;
    }

    @Override
    public Set<String> getSupportedTableTypes() {
        return null;
    }

    @Override
    public String getCatalogNameFilter() {
        return null;
    }

    @Override
    public String getSchemaNameFilter() {
        return null;
    }

}