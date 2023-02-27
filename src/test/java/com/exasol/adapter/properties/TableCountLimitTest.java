package com.exasol.adapter.properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.jdbc.RemoteMetadataReaderException;

class TableCountLimitTest {
    @ParameterizedTest
    @ValueSource(ints = { 1, 2 })
    void testValues(final int value) {
        verify(new TableCountLimit(value), value);
    }

    @ParameterizedTest
    @ValueSource(strings = { "1", "2" })
    void testFromProperties(final String value) {
        final Map<String, String> properties = Map.of(TableCountLimit.MAXTABLES_PROPERTY, value);
        final TableCountLimit testee = TableCountLimit.from(new AdapterProperties(properties));
        verify(testee, Integer.parseInt(value));
        verifyValidator(properties);
    }

    @Test
    void testFromEmptyProperties() {
        final Map<String, String> properties = Map.of();
        final TableCountLimit testee = TableCountLimit.from(new AdapterProperties(properties));
        verify(testee, 1000);
        verifyValidator(properties);
    }

    @ParameterizedTest
    @ValueSource(strings = { "-1", "0", "1.2", "2,3" })
    void testValidatorFailure(final String value) {
        final Map<String, String> properties = Map.of(TableCountLimit.MAXTABLES_PROPERTY, value);
        final PropertyValidator validator = TableCountLimit.getValidator();
        assertThrows(PropertyValidationException.class, () -> validator.validate(new AdapterProperties(properties)));
    }

    private void verifyValidator(final Map<String, String> properties) {
        final PropertyValidator validator = TableCountLimit.getValidator();
        assertDoesNotThrow(() -> validator.validate(new AdapterProperties(properties)));
    }

    private void verify(final TableCountLimit testee, final int expected) {
        assertDoesNotThrow(() -> testee.validateNumberOfTables(expected));
        assertThrows(RemoteMetadataReaderException.class, () -> testee.validateNumberOfTables(expected + 1));
    }
}
