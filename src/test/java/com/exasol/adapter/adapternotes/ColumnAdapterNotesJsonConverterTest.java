package com.exasol.adapter.adapternotes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Types;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.exasol.adapter.AdapterException;

class ColumnAdapterNotesJsonConverterTest {
    private ColumnAdapterNotesJsonConverter converter;

    @BeforeEach
    void beforeEach() {
        this.converter = ColumnAdapterNotesJsonConverter.getInstance();
    }

    @Test
    void testConvertToJson() throws JSONException {
        final int expectedType = Types.DATE;
        final String expectedTypeName = "THETYPE";
        final ColumnAdapterNotes adapterNotes = ColumnAdapterNotes.builder() //
                .jdbcDataType(expectedType) //
                .typeName(expectedTypeName) //
                .build();
        JSONAssert.assertEquals("{" //
                + "\"" + ColumnAdapterNotesJsonConverter.JDBC_DATA_TYPE + "\":" + expectedType + "," //
                + "\"" + ColumnAdapterNotesJsonConverter.TYPE_NAME + "\":\"" + expectedTypeName + "\"" //
                + "}", this.converter.convertToJson(adapterNotes), false);
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotes() throws AdapterException {
        final int expectedType = Types.VARCHAR;
        final String expectedTypeName = "ANOTHERTYPE";
        final String adapterNotesAsJson = "{" //
                + "\"" + ColumnAdapterNotesJsonConverter.JDBC_DATA_TYPE + "\":" + expectedType + ","//
                + "\"" + ColumnAdapterNotesJsonConverter.TYPE_NAME + "\":\"" + expectedTypeName + "\""//
                + "}";
        final ColumnAdapterNotes expectedAdapterNotes = ColumnAdapterNotes.builder() //
                .jdbcDataType(expectedType) //
                .typeName(expectedTypeName) //
                .build();
        assertThat(this.converter.convertFromJsonToColumnAdapterNotes(adapterNotesAsJson, "C1"),
                equalTo(expectedAdapterNotes));
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotesThrowsExceptionWhenAdapterNotesAreNull() {
        final AdapterException exception = assertThrows(AdapterException.class,
                () -> this.converter.convertFromJsonToColumnAdapterNotes(null, ""));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-3"));
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotesThrowsExceptionWithEmptyAdapterNotes() {
        final AdapterException exception = assertThrows(AdapterException.class,
                () -> this.converter.convertFromJsonToColumnAdapterNotes("", ""));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-3"));
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotesThrowsExceptionWithWrongAdapterNotes() {
        final AdapterException exception = assertThrows(AdapterException.class,
                () -> this.converter.convertFromJsonToColumnAdapterNotes("testNotes", ""));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-4"));
    }
}