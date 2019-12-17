package com.exasol.adapter.adapternotes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.exasol.adapter.AdapterException;

class ColumnAdapterNotesTest {
    private ColumnAdapterNotes columnAdapterNotes;

    @BeforeEach
    void setUp() {
        this.columnAdapterNotes = new ColumnAdapterNotes(8, "DOUBLE");
    }

    @Test
    void testGetJdbcDataType() {
        assertThat(columnAdapterNotes.getJdbcDataType(), equalTo(8));
    }

    @Test
    void testGetTypeName() {
        assertThat(columnAdapterNotes.getTypeName(), equalTo("DOUBLE"));
    }

    @Test
    void testSerialize() {
        assertThat(ColumnAdapterNotes.serialize(columnAdapterNotes),
                equalTo("{\"jdbcDataType\":8,\"typeName\":\"DOUBLE\"}"));
    }

    @Test
    void testDeserialize() throws AdapterException {
        assertThat(ColumnAdapterNotes.deserialize("{\"jdbcDataType\":8,\"typeName\":\"DOUBLE\"}", "C1"),
                equalTo(columnAdapterNotes));
    }

    @Test
    void testDeserializeEmptyAdapterNotes() {
        AdapterException exception = assertThrows(AdapterException.class,
                () -> ColumnAdapterNotes.deserialize(null, "C1"));
        assertThat(exception.getMessage(), containsString("The adapternotes field of column C1 is empty or null"));
    }
}