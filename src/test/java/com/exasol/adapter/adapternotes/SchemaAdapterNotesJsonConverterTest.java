package com.exasol.adapter.adapternotes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.exasol.adapter.AdapterException;

class SchemaAdapterNotesJsonConverterTest {
    private SchemaAdapterNotesJsonConverter converter;

    @BeforeEach
    void setUp() {
        this.converter = SchemaAdapterNotesJsonConverter.getInstance();
    }

    private static final String SERIALIZED_STRING = "{\"catalogSeparator\":\".\"," //
            + "\"identifierQuoteString\":\"\\\"\"," //
            + "\"storesLowerCaseIdentifiers\":false," //
            + "\"storesUpperCaseIdentifiers\":false," //
            + "\"storesMixedCaseIdentifiers\":false," //
            + "\"supportsMixedCaseIdentifiers\":false," //
            + "\"storesLowerCaseQuotedIdentifiers\":false," //
            + "\"storesUpperCaseQuotedIdentifiers\":false," //
            + "\"storesMixedCaseQuotedIdentifiers\":false," //
            + "\"supportsMixedCaseQuotedIdentifiers\":false," //
            + "\"areNullsSortedAtEnd\":false," //
            + "\"areNullsSortedAtStart\":false," //
            + "\"areNullsSortedHigh\":false," //
            + "\"areNullsSortedLow\":false}";

    @Test
    void testConvertToJsonWithDefaultValues() throws JSONException {
        JSONAssert.assertEquals(SERIALIZED_STRING, this.converter.convertToJson(SchemaAdapterNotes.builder().build()),
                false);
    }

    @Test
    void testConvertFromJsonToSchemaAdapterNotesWithDefaultValues() throws AdapterException {
        assertThat(this.converter.convertFromJsonToSchemaAdapterNotes(SERIALIZED_STRING, "test_name"),
                equalTo(SchemaAdapterNotes.builder().build()));
    }

    @Test
    void testConvertFromJsonToSchemaAdapterNotesThrowsExceptionWhenEmptyAdapterNotesAreBzkk() {
        assertThrows(AdapterException.class, () -> this.converter.convertFromJsonToSchemaAdapterNotes(null, ""));
    }

    @Test
    void testConvertFromJsonToSchemaAdapterNotesThrowsExceptionWithEmptyAdapterNotes() {
        assertThrows(AdapterException.class, () -> this.converter.convertFromJsonToSchemaAdapterNotes("", ""));
    }

    @Test
    void testconvertFromJsonToSchemaAdapterNotesThrowsExceptionWithWrongAdapterNotes() {
        assertThrows(AdapterException.class, () -> this.converter.convertFromJsonToSchemaAdapterNotes("testNotes", ""));
    }
}