package com.exasol.adapter.properties;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.exasol.adapter.AdapterProperties;

class CastNumberToDecimalPropertyTest {

    static final String PROPERTY = "SOME_PROPERTY";

    @ParameterizedTest
    @ValueSource(strings = { "TRUE", "123", "1,,2", "1.3" })
    void testFailure(final String value) {
        final Exception exception = assertThrows(PropertyValidationException.class, () -> validate(value));
        assertThat(exception.getMessage(), containsString("E-VSCJDBC-19"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "123,456" })
    void testSuccess(final String value) {
        assertDoesNotThrow(() -> validate(value));
    }

    @Test
    void testEmpty() {
        assertDoesNotThrow(() -> validate(Map.of()));
    }

    private void validate(final String value) throws PropertyValidationException {
        validate(Map.of(PROPERTY, value));
    }

    private void validate(final Map<String, String> properties) throws PropertyValidationException {
        final AdapterProperties adapterProperties = new AdapterProperties(properties);
        CastNumberToDecimalProperty.validator(PROPERTY).validate(adapterProperties);
    }
}
