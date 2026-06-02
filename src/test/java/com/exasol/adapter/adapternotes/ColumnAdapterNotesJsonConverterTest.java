package com.exasol.adapter.adapternotes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
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
    void testConvertToJsonWithTypeName() throws JSONException {
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
    void testConvertToJsonWithoutTypeName() throws JSONException {
        final int expectedType = Types.DATE;
        final ColumnAdapterNotes adapterNotes = ColumnAdapterNotes.builder()
                .jdbcDataType(expectedType)
                .build();
        JSONAssert.assertEquals("{"
                + "\"" + ColumnAdapterNotesJsonConverter.JDBC_DATA_TYPE + "\":" + expectedType
                + "}", this.converter.convertToJson(adapterNotes), false);
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotesWithTypeName() throws AdapterException {
        final int expectedType = Types.VARCHAR;
        final String expectedTypeName = "ANOTHERTYPE";
        final String adapterNotesAsJson = "{" //
                + "\"" + ColumnAdapterNotesJsonConverter.JDBC_DATA_TYPE + "\":" + expectedType + ","
                + "\"" + ColumnAdapterNotesJsonConverter.TYPE_NAME + "\":\"" + expectedTypeName + "\""
                + "}";
        final ColumnAdapterNotes expectedAdapterNotes = ColumnAdapterNotes.builder() //
                .jdbcDataType(expectedType) //
                .typeName(expectedTypeName) //
                .build();
        final ColumnAdapterNotes actualAdapterNotes = this.converter.convertFromJsonToColumnAdapterNotes(
                adapterNotesAsJson, "C1");
        assertAll( //
                () -> assertThat(actualAdapterNotes, equalTo(expectedAdapterNotes)),
                () -> assertThat(actualAdapterNotes.getTypeName(), equalTo(expectedTypeName)));
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotesWithoutTypeName() throws AdapterException {
        final int expectedType = Types.INTEGER;
        final String adapterNotesAsJson = "{"
                + "\"" + ColumnAdapterNotesJsonConverter.JDBC_DATA_TYPE + "\":" + expectedType
                + "}";
        final ColumnAdapterNotes actualAdapterNotes = this.converter.convertFromJsonToColumnAdapterNotes(
                adapterNotesAsJson, "C1");
        assertAll(
                () -> assertThat(actualAdapterNotes.getJdbcDataType(), equalTo(expectedType)),
                () -> assertThat(actualAdapterNotes.getTypeName(), nullValue()));
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotesWithNullTypeName() throws AdapterException {
        final int expectedType = Types.INTEGER;
        final String adapterNotesAsJson = "{"
                + "\"" + ColumnAdapterNotesJsonConverter.JDBC_DATA_TYPE + "\":" + expectedType + ","
                + "\"" + ColumnAdapterNotesJsonConverter.TYPE_NAME + "\":null"
                + "}";
        final ColumnAdapterNotes actualAdapterNotes = this.converter.convertFromJsonToColumnAdapterNotes(
                adapterNotesAsJson, "C1");
        assertAll(
                () -> assertThat(actualAdapterNotes.getJdbcDataType(), equalTo(expectedType)),
                () -> assertThat(actualAdapterNotes.getTypeName(), nullValue()));
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotesThrowsExceptionWithMissingJdbcDataType() {
        final String adapterNotesAsJson = "{"
                + "\"" + ColumnAdapterNotesJsonConverter.TYPE_NAME + "\":\"ANOTHERTYPE\""
                + "}";
        final AdapterException exception = assertThrows(AdapterException.class,
                () -> this.converter.convertFromJsonToColumnAdapterNotes(adapterNotesAsJson, "col"));
        assertThat(exception.getMessage(), equalTo(
                "E-VSCJDBC-48: The column adapter notes of column 'col' are missing mandatory field 'jdbcDataType' or it is not a number. Please refresh the virtual schema."));
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotesThrowsExceptionWithNonNumericJdbcDataType() {
        final String adapterNotesAsJson = "{"
                + "\"" + ColumnAdapterNotesJsonConverter.JDBC_DATA_TYPE + "\":\"" + Types.INTEGER + "\""
                + "}";
        final AdapterException exception = assertThrows(AdapterException.class,
                () -> this.converter.convertFromJsonToColumnAdapterNotes(adapterNotesAsJson, "col"));
        assertThat(exception.getMessage(), equalTo(
                "E-VSCJDBC-48: The column adapter notes of column 'col' are missing mandatory field 'jdbcDataType' or it is not a number. Please refresh the virtual schema."));
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotesThrowsExceptionWithNonStringTypeName() {
        final String adapterNotesAsJson = "{"
                + "\"" + ColumnAdapterNotesJsonConverter.JDBC_DATA_TYPE + "\":" + Types.INTEGER + ","
                + "\"" + ColumnAdapterNotesJsonConverter.TYPE_NAME + "\":" + Types.VARCHAR
                + "}";
        final AdapterException exception = assertThrows(AdapterException.class,
                () -> this.converter.convertFromJsonToColumnAdapterNotes(adapterNotesAsJson, "col"));
        assertThat(exception.getMessage(), equalTo(
                "E-VSCJDBC-50: Optional field 'typeName' in column adapter notes of column 'col' must be a string. Please refresh the virtual schema."));
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotesThrowsExceptionWhenAdapterNotesAreNull() {
        final AdapterException exception = assertThrows(AdapterException.class,
                () -> this.converter.convertFromJsonToColumnAdapterNotes(null, ""));
        assertThat(exception.getMessage(), equalTo(
                "E-VSCJDBC-3: Adapter notes for column \"\" are empty or NULL. Please refresh the virtual schema."));
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotesThrowsExceptionWithEmptyAdapterNotes() {
        final AdapterException exception = assertThrows(AdapterException.class,
                () -> this.converter.convertFromJsonToColumnAdapterNotes("", ""));
        assertThat(exception.getMessage(), equalTo(
                "E-VSCJDBC-3: Adapter notes for column \"\" are empty or NULL. Please refresh the virtual schema."));
    }

    @Test
    void testConvertFromJsonToColumnAdapterNotesThrowsExceptionWithWrongAdapterNotes() {
        final AdapterException exception = assertThrows(AdapterException.class,
                () -> this.converter.convertFromJsonToColumnAdapterNotes("testNotes", "col"));
        assertThat(exception.getMessage(), equalTo(
                "E-VSCJDBC-4: Could not parse the column adapter notes of column 'col'. Error: 'Unexpected char 101 at (line no=1, column no=2, offset=1), expecting 'r'' Please refresh the virtual schema."));
    }
}
