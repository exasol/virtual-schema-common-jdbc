package com.exasol.adapter.adapternotes;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Types;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.exasol.adapter.AdapterException;

class ColumnAdapterNotesJsonConverterTest {
    private static final String JDBC_DATA_TYPE = "jdbcDataType";
    private ColumnAdapterNotesJsonConverter converter;

    @BeforeEach
    void beforeEach() {
        this.converter = ColumnAdapterNotesJsonConverter.getInstance();
    }

    @Test
    void testConvertToJson() throws JSONException {
        final int expectedType = Types.DATE;
        final ColumnAdapterNotes adapterNotes = new ColumnAdapterNotes(expectedType);
        JSONAssert.assertEquals("{\"" + JDBC_DATA_TYPE + "\":" + expectedType + "}",
                this.converter.convertToJson(adapterNotes), false);
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotes() throws AdapterException {
        final int expectedType = Types.VARCHAR;
        final String adapterNotesAsJson = "{\"" + JDBC_DATA_TYPE + "\":" + expectedType + "}";
        final ColumnAdapterNotes expectedAdapterNotes = new ColumnAdapterNotes(expectedType);
        assertThat(this.converter.convertFromJsonToColumnAdapterNotes(adapterNotesAsJson, "C1"),
                equalTo(expectedAdapterNotes));
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotesThrowsExceptionWhenAdapterNotesAreNull() {
        assertThrows(AdapterException.class, () -> this.converter.convertFromJsonToColumnAdapterNotes(null, ""));
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotesThrowsExceptionWithEmptyAdapterNotes() {
        assertThrows(AdapterException.class, () -> this.converter.convertFromJsonToColumnAdapterNotes("", ""));
    }

    @Test
    void testconvertFromJsonToColumnAdapterNotesThrowsExceptionWithWrongAdapterNotes() {
        assertThrows(AdapterException.class, () -> this.converter.convertFromJsonToColumnAdapterNotes("testNotes", ""));
    }
}